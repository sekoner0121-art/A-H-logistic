package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.example.ui.MainViewModel
import com.example.ui.components.IncidentDialog
import com.example.ui.components.IncomingPickupDialog
import com.example.ui.screens.DriverProfileScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.PickupDetailScreen
import com.example.ui.screens.PickupsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.UberBlack
import com.example.ui.theme.UberDarkBorder
import com.example.ui.theme.UberDarkCard
import com.example.ui.theme.UberDarkSurface
import com.example.ui.theme.UberGray300
import com.example.ui.theme.UberGreen
import com.example.ui.theme.UberWhite

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val isAuthenticated by viewModel.isAuthenticated.collectAsStateWithLifecycle()
                val selectedPickup by viewModel.selectedPickup.collectAsStateWithLifecycle()
                val incomingAlert by viewModel.incomingPickupAlert.collectAsStateWithLifecycle()
                val incidentTarget by viewModel.incidentTargetPickup.collectAsStateWithLifecycle()
                val activeCount by viewModel.activeCount.collectAsStateWithLifecycle()
                val driverProfile by viewModel.driverProfile.collectAsStateWithLifecycle()

                var currentTab by rememberSaveable { mutableIntStateOf(0) }

                // If not authenticated, present the Uber-style LoginScreen
                if (!isAuthenticated) {
                    LoginScreen(viewModel = viewModel)
                } else {
                    // Dialogs for authenticated driver
                    incomingAlert?.let { alert ->
                        IncomingPickupDialog(
                            pickup = alert,
                            onAccept = { viewModel.acceptIncomingPickup(alert.id) },
                            onReject = { viewModel.rejectIncomingPickup(alert.id) }
                        )
                    }

                    incidentTarget?.let { target ->
                        IncidentDialog(
                            pickup = target,
                            onDismiss = { viewModel.closeIncidentDialog() },
                            onSubmitIncident = { reason, notes ->
                                viewModel.submitIncident(reason, notes)
                            }
                        )
                    }

                    // If viewing specific pickup details/signature
                    if (selectedPickup != null) {
                        PickupDetailScreen(
                            pickup = selectedPickup!!,
                            viewModel = viewModel,
                            onBack = { viewModel.selectPickup(null) }
                        )
                    } else {
                        val isOnline = driverProfile.status != DriverStatus.FUERA_TURNO

                        Scaffold(
                            containerColor = UberBlack,
                            modifier = Modifier.fillMaxSize(),
                            topBar = {
                                TopAppBar(
                                    title = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "Conductor",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Black,
                                                color = UberWhite
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = UberDarkCard,
                                                border = BorderStroke(1.dp, UberDarkBorder)
                                            ) {
                                                Text(
                                                    text = driverProfile.unitId,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = UberGreen,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                        }
                                    },
                                    actions = {
                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = if (isOnline) UberGreen.copy(alpha = 0.15f) else UberDarkCard,
                                            border = BorderStroke(1.dp, if (isOnline) UberGreen else UberDarkBorder),
                                            modifier = Modifier.padding(end = 12.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .clip(CircleShape)
                                                        .background(if (isOnline) UberGreen else Color(0xFF94A3B8))
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = if (isOnline) "En Línea" else "Desconectado",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isOnline) UberGreen else UberGray300
                                                )
                                            }
                                        }
                                    },
                                    colors = TopAppBarDefaults.topAppBarColors(
                                        containerColor = UberBlack
                                    )
                                )
                            },
                            bottomBar = {
                                NavigationBar(
                                    containerColor = UberDarkSurface,
                                    tonalElevation = 8.dp
                                ) {
                                    NavigationBarItem(
                                        selected = currentTab == 0,
                                        onClick = { currentTab = 0 },
                                        icon = {
                                            if (activeCount > 0) {
                                                BadgedBox(
                                                    badge = {
                                                        Badge(
                                                            containerColor = UberGreen,
                                                            contentColor = UberBlack
                                                        ) {
                                                            Text("$activeCount", fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.LocalShipping,
                                                        contentDescription = "Recolecciones"
                                                    )
                                                }
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.LocalShipping,
                                                    contentDescription = "Recolecciones"
                                                )
                                            }
                                        },
                                        label = { Text("Recolecciones", fontWeight = FontWeight.SemiBold) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = UberBlack,
                                            selectedTextColor = UberGreen,
                                            indicatorColor = UberGreen,
                                            unselectedIconColor = UberGray300,
                                            unselectedTextColor = UberGray300
                                        ),
                                        modifier = Modifier.testTag("nav_pickups_tab")
                                    )

                                    NavigationBarItem(
                                        selected = currentTab == 1,
                                        onClick = { currentTab = 1 },
                                        icon = {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = "Cuenta"
                                            )
                                        },
                                        label = { Text("Mi Cuenta", fontWeight = FontWeight.SemiBold) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = UberBlack,
                                            selectedTextColor = UberGreen,
                                            indicatorColor = UberGreen,
                                            unselectedIconColor = UberGray300,
                                            unselectedTextColor = UberGray300
                                        ),
                                        modifier = Modifier.testTag("nav_profile_tab")
                                    )
                                }
                            }
                        ) { innerPadding ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                            ) {
                                AnimatedContent(
                                    targetState = currentTab,
                                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                                    label = "tab_transition"
                                ) { target ->
                                    when (target) {
                                        0 -> PickupsScreen(viewModel = viewModel)
                                        1 -> DriverProfileScreen(viewModel = viewModel)
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
