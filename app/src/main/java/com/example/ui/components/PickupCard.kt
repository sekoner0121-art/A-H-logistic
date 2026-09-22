package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Pickup
import com.example.data.model.PickupStatus
import com.example.data.model.Priority
import com.example.ui.theme.UberBlack
import com.example.ui.theme.UberDarkBorder
import com.example.ui.theme.UberDarkCard
import com.example.ui.theme.UberDarkSurface
import com.example.ui.theme.UberGray300
import com.example.ui.theme.UberGray500
import com.example.ui.theme.UberGreen
import com.example.ui.theme.UberRed
import com.example.ui.theme.UberWhite

@Composable
fun PickupCard(
    pickup: Pickup,
    onClick: () -> Unit,
    onStatusAdvance: () -> Unit,
    onReportIncident: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val (badgeBg, badgeText) = when (pickup.status) {
        PickupStatus.PENDIENTE -> Pair(UberWhite.copy(alpha = 0.12f), UberWhite)
        PickupStatus.ACEPTADA -> Pair(UberGreen.copy(alpha = 0.15f), UberGreen)
        PickupStatus.EN_CAMINO -> Pair(UberGreen.copy(alpha = 0.2f), UberGreen)
        PickupStatus.EN_SITIO -> Pair(UberWhite, UberBlack)
        PickupStatus.RECOLECTADA -> Pair(UberGreen, UberBlack)
        PickupStatus.INCIDENCIA -> Pair(UberRed.copy(alpha = 0.15f), UberRed)
        PickupStatus.RECHAZADA -> Pair(UberDarkCard, UberGray500)
    }

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .testTag("pickup_card_${pickup.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = UberDarkSurface),
        border = BorderStroke(
            1.dp,
            if (pickup.priority == Priority.URGENTE) UberRed.copy(alpha = 0.6f) else UberDarkBorder
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Tracking Code & Status Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = pickup.trackingCode,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                        color = UberWhite
                    )

                    if (pickup.priority == Priority.URGENTE) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = UberRed.copy(alpha = 0.2f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PriorityHigh,
                                    contentDescription = null,
                                    tint = UberRed,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "URGENTE",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = UberRed
                                )
                            }
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = badgeBg
                ) {
                    Text(
                        text = pickup.status.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = badgeText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Client Name & Window
            Text(
                text = pickup.clientName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = UberWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Address
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = UberGreen,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = pickup.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = UberGray300,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Packages & Weight & Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Inventory2,
                        contentDescription = null,
                        tint = UberGray300,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${pickup.packagesCount} paq. (${pickup.totalWeightKg} kg)",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = UberWhite
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = UberGray300,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = pickup.timeWindow,
                        style = MaterialTheme.typography.bodySmall,
                        color = UberGray300
                    )
                }
            }

            if (pickup.status == PickupStatus.INCIDENCIA && pickup.incidentReason.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = UberRed.copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Incidencia: ${pickup.incidentReason}",
                        style = MaterialTheme.typography.bodySmall,
                        color = UberRed,
                        modifier = Modifier.padding(8.dp),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Operational Action Buttons - Uber driver style
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Quick Call
                IconButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${pickup.phone}"))
                        try { context.startActivity(intent) } catch (_: Exception) {}
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .background(UberDarkCard, RoundedCornerShape(10.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = "Llamar cliente",
                        tint = UberWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Quick Navigation
                IconButton(
                    onClick = {
                        val gmmIntentUri = Uri.parse("geo:0,0?q=${Uri.encode(pickup.address)}")
                        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                        try { context.startActivity(mapIntent) } catch (_: Exception) {}
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .background(UberDarkCard, RoundedCornerShape(10.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Directions,
                        contentDescription = "Abrir navegación GPS",
                        tint = UberGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Advance Workflow Button
                when (pickup.status) {
                    PickupStatus.ACEPTADA -> {
                        Button(
                            onClick = onStatusAdvance,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = UberGreen,
                                contentColor = UberBlack
                            )
                        ) {
                            Text("Iniciar Ruta", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    PickupStatus.EN_CAMINO -> {
                        Button(
                            onClick = onStatusAdvance,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = UberWhite,
                                contentColor = UberBlack
                            )
                        ) {
                            Text("Llegué a Sitio", fontWeight = FontWeight.Bold)
                        }
                    }
                    PickupStatus.EN_SITIO -> {
                        Button(
                            onClick = onClick,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = UberGreen,
                                contentColor = UberBlack
                            )
                        ) {
                            Text("Recolectar & Firmar", fontWeight = FontWeight.Bold)
                        }
                    }
                    PickupStatus.RECOLECTADA -> {
                        FilledTonalButton(
                            onClick = onClick,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = UberDarkCard,
                                contentColor = UberWhite
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = UberGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Comprobante")
                        }
                    }
                    else -> {
                        OutlinedButton(
                            onClick = onClick,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, UberDarkBorder),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = UberWhite)
                        ) {
                            Text("Ver Detalles")
                        }
                    }
                }

                if (pickup.status != PickupStatus.RECOLECTADA && pickup.status != PickupStatus.RECHAZADA) {
                    IconButton(
                        onClick = onReportIncident,
                        modifier = Modifier
                            .size(44.dp)
                            .background(UberDarkCard, RoundedCornerShape(10.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReportProblem,
                            contentDescription = "Reportar incidencia",
                            tint = UberRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
