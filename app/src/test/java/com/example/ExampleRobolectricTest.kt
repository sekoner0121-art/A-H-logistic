package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.PickupStatus
import com.example.data.repository.LogisticsRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Conductor Logística", appName)
  }

  @Test
  fun `repository loads initial pickups and supports workflow`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repository = LogisticsRepository(context)
    val pickups = repository.pickups.value

    assertTrue("Initial pickups should not be empty", pickups.isNotEmpty())
    assertNotNull("Driver profile should be available", repository.driverProfile.value)

    val first = pickups.first()
    repository.updatePickupStatus(first.id, PickupStatus.EN_SITIO)

    val updated = repository.pickups.value.first { it.id == first.id }
    assertEquals(PickupStatus.EN_SITIO, updated.status)
  }

  @Test
  fun `repository requires authentication and can log out`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repository = LogisticsRepository(context)

    assertFalse("Driver must not be authenticated without credentials", repository.isAuthenticated.value)
    repository.logout()
    assertFalse("Driver should be logged out", repository.isAuthenticated.value)
  }
}
