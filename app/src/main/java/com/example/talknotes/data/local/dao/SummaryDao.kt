package com.example.talknotes.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.talknotes.data.local.entity.Summary
import kotlinx.coroutines.flow.Flow

@Dao
interface SummaryDao {

    @Insert
    suspend fun insertSummary(summary: Summary)

    @Query("SELECT * FROM summaries WHERE meetingId = :meetingId")
    fun getSummary(meetingId: Long): Flow<Summary?>
}