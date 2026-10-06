package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val startTimestamp: Long,
    val endTimestamp: Long,
    val isAllDay: Boolean = false,
    val isImportantDay: Boolean = false,
    val repeatType: String = "Once", // "Once", "Daily", "Weekly", "Monthly", "Yearly"
    val reminderMinutesBefore: Int = 15, // -1 for none, 0 at time, 15, 30, 60, etc.
    val alertType: String = "Alarm & Sound, Vibrate", // "Alarm & Sound, Vibrate", "Notification", "Vibrate Only", "Silent"
    val location: String = "",
    val remark: String = ""
)
