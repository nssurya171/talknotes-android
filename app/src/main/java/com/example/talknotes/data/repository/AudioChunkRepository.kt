package com.example.talknotes.data.repository

import com.example.talknotes.data.local.dao.AudioChunkDao
import com.example.talknotes.data.local.entity.AudioChunk
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton
import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.talknotes.util.Constants
import com.example.talknotes.worker.TranscriptionWorker
import java.util.concurrent.TimeUnit

@Singleton
class AudioChunkRepository @Inject constructor(
    private val audioChunkDao: AudioChunkDao
) {

    suspend fun saveChunk(chunk: AudioChunk): Long {
        return audioChunkDao.insertChunk(chunk)
    }

    suspend fun getChunksForMeeting(meetingId: Long): List<AudioChunk> {
        return audioChunkDao.getChunksForMeeting(meetingId)
    }

    fun observeChunksForMeeting(meetingId: Long): Flow<List<AudioChunk>> {
        return audioChunkDao.observeChunksForMeeting(meetingId)
    }

    suspend fun getPendingChunks(): List<AudioChunk> {
        return audioChunkDao.getPendingChunks()
    }

    suspend fun getChunkById(chunkId: Long): AudioChunk? {
        return audioChunkDao.getChunkById(chunkId)
    }

    suspend fun updateChunk(chunk: AudioChunk) {
        audioChunkDao.updateChunk(chunk)
    }

    suspend fun markChunkProcessing(chunkId: Long, retryCount: Int) {
        audioChunkDao.updateChunkProcessingState(
            chunkId = chunkId,
            status = "PROCESSING",
            retryCount = retryCount,
            errorMessage = null,
            uploaded = false
        )
    }

    suspend fun markChunkDone(chunkId: Long, retryCount: Int) {
        audioChunkDao.updateChunkProcessingState(
            chunkId = chunkId,
            status = "DONE",
            retryCount = retryCount,
            errorMessage = null,
            uploaded = true
        )
    }

    suspend fun markChunkFailed(chunkId: Long, retryCount: Int, errorMessage: String?) {
        audioChunkDao.updateChunkProcessingState(
            chunkId = chunkId,
            status = "FAILED",
            retryCount = retryCount,
            errorMessage = errorMessage,
            uploaded = false
        )
    }

    suspend fun countIncompleteChunks(meetingId: Long): Int {
        return audioChunkDao.countIncompleteChunks(meetingId)
    }

    fun getChunkCountForMeeting(meetingId: Long): Flow<Int> {
        return audioChunkDao.getChunkCountForMeeting(meetingId)
    }

    fun enqueueTranscription(
        context: Context,
        chunkId: Long,
        meetingId: Long
    ) {
        val request = OneTimeWorkRequestBuilder<TranscriptionWorker>()
            .setInputData(
                workDataOf(
                    Constants.WORK_INPUT_CHUNK_ID to chunkId,
                    Constants.WORK_INPUT_MEETING_ID to meetingId
                )
            )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                10,
                TimeUnit.SECONDS
            )
            .build()

        WorkManager.getInstance(context).enqueue(request)
    }
}