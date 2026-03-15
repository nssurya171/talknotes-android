package com.example.talknotes.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.talknotes.data.local.entity.Transcript
import kotlinx.coroutines.flow.Flow

@Dao
interface TranscriptDao {

    @Insert
    suspend fun insertTranscript(transcript: Transcript)

    @Query("SELECT * FROM transcripts WHERE meetingId = :meetingId ORDER BY chunkIndex ASC")
    fun getTranscriptForMeeting(meetingId: Long): Flow<List<Transcript>>

    @Query("SELECT * FROM transcripts WHERE meetingId = :meetingId ORDER BY chunkIndex ASC")
    suspend fun getTranscriptListForMeeting(meetingId: Long): List<Transcript>

    @Query("DELETE FROM transcripts WHERE meetingId = :meetingId")
    suspend fun deleteTranscriptsForMeeting(meetingId: Long)
}