package com.example.data

import java.util.Calendar

data class HinduPanchang(
    val monthName: String,
    val tithiName: String,
    val vikramSamvat: Int,
    val sakaYear: Int,
    val paksha: String
)

object HinduCalendarHelper {

    private val hinduMonths = listOf(
        "Chaitra", "Vaishakha", "Jyeshtha", "Ashadha", "Shravana", "Bhadrapada",
        "Ashwin", "Kartika", "Margashirsha", "Pausha", "Magha", "Phalguna"
    )

    private val tithis = listOf(
        "Pratipada", "Dwitiya", "Tritiya", "Chaturthi", "Panchami",
        "Shashti", "Saptami", "Ashtami", "Navami", "Dashami",
        "Ekadashi", "Dwadashi", "Trayodashi", "Chaturdashi", "Purnima",
        "Pratipada", "Dwitiya", "Tritiya", "Chaturthi", "Panchami",
        "Shashti", "Saptami", "Ashtami", "Navami", "Dashami",
        "Ekadashi", "Dwadashi", "Trayodashi", "Chaturdashi", "Amavasya"
    )

    fun getHinduPanchang(year: Int, month: Int, day: Int): HinduPanchang {
        val cal = Calendar.getInstance().apply {
            set(year, month - 1, day, 12, 0, 0)
        }
        val dayOfYear = cal.get(Calendar.DAY_OF_YEAR)

        // Vikram Samvat is Gregorian + 57 (approx)
        val vikramSamvat = if (month > 3 || (month == 3 && day >= 22)) year + 57 else year + 56
        // Saka Era is Gregorian - 78
        val sakaYear = if (month > 3 || (month == 3 && day >= 22)) year - 78 else year - 79

        // Approximate Hindu Lunar Month (starts around Chaitra / mid-March)
        val offsetDays = (dayOfYear + 285) % 365
        val monthIndex = ((offsetDays / 29.53).toInt()) % 12
        val hinduMonth = hinduMonths[monthIndex]

        // Moon phase / Tithi approximation (synodic month is 29.53 days)
        val tithiIndex = ((offsetDays % 29.53).toInt()).coerceIn(0, 29)
        val tithiName = tithis[tithiIndex]
        val paksha = if (tithiIndex < 15) "Shukla" else "Krishna"

        return HinduPanchang(
            monthName = hinduMonth,
            tithiName = tithiName,
            vikramSamvat = vikramSamvat,
            sakaYear = sakaYear,
            paksha = paksha
        )
    }
}
