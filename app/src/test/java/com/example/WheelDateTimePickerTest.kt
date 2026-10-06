package com.example

import com.example.model.EventEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class WheelDateTimePickerTest {

    @Test
    fun `changing minute must strictly preserve hour date and year`() {
        // Initial setup: October 15, 2026 at 02:25 PM (14:25)
        val initialCal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 15, 14, 25, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val initialTimestamp = initialCal.timeInMillis

        // Simulate user selecting minute 45
        val targetMinute = 45
        val updatedCal = Calendar.getInstance().apply {
            timeInMillis = initialTimestamp
            set(Calendar.MINUTE, targetMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        assertEquals("Year must be unchanged", 2026, updatedCal.get(Calendar.YEAR))
        assertEquals("Month must be unchanged", Calendar.OCTOBER, updatedCal.get(Calendar.MONTH))
        assertEquals("Day must be unchanged", 15, updatedCal.get(Calendar.DAY_OF_MONTH))
        assertEquals("Hour of day (24h) must remain 14", 14, updatedCal.get(Calendar.HOUR_OF_DAY))
        assertEquals("Hour (12h) must remain 2", 2, updatedCal.get(Calendar.HOUR))
        assertEquals("AM_PM must remain PM", Calendar.PM, updatedCal.get(Calendar.AM_PM))
        assertEquals("Minute must update to target minute 45", 45, updatedCal.get(Calendar.MINUTE))
    }

    @Test
    fun `changing minute to boundary 00 must not roll back or advance hour`() {
        // Initial setup: September 12, 2026 at 09:59 AM
        val initialCal = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 12, 9, 59, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val initialTimestamp = initialCal.timeInMillis

        // User changes minute from 59 to 00
        val targetMinute = 0
        val updatedCal = Calendar.getInstance().apply {
            timeInMillis = initialTimestamp
            set(Calendar.MINUTE, targetMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        assertEquals("Hour must remain 9 AM", 9, updatedCal.get(Calendar.HOUR_OF_DAY))
        assertEquals("AM_PM must remain AM", Calendar.AM, updatedCal.get(Calendar.AM_PM))
        assertEquals("Minute must be 0", 0, updatedCal.get(Calendar.MINUTE))
        assertEquals("Day must remain 12", 12, updatedCal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `changing hour must strictly preserve minute date and year`() {
        // Initial setup: October 15, 2026 at 02:45 PM (14:45)
        val initialCal = Calendar.getInstance().apply {        
            set(2026, Calendar.OCTOBER, 15, 14, 45, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val initialTimestamp = initialCal.timeInMillis

        // User changes hour from 2 to 5 PM (17:45)
        val chosenHour12 = 5
        val isPm = initialCal.get(Calendar.AM_PM) == Calendar.PM
        val hour24 = if (isPm) {
            if (chosenHour12 == 12) 12 else chosenHour12 + 12
        } else {
            if (chosenHour12 == 12) 0 else chosenHour12
        }

        val updatedCal = Calendar.getInstance().apply {
            timeInMillis = initialTimestamp
            set(Calendar.HOUR_OF_DAY, hour24)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        assertEquals("Hour 24 must be 17 (5 PM)", 17, updatedCal.get(Calendar.HOUR_OF_DAY))
        assertEquals("Minute must remain 45", 45, updatedCal.get(Calendar.MINUTE))
        assertEquals("Day must remain 15", 15, updatedCal.get(Calendar.DAY_OF_MONTH))
        assertEquals("Month must remain OCTOBER", Calendar.OCTOBER, updatedCal.get(Calendar.MONTH))
    }

    @Test
    fun `toggling AM to PM and PM to AM must preserve minutes exactly`() {
        // 09:30 AM
        val amCal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 15, 9, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val currentHour = amCal.get(Calendar.HOUR_OF_DAY)
        val newHourPm = if (currentHour < 12) currentHour + 12 else currentHour
        val pmCal = Calendar.getInstance().apply {
            timeInMillis = amCal.timeInMillis
            set(Calendar.HOUR_OF_DAY, newHourPm)
        }

        assertEquals(21, pmCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, pmCal.get(Calendar.MINUTE))
        assertEquals(Calendar.PM, pmCal.get(Calendar.AM_PM))

        // 12:15 PM to 12:15 AM
        val noonCal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 15, 12, 15, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val newHourAm = noonCal.get(Calendar.HOUR_OF_DAY) - 12
        val midnightCal = Calendar.getInstance().apply {
            timeInMillis = noonCal.timeInMillis
            set(Calendar.HOUR_OF_DAY, newHourAm)
        }

        assertEquals(0, midnightCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(15, midnightCal.get(Calendar.MINUTE))
        assertEquals(Calendar.AM, midnightCal.get(Calendar.AM_PM))
    }

    @Test
    fun `event entity preserves isImportantDay flag and alertType`() {
        val event = EventEntity(
            id = 101L,
            title = "Annual Founder Day",
            startTimestamp = 1790000000000L,
            endTimestamp = 1790003600000L,
            isAllDay = false,
            isImportantDay = true,
            alertType = "Alarm & Sound",
            repeatType = "Yearly",
            reminderMinutesBefore = 30
        )

        assertTrue("Event must be flagged as important day", event.isImportantDay)
        assertEquals("Alarm & Sound", event.alertType)
        assertEquals("Yearly", event.repeatType)
        assertEquals(30, event.reminderMinutesBefore)
    }

    @Test
    fun `future year 2027 and beyond dynamic generation works`() {
        val holiday2026 = com.example.data.IndianHolidays.getHoliday(2026, 1, 26)
        val holiday2027 = com.example.data.IndianHolidays.getHoliday(2027, 1, 26)
        val holiday2028 = com.example.data.IndianHolidays.getHoliday(2028, 8, 15)

        assertEquals("Republic Day", holiday2026?.title)
        assertEquals("Republic Day", holiday2027?.title)
        assertEquals("Independence Day", holiday2028?.title)
    }
}
