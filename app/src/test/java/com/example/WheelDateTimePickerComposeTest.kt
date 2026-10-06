package com.example

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.ui.WheelDateTimePicker
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class WheelDateTimePickerComposeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `changing hour and then minute preserves chosen hour and does not reset to 9`() {
        val initialCal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 15, 9, 20, 0)
            set(Calendar.MILLISECOND, 0)
        }
        var resultingTimestamp = initialCal.timeInMillis

        composeTestRule.setContent {
            MyApplicationTheme {
                var currentTs by remember { mutableLongStateOf(initialCal.timeInMillis) }
                WheelDateTimePicker(
                    timestamp = currentTs,
                    isAllDay = false,
                    onTimestampChange = { newTs ->
                        currentTs = newTs
                        resultingTimestamp = newTs
                    }
                )
            }
        }

        // Initially hour is 9 (so 8, 9, 10, 11 are visible). Click visible Hour "10"
        composeTestRule.onNodeWithText("10").performClick()
        composeTestRule.waitForIdle()

        var updatedCal = Calendar.getInstance().apply { timeInMillis = resultingTimestamp }
        assertEquals("Hour must become 10", 10, updatedCal.get(Calendar.HOUR))
        assertEquals("Minute must remain 20", 20, updatedCal.get(Calendar.MINUTE))

        // Initially minute is 20 (so 19, 20, 21, 22 are visible). Click visible Minute "21"
        composeTestRule.onNodeWithText("21").performClick()
        composeTestRule.waitForIdle()

        updatedCal = Calendar.getInstance().apply { timeInMillis = resultingTimestamp }
        // Crucial test: Hour must NOT reset back to 9!
        assertEquals("Hour must strictly remain 10", 10, updatedCal.get(Calendar.HOUR))
        assertEquals("Minute must become 21", 21, updatedCal.get(Calendar.MINUTE))
        assertEquals("Day must remain 15", 15, updatedCal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `changing minute and then hour preserves chosen minute and does not reset to 00`() {
        val initialCal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 15, 9, 20, 0)
            set(Calendar.MILLISECOND, 0)
        }
        var resultingTimestamp = initialCal.timeInMillis

        composeTestRule.setContent {
            MyApplicationTheme {
                var currentTs by remember { mutableLongStateOf(initialCal.timeInMillis) }
                WheelDateTimePicker(
                    timestamp = currentTs,
                    isAllDay = false,
                    onTimestampChange = { newTs ->
                        currentTs = newTs
                        resultingTimestamp = newTs
                    }
                )
            }
        }

        // Click visible Minute "21"
        composeTestRule.onNodeWithText("21").performClick()
        composeTestRule.waitForIdle()

        var updatedCal = Calendar.getInstance().apply { timeInMillis = resultingTimestamp }
        assertEquals("Hour must remain 9", 9, updatedCal.get(Calendar.HOUR))
        assertEquals("Minute must become 21", 21, updatedCal.get(Calendar.MINUTE))

        // Click visible Hour "10"
        composeTestRule.onNodeWithText("10").performClick()
        composeTestRule.waitForIdle()

        updatedCal = Calendar.getInstance().apply { timeInMillis = resultingTimestamp }
        // Crucial test: Minute must NOT reset back to 00 or 20!
        assertEquals("Minute must strictly remain 21", 21, updatedCal.get(Calendar.MINUTE))
        assertEquals("Hour must become 10", 10, updatedCal.get(Calendar.HOUR))
        assertEquals("Day must remain 15", 15, updatedCal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `toggling to PM preserves hours and minutes exactly`() {
        val initialCal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 15, 10, 25, 0)
            set(Calendar.MILLISECOND, 0)
        }
        var resultingTimestamp = initialCal.timeInMillis

        composeTestRule.setContent {
            MyApplicationTheme {
                var currentTs by remember { mutableLongStateOf(initialCal.timeInMillis) }
                WheelDateTimePicker(
                    timestamp = currentTs,
                    isAllDay = false,
                    onTimestampChange = { newTs ->
                        currentTs = newTs
                        resultingTimestamp = newTs
                    }
                )
            }
        }

        // Click "pm"
        composeTestRule.onNodeWithText("pm").performClick()
        composeTestRule.waitForIdle()

        val updatedCal = Calendar.getInstance().apply { timeInMillis = resultingTimestamp }
        assertEquals("Hour 24 must become 22 (10 PM)", 22, updatedCal.get(Calendar.HOUR_OF_DAY))
        assertEquals("Hour 12 must remain 10", 10, updatedCal.get(Calendar.HOUR))
        assertEquals("Minute must remain 25", 25, updatedCal.get(Calendar.MINUTE))
        assertEquals("Day must remain 15", 15, updatedCal.get(Calendar.DAY_OF_MONTH))
    }
}
