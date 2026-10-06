package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
    assertEquals("Calendar", appName)
  }

  @Test
  fun `verify Indian holidays loaded correctly`() {
    val republicDay = com.example.data.IndianHolidays.getHoliday(2026, 1, 26)
    assertNotNull(republicDay)
    assertEquals("Republic Day", republicDay?.title)
  }
}
