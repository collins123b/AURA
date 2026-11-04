package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "assessments")
data class AssessmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val courseId: Long,
    val courseCode: String,
    val courseName: String,
    val title: String,
    val type: String, // ASSIGNMENT, QUIZ, CLASS_TEST, MID_SEM, FINAL_EXAM, PROJECT, PRESENTATION
    val description: String = "",
    val dueEpochMillis: Long,
    val priority: String = "HIGH", // LOW, MEDIUM, HIGH, URGENT
    val status: String = "PENDING", // PENDING, IN_PROGRESS, COMPLETED, SUBMITTED
    val notes: String = "",
    val reminderMinutesBefore: Int = 1440, // 24 hours default
    val reminderEnabled: Boolean = true,
    val examVenue: String = "",
    val seatNumber: String = "",
    val indexNumber: String = "",
    val materialsChecklist: String = "Student ID, Pens, Scientific Calculator",
    val subtasksJson: String = ""
)
