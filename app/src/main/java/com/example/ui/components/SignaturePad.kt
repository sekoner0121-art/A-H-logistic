package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.ui.theme.UberBlack
import com.example.ui.theme.UberDarkBorder
import com.example.ui.theme.UberDarkCard
import com.example.ui.theme.UberGray300
import com.example.ui.theme.UberGray500
import com.example.ui.theme.UberGreen
import com.example.ui.theme.UberRed
import com.example.ui.theme.UberWhite

@Composable
fun SignaturePad(
    modifier: Modifier = Modifier,
    hasSignature: Boolean,
    onSignatureChanged: (Boolean) -> Unit
) {
    val paths = remember { mutableStateListOf<List<Offset>>() }
    val currentPath = remember { mutableStateListOf<Offset>() }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Draw,
                    contentDescription = null,
                    tint = UberGreen
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Firma digital del remitente",
                    style = MaterialTheme.typography.titleSmall,
                    color = UberWhite
                )
            }

            if (paths.isNotEmpty() || currentPath.isNotEmpty() || hasSignature) {
                OutlinedButton(
                    onClick = {
                        paths.clear()
                        currentPath.clear()
                        onSignatureChanged(false)
                    },
                    border = BorderStroke(1.dp, UberRed.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = UberRed
                    ),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Borrar firma", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Limpiar", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(UberWhite)
                .border(
                    width = 1.5.dp,
                    color = if (hasSignature || paths.isNotEmpty()) UberGreen else UberDarkBorder,
                    shape = RoundedCornerShape(12.dp)
                )
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            currentPath.clear()
                            currentPath.add(offset)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            currentPath.add(change.position)
                            onSignatureChanged(true)
                        },
                        onDragEnd = {
                            if (currentPath.isNotEmpty()) {
                                paths.add(currentPath.toList())
                                currentPath.clear()
                                onSignatureChanged(true)
                            }
                        },
                        onDragCancel = {
                            currentPath.clear()
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                // Completed lines in dark ink
                paths.forEach { pathPoints ->
                    if (pathPoints.size > 1) {
                        val strokePath = Path().apply {
                            moveTo(pathPoints.first().x, pathPoints.first().y)
                            for (i in 1 until pathPoints.size) {
                                lineTo(pathPoints[i].x, pathPoints[i].y)
                            }
                        }
                        drawPath(
                            path = strokePath,
                            color = Color(0xFF0F172A),
                            style = Stroke(
                                width = 5.5f,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }
                }

                // Currently drawn line
                if (currentPath.size > 1) {
                    val strokePath = Path().apply {
                        moveTo(currentPath.first().x, currentPath.first().y)
                        for (i in 1 until currentPath.size) {
                            lineTo(currentPath[i].x, currentPath[i].y)
                        }
                    }
                    drawPath(
                        path = strokePath,
                        color = Color(0xFF0F172A),
                        style = Stroke(
                            width = 5.5f,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }

            if (paths.isEmpty() && currentPath.isEmpty() && !hasSignature) {
                Text(
                    text = "Dibuja la firma aquí",
                    color = Color(0xFF94A3B8),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}
