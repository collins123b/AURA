package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.CitationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CitationDao {
    @Query("SELECT * FROM academic_citations ORDER BY createdEpochMillis DESC")
    fun getAllCitations(): Flow<List<CitationEntity>>

    @Query("SELECT * FROM academic_citations ORDER BY authors ASC, year DESC")
    suspend fun getAllCitationsList(): List<CitationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCitation(citation: CitationEntity): Long

    @Update
    suspend fun updateCitation(citation: CitationEntity)

    @Delete
    suspend fun deleteCitation(citation: CitationEntity)
}
