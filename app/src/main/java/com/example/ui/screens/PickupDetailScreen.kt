package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Pickup
import com.example.data.model.PickupStatus
import com.example.ui.MainViewModel
import com.example.ui.components.SignaturePad
import com.example.ui.theme.UberBlack
import com.example.ui.theme.UberDarkBorder
import com.example.ui.theme.UberDarkCard
import com.example.ui.theme.UberDarkSurface
import com.example.ui.theme.UberGray300
import com.example.ui.theme.UberGray500
import com.example.ui.theme.UberGreen
import com.example.ui.theme.UberRed
import com.example.ui.theme.UberWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickupDetailScreen(
    pickup: Pickup,
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var signerName by remember { mutableStateOf(pickup.signatureName.ifBlank { pickup.contactPerson }) }
    var evidenceNotes by remember { mutableStateOf(pickup.evidenceNotes) }
    var hasSignature by remember { mutableStateOf(pickup.hasSignature) }
    var showSignatureError by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = UberBlack,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = pickup.trackingCode,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = UberWhite
                        )
                        Text(
                            text = pickup.status.label,
                            style = MaterialTheme.typography.bodySmall,
                            color = UberGreen
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Regresar",
                            tint = UberWhite
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.openIncidentDialog(pickup) }) {
                        Icon(
                            imageVector = Icons.Default.ReportProblem,
                            contentDescription = "Reportar incidencia",
                            tint = UberRed
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = UberBlack
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("pickup_detail_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Lifecycle Progress Stepper - Uber style
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = UberDarkSurface,
                    border = BorderStroke(1.dp, UberDarkBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "ESTADO DE LA TAREA",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = UberGreen,
                            letterSpacing = 0.5.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        val steps = listOf(
                            "Aceptada",
                            "En Camino",
                            "En Sitio",
                            "Recolectada"
                        )
                        val currentIndex = when (pickup.status) {
                            PickupStatus.PENDIENTE -> 0
                            PickupStatus.ACEPTADA -> 0
                            PickupStatus.EN_CAMINO -> 1
                            PickupStatus.EN_SITIO -> 2
                            PickupStatus.RECOLECTADA -> 3
                            else -> 0
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            steps.forEachIndexed { index, step ->
                                val isPassed = index <= currentIndex
                                val isCurrent = index == currentIndex

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(if (isPassed) UberGreen else UberDarkCard)
                                            .border(
                                                width = if (isCurrent) 2.dp else 0.dp,
                                                color = if (isCurrent) UberWhite else Color.Transparent,
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${index + 1}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isPassed) UberBlack else UberGray500
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = step,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isPassed) UberWhite else UberGray500
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Client & Contact Information Card
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = UberDarkSurface,
                    border = BorderStroke(1.dp, UberDarkBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = pickup.clientName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = UberWhite
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = UberGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Contacto: ${pickup.contactPerson} (${pickup.phone})",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = UberWhite
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = UberGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = pickup.address,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = UberGray300
                                )
                                if (pickup.reference.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Ref: ${pickup.reference}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = UberGray500
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Action Buttons: Call & Maps
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    val gmmIntentUri = Uri.parse("geo:0,0?q=${Uri.encode(pickup.address)}")
                                    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                    try { context.startActivity(mapIntent) } catch (_: Exception) {}
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = UberGreen,
                                    contentColor = UberBlack
                                )
                            ) {
                                Icon(imageVector = Icons.Default.Directions, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Navegar", fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${pickup.phone}"))
                                    try { context.startActivity(intent) } catch (_: Exception) {}
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, UberDarkBorder),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = UberWhite)
                            ) {
                                Icon(imageVector = Icons.Default.Call, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Llamar", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Cargo & Packaging Summary
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = UberDarkSurface,
                    border = BorderStroke(1.dp, UberDarkBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Detalle de Carga",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = UberWhite
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Inventory2,
                                    contentDescription = null,
                                    tint = UberGreen
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "${pickup.packagesCount} Paquetes",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = UberWhite
                                    )
                                    Text(
                                        text = pickup.packageType,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = UberGray300
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Scale,
                                    contentDescription = null,
                                    tint = UberWhite
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "${pickup.totalWeightKg} kg",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = UberWhite
                                    )
                                    Text(
                                        text = "Peso total",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = UberGray300
                                    )
                                }
                            }
                        }

                        if (pickup.specialInstructions.isNotBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = UberDarkCard,
                                border = BorderStroke(1.dp, UberDarkBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = UberGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = pickup.specialInstructions,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = UberWhite
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Lifecycle Operational Buttons (Iniciar Ruta / Llegué a Sitio)
            if (pickup.status == PickupStatus.ACEPTADA) {
                item {
                    Button(
                        onClick = { viewModel.advancePickupStatus(pickup) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("start_route_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = UberGreen,
                            contentColor = UberBlack
                        )
                    ) {
                        Text(
                            text = "INICIAR RUTA AL SITIO",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            } else if (pickup.status == PickupStatus.EN_CAMINO) {
                item {
                    Button(
                        onClick = { viewModel.advancePickupStatus(pickup) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("arrived_site_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = UberWhite,
                            contentColor = UberBlack
                        )
                    ) {
                        Text(
                            text = "LLEGUÉ AL SITIO",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            // Signature & Completion
            if (pickup.status == PickupStatus.EN_SITIO || pickup.status == PickupStatus.RECOLECTADA) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = UberDarkSurface,
                        border = BorderStroke(1.dp, UberDarkBorder)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Comprobante de Entrega",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = UberWhite
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = signerName,
                                onValueChange = { signerName = it },
                                label = { Text("Nombre de quien entrega") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("signer_name_input"),
                                shape = RoundedCornerShape(10.dp),
                                enabled = pickup.status != PickupStatus.RECOLECTADA,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = UberGreen,
                                    unfocusedBorderColor = UberDarkBorder,
                                    focusedTextColor = UberWhite,
                                    unfocusedTextColor = UberWhite,
                                    focusedContainerColor = UberDarkCard,
                                    unfocusedContainerColor = UberDarkCard,
                                    focusedLabelColor = UberGreen,
                                    unfocusedLabelColor = UberGray300
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = evidenceNotes,
                                onValueChange = { evidenceNotes = it },
                                label = { Text("Observaciones / Sellos de seguridad") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("evidence_notes_input"),
                                shape = RoundedCornerShape(10.dp),
                                enabled = pickup.status != PickupStatus.RECOLECTADA,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = UberGreen,
                                    unfocusedBorderColor = UberDarkBorder,
                                    focusedTextColor = UberWhite,
                                    unfocusedTextColor = UberWhite,
                                    focusedContainerColor = UberDarkCard,
                                    unfocusedContainerColor = UberDarkCard,
                                    focusedLabelColor = UberGreen,
                                    unfocusedLabelColor = UberGray300
                                )
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            if (pickup.status != PickupStatus.RECOLECTADA) {
                                SignaturePad(
                                    hasSignature = hasSignature,
                                    onSignatureChanged = {
                                        hasSignature = it
                                        if (it) showSignatureError = false
                                    }
                                )

                                if (showSignatureError) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Se requiere la firma del cliente para finalizar",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = UberRed
                                    )
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                Button(
                                    onClick = {
                                        if (!hasSignature) {
                                            showSignatureError = true
                                        } else {
                                            viewModel.completePickup(
                                                pickupId = pickup.id,
                                                signerName = signerName.ifBlank { "Encargado de Almacén" },
                                                evidenceNotes = evidenceNotes,
                                                hasSignature = true
                                            )
                                            onBack()
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(54.dp)
                                        .testTag("complete_pickup_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = UberGreen,
                                        contentColor = UberBlack
                                    )
                                ) {
                                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "FINALIZAR RECOLECCIÓN",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = UberGreen.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, UberGreen),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = UberGreen,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "Recolección Completada",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = UberGreen
                                            )
                                            Text(
                                                text = "Firmado por: ${pickup.signatureName}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = UberWhite
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
