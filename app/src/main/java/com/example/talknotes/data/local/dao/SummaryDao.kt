package com.example.talknotes.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.talknotes.data.local.entity.Summary
import kotlinx.coroutines.flow.Flow

@Dao
interface SummaryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSummary(summary: Summary): Long

    @Query("SELECT * FROM summaries WHERE meetingId = :meetingId ORDER BY updatedAt DESC LIMIT 1")
    fun getSummary(meetingId: Long): Flow<Summary?>

    @Query("SELECT * FROM summaries WHERE meetingId = :meetingId ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getSummaryNow(meetingId: Long): Summary?

    @Query("DELETE FROM summaries WHERE meetingId = :meetingId")
    suspend fun deleteSummaryForMeeting(meetingId: Long)
}