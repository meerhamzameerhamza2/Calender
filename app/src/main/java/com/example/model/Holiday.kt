package com.example.model

enum class HolidayType {
    NATIONAL,
    FESTIVAL,
    OBSERVANCE,
    JAYANTI,
    BIRTH_ANNIVERSARY
}

data class Holiday(
    val day: Int,
    val month: Int, // 1-12
    val year: Int,
    val title: String,
    val category: String, // e.g. "Gazetted Holiday", "Restricted Holiday"
    val type: HolidayType = HolidayType.FESTIVAL
)
