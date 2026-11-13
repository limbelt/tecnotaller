package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.OrderStatus
import com.example.data.model.PartCategory
import com.example.data.model.ServiceType
import com.example.ui.theme.StatusCancelledColor
import com.example.ui.theme.StatusDeliveredColor
import com.example.ui.theme.StatusDiagnosisColor
import com.example.ui.theme.StatusInProgressColor
import com.example.ui.theme.StatusReadyColor
import com.example.ui.theme.StatusReceivedColor
import com.example.ui.theme.StatusWaitingPartColor
import com.google.firebase.Timestamp
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

fun serviceTypeIcon(type: ServiceType): ImageVector = when (type) {
    ServiceType.CELULAR -> Icons.Filled.PhoneAndroid
    ServiceType.COMPUTADORA -> Icons.Filled.Computer
    ServiceType.CAMARAS_SEGURIDAD -> Icons.Filled.Videocam
}

fun partCategoryIcon(category: PartCategory): ImageVector = when (category) {
    PartCategory.PANTALLA -> Icons.Filled.Smartphone
    PartCategory.BATERIA -> Icons.Filled.BatteryChargingFull
    PartCategory.CONECTOR -> Icons.Filled.Usb
    PartCategory.ALMACENAMIENTO_RAM -> Icons.Filled.Memory
    PartCategory.CAMARA_CCTV -> Icons.Filled.Videocam
    PartCategory.CABLE_FUENTE -> Icons.Filled.Cable
    PartCategory.OTRO -> Icons.Filled.DeveloperBoard
}

fun orderStatusColor(status: OrderStatus): Color = when (status) {
    OrderStatus.RECIBIDO -> StatusReceivedColor
    OrderStatus.EN_DIAGNOSTICO -> StatusDiagnosisColor
    OrderStatus.ESPERANDO_REPUESTO -> StatusWaitingPartColor
    OrderStatus.EN_REPARACION -> StatusInProgressColor
    OrderStatus.LISTO -> StatusReadyColor
    OrderStatus.ENTREGADO -> StatusDeliveredColor
    OrderStatus.CANCELADO -> StatusCancelledColor
}

@Composable
fun ServiceTypeBadge(
    serviceType: ServiceType,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = serviceTypeIcon(serviceType),
                contentDescription = serviceType.label,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = serviceType.shortLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
fun OrderStatusPill(
    status: OrderStatus,
    modifier: Modifier = Modifier
) {
    val accent = orderStatusColor(status)
    Surface(
        color = accent.copy(alpha = 0.14f),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
    ) {
        Text(
            text = status.label,
            style = MaterialTheme.typography.labelMedium,
            color = accent,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun PartCategoryBadge(
    category: PartCategory,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = partCategoryIcon(category),
                contentDescription = category.label,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = category.label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
fun EmptyStateBanner(
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    showIllustration: Boolean = true,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (showIllustration) {
            Image(
                painter = painterResource(id = R.drawable.img_empty_orders),
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(width = 220.dp, height = 165.dp)
                    .clip(RoundedCornerShape(20.dp))
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onAction) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(actionLabel)
            }
        }
    }
}

fun formatCurrency(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("es", "MX"))
    return format.format(amount)
}

fun formatTimestamp(timestamp: Timestamp?): String {
    if (timestamp == null) return "Reciente"
    val formatter = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("es", "ES"))
    return formatter.format(timestamp.toDate())
}
