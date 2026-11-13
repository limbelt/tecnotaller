package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.Client
import com.example.data.model.OrderStatus
import com.example.data.model.PartCategory
import com.example.data.model.RepairOrder
import com.example.data.model.ServiceType
import com.example.data.model.SparePart
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class WorkshopRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createAndObserveClient_authenticatedOwner_succeeds() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = WorkshopRepository(firestore)
        val uniqueName = "Cliente ${UUID.randomUUID().toString().take(6)}"

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createClient(
                Client(
                    name = uniqueName,
                    phone = "5512345678",
                    address = "Calle Central 45",
                    email = "cliente@ejemplo.com"
                )
            )
        }
        assertTrue("createClient failed: ${createResult.exceptionOrNull()}", createResult.isSuccess)
        val clientId = createResult.getOrThrow()

        val clients = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeClients().first { list -> list.any { it.id == clientId } }
        }
        val created = clients.first { it.id == clientId }
        assertEquals(uniqueName, created.name)
    }

    @Test
    fun createPart_andAttachToOrder_atomicallyDeductsInventory() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = WorkshopRepository(firestore)

        val partId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createPart(
                SparePart(
                    sku = "BAT-${UUID.randomUUID().toString().take(4)}",
                    name = "Batería Galaxy A54",
                    category = PartCategory.BATERIA.name,
                    compatibility = "Galaxy A54",
                    stock = 4,
                    minStock = 1,
                    costPrice = 300.0,
                    salePrice = 650.0
                )
            ).getOrThrow()
        }

        val orderId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createOrder(
                RepairOrder(
                    orderCode = "ORD-${UUID.randomUUID().toString().take(5)}",
                    clientName = "Ana López",
                    clientPhone = "5544332211",
                    clientAddress = "Av. Norte 12",
                    serviceType = ServiceType.CELULAR.name,
                    deviceTitle = "Samsung Galaxy A54",
                    issueDescription = "No retiene carga",
                    status = OrderStatus.EN_DIAGNOSTICO.name,
                    laborCost = 350.0
                )
            ).getOrThrow()
        }

        val attachResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.attachPartToOrder(orderId = orderId, partId = partId, quantity = 2)
        }
        assertTrue("attachPartToOrder failed: ${attachResult.exceptionOrNull()}", attachResult.isSuccess)

        val updatedOrder = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getOrderById(orderId).getOrThrow()
        }
        assertNotNull(updatedOrder)
        assertEquals(1300.0, updatedOrder!!.partsTotal, 0.01)
        assertEquals(1, updatedOrder.usedParts.size)
        assertEquals(2, updatedOrder.usedParts.first().quantity)

        val parts = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeParts().first { list -> list.any { it.id == partId && it.stock == 2 } }
        }
        assertEquals(2, parts.first { it.id == partId }.stock)
    }

    @Test
    fun updateOrderStatus_terminalStateLocked_rejectsSubsequentUpdates() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = WorkshopRepository(firestore)

        val orderId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createOrder(
                RepairOrder(
                    orderCode = "ORD-${UUID.randomUUID().toString().take(5)}",
                    clientName = "Luis Torres",
                    clientPhone = "5588990011",
                    clientAddress = "Residencial Sur 8",
                    serviceType = ServiceType.CAMARAS_SEGURIDAD.name,
                    deviceTitle = "Kit 4 Cámaras Hikvision",
                    issueDescription = "Instalación exterior con DVR",
                    status = OrderStatus.LISTO.name,
                    laborCost = 1800.0
                )
            ).getOrThrow()
        }

        val deliverResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.updateOrderStatus(orderId, OrderStatus.ENTREGADO)
        }
        assertTrue(deliverResult.isSuccess)

        val postTerminalUpdate = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.updateOrderStatus(orderId, OrderStatus.EN_REPARACION)
        }
        assertTrue("Expected terminal lock rejection", postTerminalUpdate.isFailure)
        val firestoreException = postTerminalUpdate.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull(firestoreException)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
    }

    @Test
    fun crossUserAccess_bobCannotReadAliceOrders() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = WorkshopRepository(firestore)
        val orderId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createOrder(
                RepairOrder(
                    orderCode = "ORD-${UUID.randomUUID().toString().take(5)}",
                    clientName = "Cliente Privado",
                    clientPhone = "5500112233",
                    serviceType = ServiceType.COMPUTADORA.name,
                    deviceTitle = "MacBook Air M2",
                    issueDescription = "Limpieza y pasta térmica"
                )
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("users").document(aliceUid)
                    .collection("orders").document(orderId)
                    .get().await()
            }
            fail("Expected PERMISSION_DENIED for cross-user order read")
        } catch (e: Exception) {
            val fsError = e.unwrapFirestoreException()
            assertNotNull("Expected FirebaseFirestoreException, got $e", fsError)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsError?.code)
        }
    }

    @Test
    fun unauthenticatedAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        auth.signOut()

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                firestore.collection("users").document(aliceUid)
                    .collection("orders").snapshots().first()
            }
            fail("Expected PERMISSION_DENIED for unauthenticated listener")
        } catch (e: Exception) {
            val fsError = e.unwrapFirestoreException()
            assertNotNull("Expected FirebaseFirestoreException, got $e", fsError)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsError?.code)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice_taller@test.com"
        const val BOB_EMAIL = "bob_taller@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
