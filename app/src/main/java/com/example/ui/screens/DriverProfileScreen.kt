package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainViewModel
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
fun DriverProfileScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val driverProfile by viewModel.driverProfile.collectAsStateWithLifecycle()
    val collectedCount by viewModel.collectedTodayCount.collectAsStateWithLifecycle()
    val totalWeight by viewModel.totalWeightKgToday.collectAsStateWithLifecycle()

    var name by remember(driverProfile.name) { mutableStateOf(driverProfile.name) }
    var unitId by remember(driverProfile.unitId) { mutableStateOf(driverProfile.unitId) }
    var plate by remember(driverProfile.licensePlate) { mutableStateOf(driverProfile.licensePlate) }
    var phone by remember(driverProfile.phone) { mutableStateOf(driverProfile.phone) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(UberBlack)
            .testTag("driver_profile_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Driver ID & Uber-style Account Header
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = UberDarkSurface,
                border = BorderStroke(1.dp, UberDarkBorder)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(UberDarkCard)
                            .border(2.dp, UberGreen, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = UberGreen,
                            modifier = Modifier.size(42.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = driverProfile.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = UberWhite
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = driverProfile.email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = UberGray300
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = UberGreen.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, UberGreen)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = UberGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Autenticado con Firebase",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = UberGreen
                            )
                        }
                    }
                }
            }
        }

        // Shift Stats
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = UberDarkSurface,
                border = BorderStroke(1.dp, UberDarkBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "RESUMEN DEL TURNO HOY",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = UberGreen,
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .background(UberDarkCard, RoundedCornerShape(12.dp))
                                .border(1.dp, UberDarkBorder, RoundedCornerShape(12.dp))
                                .padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "$collectedCount",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                                color = UberWhite
                            )
                            Text(
                                text = "Recolectadas",
                                style = MaterialTheme.typography.bodySmall,
                                color = UberGray300
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .background(UberDarkCard, RoundedCornerShape(12.dp))
                                .border(1.dp, UberDarkBorder, RoundedCornerShape(12.dp))
                                .padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${totalWeight.toInt()} kg",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                                color = UberGreen
                            )
                            Text(
                                text = "Carga Total",
                                style = MaterialTheme.typography.bodySmall,
                                color = UberGray300
                            )
                        }
                    }
                }
            }
        }

        // Vehicle Data Form
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = UberDarkSurface,
                border = BorderStroke(1.dp, UberDarkBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = null,
                            tint = UberGreen
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Datos de Unidad y Vehículo",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = UberWhite
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nombre del Conductor") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("driver_name_input"),
                        shape = RoundedCornerShape(10.dp),
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

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = unitId,
                        onValueChange = { unitId = it },
                        label = { Text("Código de Unidad") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("unit_id_input"),
                        shape = RoundedCornerShape(10.dp),
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

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = plate,
                        onValueChange = { plate = it },
                        label = { Text("Placa del Vehículo") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("license_plate_input"),
                        shape = RoundedCornerShape(10.dp),
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

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Teléfono de Contacto") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
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

                    Button(
                        onClick = {
                            viewModel.updateDriverProfile(name, unitId, plate, phone)
                            Toast.makeText(context, "Perfil y unidad actualizados", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("save_profile_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = UberGreen,
                            contentColor = UberBlack
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Guardar Cambios", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Sign Out Button
        item {
            OutlinedButton(
                onClick = { viewModel.logout() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("logout_button"),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, UberRed.copy(alpha = 0.6f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = UberRed)
            ) {
                Icon(imageVector = Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cerrar Sesión del Conductor", fontWeight = FontWeight.Bold)
            }
        }
    }
}
