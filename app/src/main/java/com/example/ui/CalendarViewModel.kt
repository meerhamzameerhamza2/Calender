package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.HijriCalendarHelper
import com.example.data.HinduCalendarHelper
import com.example.data.HolidayRepository
import com.example.data.IndianHolidays
import com.example.model.CalendarDayModel
import com.example.model.CalendarSettings
import com.example.model.EventEntity
import com.example.model.Holiday
import com.example.notification.NotificationHelper
import com.example.widget.DynamicCalendarWidgetProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

enum class CalendarViewMode {
    MONTH,
    YEAR,
    WEEK,
    AGENDA,
    IMPORTANT_DAYS
}

class CalendarViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val eventDao = db.eventDao()

    private val todayCal = Calendar.getInstance()
    val todayYear = todayCal.get(Calendar.YEAR)
    val todayMonth = todayCal.get(Calendar.MONTH) + 1
    val todayDay = todayCal.get(Calendar.DAY_OF_MONTH)

    private val _viewYear = MutableStateFlow(todayYear)
    val viewYear: StateFlow<Int> = _viewYear.asStateFlow()

    private val _viewMonth = MutableStateFlow(todayMonth)
    val viewMonth: StateFlow<Int> = _viewMonth.asStateFlow()

    private val _selectedYear = MutableStateFlow(todayYear)
    val selectedYear: StateFlow<Int> = _selectedYear.asStateFlow()

    private val _selectedMonth = MutableStateFlow(todayMonth)
    val selectedMonth: StateFlow<Int> = _selectedMonth.asStateFlow()

    private val _selectedDay = MutableStateFlow(todayDay)
    val selectedDay: StateFlow<Int> = _selectedDay.asStateFlow()

    init {
        // Fetch dynamic global and regional holidays, Jayantis, and birth anniversaries asynchronously
        viewModelScope.launch {
            HolidayRepository.loadHolidays(todayYear - 1)
            HolidayRepository.loadHolidays(todayYear)
            HolidayRepository.loadHolidays(todayYear + 1)
            HolidayRepository.loadHolidays(todayYear + 2)
        }
        viewModelScope.launch {
            _viewYear.collect { yr ->
                HolidayRepository.loadHolidays(yr - 1)
                HolidayRepository.loadHolidays(yr)
                HolidayRepository.loadHolidays(yr + 1)
            }
        }
    }

    private val _viewMode = MutableStateFlow(CalendarViewMode.MONTH)
    val viewMode: StateFlow<CalendarViewMode> = _viewMode.asStateFlow()

    private val _settings = MutableStateFlow(CalendarSettings())
    val settings: StateFlow<CalendarSettings> = _settings.asStateFlow()

    val allEvents: StateFlow<List<EventEntity>> = eventDao.getAllEvents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val importantDays: StateFlow<List<EventEntity>> = eventDao.getImportantDays()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Days grid for current viewYear and viewMonth
    val daysForViewMonth: StateFlow<List<CalendarDayModel>> = combine(
        _viewYear,
        _viewMonth,
        _selectedYear,
        _selectedMonth,
        _selectedDay,
        allEvents,
        _settings,
        HolidayRepository.holidaysByYear
    ) { params ->
        val vYear = params[0] as Int
        val vMonth = params[1] as Int
        val sYear = params[2] as Int
        val sMonth = params[3] as Int
        val sDay = params[4] as Int
        @Suppress("UNCHECKED_CAST")
        val evs = params[5] as List<EventEntity>
        val currentSettings = params[6] as CalendarSettings

        generateDaysForMonth(
            vYear, vMonth,
            sYear, sMonth, sDay,
            todayYear, todayMonth, todayDay,
            evs, currentSettings
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Days for the selected 7-day week (used in collapsed 1-week row and week view)
    val daysForSelectedWeek: StateFlow<List<CalendarDayModel>> = combine(
        _selectedYear,
        _selectedMonth,
        _selectedDay,
        allEvents,
        _settings,
        HolidayRepository.holidaysByYear
    ) { params ->
        val sYear = params[0] as Int
        val sMonth = params[1] as Int
        val sDay = params[2] as Int
        @Suppress("UNCHECKED_CAST")
        val evs = params[3] as List<EventEntity>
        val currentSettings = params[4] as CalendarSettings

        generateDaysForWeek(
            sYear, sMonth, sDay,
            todayYear, todayMonth, todayDay,
            evs, currentSettings
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Summary for the currently selected date: e.g., "Mon, 14 Sept, 4 days later" or "Today"
    val selectedDateSummary: StateFlow<String> = combine(
        _selectedYear,
        _selectedMonth,
        _selectedDay
    ) { y, m, d ->
        formatSelectedDateSummary(y, m, d, todayYear, todayMonth, todayDay)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    // Events on the currently selected date (supports recurring events across all years)
    val selectedDateEvents: StateFlow<List<EventEntity>> = combine(
        _selectedYear,
        _selectedMonth,
        _selectedDay,
        allEvents
    ) { y, m, d, evs ->
        evs.filter { event ->
            doesEventOccurOnDate(event, y, m, d)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Holiday on the currently selected date (if any)
    val selectedDateHoliday: StateFlow<Holiday?> = combine(
        _selectedYear,
        _selectedMonth,
        _selectedDay,
        HolidayRepository.holidaysByYear
    ) { y, m, d, _ ->
        HolidayRepository.getHoliday(y, m, d)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Holidays in the currently viewed month
    val monthHolidays: StateFlow<List<Holiday>> = combine(
        _viewYear,
        _viewMonth,
        HolidayRepository.holidaysByYear
    ) { y, m, _ ->
        HolidayRepository.getHolidaysForMonth(y, m)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Events in the currently viewed month (supports recurring events across all years)
    val monthEvents: StateFlow<List<EventEntity>> = combine(
        _viewYear,
        _viewMonth,
        allEvents
    ) { y, m, evs ->
        evs.filter { ev ->
            doesEventOccurInMonth(ev, y, m)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectDate(year: Int, month: Int, day: Int) {
        _selectedYear.value = year
        _selectedMonth.value = month
        _selectedDay.value = day
        _viewYear.value = year
        _viewMonth.value = month
    }

    fun nextMonth() {
        val newYear: Int
        val newMonth: Int
        if (_viewMonth.value == 12) {
            newMonth = 1
            newYear = _viewYear.value + 1
        } else {
            newMonth = _viewMonth.value + 1
            newYear = _viewYear.value
        }
        _viewYear.value = newYear
        _viewMonth.value = newMonth

        // Update selected date to the same day in the new month (clamped to max days)
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, newYear)
            set(Calendar.MONTH, newMonth - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val targetDay = _selectedDay.value.coerceIn(1, maxDays)
        _selectedYear.value = newYear
        _selectedMonth.value = newMonth
        _selectedDay.value = targetDay
    }

    fun prevMonth() {
        val newYear: Int
        val newMonth: Int
        if (_viewMonth.value == 1) {
            newMonth = 12
            newYear = _viewYear.value - 1
        } else {
            newMonth = _viewMonth.value - 1
            newYear = _viewYear.value
        }
        _viewYear.value = newYear
        _viewMonth.value = newMonth

        // Update selected date to the same day in the new month (clamped to max days)
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, newYear)
            set(Calendar.MONTH, newMonth - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val targetDay = _selectedDay.value.coerceIn(1, maxDays)
        _selectedYear.value = newYear
        _selectedMonth.value = newMonth
        _selectedDay.value = targetDay
    }

    fun nextWeek() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, _selectedYear.value)
            set(Calendar.MONTH, _selectedMonth.value - 1)
            set(Calendar.DAY_OF_MONTH, _selectedDay.value)
            set(Calendar.HOUR_OF_DAY, 12)
            add(Calendar.DAY_OF_MONTH, 7)
        }
        val newYear = cal.get(Calendar.YEAR)
        val newMonth = cal.get(Calendar.MONTH) + 1
        val newDay = cal.get(Calendar.DAY_OF_MONTH)

        _selectedYear.value = newYear
        _selectedMonth.value = newMonth
        _selectedDay.value = newDay
        _viewYear.value = newYear
        _viewMonth.value = newMonth
    }

    fun prevWeek() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, _selectedYear.value)
            set(Calendar.MONTH, _selectedMonth.value - 1)
            set(Calendar.DAY_OF_MONTH, _selectedDay.value)
            set(Calendar.HOUR_OF_DAY, 12)
            add(Calendar.DAY_OF_MONTH, -7)
        }
        val newYear = cal.get(Calendar.YEAR)
        val newMonth = cal.get(Calendar.MONTH) + 1
        val newDay = cal.get(Calendar.DAY_OF_MONTH)

        _selectedYear.value = newYear
        _selectedMonth.value = newMonth
        _selectedDay.value = newDay
        _viewYear.value = newYear
        _viewMonth.value = newMonth
    }

    fun goToToday() {
        _selectedYear.value = todayYear
        _selectedMonth.value = todayMonth
        _selectedDay.value = todayDay
        _viewYear.value = todayYear
        _viewMonth.value = todayMonth
        _viewMode.value = CalendarViewMode.MONTH
    }

    fun setViewMode(mode: CalendarViewMode) {
        _viewMode.value = mode
    }

    fun updateSettings(newSettings: CalendarSettings) {
        _settings.value = newSettings
    }

    fun addEvent(event: EventEntity) {
        viewModelScope.launch {
            val id = eventDao.insertEvent(event)
            val savedEvent = event.copy(id = id)
            NotificationHelper.scheduleAlarm(getApplication(), savedEvent)
            DynamicCalendarWidgetProvider.triggerUpdate(getApplication())
        }
    }

    fun updateEvent(event: EventEntity) {
        viewModelScope.launch {
            eventDao.updateEvent(event)
            NotificationHelper.scheduleAlarm(getApplication(), event)
            DynamicCalendarWidgetProvider.triggerUpdate(getApplication())
        }
    }

    fun deleteEvent(event: EventEntity) {
        viewModelScope.launch {
            eventDao.deleteEvent(event)
            NotificationHelper.cancelAlarm(getApplication(), event.id.toInt())
            DynamicCalendarWidgetProvider.triggerUpdate(getApplication())
        }
    }

    private fun generateDaysForMonth(
        viewYear: Int,
        viewMonth: Int,
        selectedYear: Int,
        selectedMonth: Int,
        selectedDay: Int,
        tYear: Int,
        tMonth: Int,
        tDay: Int,
        events: List<EventEntity>,
        settings: CalendarSettings
    ): List<CalendarDayModel> {
        val days = mutableListOf<CalendarDayModel>()

        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, viewYear)
            set(Calendar.MONTH, viewMonth - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }

        val daysInCurrentMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1 for Sunday, 2 for Monday...

        val startOffset = if (settings.startWeekOn == "Monday") {
            (firstDayOfWeek + 5) % 7 // Monday is 0, Sunday is 6
        } else {
            firstDayOfWeek - 1 // Sunday is 0
        }

        // Empty placeholder cells before Day 1 (no other month dates shown)
        for (i in 0 until startOffset) {
            days.add(
                CalendarDayModel(
                    dayNumber = 0,
                    month = viewMonth,
                    year = viewYear,
                    isCurrentMonth = false,
                    isToday = false,
                    isSelected = false,
                    holiday = null,
                    events = emptyList(),
                    subtitleText = "",
                    hasEvents = false,
                    hasImportantDay = false
                )
            )
        }

        // Days of the current month
        for (dayNum in 1..daysInCurrentMonth) {
            days.add(
                createCalendarDay(
                    dayNum, viewMonth, viewYear,
                    isCurrentMonth = true,
                    selectedYear, selectedMonth, selectedDay,
                    tYear, tMonth, tDay,
                    events, settings
                )
            )
        }

        // Complete ONLY the final active row (do not add any extra 6th row)
        val remainder = days.size % 7
        if (remainder != 0) {
            val trailingBlanks = 7 - remainder
            for (i in 0 until trailingBlanks) {
                days.add(
                    CalendarDayModel(
                        dayNumber = 0,
                        month = viewMonth,
                        year = viewYear,
                        isCurrentMonth = false,
                        isToday = false,
                        isSelected = false,
                        holiday = null,
                        events = emptyList(),
                        subtitleText = "",
                        hasEvents = false,
                        hasImportantDay = false
                    )
                )
            }
        }

        return days
    }

    private fun generateDaysForWeek(
        sYear: Int,
        sMonth: Int,
        sDay: Int,
        tYear: Int,
        tMonth: Int,
        tDay: Int,
        events: List<EventEntity>,
        settings: CalendarSettings
    ): List<CalendarDayModel> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, sYear)
            set(Calendar.MONTH, sMonth - 1)
            set(Calendar.DAY_OF_MONTH, sDay)
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val startDayOfWeek = if (settings.startWeekOn == "Monday") Calendar.MONDAY else Calendar.SUNDAY
        while (cal.get(Calendar.DAY_OF_WEEK) != startDayOfWeek) {
            cal.add(Calendar.DAY_OF_MONTH, -1)
        }

        val list = ArrayList<CalendarDayModel>(7)
        for (i in 0 until 7) {
            val y = cal.get(Calendar.YEAR)
            val m = cal.get(Calendar.MONTH) + 1
            val d = cal.get(Calendar.DAY_OF_MONTH)

            list.add(
                createCalendarDay(
                    dayNum = d,
                    month = m,
                    year = y,
                    isCurrentMonth = (m == sMonth && y == sYear),
                    sYear = sYear,
                    sMonth = sMonth,
                    sDay = sDay,
                    tYear = tYear,
                    tMonth = tMonth,
                    tDay = tDay,
                    events = events,
                    settings = settings
                )
            )
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        return list
    }

    private fun createCalendarDay(
        dayNum: Int,
        month: Int,
        year: Int,
        isCurrentMonth: Boolean,
        sYear: Int,
        sMonth: Int,
        sDay: Int,
        tYear: Int,
        tMonth: Int,
        tDay: Int,
        events: List<EventEntity>,
        settings: CalendarSettings
    ): CalendarDayModel {
        val isToday = (year == tYear && month == tMonth && dayNum == tDay)
        val isSelected = (year == sYear && month == sMonth && dayNum == sDay)
        val holiday = HolidayRepository.getHoliday(year, month, dayNum)

        val dayEvents = events.filter { event ->
            doesEventOccurOnDate(event, year, month, dayNum)
        }

        val hasImportant = dayEvents.any { it.isImportantDay }

        // Subtitle text (Islamic lunar day or Hindu Tithi)
        val subtitle = when (settings.otherCalendar) {
            "Muslim Calendar" -> {
                val hijri = HijriCalendarHelper.gregorianToHijri(year, month, dayNum, settings.ramadanAdjustment)
                "${hijri.day}"
            }
            "Hindu Calendar" -> {
                val panchang = HinduCalendarHelper.getHinduPanchang(year, month, dayNum)
                panchang.tithiName.take(5)
            }
            else -> ""
        }

        return CalendarDayModel(
            dayNumber = dayNum,
            month = month,
            year = year,
            isCurrentMonth = isCurrentMonth,
            isToday = isToday,
            isSelected = isSelected,
            holiday = holiday,
            events = dayEvents,
            subtitleText = subtitle,
            hasEvents = dayEvents.isNotEmpty(),
            hasImportantDay = hasImportant
        )
    }

    private fun formatSelectedDateSummary(
        year: Int,
        month: Int,
        day: Int,
        tYear: Int,
        tMonth: Int,
        tDay: Int
    ): String {
        val selCal = Calendar.getInstance().apply {
            set(year, month - 1, day, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayC = Calendar.getInstance().apply {
            set(tYear, tMonth - 1, tDay, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val dayDiff = TimeUnit.MILLISECONDS.toDays(selCal.timeInMillis - todayC.timeInMillis)

        val monthNames = listOf(
            "", "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sept", "Oct", "Nov", "Dec"
        )
        val dayNames = listOf("", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

        val dayOfWeekName = dayNames[selCal.get(Calendar.DAY_OF_WEEK)]
        val monthName = monthNames[month]

        val relativeText = when {
            dayDiff == 0L -> "today"
            dayDiff == 1L -> "tomorrow"
            dayDiff == -1L -> "yesterday"
            dayDiff > 1L -> "in $dayDiff days"
            else -> "${-dayDiff} days ago"
        }

        return "$dayOfWeekName, $day $monthName, $relativeText"
    }

    private fun doesEventOccurOnDate(event: EventEntity, year: Int, month: Int, dayNum: Int): Boolean {
        val cal = Calendar.getInstance().apply { timeInMillis = event.startTimestamp }
        val eYear = cal.get(Calendar.YEAR)
        val eMonth = cal.get(Calendar.MONTH) + 1
        val eDay = cal.get(Calendar.DAY_OF_MONTH)

        // Event cannot occur before its scheduled start date
        if (year < eYear) return false
        if (year == eYear && month < eMonth) return false
        if (year == eYear && month == eMonth && dayNum < eDay) return false

        val rep = event.repeatType.trim()
        return when {
            rep.equals("Daily", ignoreCase = true) || rep.equals("Every day", ignoreCase = true) -> true
            rep.equals("Weekly", ignoreCase = true) || rep.equals("Every week", ignoreCase = true) -> {
                val targetCal = Calendar.getInstance().apply { set(year, month - 1, dayNum) }
                targetCal.get(Calendar.DAY_OF_WEEK) == cal.get(Calendar.DAY_OF_WEEK)
            }
            rep.equals("Monthly", ignoreCase = true) || rep.equals("Every month", ignoreCase = true) -> {
                eDay == dayNum
            }
            rep.equals("Yearly", ignoreCase = true) || rep.equals("Every year", ignoreCase = true) -> {
                eMonth == month && eDay == dayNum
            }
            else -> {
                eYear == year && eMonth == month && eDay == dayNum
            }
        }
    }

    private fun doesEventOccurInMonth(event: EventEntity, year: Int, month: Int): Boolean {
        val cal = Calendar.getInstance().apply { timeInMillis = event.startTimestamp }
        val eYear = cal.get(Calendar.YEAR)
        val eMonth = cal.get(Calendar.MONTH) + 1

        if (year < eYear) return false
        if (year == eYear && month < eMonth) return false

        val rep = event.repeatType.trim()
        return when {
            rep.equals("Daily", ignoreCase = true) || rep.equals("Every day", ignoreCase = true) -> true
            rep.equals("Weekly", ignoreCase = true) || rep.equals("Every week", ignoreCase = true) -> true
            rep.equals("Monthly", ignoreCase = true) || rep.equals("Every month", ignoreCase = true) -> true
            rep.equals("Yearly", ignoreCase = true) || rep.equals("Every year", ignoreCase = true) -> {
                eMonth == month
            }
            else -> {
                eYear == year && eMonth == month
            }
        }
    }
}
