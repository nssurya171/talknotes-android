package com.example.talknotes.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.talknotes.data.local.entity.AudioChunk
import kotlinx.coroutines.flow.Flow

@Dao
interface AudioChunkDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChunk(chunk: AudioChunk): Long

    @Query("SELECT * FROM audio_chunks WHERE meetingId = :meetingId ORDER BY chunkIndex ASC")
    suspend fun getChunksForMeeting(meetingId: Long): List<AudioChunk>

    @Query("SELECT * FROM audio_chunks WHERE meetingId = :meetingId ORDER BY chunkIndex ASC")
    fun observeChunksForMeeting(meetingId: Long): Flow<List<AudioChunk>>

    @Query("SELECT * FROM audio_chunks WHERE transcriptionStatus IN ('PENDING', 'FAILED') ORDER BY meetingId ASC, chunkIndex ASC")
    suspend fun getPendingChunks(): List<AudioChunk>

    @Query("SELECT * FROM audio_chunks WHERE id = :chunkId LIMIT 1")
    suspend fun getChunkById(chunkId: Long): AudioChunk?

    @Update
    suspend fun updateChunk(chunk: AudioChunk)

    @Query("""
        UPDATE audio_chunks
        SET transcriptionStatus = :status,
            retryCount = :retryCount,
            errorMessage = :errorMessage,
            uploaded = :uploaded
        WHERE id = :chunkId
    """)
    suspend fun updateChunkProcessingState(
        chunkId: Long,
        status: String,
        retryCount: Int,
        errorMessage: String?,
        uploaded: Boolean
    )

    @Query("SELECT COUNT(*) FROM audio_chunks WHERE meetingId = :meetingId")
    fun getChunkCountForMeeting(meetingId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM audio_chunks WHERE meetingId = :meetingId AND transcriptionStatus != 'DONE'")
    suspend fun countIncompleteChunks(meetingId: Long): Int
}