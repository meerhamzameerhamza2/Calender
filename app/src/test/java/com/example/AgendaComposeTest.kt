package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.model.EventEntity
import com.example.ui.AgendaViewContent
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AgendaComposeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `agenda view renders single list without old filter header chips and displays items`() {
        val tomorrowCal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val testEvent = EventEntity(
            id = 999,
            title = "Annual Company Dinner",
            startTimestamp = tomorrowCal.timeInMillis,
            endTimestamp = tomorrowCal.timeInMillis + 7200000,
            location = "Main Banquet Hall",
            isImportantDay = true
        )

        var addEventClicked = false

        composeTestRule.setContent {
            MyApplicationTheme {
                AgendaViewContent(
                    events = listOf(testEvent),
                    onAddEventClick = { addEventClicked = true },
                    onEventClick = {}
                )
            }
        }

        composeTestRule.waitForIdle()

        // 1. Verify old filter chips are completely removed
        composeTestRule.onNodeWithTag("agenda_filter_all").assertDoesNotExist()
        composeTestRule.onNodeWithTag("agenda_filter_holidays").assertDoesNotExist()
        composeTestRule.onNodeWithTag("agenda_filter_events").assertDoesNotExist()
        composeTestRule.onNodeWithTag("agenda_filter_important_days").assertDoesNotExist()

        // 2. Verify upcoming item title is displayed in the list
        composeTestRule.onNodeWithText("Annual Company Dinner").assertIsDisplayed()

        // 3. Verify FAB button is present and clickable
        val fabNode = composeTestRule.onNodeWithTag("agenda_add_event_fab")
        fabNode.assertIsDisplayed()
        fabNode.performClick()
        assertTrue("Add event callback should be triggered on FAB click", addEventClicked)
    }
}
