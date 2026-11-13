package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.Client
import com.example.data.model.OrderStatus
import com.example.data.model.RepairOrder
import com.example.data.model.ServiceType
import com.example.ui.components.serviceTypeIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderFormScreen(
    existingOrder: RepairOrder?,
    preselectedClient: Client?,
    clients: List<Client>,
    isMutating: Boolean,
    onBack: () -> Unit,
    onSave: (RepairOrder, Boolean) -> Unit
) {
    val isEditing = existingOrder != null

    var clientId by remember(existingOrder, preselectedClient) {
        mutableStateOf(existingOrder?.clientId ?: preselectedClient?.id.orEmpty())
    }
    var clientName by remember(existingOrder, preselectedClient) {
        mutableStateOf(existingOrder?.clientName ?: preselectedClient?.name.orEmpty())
    }
    var clientPhone by remember(existingOrder, preselectedClient) {
        mutableStateOf(existingOrder?.clientPhone ?: preselectedClient?.phone.orEmpty())
    }
    var clientAddress by remember(existingOrder, preselectedClient) {
        mutableStateOf(existingOrder?.clientAddress ?: preselectedClient?.address.orEmpty())
    }
    var serviceType by remember(existingOrder) {
        mutableStateOf(existingOrder?.serviceTypeEnum ?: ServiceType.CELULAR)
    }
    var deviceTitle by remember(existingOrder) {
        mutableStateOf(existingOrder?.deviceTitle.orEmpty())
    }
    var deviceSerial by remember(existingOrder) {
        mutableStateOf(existingOrder?.deviceSerial.orEmpty())
    }
    var issueDescription by remember(existingOrder) {
        mutableStateOf(existingOrder?.issueDescription.orEmpty())
    }
    var diagnosis by remember(existingOrder) {
        mutableStateOf(existingOrder?.diagnosis.orEmpty())
    }
    var laborCostText by remember(existingOrder) {
        mutableStateOf(
            if (existingOrder != null && existingOrder.laborCost > 0.0) {
                existingOrder.laborCost.toString()
            } else {
                ""
            }
        )
    }
    var status by remember(existingOrder) {
        mutableStateOf(existingOrder?.statusEnum ?: OrderStatus.RECIBIDO)
    }
    var notes by remember(existingOrder) {
        mutableStateOf(existingOrder?.notes.orEmpty())
    }
    var saveNewClient by remember(existingOrder, preselectedClient) {
        mutableStateOf(existingOrder == null && preselectedClient == null)
    }
    var showClientPicker by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (isEditing) "Editar ${existingOrder!!.orderCode}" else "Nueva orden de servicio")
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cancelar")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 720.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Tipo de servicio", style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ServiceType.entries.forEach { type ->
                        FilterChip(
                            selected = serviceType == type,
                            onClick = { serviceType = type },
                            leadingIcon = {
                                Icon(
                                    imageVector = serviceTypeIcon(type),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            label = { Text(type.label) },
                            modifier = Modifier.testTag("service_type_${type.name}")
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Datos del cliente", style = MaterialTheme.typography.titleMedium)
                    if (clients.isNotEmpty()) {
                        OutlinedButton(onClick = { showClientPicker = true }) {
                            Icon(Icons.Filled.People, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Elegir cliente")
                        }
                    }
                }

                OutlinedTextField(
                    value = clientName,
                    onValueChange = {
                        clientName = it
                        if (clientId.isNotBlank()) clientId = ""
                    },
                    label = { Text("Nombre del cliente *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("order_client_name_input")
                )

                OutlinedTextField(
                    value = clientPhone,
                    onValueChange = { clientPhone = it },
                    label = { Text("Teléfono de contacto *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("order_client_phone_input")
                )

                OutlinedTextField(
                    value = clientAddress,
                    onValueChange = { clientAddress = it },
                    label = {
                        Text(
                            if (serviceType == ServiceType.CAMARAS_SEGURIDAD) {
                                "Dirección de instalación *"
                            } else {
                                "Dirección (opcional)"
                            }
                        )
                    },
                    singleLine = false,
                    maxLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("order_client_address_input")
                )

                if (!isEditing && clientId.isBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { saveNewClient = !saveNewClient }
                    ) {
                        Checkbox(
                            checked = saveNewClient,
                            onCheckedChange = { saveNewClient = it }
                        )
                        Text(
                            text = "Guardar cliente en el directorio",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Text(
                    text = if (serviceType == ServiceType.CAMARAS_SEGURIDAD) {
                        "Detalles del sistema CCTV"
                    } else {
                        "Equipo y diagnóstico"
                    },
                    style = MaterialTheme.typography.titleMedium
                )

                OutlinedTextField(
                    value = deviceTitle,
                    onValueChange = { deviceTitle = it },
                    label = {
                        Text(
                            when (serviceType) {
                                ServiceType.CELULAR -> "Marca y modelo (ej. iPhone 14 Pro) *"
                                ServiceType.COMPUTADORA -> "Equipo / modelo (ej. Dell Inspiron 15) *"
                                ServiceType.CAMARAS_SEGURIDAD -> "Sistema / kit (ej. 4 Cámaras Hikvision 2MP) *"
                            }
                        )
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("order_device_title_input")
                )

                OutlinedTextField(
                    value = deviceSerial,
                    onValueChange = { deviceSerial = it },
                    label = {
                        Text(
                            if (serviceType == ServiceType.CAMARAS_SEGURIDAD) {
                                "Modelo DVR/NVR o serie (opcional)"
                            } else {
                                "IMEI o número de serie (opcional)"
                            }
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = issueDescription,
                    onValueChange = { issueDescription = it },
                    label = {
                        Text(
                            if (serviceType == ServiceType.CAMARAS_SEGURIDAD) {
                                "Alcance de instalación / puntos de cámara *"
                            } else {
                                "Falla reportada *"
                            }
                        )
                    },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("order_issue_input")
                )

                OutlinedTextField(
                    value = diagnosis,
                    onValueChange = { diagnosis = it },
                    label = { Text("Diagnóstico técnico / trabajo realizado") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = laborCostText,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                            laborCostText = input
                        }
                    },
                    label = { Text("Costo de mano de obra ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("order_labor_cost_input")
                )

                Text("Estado actual", style = MaterialTheme.typography.titleSmall)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OrderStatus.entries.filter { !it.isTerminal || isEditing }.forEach { item ->
                        FilterChip(
                            selected = status == item,
                            onClick = { status = item },
                            label = { Text(item.label) }
                        )
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Observaciones / garantía") },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                if (validationError != null) {
                    Text(
                        text = validationError!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Button(
                    onClick = {
                        when {
                            clientName.isBlank() -> validationError = "Ingresa el nombre del cliente"
                            clientPhone.isBlank() -> validationError = "Ingresa el teléfono de contacto"
                            serviceType == ServiceType.CAMARAS_SEGURIDAD && clientAddress.isBlank() ->
                                validationError = "Ingresa la dirección para la instalación de cámaras"
                            deviceTitle.isBlank() -> validationError = "Ingresa el equipo o sistema"
                            issueDescription.isBlank() -> validationError = "Describe la falla o alcance del servicio"
                            else -> {
                                validationError = null
                                val labor = laborCostText.toDoubleOrNull() ?: 0.0
                                val orderToSave = (existingOrder ?: RepairOrder()).copy(
                                    clientId = clientId,
                                    clientName = clientName.trim(),
                                    clientPhone = clientPhone.trim(),
                                    clientAddress = clientAddress.trim(),
                                    serviceType = serviceType.name,
                                    deviceTitle = deviceTitle.trim(),
                                    deviceSerial = deviceSerial.trim(),
                                    issueDescription = issueDescription.trim(),
                                    diagnosis = diagnosis.trim(),
                                    status = status.name,
                                    laborCost = labor,
                                    notes = notes.trim()
                                )
                                onSave(orderToSave, saveNewClient)
                            }
                        }
                    },
                    enabled = !isMutating,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("save_order_button")
                ) {
                    Text(if (isEditing) "Guardar cambios" else "Registrar orden")
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showClientPicker) {
        ModalBottomSheet(onDismissRequest = { showClientPicker = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Seleccionar cliente", style = MaterialTheme.typography.titleLarge)
                clients.take(12).forEach { client ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                clientId = client.id
                                clientName = client.name
                                clientPhone = client.phone
                                clientAddress = client.address
                                saveNewClient = false
                                showClientPicker = false
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(client.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                Text(client.phone, style = MaterialTheme.typography.bodySmall)
                                if (client.address.isNotBlank()) {
                                    Text(client.address, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            if (client.id == clientId) {
                                Icon(Icons.Filled.Check, contentDescription = "Seleccionado", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
