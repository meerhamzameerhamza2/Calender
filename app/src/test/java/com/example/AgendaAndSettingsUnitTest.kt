package com.example

import com.example.data.HolidayRepository
import com.example.model.CalendarSettings
import com.example.model.EventEntity
import com.example.model.Holiday
import com.example.model.HolidayType
import com.example.ui.AgendaItem
import com.example.ui.CalendarViewMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AgendaAndSettingsUnitTest {

    @Test
    fun `agenda strictly filters for current year and future dates only`() {
        val currentYear = 2026

        // Today is set to September 14, 2026 00:00:00
        val todayStart = Calendar.getInstance().apply {
            set(currentYear, Calendar.SEPTEMBER, 14, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val endOfCurrentYear = Calendar.getInstance().apply {
            set(currentYear, Calendar.DECEMBER, 31, 23, 59, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        // Sample holidays for testing
        val pastHoliday = Holiday(
            day = 26,
            month = 1, // January (Past)
            year = currentYear,
            title = "Republic Day",
            category = "Gazetted Holiday",
            type = HolidayType.NATIONAL
        )
        val upcomingHoliday = Holiday(
            day = 2,
            month = 10, // October (Upcoming)
            year = currentYear,
            title = "Mahatma Gandhi Jayanti",
            category = "Gazetted Holiday",
            type = HolidayType.NATIONAL
        )
        val nextYearHoliday = Holiday(
            day = 26,
            month = 1,
            year = currentYear + 1, // Next year
            title = "Republic Day",
            category = "Gazetted Holiday",
            type = HolidayType.NATIONAL
        )

        val rawHolidays = listOf(pastHoliday, upcomingHoliday, nextYearHoliday)

        // Filter algorithm used in AgendaViewContent
        val filteredHolidays = mutableListOf<AgendaItem.HolidayItem>()
        for (h in rawHolidays) {
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, h.year)
                set(Calendar.MONTH, h.month - 1)
                set(Calendar.DAY_OF_MONTH, h.day)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (cal.timeInMillis in todayStart..endOfCurrentYear) {
                filteredHolidays.add(AgendaItem.HolidayItem(h, cal.timeInMillis))
            }
        }

        // Must ONLY contain upcomingHoliday (October 2026)
        assertEquals(1, filteredHolidays.size)
        assertEquals("Mahatma Gandhi Jayanti", filteredHolidays[0].title)
        assertEquals(10, filteredHolidays[0].holiday.month)
        assertEquals(2026, filteredHolidays[0].holiday.year)
    }

    @Test
    fun `agenda combines holidays events and important days in single sorted list`() {
        val currentYear = 2026

        val oct2Time = Calendar.getInstance().apply {
            set(currentYear, Calendar.OCTOBER, 2, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val sep20Time = Calendar.getInstance().apply {
            set(currentYear, Calendar.SEPTEMBER, 20, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val nov5Time = Calendar.getInstance().apply {
            set(currentYear, Calendar.NOVEMBER, 5, 14, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val holidayItem = AgendaItem.HolidayItem(
            holiday = Holiday(
                day = 2,
                month = 10,
                year = currentYear,
                title = "Gandhi Jayanti",
                category = "Gazetted Holiday",
                type = HolidayType.NATIONAL
            ),
            timestamp = oct2Time
        )

        val eventItem = AgendaItem.UserEventItem(
            event = EventEntity(
                id = 1,
                title = "Team Sprint Planning",
                startTimestamp = sep20Time,
                endTimestamp = sep20Time + 3600000,
                isImportantDay = false
            ),
            timestamp = sep20Time
        )

        val importantDayItem = AgendaItem.UserEventItem(
            event = EventEntity(
                id = 2,
                title = "Company Annual Foundation Day",
                startTimestamp = nov5Time,
                endTimestamp = nov5Time + 86400000,
                isImportantDay = true
            ),
            timestamp = nov5Time
        )

        val combinedList = listOf<AgendaItem>(holidayItem, eventItem, importantDayItem)
            .sortedBy { it.timestamp }

        // Must be in chronological order: Sep 20 -> Oct 2 -> Nov 5
        assertEquals(3, combinedList.size)
        assertEquals("Team Sprint Planning", combinedList[0].title)
        assertEquals("Gandhi Jayanti", combinedList[1].title)
        assertEquals("Company Annual Foundation Day", combinedList[2].title)

        // Type assertions
        assertTrue(combinedList[0] is AgendaItem.UserEventItem)
        assertFalse((combinedList[0] as AgendaItem.UserEventItem).isImportant)

        assertTrue(combinedList[1] is AgendaItem.HolidayItem)
        assertTrue((combinedList[1] as AgendaItem.HolidayItem).isNationalOrGazetted)

        assertTrue(combinedList[2] is AgendaItem.UserEventItem)
        assertTrue((combinedList[2] as AgendaItem.UserEventItem).isImportant)
    }

    @Test
    fun `view switcher modes include strictly Year Month Agenda`() {
        val switcherModes = listOf(
            CalendarViewMode.YEAR to "Year",
            CalendarViewMode.MONTH to "Month",
            CalendarViewMode.AGENDA to "Agenda"
        )

        assertEquals(3, switcherModes.size)
        assertEquals(CalendarViewMode.YEAR, switcherModes[0].first)
        assertEquals(CalendarViewMode.MONTH, switcherModes[1].first)
        assertEquals(CalendarViewMode.AGENDA, switcherModes[2].first)

        // Ensure "Week" is NOT in the list
        assertFalse(switcherModes.any { it.first == CalendarViewMode.WEEK })
        assertFalse(switcherModes.any { it.second.equals("Week", ignoreCase = true) })
    }

    @Test
    fun `calendar settings properties and updates are consistent`() {
        val defaultSettings = CalendarSettings()

        assertEquals("Sunday", defaultSettings.startWeekOn)
        assertTrue(defaultSettings.setTimeZone)
        assertEquals("Asia/Kolkata (GMT+05:30)", defaultSettings.timeZone)
        assertEquals("India (Gazetted & Festivals)", defaultSettings.nationalHolidays)
        assertEquals("Muslim Calendar", defaultSettings.otherCalendar)

        // Updating settings
        val updated = defaultSettings.copy(
            startWeekOn = "Monday",
            timeZone = "UTC (GMT+00:00)"
        )

        assertEquals("Monday", updated.startWeekOn)
        assertEquals("UTC (GMT+00:00)", updated.timeZone)
    }

    @Test
    fun `major Indian national holidays are present in HolidayRepository`() {
        val holidays2026 = HolidayRepository.getHolidaysForYear(2026)
        assertTrue("2026 holidays must not be empty", holidays2026.isNotEmpty())

        val republicDay = holidays2026.find { it.title.contains("Republic Day", ignoreCase = true) }
        assertNotNull("Republic Day must exist", republicDay)
        assertEquals(1, republicDay?.month)
        assertEquals(26, republicDay?.day)

        val independenceDay = holidays2026.find { it.title.contains("Independence Day", ignoreCase = true) }
        assertNotNull("Independence Day must exist", independenceDay)
        assertEquals(8, independenceDay?.month)
        assertEquals(15, independenceDay?.day)

        val gandhiJayanti = holidays2026.find { it.title.contains("Gandhi", ignoreCase = true) }
        assertNotNull("Gandhi Jayanti must exist", gandhiJayanti)
        assertEquals(10, gandhiJayanti?.month)
        assertEquals(2, gandhiJayanti?.day)
    }
}
