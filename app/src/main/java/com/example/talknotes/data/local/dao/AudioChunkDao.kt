package com.example.talknotes.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.talknotes.data.local.entity.AudioChunk

@Dao
interface AudioChunkDao {

    @Insert
    suspend fun insertChunk(chunk: AudioChunk)

    @Query("SELECT * FROM audio_chunks WHERE meetingId = :meetingId ORDER BY chunkIndex ASC")
    suspend fun getChunksForMeeting(meetingId: Long): List<AudioChunk>

    @Query("SELECT * FROM audio_chunks WHERE uploaded = 0")
    suspend fun getPendingChunks(): List<AudioChunk>

    @Update
    suspend fun updateChunk(chunk: AudioChunk)
}