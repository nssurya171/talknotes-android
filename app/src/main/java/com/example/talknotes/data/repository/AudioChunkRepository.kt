package com.example.talknotes.data.repository

import com.example.talknotes.data.local.dao.AudioChunkDao
import com.example.talknotes.data.local.entity.AudioChunk
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioChunkRepository @Inject constructor(
    private val audioChunkDao: AudioChunkDao
) {

    suspend fun saveChunk(chunk: AudioChunk) {
        audioChunkDao.insertChunk(chunk)
    }

    suspend fun getChunksForMeeting(meetingId: Long): List<AudioChunk> {
        return audioChunkDao.getChunksForMeeting(meetingId)
    }

    suspend fun getPendingChunks(): List<AudioChunk> {
        return audioChunkDao.getPendingChunks()
    }

    suspend fun updateChunk(chunk: AudioChunk) {
        audioChunkDao.updateChunk(chunk)
    }

    fun getChunkCountForMeeting(meetingId: Long): Flow<Int> {
        return audioChunkDao.getChunkCountForMeeting(meetingId)
    }
}