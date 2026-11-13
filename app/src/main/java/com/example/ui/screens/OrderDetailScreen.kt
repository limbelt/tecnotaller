package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.OrderStatus
import com.example.data.model.RepairOrder
import com.example.data.model.SparePart
import com.example.ui.components.EmptyStateBanner
import com.example.ui.components.OrderStatusPill
import com.example.ui.components.ServiceTypeBadge
import com.example.ui.components.formatCurrency
import com.example.ui.components.formatTimestamp
import com.example.ui.components.orderStatusColor
import com.example.ui.theme.JetBrainsMonoFontFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
    order: RepairOrder?,
    availableParts: List<SparePart>,
    onBack: () -> Unit,
    onEditOrder: (String) -> Unit,
    onUpdateStatus: (String, OrderStatus) -> Unit,
    onAttachPart: (String, String, Int) -> Unit,
    onRemovePart: (String, String) -> Unit,
    onDeleteOrder: (String) -> Unit
) {
    val context = LocalContext.current
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var pendingTerminalStatus by remember { mutableStateOf<OrderStatus?>(null) }
    var showAddPartSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = order?.orderCode ?: "Detalle de orden",
                        style = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMonoFontFamily)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("order_detail_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                actions = {
                    if (order != null && !order.statusEnum.isTerminal) {
                        IconButton(
                            onClick = { onEditOrder(order.id) },
                            modifier = Modifier.testTag("order_detail_edit")
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = "Editar orden")
                        }
                    }
                    if (order != null) {
                        IconButton(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier.testTag("order_detail_delete")
                        ) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "Eliminar orden",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (order == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                EmptyStateBanner(
                    title = "Orden no encontrada",
                    subtitle = "Es posible que esta orden haya sido eliminada.",
                    showIllustration = false
                )
            }
            return@Scaffold
        }

        val isTerminal = order.statusEnum.isTerminal

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 840.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header card
                item(key = "header") {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ServiceTypeBadge(serviceType = order.serviceTypeEnum)
                                OrderStatusPill(status = order.statusEnum)
                            }
                            Text(
                                text = order.deviceTitle,
                                style = MaterialTheme.typography.headlineSmall
                            )
                            if (order.deviceSerial.isNotBlank()) {
                                Text(
                                    text = "Serie / IMEI: ${order.deviceSerial}",
                                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFontFamily),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "Ingresado: ${formatTimestamp(order.createdAt)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (isTerminal) {
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Lock,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                        Text(
                                            text = "Orden cerrada (${order.statusEnum.label}). No admite modificaciones.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Status timeline card
                item(key = "status_timeline") {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Seguimiento de estado",
                                style = MaterialTheme.typography.titleMedium
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                OrderStatus.activeWorkflowSteps.forEach { step ->
                                    val isCurrent = order.statusEnum == step
                                    val isPassed = order.statusEnum.stepIndex > step.stepIndex &&
                                        order.statusEnum != OrderStatus.CANCELADO
                                    val color = if (isCurrent || isPassed) {
                                        orderStatusColor(step)
                                    } else {
                                        MaterialTheme.colorScheme.outline
                                    }

                                    Surface(
                                        color = if (isCurrent) color.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable(enabled = !isTerminal && !isCurrent) {
                                                if (step.isTerminal) {
                                                    pendingTerminalStatus = step
                                                } else {
                                                    onUpdateStatus(order.id, step)
                                                }
                                            }
                                            .testTag("status_step_${step.name}")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(26.dp)
                                                    .background(
                                                        color = if (isCurrent || isPassed) color else color.copy(alpha = 0.25f),
                                                        shape = CircleShape
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isPassed || isCurrent) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Check,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onPrimary,
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = step.label,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isCurrent) color else MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (isCurrent) {
                                                Text(
                                                    text = "Actual",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = color
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            if (!isTerminal) {
                                OutlinedButton(
                                    onClick = { pendingTerminalStatus = OrderStatus.CANCELADO },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("cancel_order_button")
                                ) {
                                    Icon(Icons.Filled.Block, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Cancelar orden")
                                }
                            }
                        }
                    }
                }

                // Customer info card
                item(key = "customer_info") {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Datos del cliente",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Filled.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text(order.clientName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Filled.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text(
                                    text = order.clientPhone,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontFamily = JetBrainsMonoFontFamily)
                                )
                            }
                            if (order.clientAddress.isNotBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(Icons.Filled.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Text(order.clientAddress, style = MaterialTheme.typography.bodyMedium)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                FilledTonalButton(
                                    onClick = {
                                        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${order.clientPhone}"))
                                        runCatching { context.startActivity(dialIntent) }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Filled.Phone, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Llamar")
                                }
                                if (order.clientAddress.isNotBlank()) {
                                    FilledTonalButton(
                                        onClick = {
                                            val mapUri = Uri.parse("geo:0,0?q=${Uri.encode(order.clientAddress)}")
                                            val mapIntent = Intent(Intent.ACTION_VIEW, mapUri)
                                            runCatching { context.startActivity(mapIntent) }
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Filled.Map, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Ver mapa")
                                    }
                                }
                            }
                        }
                    }
                }

                // Technical report card
                item(key = "technical_details") {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Falla o alcance reportado", style = MaterialTheme.typography.titleMedium)
                            Text(order.issueDescription, style = MaterialTheme.typography.bodyLarge)

                            if (order.diagnosis.isNotBlank()) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                                Text("Diagnóstico técnico", style = MaterialTheme.typography.titleSmall)
                                Text(order.diagnosis, style = MaterialTheme.typography.bodyMedium)
                            }
                            if (order.notes.isNotBlank()) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                                Text("Observaciones", style = MaterialTheme.typography.titleSmall)
                                Text(order.notes, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }

                // Parts & Billing card
                item(key = "parts_and_billing") {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Repuestos utilizados", style = MaterialTheme.typography.titleMedium)
                                if (!isTerminal) {
                                    FilledTonalButton(
                                        onClick = { showAddPartSheet = true },
                                        modifier = Modifier.testTag("attach_part_button")
                                    ) {
                                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Agregar")
                                    }
                                }
                            }

                            if (order.usedParts.isEmpty()) {
                                Text(
                                    text = "No se han asignado repuestos del inventario a esta orden.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                order.usedParts.forEach { used ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(used.partName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                            Text(
                                                text = "${used.quantity} x ${formatCurrency(used.unitPrice)}",
                                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFontFamily),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = formatCurrency(used.subtotal),
                                                style = MaterialTheme.typography.bodyLarge.copy(fontFamily = JetBrainsMonoFontFamily),
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            if (!isTerminal) {
                                                IconButton(onClick = { onRemovePart(order.id, used.partId) }) {
                                                    Icon(
                                                        Icons.Filled.RemoveCircleOutline,
                                                        contentDescription = "Retirar repuesto",
                                                        tint = MaterialTheme.colorScheme.error
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Mano de obra", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = formatCurrency(order.laborCost),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMonoFontFamily)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Subtotal repuestos", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = formatCurrency(order.partsTotal),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMonoFontFamily)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Total de la orden", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = formatCurrency(order.totalCost),
                                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMonoFontFamily),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm && order != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Eliminar orden") },
            text = { Text("¿Seguro que deseas eliminar la orden ${order.orderCode}? Esta acción no se puede deshacer.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteOrder(order.id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (pendingTerminalStatus != null && order != null) {
        val target = pendingTerminalStatus!!
        AlertDialog(
            onDismissRequest = { pendingTerminalStatus = null },
            title = { Text("Cerrar orden como ${target.label}") },
            text = {
                Text("Al cambiar el estado a ${target.label}, la orden quedará cerrada y bloqueada contra futuras ediciones.")
            },
            confirmButton = {
                Button(onClick = {
                    pendingTerminalStatus = null
                    onUpdateStatus(order.id, target)
                }) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingTerminalStatus = null }) {
                    Text("Volver")
                }
            }
        )
    }

    if (showAddPartSheet && order != null) {
        AttachPartBottomSheet(
            parts = availableParts.filter { it.stock > 0 },
            onDismiss = { showAddPartSheet = false },
            onAttach = { partId, quantity ->
                showAddPartSheet = false
                onAttachPart(order.id, partId, quantity)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AttachPartBottomSheet(
    parts: List<SparePart>,
    onDismiss: () -> Unit,
    onAttach: (String, Int) -> Unit
) {
    var search by remember { mutableStateOf("") }
    var selectedPart by remember { mutableStateOf<SparePart?>(null) }
    var quantity by remember { mutableIntStateOf(1) }

    val filtered = remember(parts, search) {
        val q = search.trim().lowercase()
        if (q.isEmpty()) parts else parts.filter {
            it.name.lowercase().contains(q) ||
                it.sku.lowercase().contains(q) ||
                it.compatibility.lowercase().contains(q)
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Asignar repuesto del inventario", style = MaterialTheme.typography.titleLarge)

            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = { Text("Buscar repuesto por nombre o SKU…") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (filtered.isEmpty()) {
                Text(
                    text = "No hay repuestos con stock disponible que coincidan.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    filtered.take(6).forEach { part ->
                        val isSelected = selectedPart?.id == part.id
                        Surface(
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedPart = part
                                    quantity = 1
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(part.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                    Text(
                                        text = "Disponible: ${part.stock}  •  ${formatCurrency(part.salePrice)}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFontFamily)
                                    )
                                }
                                if (isSelected) {
                                    Icon(Icons.Filled.Check, contentDescription = "Seleccionado", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }

            selectedPart?.let { chosen ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Cantidad (máx. ${chosen.stock}):", style = MaterialTheme.typography.bodyLarge)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { if (quantity > 1) quantity-- },
                            enabled = quantity > 1
                        ) { Text("-") }
                        Text(
                            text = quantity.toString(),
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFontFamily)
                        )
                        OutlinedButton(
                            onClick = { if (quantity < chosen.stock) quantity++ },
                            enabled = quantity < chosen.stock
                        ) { Text("+") }
                    }
                }

                Button(
                    onClick = { onAttach(chosen.id, quantity) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Descontar $quantity y agregar (${formatCurrency(chosen.salePrice * quantity)})")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
