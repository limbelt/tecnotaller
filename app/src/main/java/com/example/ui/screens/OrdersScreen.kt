package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.OrderStatus
import com.example.data.model.RepairOrder
import com.example.data.model.ServiceType
import com.example.ui.components.EmptyStateBanner
import com.example.ui.components.OrderStatusPill
import com.example.ui.components.ServiceTypeBadge
import com.example.ui.components.formatCurrency
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.StatusInProgressColor
import com.example.ui.theme.StatusReadyColor
import com.example.ui.theme.StatusWaitingPartColor
import com.example.ui.viewmodel.UiState

@Composable
fun OrdersScreen(
    ordersState: UiState<List<RepairOrder>>,
    onOrderClick: (String) -> Unit,
    onAdvanceStatus: (RepairOrder, OrderStatus) -> Unit,
    onCreateOrderClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedServiceFilter by remember { mutableStateOf<ServiceType?>(null) }
    var selectedStatusFilter by remember { mutableStateOf<OrderStatus?>(null) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        when (ordersState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is UiState.Error -> {
                EmptyStateBanner(
                    title = "Error al cargar órdenes",
                    subtitle = ordersState.message,
                    showIllustration = false,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            is UiState.Success -> {
                val allOrders = ordersState.data
                val filteredOrders by remember(allOrders, searchQuery, selectedServiceFilter, selectedStatusFilter) {
                    derivedStateOf {
                        allOrders.filter { order ->
                            val matchesService = selectedServiceFilter == null ||
                                order.serviceTypeEnum == selectedServiceFilter
                            val matchesStatus = selectedStatusFilter == null ||
                                order.statusEnum == selectedStatusFilter
                            val q = searchQuery.trim().lowercase()
                            val matchesSearch = q.isEmpty() ||
                                order.orderCode.lowercase().contains(q) ||
                                order.clientName.lowercase().contains(q) ||
                                order.clientPhone.lowercase().contains(q) ||
                                order.deviceTitle.lowercase().contains(q) ||
                                order.clientAddress.lowercase().contains(q)
                            matchesService && matchesStatus && matchesSearch
                        }
                    }
                }

                val activeCount = remember(allOrders) { allOrders.count { !it.statusEnum.isTerminal } }
                val readyCount = remember(allOrders) { allOrders.count { it.statusEnum == OrderStatus.LISTO } }
                val waitingPartsCount = remember(allOrders) { allOrders.count { it.statusEnum == OrderStatus.ESPERANDO_REPUESTO } }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 840.dp),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item(key = "summary_kpis") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OrderKpiCard(
                                label = "Activas",
                                value = activeCount.toString(),
                                accentColor = StatusInProgressColor,
                                modifier = Modifier.weight(1f)
                            )
                            OrderKpiCard(
                                label = "Esp. repuesto",
                                value = waitingPartsCount.toString(),
                                accentColor = StatusWaitingPartColor,
                                modifier = Modifier.weight(1f)
                            )
                            OrderKpiCard(
                                label = "Listas",
                                value = readyCount.toString(),
                                accentColor = StatusReadyColor,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item(key = "search_and_filters") {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Buscar por cliente, folio, equipo o dirección…") },
                                leadingIcon = {
                                    Icon(Icons.Filled.Search, contentDescription = "Buscar")
                                },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(Icons.Filled.Clear, contentDescription = "Limpiar búsqueda")
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("orders_search_input")
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = selectedServiceFilter == null,
                                    onClick = { selectedServiceFilter = null },
                                    label = { Text("Todos los servicios") }
                                )
                                ServiceType.entries.forEach { type ->
                                    FilterChip(
                                        selected = selectedServiceFilter == type,
                                        onClick = {
                                            selectedServiceFilter = if (selectedServiceFilter == type) null else type
                                        },
                                        label = { Text(type.shortLabel) }
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = selectedStatusFilter == null,
                                    onClick = { selectedStatusFilter = null },
                                    label = { Text("Todos los estados") }
                                )
                                OrderStatus.entries.forEach { status ->
                                    FilterChip(
                                        selected = selectedStatusFilter == status,
                                        onClick = {
                                            selectedStatusFilter = if (selectedStatusFilter == status) null else status
                                        },
                                        label = { Text(status.label) }
                                    )
                                }
                            }
                        }
                    }

                    if (filteredOrders.isEmpty()) {
                        item(key = "empty_orders") {
                            EmptyStateBanner(
                                title = if (allOrders.isEmpty()) {
                                    "Aún no hay órdenes registradas"
                                } else {
                                    "Sin resultados con los filtros actuales"
                                },
                                subtitle = if (allOrders.isEmpty()) {
                                    "Registra reparaciones de celulares, computadoras o instalaciones de cámaras de seguridad."
                                } else {
                                    "Prueba limpiando la búsqueda o seleccionando otro estado."
                                },
                                actionLabel = if (allOrders.isEmpty()) "Registrar primera orden" else null,
                                onAction = if (allOrders.isEmpty()) onCreateOrderClick else null,
                                showIllustration = allOrders.isEmpty()
                            )
                        }
                    } else {
                        items(filteredOrders, key = { it.id }) { order ->
                            OrderCard(
                                order = order,
                                onClick = { onOrderClick(order.id) },
                                onAdvanceStatus = onAdvanceStatus
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderKpiCard(
    label: String,
    value: String,
    accentColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                color = accentColor,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun OrderCard(
    order: RepairOrder,
    onClick: () -> Unit,
    onAdvanceStatus: (RepairOrder, OrderStatus) -> Unit
) {
    val nextStatus = order.statusEnum.nextRecommended()
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("order_card_${order.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = order.orderCode,
                        style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFontFamily),
                        color = MaterialTheme.colorScheme.primary
                    )
                    ServiceTypeBadge(serviceType = order.serviceTypeEnum)
                }
                OrderStatusPill(status = order.statusEnum)
            }

            Text(
                text = order.deviceTitle,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = order.issueDescription,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = order.clientName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f, fill = false),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Filled.Phone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = order.clientPhone,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFontFamily),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (order.clientAddress.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = order.clientAddress,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = formatCurrency(order.totalCost),
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (order.usedParts.isNotEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Build,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = "${order.usedParts.sumOf { it.quantity }} rep.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }

                if (nextStatus != null && !nextStatus.isTerminal) {
                    TextButton(
                        onClick = { onAdvanceStatus(order, nextStatus) },
                        modifier = Modifier.testTag("advance_status_${order.id}")
                    ) {
                        Text("Pasar a ${nextStatus.label}")
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
