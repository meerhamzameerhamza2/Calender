package com.example.model

data class CalendarSettings(
    val showWeekNumbers: Boolean = false,
    val startWeekOn: String = "Sunday", // "Sunday" or "Monday"
    val setTimeZone: Boolean = true,
    val timeZone: String = "Asia/Kolkata (GMT+05:30)",
    val nationalHolidays: String = "India (Gazetted & Festivals)",
    val religiousHolidays: String = "All",
    val otherCalendar: String = "Muslim Calendar", // "None", "Muslim Calendar", "Hindu Calendar"
    val ramadanAdjustment: Int = 0 // -2, -1, 0, +1, +2
)
