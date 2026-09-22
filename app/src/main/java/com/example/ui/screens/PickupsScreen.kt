package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.DriverStatus
import com.example.data.model.PickupStatus
import com.example.ui.MainViewModel
import com.example.ui.PickupFilter
import com.example.ui.components.PickupCard
import com.example.ui.theme.UberBlack
import com.example.ui.theme.UberDarkBorder
import com.example.ui.theme.UberDarkCard
import com.example.ui.theme.UberDarkSurface
import com.example.ui.theme.UberGray300
import com.example.ui.theme.UberGray500
import com.example.ui.theme.UberGreen
import com.example.ui.theme.UberGreenDark
import com.example.ui.theme.UberRed
import com.example.ui.theme.UberWhite

@Composable
fun PickupsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val pickups by viewModel.filteredPickups.collectAsStateWithLifecycle()
    val allPickups by viewModel.pickups.collectAsStateWithLifecycle()
    val currentFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val driverProfile by viewModel.driverProfile.collectAsStateWithLifecycle()

    val isOnline = driverProfile.status != DriverStatus.FUERA_TURNO
    val activePickups = allPickups.filter {
        it.status == PickupStatus.EN_CAMINO || it.status == PickupStatus.EN_SITIO
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(UberBlack)
            .testTag("pickups_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Uber-style Driver Status & Go Online / Offline Header
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("driver_status_banner"),
                shape = RoundedCornerShape(16.dp),
                color = UberDarkSurface,
                border = BorderStroke(1.dp, if (isOnline) UberGreen.copy(alpha = 0.4f) else UberDarkBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = driverProfile.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = UberWhite
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${driverProfile.unitId} • ${driverProfile.licensePlate}",
                                style = MaterialTheme.typography.bodySmall,
                                color = UberGray300
                            )
                        }

                        // Uber Online / Offline status badge
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isOnline) UberGreen.copy(alpha = 0.15f) else UberDarkCard,
                            border = BorderStroke(1.dp, if (isOnline) UberGreen else UberDarkBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (isOnline) UberGreen else UberGray500)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isOnline) "EN LÍNEA" else "DESCONECTADO",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isOnline) UberGreen else UberGray300
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Tactical Online Switcher Button
                    Button(
                        onClick = { viewModel.toggleDriverStatus() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("toggle_online_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = if (isOnline) UberDarkCard else UberGreen,
                            contentColor = if (isOnline) UberWhite else UberBlack
                        ),
                        border = if (isOnline) BorderStroke(1.dp, UberDarkBorder) else null
                    ) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = if (isOnline) UberGreen else UberBlack
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isOnline) "Cambiar Estado / Desconectar" else "Conectar y Recibir Tareas",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }

        // Active In-Progress Recolección Banner
        if (activePickups.isNotEmpty()) {
            val active = activePickups.first()
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.selectPickup(active) },
                    shape = RoundedCornerShape(16.dp),
                    color = UberDarkCard,
                    border = BorderStroke(1.5.dp, UberGreen)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(UberGreen),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalShipping,
                                contentDescription = null,
                                tint = UberBlack,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "EN CURSO",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = UberGreen,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = active.clientName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = UberWhite
                            )
                            Text(
                                text = active.address,
                                style = MaterialTheme.typography.bodySmall,
                                color = UberGray300,
                                maxLines = 1
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = UberGreen
                        ) {
                            Text(
                                text = "Abrir",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = UberBlack,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // Filter Chips Row
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(PickupFilter.values()) { filter ->
                    val isSelected = currentFilter == filter
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setFilter(filter) },
                        label = {
                            Text(
                                text = filter.label,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = UberWhite,
                            selectedLabelColor = UberBlack,
                            containerColor = UberDarkSurface,
                            labelColor = UberGray300
                        ),
                        border = BorderStroke(1.dp, if (isSelected) UberWhite else UberDarkBorder)
                    )
                }
            }
        }

        // Section Title & Simulation Button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recolecciones (${pickups.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = UberWhite
                )

                OutlinedButton(
                    onClick = { viewModel.triggerSimulatedDispatch() },
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, UberDarkBorder),
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("simulate_dispatch_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AddAlert,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = UberGreen
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "+ Simular Tarea",
                        style = MaterialTheme.typography.labelSmall,
                        color = UberWhite
                    )
                }
            }
        }

        // Pickups List
        if (pickups.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = UberDarkSurface,
                    border = BorderStroke(1.dp, UberDarkBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = UberGreen,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Sin tareas pendientes",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = UberWhite
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Las nuevas recolecciones asignadas aparecerán aquí automáticamente.",
                            style = MaterialTheme.typography.bodySmall,
                            color = UberGray300,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(pickups, key = { it.id }) { pickup ->
                PickupCard(
                    pickup = pickup,
                    onClick = { viewModel.selectPickup(pickup) },
                    onStatusAdvance = { viewModel.advancePickupStatus(pickup) },
                    onReportIncident = { viewModel.openIncidentDialog(pickup) }
                )
            }
        }
    }
}
