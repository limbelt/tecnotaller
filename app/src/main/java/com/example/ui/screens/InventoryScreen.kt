package com.example.ui.screens

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalIconButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.PartCategory
import com.example.data.model.SparePart
import com.example.ui.components.EmptyStateBanner
import com.example.ui.components.PartCategoryBadge
import com.example.ui.components.formatCurrency
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.StatusCancelledColor
import com.example.ui.theme.StatusReadyColor
import com.example.ui.theme.StatusWaitingPartColor
import com.example.ui.viewmodel.UiState

@Composable
fun InventoryScreen(
    partsState: UiState<List<SparePart>>,
    onAdjustStock: (SparePart, Int) -> Unit,
    onSavePart: (SparePart, (Boolean) -> Unit) -> Unit,
    onDeletePart: (String) -> Unit,
    showCreateSheet: Boolean,
    onDismissCreateSheet: () -> Unit,
    onOpenCreateSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<PartCategory?>(null) }
    var onlyLowStock by remember { mutableStateOf(false) }
    var editingPart by remember { mutableStateOf<SparePart?>(null) }
    var deletingPart by remember { mutableStateOf<SparePart?>(null) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        when (partsState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is UiState.Error -> {
                EmptyStateBanner(
                    title = "Error al cargar inventario",
                    subtitle = partsState.message,
                    showIllustration = false,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            is UiState.Success -> {
                val allParts = partsState.data
                val filteredParts by remember(allParts, searchQuery, selectedCategory, onlyLowStock) {
                    derivedStateOf {
                        val q = searchQuery.trim().lowercase()
                        allParts.filter { part ->
                            val matchesCat = selectedCategory == null || part.categoryEnum == selectedCategory
                            val matchesLow = !onlyLowStock || part.isLowStock
                            val matchesQ = q.isEmpty() ||
                                part.name.lowercase().contains(q) ||
                                part.sku.lowercase().contains(q) ||
                                part.compatibility.lowercase().contains(q) ||
                                part.location.lowercase().contains(q)
                            matchesCat && matchesLow && matchesQ
                        }
                    }
                }

                val lowStockCount = remember(allParts) { allParts.count { it.isLowStock } }
                val totalUnits = remember(allParts) { allParts.sumOf { it.stock } }
                val totalValue = remember(allParts) { allParts.sumOf { it.stock * it.salePrice } }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 840.dp),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item(key = "inventory_kpis") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            InventoryKpiCard(
                                label = "Unidades",
                                value = totalUnits.toString(),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            InventoryKpiCard(
                                label = "Stock bajo",
                                value = lowStockCount.toString(),
                                color = if (lowStockCount > 0) StatusWaitingPartColor else StatusReadyColor,
                                modifier = Modifier.weight(1f)
                            )
                            InventoryKpiCard(
                                label = "Valor venta",
                                value = formatCurrency(totalValue),
                                color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.weight(1.3f)
                            )
                        }
                    }

                    item(key = "inventory_filters") {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Buscar por repuesto, SKU o modelo compatible…") },
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
                                    .testTag("inventory_search_input")
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = onlyLowStock,
                                    onClick = { onlyLowStock = !onlyLowStock },
                                    leadingIcon = {
                                        Icon(Icons.Filled.Warning, contentDescription = null, modifier = Modifier.size(16.dp))
                                    },
                                    label = { Text("Stock bajo ($lowStockCount)") }
                                )
                                FilterChip(
                                    selected = selectedCategory == null,
                                    onClick = { selectedCategory = null },
                                    label = { Text("Todas") }
                                )
                                PartCategory.entries.forEach { cat ->
                                    FilterChip(
                                        selected = selectedCategory == cat,
                                        onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                                        label = { Text(cat.label) }
                                    )
                                }
                            }
                        }
                    }

                    if (filteredParts.isEmpty()) {
                        item(key = "empty_inventory") {
                            EmptyStateBanner(
                                title = if (allParts.isEmpty()) {
                                    "Inventario vacío"
                                } else {
                                    "Sin repuestos que coincidan"
                                },
                                subtitle = if (allParts.isEmpty()) {
                                    "Agrega pantallas, baterías, discos SSD, cámaras CCTV o conectores para llevar control de stock."
                                } else {
                                    "Ajusta la búsqueda o desactiva los filtros activos."
                                },
                                actionLabel = if (allParts.isEmpty()) "Agregar primer repuesto" else null,
                                onAction = if (allParts.isEmpty()) onOpenCreateSheet else null,
                                showIllustration = false
                            )
                        }
                    } else {
                        items(filteredParts, key = { it.id }) { part ->
                            SparePartCard(
                                part = part,
                                onDecrement = { onAdjustStock(part, -1) },
                                onIncrement = { onAdjustStock(part, +1) },
                                onEdit = { editingPart = part },
                                onDelete = { deletingPart = part }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateSheet || editingPart != null) {
        PartFormBottomSheet(
            existingPart = editingPart,
            onDismiss = {
                editingPart = null
                onDismissCreateSheet()
            },
            onSave = { part ->
                onSavePart(part) { success ->
                    if (success) {
                        editingPart = null
                        onDismissCreateSheet()
                    }
                }
            }
        )
    }

    if (deletingPart != null) {
        val target = deletingPart!!
        AlertDialog(
            onDismissRequest = { deletingPart = null },
            title = { Text("Eliminar repuesto") },
            text = { Text("¿Eliminar \"${target.name}\" del inventario?") },
            confirmButton = {
                Button(
                    onClick = {
                        deletingPart = null
                        onDeletePart(target.id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingPart = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun InventoryKpiCard(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
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
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMonoFontFamily),
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SparePartCard(
    part: SparePart,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val stockColor = when {
        part.stock == 0 -> StatusCancelledColor
        part.isLowStock -> StatusWaitingPartColor
        else -> StatusReadyColor
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("part_card_${part.id}")
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
                PartCategoryBadge(category = part.categoryEnum)
                if (part.sku.isNotBlank()) {
                    Text(
                        text = part.sku,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = part.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (part.compatibility.isNotBlank() || part.location.isNotBlank()) {
                Text(
                    text = listOfNotNull(
                        part.compatibility.takeIf { it.isNotBlank() }?.let { "Compat.: $it" },
                        part.location.takeIf { it.isNotBlank() }?.let { "Ubic.: $it" }
                    ).joinToString("  •  "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Venta: ${formatCurrency(part.salePrice)}",
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Costo: ${formatCurrency(part.costPrice)}",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFontFamily),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalIconButton(
                        onClick = onDecrement,
                        enabled = part.stock > 0,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("part_dec_${part.id}")
                    ) {
                        Icon(Icons.Filled.Remove, contentDescription = "Disminuir stock")
                    }

                    Surface(
                        color = stockColor.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = part.stock.toString(),
                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                                fontWeight = FontWeight.Bold,
                                color = stockColor
                            )
                            Text(
                                text = if (part.isLowStock) "Bajo" else "Stock",
                                style = MaterialTheme.typography.labelSmall,
                                color = stockColor
                            )
                        }
                    }

                    FilledTonalIconButton(
                        onClick = onIncrement,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("part_inc_${part.id}")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Aumentar stock")
                    }
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
private fun PartFormBottomSheet(
    existingPart: SparePart?,
    onDismiss: () -> Unit,
    onSave: (SparePart) -> Unit
) {
    var name by remember(existingPart) { mutableStateOf(existingPart?.name.orEmpty()) }
    var sku by remember(existingPart) { mutableStateOf(existingPart?.sku.orEmpty()) }
    var category by remember(existingPart) { mutableStateOf(existingPart?.categoryEnum ?: PartCategory.PANTALLA) }
    var compatibility by remember(existingPart) { mutableStateOf(existingPart?.compatibility.orEmpty()) }
    var stockText by remember(existingPart) { mutableStateOf((existingPart?.stock ?: 1).toString()) }
    var minStockText by remember(existingPart) { mutableStateOf((existingPart?.minStock ?: 2).toString()) }
    var costText by remember(existingPart) {
        mutableStateOf(if (existingPart != null) existingPart.costPrice.toString() else "")
    }
    var saleText by remember(existingPart) {
        mutableStateOf(if (existingPart != null) existingPart.salePrice.toString() else "")
    }
    var supplier by remember(existingPart) { mutableStateOf(existingPart?.supplier.orEmpty()) }
    var location by remember(existingPart) { mutableStateOf(existingPart?.location.orEmpty()) }
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
                Icon(Icons.Filled.Inventory2, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    text = if (existingPart == null) "Nuevo repuesto" else "Editar repuesto",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre del repuesto *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = sku,
                onValueChange = { sku = it },
                label = { Text("SKU / Código (opcional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text("Categoría", style = MaterialTheme.typography.titleSmall)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PartCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = { category = cat },
                        label = { Text(cat.label) }
                    )
                }
            }

            OutlinedTextField(
                value = compatibility,
                onValueChange = { compatibility = it },
                label = { Text("Compatibilidad (ej. iPhone 13, DDR4, Hikvision)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = stockText,
                    onValueChange = { if (it.all(Char::isDigit)) stockText = it },
                    label = { Text("Stock actual *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = minStockText,
                    onValueChange = { if (it.all(Char::isDigit)) minStockText = it },
                    label = { Text("Stock mínimo") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = costText,
                    onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d{0,2}$"))) costText = it },
                    label = { Text("Costo ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = saleText,
                    onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d{0,2}$"))) saleText = it },
                    label = { Text("Precio venta ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = supplier,
                    onValueChange = { supplier = it },
                    label = { Text("Proveedor") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Ubicación") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            if (errorMsg != null) {
                Text(errorMsg!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = {
                    if (name.isBlank()) {
                        errorMsg = "El nombre del repuesto es obligatorio"
                        return@Button
                    }
                    errorMsg = null
                    val part = (existingPart ?: SparePart()).copy(
                        name = name.trim(),
                        sku = sku.trim(),
                        category = category.name,
                        compatibility = compatibility.trim(),
                        stock = stockText.toIntOrNull() ?: 0,
                        minStock = minStockText.toIntOrNull() ?: 2,
                        costPrice = costText.toDoubleOrNull() ?: 0.0,
                        salePrice = saleText.toDoubleOrNull() ?: 0.0,
                        supplier = supplier.trim(),
                        location = location.trim()
                    )
                    onSave(part)
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(if (existingPart == null) "Guardar repuesto" else "Actualizar repuesto")
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
