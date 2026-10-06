package com.example.model

data class CalendarDayInfo(
    val year: Int,
    val month: Int, // 1-12
    val day: Int,
    val dayOfWeek: Int,
    val isCurrentMonth: Boolean,
    val isToday: Boolean,
    val secondaryDateText: String = "",
    val holidays: List<Holiday> = emptyList(),
    val events: List<EventEntity> = emptyList()
) {
    val hasEventsOrHolidays: Boolean
        get() = holidays.isNotEmpty() || events.isNotEmpty()
}
