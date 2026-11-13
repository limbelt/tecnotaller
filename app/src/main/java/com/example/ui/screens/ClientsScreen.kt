package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.Client
import com.example.data.model.RepairOrder
import com.example.ui.components.EmptyStateBanner
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.viewmodel.UiState

@Composable
fun ClientsScreen(
    clientsState: UiState<List<Client>>,
    orders: List<RepairOrder>,
    onSaveClient: (Client, (Boolean) -> Unit) -> Unit,
    onDeleteClient: (String) -> Unit,
    onCreateOrderForClient: (Client) -> Unit,
    showCreateSheet: Boolean,
    onDismissCreateSheet: () -> Unit,
    onOpenCreateSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var editingClient by remember { mutableStateOf<Client?>(null) }
    var deletingClient by remember { mutableStateOf<Client?>(null) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        when (clientsState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is UiState.Error -> {
                EmptyStateBanner(
                    title = "Error al cargar clientes",
                    subtitle = clientsState.message,
                    showIllustration = false,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            is UiState.Success -> {
                val allClients = clientsState.data
                val filteredClients by remember(allClients, searchQuery) {
                    derivedStateOf {
                        val q = searchQuery.trim().lowercase()
                        if (q.isEmpty()) allClients else allClients.filter {
                            it.name.lowercase().contains(q) ||
                                it.phone.lowercase().contains(q) ||
                                it.address.lowercase().contains(q) ||
                                it.email.lowercase().contains(q)
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 840.dp),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item(key = "clients_search") {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Buscar cliente por nombre, teléfono o dirección…") },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Buscar") },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Filled.Clear, contentDescription = "Limpiar")
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("clients_search_input")
                        )
                    }

                    if (filteredClients.isEmpty()) {
                        item(key = "empty_clients") {
                            EmptyStateBanner(
                                title = if (allClients.isEmpty()) {
                                    "Sin clientes registrados"
                                } else {
                                    "Sin coincidencias"
                                },
                                subtitle = if (allClients.isEmpty()) {
                                    "Guarda nombres, teléfonos y direcciones para agilizar futuras reparaciones e instalaciones."
                                } else {
                                    "No encontramos clientes con esa búsqueda."
                                },
                                actionLabel = if (allClients.isEmpty()) "Registrar primer cliente" else null,
                                onAction = if (allClients.isEmpty()) onOpenCreateSheet else null,
                                showIllustration = false
                            )
                        }
                    } else {
                        items(filteredClients, key = { it.id }) { client ->
                            val orderCount = remember(orders, client) {
                                orders.count {
                                    (it.clientId.isNotBlank() && it.clientId == client.id) ||
                                        it.clientPhone == client.phone
                                }
                            }
                            ClientCard(
                                client = client,
                                orderCount = orderCount,
                                onCall = {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${client.phone}"))
                                    runCatching { context.startActivity(intent) }
                                },
                                onNewOrder = { onCreateOrderForClient(client) },
                                onEdit = { editingClient = client },
                                onDelete = { deletingClient = client }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateSheet || editingClient != null) {
        ClientFormBottomSheet(
            existingClient = editingClient,
            onDismiss = {
                editingClient = null
                onDismissCreateSheet()
            },
            onSave = { client ->
                onSaveClient(client) { success ->
                    if (success) {
                        editingClient = null
                        onDismissCreateSheet()
                    }
                }
            }
        )
    }

    if (deletingClient != null) {
        val target = deletingClient!!
        AlertDialog(
            onDismissRequest = { deletingClient = null },
            title = { Text("Eliminar cliente") },
            text = { Text("¿Eliminar a ${target.name} del directorio? Sus órdenes existentes se mantendrán.") },
            confirmButton = {
                Button(
                    onClick = {
                        deletingClient = null
                        onDeleteClient(target.id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingClient = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun ClientCard(
    client: Client,
    orderCount: Int,
    onCall: () -> Unit,
    onNewOrder: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("client_card_${client.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = client.name.trim().firstOrNull()?.uppercase() ?: "C",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(client.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = client.phone,
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "$orderCount ord.",
                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (client.address.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(client.address, style = MaterialTheme.typography.bodySmall)
                }
            }
            if (client.email.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Email,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(client.email, style = MaterialTheme.typography.bodySmall)
                }
            }
            if (client.notes.isNotBlank()) {
                Text(
                    text = client.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = onCall,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Llamar")
                }
                FilledTonalButton(
                    onClick = onNewOrder,
                    modifier = Modifier.weight(1.2f)
                ) {
                    Icon(Icons.Filled.AddCircleOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Nueva orden")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Editar")
                }
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Eliminar")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClientFormBottomSheet(
    existingClient: Client?,
    onDismiss: () -> Unit,
    onSave: (Client) -> Unit
) {
    var name by remember(existingClient) { mutableStateOf(existingClient?.name.orEmpty()) }
    var phone by remember(existingClient) { mutableStateOf(existingClient?.phone.orEmpty()) }
    var address by remember(existingClient) { mutableStateOf(existingClient?.address.orEmpty()) }
    var email by remember(existingClient) { mutableStateOf(existingClient?.email.orEmpty()) }
    var notes by remember(existingClient) { mutableStateOf(existingClient?.notes.orEmpty()) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Filled.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    text = if (existingClient == null) "Nuevo cliente" else "Editar cliente",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre completo *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Teléfono *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Dirección") },
                maxLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Correo electrónico (opcional)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notas (referencias de domicilio, horario, etc.)") },
                maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            if (errorMsg != null) {
                Text(errorMsg!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = {
                    when {
                        name.isBlank() -> errorMsg = "Ingresa el nombre del cliente"
                        phone.isBlank() -> errorMsg = "Ingresa el número de teléfono"
                        else -> {
                            errorMsg = null
                            onSave(
                                (existingClient ?: Client()).copy(
                                    name = name.trim(),
                                    phone = phone.trim(),
                                    address = address.trim(),
                                    email = email.trim(),
                                    notes = notes.trim()
                                )
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(if (existingClient == null) "Guardar cliente" else "Actualizar cliente")
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
