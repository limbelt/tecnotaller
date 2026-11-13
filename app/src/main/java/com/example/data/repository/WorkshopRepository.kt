package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.Client
import com.example.data.model.OrderStatus
import com.example.data.model.RepairOrder
import com.example.data.model.SparePart
import com.example.data.model.UsedPartItem
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.example.data.remote.unwrapFirestoreException
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class WorkshopRepository(
    private val db: FirebaseFirestore
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("El usuario debe iniciar sesión con Google para acceder a Firestore.")
    }

    private fun userRoot(uid: String) = db.collection("users").document(uid)

    // ==================== CLIENTS ====================

    fun observeClients(): Flow<List<Client>> = flow {
        val uid = requireUserId()
        val collectionRef = userRoot(uid).collection("clients")
        emitAll(
            collectionRef.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Client::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.name.lowercase() }
                }
                .catch { error ->
                    val firestoreError = error.unwrapFirestoreException()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, collectionRef.path)
                    }
                    throw error
                }
        )
    }

    suspend fun createClient(client: Client): Result<String> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = userRoot(uid).collection("clients").document()
        val payload = mapOf(
            "userId" to uid,
            "name" to client.name.trim(),
            "phone" to client.phone.trim(),
            "address" to client.address.trim(),
            "email" to client.email.trim(),
            "notes" to client.notes.trim(),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateClient(client: Client): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        require(client.id.isNotBlank()) { "El ID del cliente no puede estar vacío" }
        val docRef = userRoot(uid).collection("clients").document(client.id)
        val payload = mapOf(
            "name" to client.name.trim(),
            "phone" to client.phone.trim(),
            "address" to client.address.trim(),
            "email" to client.email.trim(),
            "notes" to client.notes.trim(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.update(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteClient(clientId: String): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = userRoot(uid).collection("clients").document(clientId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // ==================== SPARE PARTS INVENTORY ====================

    fun observeParts(): Flow<List<SparePart>> = flow {
        val uid = requireUserId()
        val collectionRef = userRoot(uid).collection("parts")
        emitAll(
            collectionRef.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        SparePart::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.name.lowercase() }
                }
                .catch { error ->
                    val firestoreError = error.unwrapFirestoreException()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, collectionRef.path)
                    }
                    throw error
                }
        )
    }

    suspend fun createPart(part: SparePart): Result<String> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = userRoot(uid).collection("parts").document()
        val payload = mapOf(
            "userId" to uid,
            "sku" to part.sku.trim(),
            "name" to part.name.trim(),
            "category" to part.category,
            "compatibility" to part.compatibility.trim(),
            "stock" to part.stock.coerceAtLeast(0),
            "minStock" to part.minStock.coerceAtLeast(0),
            "costPrice" to part.costPrice.coerceAtLeast(0.0),
            "salePrice" to part.salePrice.coerceAtLeast(0.0),
            "supplier" to part.supplier.trim(),
            "location" to part.location.trim(),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updatePart(part: SparePart): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        require(part.id.isNotBlank()) { "El ID del repuesto no puede estar vacío" }
        val docRef = userRoot(uid).collection("parts").document(part.id)
        val payload = mapOf(
            "sku" to part.sku.trim(),
            "name" to part.name.trim(),
            "category" to part.category,
            "compatibility" to part.compatibility.trim(),
            "stock" to part.stock.coerceAtLeast(0),
            "minStock" to part.minStock.coerceAtLeast(0),
            "costPrice" to part.costPrice.coerceAtLeast(0.0),
            "salePrice" to part.salePrice.coerceAtLeast(0.0),
            "supplier" to part.supplier.trim(),
            "location" to part.location.trim(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.update(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun adjustPartStock(partId: String, newStock: Int): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = userRoot(uid).collection("parts").document(partId)
        val payload = mapOf(
            "stock" to newStock.coerceAtLeast(0),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.update(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deletePart(partId: String): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = userRoot(uid).collection("parts").document(partId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // ==================== REPAIR ORDERS ====================

    fun observeOrders(): Flow<List<RepairOrder>> = flow {
        val uid = requireUserId()
        val collectionRef = userRoot(uid).collection("orders")
        emitAll(
            collectionRef.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        RepairOrder::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = error.unwrapFirestoreException()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, collectionRef.path)
                    }
                    throw error
                }
        )
    }

    suspend fun getOrderById(orderId: String): Result<RepairOrder?> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = userRoot(uid).collection("orders").document(orderId)
        return try {
            val snap = docRef.get().await()
            val order = snap.toObject(
                RepairOrder::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            Result.success(order)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun createOrder(order: RepairOrder): Result<String> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = userRoot(uid).collection("orders").document()
        val generatedCode = order.orderCode.ifBlank {
            "ORD-${(System.currentTimeMillis() % 100000).toString().padStart(5, '0')}"
        }
        val payload = mapOf(
            "userId" to uid,
            "orderCode" to generatedCode,
            "clientId" to order.clientId,
            "clientName" to order.clientName.trim(),
            "clientPhone" to order.clientPhone.trim(),
            "clientAddress" to order.clientAddress.trim(),
            "serviceType" to order.serviceType,
            "deviceTitle" to order.deviceTitle.trim(),
            "deviceSerial" to order.deviceSerial.trim(),
            "issueDescription" to order.issueDescription.trim(),
            "diagnosis" to order.diagnosis.trim(),
            "status" to order.status,
            "laborCost" to order.laborCost.coerceAtLeast(0.0),
            "partsTotal" to order.partsTotal.coerceAtLeast(0.0),
            "usedParts" to order.usedParts.map { it.toMap() },
            "notes" to order.notes.trim(),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateOrder(order: RepairOrder): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        require(order.id.isNotBlank()) { "El ID de la orden no puede estar vacío" }
        val docRef = userRoot(uid).collection("orders").document(order.id)
        val payload = mapOf(
            "clientId" to order.clientId,
            "clientName" to order.clientName.trim(),
            "clientPhone" to order.clientPhone.trim(),
            "clientAddress" to order.clientAddress.trim(),
            "serviceType" to order.serviceType,
            "deviceTitle" to order.deviceTitle.trim(),
            "deviceSerial" to order.deviceSerial.trim(),
            "issueDescription" to order.issueDescription.trim(),
            "diagnosis" to order.diagnosis.trim(),
            "status" to order.status,
            "laborCost" to order.laborCost.coerceAtLeast(0.0),
            "partsTotal" to order.partsTotal.coerceAtLeast(0.0),
            "usedParts" to order.usedParts.map { it.toMap() },
            "notes" to order.notes.trim(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.update(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateOrderStatus(orderId: String, newStatus: OrderStatus): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = userRoot(uid).collection("orders").document(orderId)
        val payload = mapOf(
            "status" to newStatus.name,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.update(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun attachPartToOrder(
        orderId: String,
        partId: String,
        quantity: Int
    ): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        if (quantity <= 0) return Result.failure(IllegalArgumentException("La cantidad debe ser mayor a 0"))

        val orderRef = userRoot(uid).collection("orders").document(orderId)
        val partRef = userRoot(uid).collection("parts").document(partId)

        return try {
            db.runTransaction { transaction ->
                val orderSnap = transaction.get(orderRef)
                val partSnap = transaction.get(partRef)

                val currentOrder = orderSnap.toObject(RepairOrder::class.java)
                    ?: throw IllegalStateException("La orden no existe")
                val currentPart = partSnap.toObject(SparePart::class.java)
                    ?: throw IllegalStateException("El repuesto no existe")

                if (currentOrder.statusEnum.isTerminal) {
                    throw IllegalStateException("La orden está cerrada y no permite agregar repuestos")
                }
                if (currentPart.stock < quantity) {
                    throw IllegalStateException("Stock insuficiente (${currentPart.stock} disponibles)")
                }
                if (currentOrder.usedParts.size >= 30 && currentOrder.usedParts.none { it.partId == partId }) {
                    throw IllegalStateException("Límite máximo de 30 repuestos por orden alcanzado")
                }

                val updatedPartsList = currentOrder.usedParts.toMutableList()
                val existingIndex = updatedPartsList.indexOfFirst { it.partId == partId }
                if (existingIndex >= 0) {
                    val existing = updatedPartsList[existingIndex]
                    updatedPartsList[existingIndex] = existing.copy(
                        quantity = existing.quantity + quantity,
                        unitPrice = currentPart.salePrice
                    )
                } else {
                    updatedPartsList.add(
                        UsedPartItem(
                            partId = partId,
                            partName = currentPart.name,
                            quantity = quantity,
                            unitPrice = currentPart.salePrice
                        )
                    )
                }

                val newPartsTotal = updatedPartsList.sumOf { it.subtotal }
                val newStock = currentPart.stock - quantity

                transaction.update(
                    partRef,
                    mapOf(
                        "stock" to newStock,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                transaction.update(
                    orderRef,
                    mapOf(
                        "usedParts" to updatedPartsList.map { it.toMap() },
                        "partsTotal" to newPartsTotal,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, orderRef.path)
            Result.failure(e)
        }
    }

    suspend fun removePartFromOrder(orderId: String, partId: String): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }

        val orderRef = userRoot(uid).collection("orders").document(orderId)
        val partRef = userRoot(uid).collection("parts").document(partId)

        return try {
            db.runTransaction { transaction ->
                val orderSnap = transaction.get(orderRef)
                val partSnap = transaction.get(partRef)

                val currentOrder = orderSnap.toObject(RepairOrder::class.java)
                    ?: throw IllegalStateException("La orden no existe")

                if (currentOrder.statusEnum.isTerminal) {
                    throw IllegalStateException("La orden está cerrada y no permite modificar repuestos")
                }

                val removedItem = currentOrder.usedParts.firstOrNull { it.partId == partId }
                    ?: return@runTransaction

                val updatedPartsList = currentOrder.usedParts.filterNot { it.partId == partId }
                val newPartsTotal = updatedPartsList.sumOf { it.subtotal }

                if (partSnap.exists()) {
                    val currentPart = partSnap.toObject(SparePart::class.java)
                    if (currentPart != null) {
                        transaction.update(
                            partRef,
                            mapOf(
                                "stock" to (currentPart.stock + removedItem.quantity),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                }

                transaction.update(
                    orderRef,
                    mapOf(
                        "usedParts" to updatedPartsList.map { it.toMap() },
                        "partsTotal" to newPartsTotal,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, orderRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteOrder(orderId: String): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = userRoot(uid).collection("orders").document(orderId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }
}
