package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Pickup
import com.example.ui.theme.UberBlack
import com.example.ui.theme.UberDarkBorder
import com.example.ui.theme.UberDarkCard
import com.example.ui.theme.UberDarkSurface
import com.example.ui.theme.UberGray300
import com.example.ui.theme.UberRed
import com.example.ui.theme.UberWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncidentDialog(
    pickup: Pickup,
    onDismiss: () -> Unit,
    onSubmitIncident: (reason: String, notes: String) -> Unit
) {
    val incidentReasons = listOf(
        "Cliente no se encuentra en el domicilio",
        "Mercancía no está lista para recolección",
        "Dirección / Localización inaccesible para la unidad",
        "Mercancía dañada / Rechazada por protocolo",
        "Diferencia en conteo de paquetes reportados",
        "Cancelación directa por parte del remitente",
        "Falla mecánica o contratiempo de unidad"
    )

    var selectedReason by remember { mutableStateOf(incidentReasons.first()) }
    var expanded by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = UberDarkSurface,
        titleContentColor = UberWhite,
        textContentColor = UberGray300,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.ReportProblem,
                    contentDescription = null,
                    tint = UberRed
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Reportar Incidencia",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = UberWhite
                )
            }
        },
        text = {
            Column {
                Text(
                    text = "Recolección: ${pickup.trackingCode} (${pickup.clientName})",
                    style = MaterialTheme.typography.bodySmall,
                    color = UberGray300
                )

                Spacer(modifier = Modifier.height(14.dp))

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedReason,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Motivo de la incidencia") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = UberRed,
                            unfocusedBorderColor = UberDarkBorder,
                            focusedTextColor = UberWhite,
                            unfocusedTextColor = UberWhite,
                            focusedContainerColor = UberDarkCard,
                            unfocusedContainerColor = UberDarkCard,
                            focusedLabelColor = UberRed,
                            unfocusedLabelColor = UberGray300
                        )
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        incidentReasons.forEach { reason ->
                            DropdownMenuItem(
                                text = { Text(reason) },
                                onClick = {
                                    selectedReason = reason
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Detalles adicionales / Evidencia") },
                    placeholder = { Text("Describe brevemente la situación ocurrida...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .testTag("incident_notes_input"),
                    shape = RoundedCornerShape(10.dp),
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = UberRed,
                        unfocusedBorderColor = UberDarkBorder,
                        focusedTextColor = UberWhite,
                        unfocusedTextColor = UberWhite,
                        focusedContainerColor = UberDarkCard,
                        unfocusedContainerColor = UberDarkCard,
                        focusedLabelColor = UberRed,
                        unfocusedLabelColor = UberGray300
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSubmitIncident(selectedReason, notes)
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = UberRed,
                    contentColor = UberWhite
                ),
                modifier = Modifier.testTag("submit_incident_button")
            ) {
                Text("Transmitir Incidencia", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, UberDarkBorder),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = UberWhite)
            ) {
                Text("Cancelar")
            }
        }
    )
}
