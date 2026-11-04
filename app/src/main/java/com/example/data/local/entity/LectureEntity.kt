package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lectures")
data class LectureEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val courseId: Long,
    val courseCode: String,
    val courseName: String,
    val dayOfWeek: Int, // 1 = Monday, 2 = Tuesday, 3 = Wednesday, 4 = Thursday, 5 = Friday, 6 = Saturday, 7 = Sunday
    val startTime: String, // HH:mm format e.g. "08:30"
    val endTime: String, // HH:mm format e.g. "10:30"
    val venue: String = "",
    val lecturer: String = "",
    val isRecurring: Boolean = true,
    val specificDateMillis: Long? = null,
    val reminderMinutesBefore: Int = 15,
    val reminderEnabled: Boolean = true,
    val colorHex: String = "#0A84FF",
    val alarmToneId: String = "", // Empty = use global default alarm tone; otherwise preset ID or "CUSTOM_AUDIO"
    val customAlarmPath: String = "",
    val customAlarmLabel: String = "",
    val walkingMinutes: Int = 10
)
