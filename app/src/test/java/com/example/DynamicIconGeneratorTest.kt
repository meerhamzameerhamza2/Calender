package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.util.DynamicIconBitmapGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DynamicIconGeneratorTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun testDateKeyFormatting_LeapYearFeb28AndFeb29() {
        val calLeap = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2024)
            set(Calendar.MONTH, Calendar.FEBRUARY)
            set(Calendar.DAY_OF_MONTH, 28)
        }
        assertEquals("2024-02-28", DynamicIconBitmapGenerator.getDateKey(calLeap))

        // Feb 29 leap year
        calLeap.add(Calendar.DAY_OF_MONTH, 1)
        assertEquals(29, calLeap.get(Calendar.DAY_OF_MONTH))
        assertEquals("2024-02-29", DynamicIconBitmapGenerator.getDateKey(calLeap))

        // Non leap year 2025: Feb 28 -> Mar 1
        val calNonLeap = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2025)
            set(Calendar.MONTH, Calendar.FEBRUARY)
            set(Calendar.DAY_OF_MONTH, 28)
        }
        assertEquals("2025-02-28", DynamicIconBitmapGenerator.getDateKey(calNonLeap))
        calNonLeap.add(Calendar.DAY_OF_MONTH, 1)
        assertEquals(Calendar.MARCH, calNonLeap.get(Calendar.MONTH))
        assertEquals(1, calNonLeap.get(Calendar.DAY_OF_MONTH))
        assertEquals("2025-03-01", DynamicIconBitmapGenerator.getDateKey(calNonLeap))
    }

    @Test
    fun testDateKeyFormatting_30And31DayMonths() {
        // April (30 days) -> May 1
        val calApril = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.APRIL)
            set(Calendar.DAY_OF_MONTH, 30)
        }
        assertEquals("2026-04-30", DynamicIconBitmapGenerator.getDateKey(calApril))
        calApril.add(Calendar.DAY_OF_MONTH, 1)
        assertEquals(Calendar.MAY, calApril.get(Calendar.MONTH))
        assertEquals(1, calApril.get(Calendar.DAY_OF_MONTH))
        assertEquals("2026-05-01", DynamicIconBitmapGenerator.getDateKey(calApril))

        // May (31 days) -> June 1
        val calMay = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.MAY)
            set(Calendar.DAY_OF_MONTH, 31)
        }
        assertEquals("2026-05-31", DynamicIconBitmapGenerator.getDateKey(calMay))
        calMay.add(Calendar.DAY_OF_MONTH, 1)
        assertEquals(Calendar.JUNE, calMay.get(Calendar.MONTH))
        assertEquals(1, calMay.get(Calendar.DAY_OF_MONTH))
        assertEquals("2026-06-01", DynamicIconBitmapGenerator.getDateKey(calMay))
    }

    @Test
    fun testDateKeyFormatting_YearRolloverDec31ToJan1() {
        val calYearEnd = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.DECEMBER)
            set(Calendar.DAY_OF_MONTH, 31)
        }
        assertEquals("2026-12-31", DynamicIconBitmapGenerator.getDateKey(calYearEnd))

        // Rollover to new year
        calYearEnd.add(Calendar.DAY_OF_MONTH, 1)
        assertEquals(2027, calYearEnd.get(Calendar.YEAR))
        assertEquals(Calendar.JANUARY, calYearEnd.get(Calendar.MONTH))
        assertEquals(1, calYearEnd.get(Calendar.DAY_OF_MONTH))
        assertEquals("2027-01-01", DynamicIconBitmapGenerator.getDateKey(calYearEnd))
    }

    @Test
    fun testBitmapGenerationAndCaching() {
        val testCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.OCTOBER)
            set(Calendar.DAY_OF_MONTH, 5) // Monday Oct 5
        }

        val bitmap = DynamicIconBitmapGenerator.generateIconBitmap(context, testCal, size = 192)
        assertNotNull(bitmap)
        assertEquals(192, bitmap.width)
        assertEquals(192, bitmap.height)

        val cachedFile = DynamicIconBitmapGenerator.cacheIconAsWebp(context, testCal, forceRegenerate = true)
        assertNotNull(cachedFile)
        assertTrue(cachedFile!!.exists())
        assertTrue(cachedFile.length() > 0)
    }
}
