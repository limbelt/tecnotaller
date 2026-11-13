package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class ServiceType(val label: String, val shortLabel: String) {
    CELULAR("Celular", "Celulares"),
    COMPUTADORA("Computadora", "Computadoras"),
    CAMARAS_SEGURIDAD("Cámaras de Seguridad", "Cámaras CCTV");

    companion object {
        fun fromString(value: String?): ServiceType =
            entries.firstOrNull { it.name == value } ?: CELULAR
    }
}

enum class OrderStatus(val label: String, val stepIndex: Int, val isTerminal: Boolean) {
    RECIBIDO("Recibido", 0, false),
    EN_DIAGNOSTICO("En diagnóstico", 1, false),
    ESPERANDO_REPUESTO("Esperando repuesto", 2, false),
    EN_REPARACION("En reparación", 3, false),
    LISTO("Listo para entrega", 4, false),
    ENTREGADO("Entregado", 5, true),
    CANCELADO("Cancelado", -1, true);

    fun nextRecommended(): OrderStatus? = when (this) {
        RECIBIDO -> EN_DIAGNOSTICO
        EN_DIAGNOSTICO -> EN_REPARACION
        ESPERANDO_REPUESTO -> EN_REPARACION
        EN_REPARACION -> LISTO
        LISTO -> ENTREGADO
        ENTREGADO, CANCELADO -> null
    }

    companion object {
        val activeWorkflowSteps: List<OrderStatus> = listOf(
            RECIBIDO,
            EN_DIAGNOSTICO,
            ESPERANDO_REPUESTO,
            EN_REPARACION,
            LISTO,
            ENTREGADO
        )

        fun fromString(value: String?): OrderStatus =
            entries.firstOrNull { it.name == value } ?: RECIBIDO
    }
}

enum class PartCategory(val label: String) {
    PANTALLA("Pantallas / Displays"),
    BATERIA("Baterías"),
    CONECTOR("Pines / Conectores"),
    ALMACENAMIENTO_RAM("Discos / RAM"),
    CAMARA_CCTV("Cámaras / DVR / NVR"),
    CABLE_FUENTE("Cables / Fuentes"),
    OTRO("Otros repuestos");

    companion object {
        fun fromString(value: String?): PartCategory =
            entries.firstOrNull { it.name == value } ?: OTRO
    }
}

@IgnoreExtraProperties
data class Client(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val phone: String = "",
    val address: String = "",
    val email: String = "",
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SparePart(
    @DocumentId val id: String = "",
    val userId: String = "",
    val sku: String = "",
    val name: String = "",
    val category: String = PartCategory.OTRO.name,
    val compatibility: String = "",
    val stock: Int = 0,
    val minStock: Int = 2,
    val costPrice: Double = 0.0,
    val salePrice: Double = 0.0,
    val supplier: String = "",
    val location: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val categoryEnum: PartCategory
        get() = PartCategory.fromString(category)

    val isLowStock: Boolean
        get() = stock <= minStock
}

@IgnoreExtraProperties
data class UsedPartItem(
    val partId: String = "",
    val partName: String = "",
    val quantity: Int = 1,
    val unitPrice: Double = 0.0
) {
    val subtotal: Double
        get() = quantity * unitPrice

    fun toMap(): Map<String, Any> = mapOf(
        "partId" to partId,
        "partName" to partName,
        "quantity" to quantity,
        "unitPrice" to unitPrice
    )
}

@IgnoreExtraProperties
data class RepairOrder(
    @DocumentId val id: String = "",
    val userId: String = "",
    val orderCode: String = "",
    val clientId: String = "",
    val clientName: String = "",
    val clientPhone: String = "",
    val clientAddress: String = "",
    val serviceType: String = ServiceType.CELULAR.name,
    val deviceTitle: String = "",
    val deviceSerial: String = "",
    val issueDescription: String = "",
    val diagnosis: String = "",
    val status: String = OrderStatus.RECIBIDO.name,
    val laborCost: Double = 0.0,
    val partsTotal: Double = 0.0,
    val usedParts: List<UsedPartItem> = emptyList(),
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val serviceTypeEnum: ServiceType
        get() = ServiceType.fromString(serviceType)

    val statusEnum: OrderStatus
        get() = OrderStatus.fromString(status)

    val totalCost: Double
        get() = laborCost + partsTotal
}
