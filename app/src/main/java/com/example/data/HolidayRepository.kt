package com.example.data

import android.util.Log
import com.example.model.Holiday
import com.example.model.HolidayType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

object HolidayRepository {
    private const val TAG = "HolidayRepository"

    private val _holidaysByYear = MutableStateFlow<Map<Int, List<Holiday>>>(emptyMap())
    val holidaysByYear: StateFlow<Map<Int, List<Holiday>>> = _holidaysByYear.asStateFlow()

    private val cachedYears = mutableSetOf<Int>()

    /**
     * Retrieves holidays for a given year. If not already fetched from the API,
     * returns local calculated holidays immediately and triggers background fetch.
     */
    fun getHolidaysForYear(year: Int): List<Holiday> {
        val dynamicList = _holidaysByYear.value[year]
        if (dynamicList != null && dynamicList.isNotEmpty()) {
            return dynamicList
        }
        return IndianHolidays.getHolidaysForYear(year)
    }

    fun getHolidaysForMonth(year: Int, month: Int): List<Holiday> {
        return getHolidaysForYear(year).filter { it.month == month }
    }

    fun getHoliday(year: Int, month: Int, day: Int): Holiday? {
        return getHolidaysForYear(year).firstOrNull { it.month == month && it.day == day }
    }

    /**
     * Asynchronously loads dynamic holidays, Jayantis, birth anniversaries, and observances
     * from global and regional public APIs.
     */
    suspend fun loadHolidays(year: Int) {
        if (cachedYears.contains(year) && (_holidaysByYear.value[year]?.isNotEmpty() == true)) {
            return
        }

        withContext(Dispatchers.IO) {
            try {
                val apiHolidays = fetchFromApi(year)
                val localHolidays = IndianHolidays.getHolidaysForYear(year)

                // Merge API holidays with local holidays, prioritizing API's rich Jayanti / Anniversary info
                val mergedMap = mutableMapOf<String, Holiday>()

                // Add local calculated holidays first
                for (h in localHolidays) {
                    val key = "${h.year}-${h.month}-${h.day}-${h.title.lowercase(Locale.ROOT)}"
                    mergedMap[key] = h
                }

                // Add or enrich with dynamic API holidays
                for (h in apiHolidays) {
                    val key = "${h.year}-${h.month}-${h.day}-${h.title.lowercase(Locale.ROOT)}"
                    mergedMap[key] = h
                }

                val sortedList = mergedMap.values.sortedWith(
                    compareBy({ it.month }, { it.day })
                )

                if (sortedList.isNotEmpty()) {
                    cachedYears.add(year)
                    val updated = _holidaysByYear.value.toMutableMap()
                    updated[year] = sortedList
                    _holidaysByYear.value = updated
                    Log.d(TAG, "Successfully loaded ${sortedList.size} dynamic holidays for year $year")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching dynamic holidays for year $year: ${e.message}")
            }
        }
    }

    private fun fetchFromApi(year: Int): List<Holiday> {
        val result = mutableListOf<Holiday>()
        val urlString = "https://jayantur13.github.io/calendar-bharat/calendar/$year.json"
        var conn: HttpURLConnection? = null

        try {
            val url = URL(urlString)
            conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android Calendar App)")

            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val sb = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    sb.append(line)
                }
                reader.close()

                val rootObj = JSONObject(sb.toString())
                val yearKey = year.toString()
                if (rootObj.has(yearKey)) {
                    val yearObj = rootObj.getJSONObject(yearKey)
                    val monthKeys = yearObj.keys()
                    while (monthKeys.hasNext()) {
                        val monthNameKey = monthKeys.next()
                        val monthObj = yearObj.getJSONObject(monthNameKey)
                        val dateKeys = monthObj.keys()

                        while (dateKeys.hasNext()) {
                            val dateStringKey = dateKeys.next() // e.g. "January 1, 2026, Thursday"
                            val eventObj = monthObj.getJSONObject(dateStringKey)

                            val eventTitle = eventObj.optString("event", "").trim()
                            val eventType = eventObj.optString("type", "").trim()
                            val eventExtras = eventObj.optString("extras", "").trim()

                            val parsedDate = parseDateKey(dateStringKey, year)
                            if (parsedDate != null && eventTitle.isNotEmpty()) {
                                val (d, m) = parsedDate
                                val (cat, hType) = determineCategoryAndType(eventTitle, eventType, eventExtras)
                                result.add(
                                    Holiday(
                                        day = d,
                                        month = m,
                                        year = year,
                                        title = eventTitle,
                                        category = cat,
                                        type = hType
                                    )
                                )
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "API fetch warning: ${e.message}")
        } finally {
            conn?.disconnect()
        }

        return result
    }

    private fun parseDateKey(key: String, fallbackYear: Int): Pair<Int, Int>? {
        try {
            // Format example: "January 1, 2026, Thursday" or "September 11, 2026, Friday"
            val commaParts = key.split(",")
            if (commaParts.isEmpty()) return null
            val monthAndDay = commaParts[0].trim().split(" ")
            if (monthAndDay.size < 2) return null

            val monthName = monthAndDay[0].trim()
            val day = monthAndDay[1].trim().toIntOrNull() ?: return null
            val month = monthNameToNumber(monthName) ?: return null

            return Pair(day, month)
        } catch (e: Exception) {
            return null
        }
    }

    private fun monthNameToNumber(name: String): Int? {
        return when (name.lowercase(Locale.ROOT)) {
            "january", "jan" -> 1
            "february", "feb" -> 2
            "march", "mar" -> 3
            "april", "apr" -> 4
            "may" -> 5
            "june", "jun" -> 6
            "july", "jul" -> 7
            "august", "aug" -> 8
            "september", "sep", "sept" -> 9
            "october", "oct" -> 10
            "november", "nov" -> 11
            "december", "dec" -> 12
            else -> null
        }
    }

    private fun determineCategoryAndType(title: String, type: String, extras: String): Pair<String, HolidayType> {
        val lower = title.lowercase(Locale.ROOT)
        return when {
            lower.contains("jayanti") -> {
                Pair("Jayanti", HolidayType.JAYANTI)
            }
            lower.contains("birthday") || lower.contains("birth anniversary") || lower.contains("janmotsav") -> {
                Pair("Birth Anniversary", HolidayType.BIRTH_ANNIVERSARY)
            }
            lower.contains("republic day") || lower.contains("independence day") || lower.contains("gandhi jayanti") -> {
                Pair("Gazetted National Holiday", HolidayType.NATIONAL)
            }
            type.contains("Religional", ignoreCase = true) || type.contains("Festival", ignoreCase = true) -> {
                Pair("Religious Festival", HolidayType.FESTIVAL)
            }
            type.contains("Gazetted", ignoreCase = true) -> {
                Pair("Gazetted Holiday", HolidayType.NATIONAL)
            }
            type.contains("Restricted", ignoreCase = true) -> {
                Pair("Restricted Holiday", HolidayType.FESTIVAL)
            }
            else -> {
                Pair("Observance / Special Day", HolidayType.OBSERVANCE)
            }
        }
    }
}
