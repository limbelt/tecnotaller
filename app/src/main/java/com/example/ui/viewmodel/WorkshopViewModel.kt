package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Client
import com.example.data.model.OrderStatus
import com.example.data.model.RepairOrder
import com.example.data.model.SparePart
import com.example.data.remote.unwrapFirestoreException
import com.example.data.repository.WorkshopRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

class WorkshopViewModel(
    private val repository: WorkshopRepository,
    private val currentUserId: String
) : ViewModel() {

    val ordersState: StateFlow<UiState<List<RepairOrder>>> = repository.observeOrders()
        .map<List<RepairOrder>, UiState<List<RepairOrder>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = error.unwrapFirestoreException()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing orders for $currentUserId", rootError)
            emit(UiState.Error(rootError.localizedMessage ?: "No se pudieron cargar las órdenes"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val partsState: StateFlow<UiState<List<SparePart>>> = repository.observeParts()
        .map<List<SparePart>, UiState<List<SparePart>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = error.unwrapFirestoreException()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing parts for $currentUserId", rootError)
            emit(UiState.Error(rootError.localizedMessage ?: "No se pudo cargar el inventario"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val clientsState: StateFlow<UiState<List<Client>>> = repository.observeClients()
        .map<List<Client>, UiState<List<Client>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = error.unwrapFirestoreException()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing clients for $currentUserId", rootError)
            emit(UiState.Error(rootError.localizedMessage ?: "No se pudieron cargar los clientes"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    private val _isMutating = MutableStateFlow(false)
    val isMutating: StateFlow<Boolean> = _isMutating.asStateFlow()

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    fun clearFeedback() {
        _feedbackMessage.value = null
    }

    fun saveOrder(
        order: RepairOrder,
        saveClientToDirectory: Boolean = false,
        onComplete: (Boolean, String?) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            _isMutating.value = true
            try {
                var resolvedOrder = order
                if (saveClientToDirectory && order.clientId.isBlank() && order.clientName.isNotBlank()) {
                    val clientResult = repository.createClient(
                        Client(
                            name = order.clientName,
                            phone = order.clientPhone,
                            address = order.clientAddress
                        )
                    )
                    if (clientResult.isSuccess) {
                        resolvedOrder = order.copy(clientId = clientResult.getOrNull().orEmpty())
                    }
                }

                val result = if (resolvedOrder.id.isBlank()) {
                    repository.createOrder(resolvedOrder).map { it }
                } else {
                    repository.updateOrder(resolvedOrder).map { resolvedOrder.id }
                }

                if (result.isSuccess) {
                    _feedbackMessage.value = if (order.id.isBlank()) {
                        "Orden registrada correctamente"
                    } else {
                        "Orden actualizada"
                    }
                    onComplete(true, result.getOrNull())
                } else {
                    val msg = result.exceptionOrNull()?.localizedMessage ?: "Error al guardar la orden"
                    _feedbackMessage.value = msg
                    onComplete(false, null)
                }
            } finally {
                _isMutating.value = false
            }
        }
    }

    fun updateOrderStatus(orderId: String, newStatus: OrderStatus) {
        viewModelScope.launch {
            _isMutating.value = true
            try {
                val result = repository.updateOrderStatus(orderId, newStatus)
                _feedbackMessage.value = if (result.isSuccess) {
                    "Estado actualizado a ${newStatus.label}"
                } else {
                    result.exceptionOrNull()?.localizedMessage ?: "No se pudo actualizar el estado"
                }
            } finally {
                _isMutating.value = false
            }
        }
    }

    fun attachPartToOrder(orderId: String, partId: String, quantity: Int) {
        viewModelScope.launch {
            _isMutating.value = true
            try {
                val result = repository.attachPartToOrder(orderId, partId, quantity)
                _feedbackMessage.value = if (result.isSuccess) {
                    "Repuesto asignado y descontado del inventario"
                } else {
                    result.exceptionOrNull()?.localizedMessage ?: "No se pudo asignar el repuesto"
                }
            } finally {
                _isMutating.value = false
            }
        }
    }

    fun removePartFromOrder(orderId: String, partId: String) {
        viewModelScope.launch {
            _isMutating.value = true
            try {
                val result = repository.removePartFromOrder(orderId, partId)
                _feedbackMessage.value = if (result.isSuccess) {
                    "Repuesto retirado y devuelto al inventario"
                } else {
                    result.exceptionOrNull()?.localizedMessage ?: "No se pudo retirar el repuesto"
                }
            } finally {
                _isMutating.value = false
            }
        }
    }

    fun deleteOrder(orderId: String, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            _isMutating.value = true
            try {
                val result = repository.deleteOrder(orderId)
                if (result.isSuccess) {
                    _feedbackMessage.value = "Orden eliminada"
                    onDeleted()
                } else {
                    _feedbackMessage.value = result.exceptionOrNull()?.localizedMessage ?: "No se pudo eliminar la orden"
                }
            } finally {
                _isMutating.value = false
            }
        }
    }

    fun savePart(part: SparePart, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _isMutating.value = true
            try {
                val result = if (part.id.isBlank()) {
                    repository.createPart(part).map { Unit }
                } else {
                    repository.updatePart(part)
                }
                if (result.isSuccess) {
                    _feedbackMessage.value = if (part.id.isBlank()) {
                        "Repuesto agregado al inventario"
                    } else {
                        "Repuesto actualizado"
                    }
                    onComplete(true)
                } else {
                    _feedbackMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Error al guardar repuesto"
                    onComplete(false)
                }
            } finally {
                _isMutating.value = false
            }
        }
    }

    fun adjustPartStock(part: SparePart, delta: Int) {
        val nextStock = (part.stock + delta).coerceAtLeast(0)
        if (nextStock == part.stock) return
        viewModelScope.launch {
            val result = repository.adjustPartStock(part.id, nextStock)
            if (result.isFailure) {
                _feedbackMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Error al ajustar stock"
            }
        }
    }

    fun deletePart(partId: String) {
        viewModelScope.launch {
            _isMutating.value = true
            try {
                val result = repository.deletePart(partId)
                _feedbackMessage.value = if (result.isSuccess) {
                    "Repuesto eliminado"
                } else {
                    result.exceptionOrNull()?.localizedMessage ?: "No se pudo eliminar"
                }
            } finally {
                _isMutating.value = false
            }
        }
    }

    fun saveClient(client: Client, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _isMutating.value = true
            try {
                val result = if (client.id.isBlank()) {
                    repository.createClient(client).map { Unit }
                } else {
                    repository.updateClient(client)
                }
                if (result.isSuccess) {
                    _feedbackMessage.value = if (client.id.isBlank()) {
                        "Cliente registrado"
                    } else {
                        "Datos de cliente actualizados"
                    }
                    onComplete(true)
                } else {
                    _feedbackMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Error al guardar cliente"
                    onComplete(false)
                }
            } finally {
                _isMutating.value = false
            }
        }
    }

    fun deleteClient(clientId: String) {
        viewModelScope.launch {
            _isMutating.value = true
            try {
                val result = repository.deleteClient(clientId)
                _feedbackMessage.value = if (result.isSuccess) {
                    "Cliente eliminado"
                } else {
                    result.exceptionOrNull()?.localizedMessage ?: "No se pudo eliminar el cliente"
                }
            } finally {
                _isMutating.value = false
            }
        }
    }

    private companion object {
        const val TAG = "WorkshopVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
