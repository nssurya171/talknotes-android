package com.example.talknotes.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.talknotes.data.local.entity.Meeting
import kotlinx.coroutines.flow.Flow

@Dao
interface MeetingDao {

    @Insert
    suspend fun insertMeeting(meeting: Meeting): Long

    @Query("SELECT * FROM meetings ORDER BY startTime DESC")
    fun getMeetings(): Flow<List<Meeting>>

    @Update
    suspend fun updateMeeting(meeting: Meeting)

    @Query("SELECT * FROM meetings WHERE id = :meetingId")
    suspend fun getMeetingById(meetingId: Long): Meeting?
}