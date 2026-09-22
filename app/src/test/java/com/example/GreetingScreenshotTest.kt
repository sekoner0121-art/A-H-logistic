package com.example

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.example.data.model.Pickup
import com.example.data.model.PickupStatus
import com.example.data.model.Priority
import com.example.ui.components.PickupCard
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    composeTestRule.setContent {
      MyApplicationTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
          PickupCard(
            pickup = Pickup(
              id = "REC-801",
              trackingCode = "REC-801-CDMX",
              clientName = "Distribuidora Médica del Valle",
              contactPerson = "Lic. Roberto Garza",
              phone = "+52 55 5678 1234",
              address = "Av. Insurgentes Sur 1450, Col. Actipan, CDMX",
              reference = "Andén 3 posterior",
              timeWindow = "09:30 - 10:30 hrs",
              packagesCount = 4,
              totalWeightKg = 32.5,
              packageType = "Insumos médicos",
              priority = Priority.URGENTE,
              status = PickupStatus.EN_CAMINO
            ),
            onClick = {},
            onStatusAdvance = {},
            onReportIncident = {}
          )
        }
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
