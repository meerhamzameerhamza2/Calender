package com.example.data

import com.example.model.Holiday
import com.example.model.HolidayType
import java.util.Calendar

object IndianHolidays {

    // Dynamic holiday cache by year to maintain high performance
    private val holidayCache = mutableMapOf<Int, List<Holiday>>()

    /**
     * Dynamically generates all Indian National, Gazetted, and Festival holidays
     * for ANY given year using astronomical calculations, solar calendar,
     * ecclesiastical Computus (for Easter / Good Friday), and lunar models.
     */
    fun getHolidaysForYear(year: Int): List<Holiday> {
        holidayCache[year]?.let { return it }

        val holidays = mutableListOf<Holiday>()

        // --- 1. Fixed Solar & National Holidays ---
        holidays.add(Holiday(1, 1, year, "New Year's Day", "Observance", HolidayType.OBSERVANCE))
        holidays.add(Holiday(14, 1, year, "Makar Sankranti / Pongal", "Restricted Holiday", HolidayType.FESTIVAL))
        holidays.add(Holiday(26, 1, year, "Republic Day", "Gazetted Holiday", HolidayType.NATIONAL))
        holidays.add(Holiday(14, 4, year, "Dr. Ambedkar Jayanti", "Gazetted Holiday", HolidayType.NATIONAL))
        holidays.add(Holiday(1, 5, year, "May Day / Labor Day", "Restricted Holiday", HolidayType.OBSERVANCE))
        holidays.add(Holiday(15, 8, year, "Independence Day", "Gazetted Holiday", HolidayType.NATIONAL))
        holidays.add(Holiday(2, 10, year, "Mahatma Gandhi Jayanti", "Gazetted Holiday", HolidayType.NATIONAL))
        holidays.add(Holiday(25, 12, year, "Christmas Day", "Gazetted Holiday", HolidayType.FESTIVAL))
        holidays.add(Holiday(31, 12, year, "New Year's Eve", "Observance", HolidayType.OBSERVANCE))

        // --- 2. Good Friday & Easter (Astronomical Computus) ---
        val easter = calculateEaster(year)
        val goodFridayCal = Calendar.getInstance().apply {
            set(year, easter.month - 1, easter.day)
            add(Calendar.DAY_OF_MONTH, -2)
        }
        holidays.add(
            Holiday(
                day = goodFridayCal.get(Calendar.DAY_OF_MONTH),
                month = goodFridayCal.get(Calendar.MONTH) + 1,
                year = year,
                title = "Good Friday",
                category = "Gazetted Holiday",
                type = HolidayType.FESTIVAL
            )
        )

        // --- 3. Dynamic Islamic Lunar Holidays ---
        // Scan the year's days to find when Hijri lunar events occur
        val cal = Calendar.getInstance().apply {
            set(year, Calendar.JANUARY, 1, 12, 0, 0)
        }
        var foundEidFitr = false
        var foundEidAdha = false
        var foundMuharram = false
        var foundMilad = false

        val daysInYear = if (isLeapYear(year)) 366 else 365
        for (i in 0 until daysInYear) {
            val d = cal.get(Calendar.DAY_OF_MONTH)
            val m = cal.get(Calendar.MONTH) + 1
            val hijri = HijriCalendarHelper.gregorianToHijri(year, m, d, 0)

            // Eid ul-Fitr: 1 Shawwal (Month 10, Day 1)
            if (hijri.month == 10 && hijri.day == 1 && !foundEidFitr) {
                holidays.add(Holiday(d, m, year, "Eid-ul-Fitr (Ramzan Eid)", "Gazetted Holiday", HolidayType.FESTIVAL))
                foundEidFitr = true
            }
            // Eid al-Adha (Bakrid): 10 Dhu al-Hijjah (Month 12, Day 10)
            if (hijri.month == 12 && hijri.day == 10 && !foundEidAdha) {
                holidays.add(Holiday(d, m, year, "Bakrid / Eid al-Adha", "Gazetted Holiday", HolidayType.FESTIVAL))
                foundEidAdha = true
            }
            // Muharram / Ashura: 10 Muharram (Month 1, Day 10)
            if (hijri.month == 1 && hijri.day == 10 && !foundMuharram) {
                holidays.add(Holiday(d, m, year, "Muharram (Ashura)", "Gazetted Holiday", HolidayType.FESTIVAL))
                foundMuharram = true
            }
            // Milad-un-Nabi (Mawlid): 12 Rabi' al-Awwal (Month 3, Day 12)
            if (hijri.month == 3 && hijri.day == 12 && !foundMilad) {
                holidays.add(Holiday(d, m, year, "Milad-un-Nabi (Id-e-Milad)", "Gazetted Holiday", HolidayType.FESTIVAL))
                foundMilad = true
            }
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }

        // --- 4. Dynamic Hindu Lunar Festivals ---
        val hinduFestivals = calculateHinduFestivals(year)
        holidays.addAll(hinduFestivals)

        val sorted = holidays.distinctBy { "${it.year}_${it.month}_${it.day}_${it.title}" }
            .sortedWith(compareBy({ it.month }, { it.day }))
        holidayCache[year] = sorted
        return sorted
    }

