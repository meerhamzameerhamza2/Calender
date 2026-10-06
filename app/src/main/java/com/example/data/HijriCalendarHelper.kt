package com.example.data

import java.util.Calendar

data class HijriDate(
    val day: Int,
    val month: Int,
    val year: Int,
    val monthName: String
)

object HijriCalendarHelper {

    private val hijriMonths = listOf(
        "Muharram", "Safar", "Rabi' I", "Rabi' II", "Jumada I", "Jumada II",
        "Rajab", "Sha'ban", "Ramadan", "Shawwal", "Dhu al-Qi'dah", "Dhu al-Hijjah"
    )

    fun getHijriMonthName(monthNumber: Int): String {
        return hijriMonths.getOrElse(monthNumber - 1) { "Hijri" }
    }

    /**
     * Converts a Gregorian date into Hijri (Islamic) date using the standard Umm al-Qura algorithmic conversion.
     * Applies ramadanAdjustment offset in days.
     */
    fun gregorianToHijri(year: Int, month: Int, day: Int, adjustmentDays: Int = 0): HijriDate {
        val cal = Calendar.getInstance().apply {
            set(year, month - 1, day, 12, 0, 0)
            if (adjustmentDays != 0) {
                add(Calendar.DAY_OF_MONTH, adjustmentDays)
            }
        }

        val y = cal.get(Calendar.YEAR)
        val m = cal.get(Calendar.MONTH) + 1
        val d = cal.get(Calendar.DAY_OF_MONTH)

        // Julian Day calculation
        val a = (14 - m) / 12
        val yPrime = y + 4800 - a
        val mPrime = m + 12 * a - 3
        val jdn = d + (153 * mPrime + 2) / 5 + 365 * yPrime + yPrime / 4 - yPrime / 100 + yPrime / 400 - 32045

        // Hijri calculation from JDN
        val l = jdn - 1948440 + 10632
        val n = (l - 1) / 10631
        val l2 = l - 10631 * n + 354
        val j = ((10985 - l2) / 5316) * ((50 * l2) / 17719) + (l2 / 5670) * ((43 * l2) / 15238)
        val l3 = l2 - ((30 - j) / 15) * ((17719 * j) / 50) - (j / 16) * ((15238 * j) / 43) + 29
        val hMonth = (24 * l3) / 709
        val hDay = l3 - (709 * hMonth) / 24
        val hYear = 30 * n + j - 30

        val normalizedMonth = ((hMonth - 1) % 12 + 12) % 12 + 1
        val monthName = hijriMonths.getOrElse(normalizedMonth - 1) { "Hijri" }

        return HijriDate(
            day = hDay.coerceIn(1, 30),
            month = normalizedMonth,
            year = hYear,
            monthName = monthName
        )
    }
}
