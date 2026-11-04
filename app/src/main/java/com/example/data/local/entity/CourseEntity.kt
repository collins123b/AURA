package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "courses")
data class CourseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val code: String,
    val name: String,
    val lecturer: String = "",
    val venue: String = "",
    val colorHex: String = "#0A84FF",
    val creditHours: Int = 3,
    val attendanceTargetPercent: Int = 75,
    val notes: String = ""
)