    fun getHoliday(year: Int, month: Int, day: Int): Holiday? {
        val list = getHolidaysForYear(year)
        return list.find { it.year == year && it.month == month && it.day == day }
    }

    fun getHolidaysForMonth(year: Int, month: Int): List<Holiday> {
        return getHolidaysForYear(year).filter { it.month == month }
    }

    fun getAllHolidays(): List<Holiday> {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        return getHolidaysForYear(currentYear) + getHolidaysForYear(currentYear + 1)
    }

    private data class MonthDay(val month: Int, val day: Int)

    /**
     * Anonymous Gregorian algorithm for calculating Easter Sunday.
     */
    private fun calculateEaster(year: Int): MonthDay {
        val a = year % 19
        val b = year / 100
        val c = year % 100
        val d = b / 4
        val e = b % 4
        val f = (b + 8) / 25
        val g = (b - f + 1) / 3
        val h = (19 * a + b - d - g + 15) % 30
        val i = c / 4
        val k = c % 4
        val l = (32 + 2 * e + 2 * i - h - k) % 7
        val m = (a + 11 * h + 22 * l) / 451
        val month = (h + l - 7 * m + 114) / 31
        val day = ((h + l - 7 * m + 114) % 31) + 1
        return MonthDay(month, day)
    }

    /**
     * Calculates major Indian lunar festivals dynamically for ANY year using
     * celestial mechanics and astronomical phase calculations (Jean Meeus algorithm).
     * Works for any Gregorian year without relying on static year tables.
     */
    private fun calculateHinduFestivals(year: Int): List<Holiday> {
        val festivals = mutableListOf<Holiday>()

        // Find all New Moons (k integer) and Full Moons (k + 0.5) for the year
        val kStart = Math.floor((year - 2000) * 12.3685).toInt() - 1
        val newMoons = mutableListOf<PhaseDate>()
        val fullMoons = mutableListOf<PhaseDate>()

        for (i in 0..15) {
            val kNew = (kStart + i).toDouble()
            val nm = getPhaseDate(kNew)
            if (nm.year == year) {
                newMoons.add(nm)
            }

            val kFull = (kStart + i) + 0.5
            val fm = getPhaseDate(kFull)
            if (fm.year == year) {
                fullMoons.add(fm)
            }
        }

        // 1. Phalguna Purnima (Holi): Full Moon occurring in late Feb or March
        val holiFm = fullMoons.firstOrNull { it.month == 3 || (it.month == 2 && it.day >= 20) }
            ?: PhaseDate(year, 3, 15)

        val holiCal = Calendar.getInstance().apply {
            set(year, holiFm.month - 1, holiFm.day, 0, 0, 0)
        }

        // Maha Shivratri: Phalguna Krishna Chaturdashi (~16 days before Holi)
        val shivratriCal = Calendar.getInstance().apply {
            timeInMillis = holiCal.timeInMillis
            add(Calendar.DAY_OF_MONTH, -16)
        }
        festivals.add(
            Holiday(
                day = shivratriCal.get(Calendar.DAY_OF_MONTH),
                month = shivratriCal.get(Calendar.MONTH) + 1,
                year = year,
                title = "Maha Shivratri",
                category = "Gazetted Holiday",
                type = HolidayType.FESTIVAL
            )
        )

        // Holi: Phalguna Purnima
        festivals.add(
            Holiday(
                day = holiCal.get(Calendar.DAY_OF_MONTH),
                month = holiCal.get(Calendar.MONTH) + 1,
                year = year,
                title = "Holi",
                category = "Gazetted Holiday",
                type = HolidayType.FESTIVAL
            )
        )

        // Ugadi / Gudi Padwa: Chaitra Shukla Pratipada (New Moon after Holi + 1 day)
        val ugadiNm = newMoons.firstOrNull {
            val nmCal = Calendar.getInstance().apply { set(it.year, it.month - 1, it.day) }
            nmCal.timeInMillis > holiCal.timeInMillis
        }
        val ugadiCal = Calendar.getInstance().apply {
            if (ugadiNm != null) {
                set(ugadiNm.year, ugadiNm.month - 1, ugadiNm.day)
                add(Calendar.DAY_OF_MONTH, 1)
            } else {
                timeInMillis = holiCal.timeInMillis
                add(Calendar.DAY_OF_MONTH, 16)
            }
        }
        festivals.add(
            Holiday(
                day = ugadiCal.get(Calendar.DAY_OF_MONTH),
                month = ugadiCal.get(Calendar.MONTH) + 1,
                year = year,
                title = "Ugadi / Gudi Padwa",
                category = "Restricted Holiday",
                type = HolidayType.FESTIVAL
            )
        )

        // Ram Navami: Chaitra Shukla Navami (8 days after Ugadi)
        val ramNavamiCal = Calendar.getInstance().apply {
            timeInMillis = ugadiCal.timeInMillis
            add(Calendar.DAY_OF_MONTH, 8)
        }
        festivals.add(
            Holiday(
                day = ramNavamiCal.get(Calendar.DAY_OF_MONTH),
                month = ramNavamiCal.get(Calendar.MONTH) + 1,
                year = year,
                title = "Ram Navami",
                category = "Restricted Holiday",
                type = HolidayType.FESTIVAL
            )
        )

        // 2. Shravana Purnima (Raksha Bandhan): Full Moon in August
        val rakshaFm = fullMoons.firstOrNull { it.month == 8 }
            ?: fullMoons.firstOrNull { it.month == 9 && it.day <= 5 }
            ?: PhaseDate(year, 8, 20)

        val rakshaCal = Calendar.getInstance().apply {
            set(year, rakshaFm.month - 1, rakshaFm.day, 0, 0, 0)
        }
        festivals.add(
            Holiday(
                day = rakshaCal.get(Calendar.DAY_OF_MONTH),
                month = rakshaCal.get(Calendar.MONTH) + 1,
                year = year,
                title = "Raksha Bandhan",
                category = "Restricted Holiday",
                type = HolidayType.FESTIVAL
            )
        )

        // Janmashtami: Bhadrapada Krishna Ashtami (7-8 days after Raksha Bandhan)
        val janmashtamiCal = Calendar.getInstance().apply {
            timeInMillis = rakshaCal.timeInMillis
            add(Calendar.DAY_OF_MONTH, 7)
        }
        festivals.add(
            Holiday(
                day = janmashtamiCal.get(Calendar.DAY_OF_MONTH),
                month = janmashtamiCal.get(Calendar.MONTH) + 1,
                year = year,
                title = "Janmashtami",
                category = "Restricted Holiday",
                type = HolidayType.FESTIVAL
            )
        )

        // Ganesh Chaturthi: Bhadrapada Shukla Chaturthi (approx 18-19 days after Raksha Bandhan)
        val ganeshCal = Calendar.getInstance().apply {
            timeInMillis = rakshaCal.timeInMillis
            add(Calendar.DAY_OF_MONTH, 18)
        }
        festivals.add(
            Holiday(
                day = ganeshCal.get(Calendar.DAY_OF_MONTH),
                month = ganeshCal.get(Calendar.MONTH) + 1,
                year = year,
                title = "Ganesh Chaturthi",
                category = "Restricted Holiday",
                type = HolidayType.FESTIVAL
            )
        )

        // 3. Kartika Amavasya (Diwali): New Moon occurring between mid-October and mid-November
        val diwaliNm = newMoons.firstOrNull {
            (it.month == 10 && it.day >= 15) || (it.month == 11 && it.day <= 18)
        } ?: PhaseDate(year, 11, 1)

        val diwaliCal = Calendar.getInstance().apply {
            set(year, diwaliNm.month - 1, diwaliNm.day, 0, 0, 0)
        }

        // Dussehra (Vijayadashami): Ashwin Shukla Dashami (20 days before Diwali)
        val dussehraCal = Calendar.getInstance().apply {
            timeInMillis = diwaliCal.timeInMillis
            add(Calendar.DAY_OF_MONTH, -20)
        }
        festivals.add(
            Holiday(
                day = dussehraCal.get(Calendar.DAY_OF_MONTH),
                month = dussehraCal.get(Calendar.MONTH) + 1,
                year = year,
                title = "Dussehra (Vijayadashami)",
                category = "Gazetted Holiday",
                type = HolidayType.FESTIVAL
            )
        )

        // Diwali (Deepavali): Kartika Amavasya
        festivals.add(
            Holiday(
                day = diwaliCal.get(Calendar.DAY_OF_MONTH),
                month = diwaliCal.get(Calendar.MONTH) + 1,
                year = year,
                title = "Diwali (Deepavali)",
                category = "Gazetted Holiday",
                type = HolidayType.FESTIVAL
            )
        )

        // Bhai Dooj: Kartika Shukla Dwitiya (2 days after Diwali)
        val bhaiDoojCal = Calendar.getInstance().apply {
            timeInMillis = diwaliCal.timeInMillis
            add(Calendar.DAY_OF_MONTH, 2)
        }
        festivals.add(
            Holiday(
                day = bhaiDoojCal.get(Calendar.DAY_OF_MONTH),
                month = bhaiDoojCal.get(Calendar.MONTH) + 1,
                year = year,
                title = "Bhai Dooj",
                category = "Restricted Holiday",
                type = HolidayType.FESTIVAL
            )
        )

        // Guru Nanak Jayanti: Kartika Purnima (Full Moon ~15-16 days after Diwali)
        val guruNanakFm = fullMoons.firstOrNull {
            val fmCal = Calendar.getInstance().apply { set(it.year, it.month - 1, it.day) }
            val diff = fmCal.timeInMillis - diwaliCal.timeInMillis
            diff in (12L * 86400000L)..(17L * 86400000L)
        }
        val guruNanakCal = Calendar.getInstance().apply {
            if (guruNanakFm != null) {
                set(guruNanakFm.year, guruNanakFm.month - 1, guruNanakFm.day)
            } else {
                timeInMillis = diwaliCal.timeInMillis
                add(Calendar.DAY_OF_MONTH, 15)
            }
        }
        festivals.add(
            Holiday(
                day = guruNanakCal.get(Calendar.DAY_OF_MONTH),
                month = guruNanakCal.get(Calendar.MONTH) + 1,
                year = year,
                title = "Guru Nanak Jayanti",
                category = "Gazetted Holiday",
                type = HolidayType.FESTIVAL
            )
        )

        return festivals
    }

