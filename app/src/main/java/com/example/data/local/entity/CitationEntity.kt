package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "academic_citations")
data class CitationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val courseCode: String = "General",
    val sourceType: String = "JOURNAL", // BOOK, JOURNAL, WEBSITE, LECTURE
    val authors: String,
    val year: String,
    val title: String,
    val publicationOrPublisher: String = "",
    val volumeOrPages: String = "",
    val urlOrDoi: String = "",
    val style: String = "APA7", // APA7, IEEE, MLA, HARVARD
    val formattedCitation: String,
    val createdEpochMillis: Long = System.currentTimeMillis()
)
