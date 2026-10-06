package com.example.model

data class CalendarDayModel(
    val dayNumber: Int,
    val month: Int,
    val year: Int,
    val isCurrentMonth: Boolean,
    val isToday: Boolean,
    val isSelected: Boolean,
    val holiday: Holiday? = null,
    val events: List<EventEntity> = emptyList(),
    val subtitleText: String = "", // e.g. Hijri day or Hindu Tithi
    val hasEvents: Boolean = false,
    val hasImportantDay: Boolean = false
)