    private data class PhaseDate(val year: Int, val month: Int, val day: Int)

    /**
     * Jean Meeus Astronomical Ephemeris algorithm for lunar phase dates.
     * k is the lunation index (integer for New Moon, integer + 0.5 for Full Moon).
     */
    private fun getPhaseDate(k: Double): PhaseDate {
        val T = k / 1236.85
        val T2 = T * T
        val T3 = T2 * T
        val T4 = T3 * T

        var jde = 2451550.09766 + 29.530588861 * k + 0.00015437 * T2 - 0.000000150 * T3 + 0.00000000073 * T4

        val M = Math.toRadians((2.5534 + 29.10535670 * k - 0.0000014 * T2 - 0.00000011 * T3) % 360.0)
        val Mp = Math.toRadians((201.5643 + 385.81693528 * k + 0.0107588 * T2 + 0.00001238 * T3) % 360.0)
        val F = Math.toRadians((160.7108 + 390.67050284 * k - 0.0016118 * T2 - 0.00000227 * T3) % 360.0)
        val Om = Math.toRadians((124.7746 - 1.56375588 * k + 0.0020672 * T2) % 360.0)

        val E = 1.0 - 0.002516 * T - 0.0000074 * T2
        val isFull = (Math.abs(k - Math.round(k)) > 0.25)

        val dJde = if (!isFull) {
            -0.40720 * Math.sin(Mp) +
            0.17241 * E * Math.sin(M) +
            0.01608 * Math.sin(2 * Mp) +
            0.01039 * Math.sin(2 * F) +
            0.00739 * E * Math.sin(Mp - M) -
            0.00514 * E * Math.sin(Mp + M) +
            0.00208 * E * E * Math.sin(2 * M) -
            0.00111 * Math.sin(Mp - 2 * F) -
            0.00057 * Math.sin(Mp + 2 * F) +
            0.00056 * E * Math.sin(2 * Mp + M) -
            0.00042 * Math.sin(3 * Mp) +
            0.00042 * E * Math.sin(M + 2 * F) +
            0.00038 * E * Math.sin(M - 2 * F) -
            0.00024 * E * Math.sin(2 * Mp - M) -
            0.00017 * Math.sin(Om)
        } else {
            -0.40614 * Math.sin(Mp) +
            0.17302 * E * Math.sin(M) +
            0.01614 * Math.sin(2 * Mp) +
            0.01043 * Math.sin(2 * F) +
            0.00734 * E * Math.sin(Mp - M) -
            0.00515 * E * Math.sin(Mp + M) +
            0.00209 * E * E * Math.sin(2 * M) -
            0.00111 * Math.sin(Mp - 2 * F) -
            0.00057 * Math.sin(Mp + 2 * F) +
            0.00056 * E * Math.sin(2 * Mp + M) -
            0.00042 * Math.sin(3 * Mp) +
            0.00042 * E * Math.sin(M + 2 * F) +
            0.00038 * E * Math.sin(M - 2 * F) -
            0.00024 * E * Math.sin(2 * Mp - M) -
            0.00017 * Math.sin(Om)
        }

        jde += dJde
        // Convert Julian Ephemeris Day to IST (UTC + 5:30)
        val jdIst = jde + 0.5 + (5.5 / 24.0)

        val z = Math.floor(jdIst).toLong()
        val alpha = Math.floor((z - 1867216.25) / 36524.25).toLong()
        val a = z + 1 + alpha - Math.floor(alpha / 4.0).toLong()
        val b = a + 1524
        val c = Math.floor((b - 122.1) / 365.25).toLong()
        val d = Math.floor(365.25 * c).toLong()
        val e = Math.floor((b - d) / 30.6001).toLong()

        val day = (b - d - Math.floor(30.6001 * e)).toInt()
        val month = (if (e < 14) e - 1 else e - 13).toInt()
        val yearRes = (if (month > 2) c - 4716 else c - 4715).toInt()

        return PhaseDate(yearRes, month, day)
    }

    private fun isLeapYear(year: Int): Boolean {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
    }
}
