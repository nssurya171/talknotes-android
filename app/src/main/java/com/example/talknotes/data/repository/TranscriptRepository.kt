package com.example.talknotes.data.repository

import com.example.talknotes.data.local.dao.TranscriptDao
import com.example.talknotes.data.local.entity.Transcript
import com.example.talknotes.data.remote.api.TranscriptionApi
import com.example.talknotes.util.Constants
import com.example.talknotes.util.FileUtils
import kotlinx.coroutines.flow.Flow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranscriptRepository @Inject constructor(
    private val transcriptDao: TranscriptDao,
    private val transcriptionApi: TranscriptionApi
) {

    suspend fun saveTranscript(transcript: Transcript): Long {
        return transcriptDao.insertTranscript(transcript)
    }

    fun getTranscript(meetingId: Long): Flow<List<Transcript>> {
        return transcriptDao.getTranscriptForMeeting(meetingId)
    }

    suspend fun getTranscriptListForMeeting(meetingId: Long): List<Transcript> {
        return transcriptDao.getTranscriptListForMeeting(meetingId)
    }

    suspend fun getTranscriptByChunkId(chunkId: Long): Transcript? {
        return transcriptDao.getTranscriptByChunkId(chunkId)
    }

    suspend fun transcribeAudioFile(filePath: String): Result<String> {
        return try {
            val file = File(filePath)

            if (!file.exists()) {
                return Result.failure(
                    IllegalStateException("Audio file not found: $filePath")
                )
            }

            val filePart = FileUtils.createAudioPart(file)
            val modelPart = FileUtils.createModelPart(Constants.WHISPER_MODEL)

            val response = transcriptionApi.transcribeAudio(
                file = filePart,
                model = modelPart
            )

            val text = response.text.trim()
            if (text.isBlank()) {
                Result.failure(IllegalStateException("Empty transcript received"))
            } else {
                Result.success(text)
            }
        } catch (e: retrofit2.HttpException) {
            if (e.code() == 429) {
                Result.failure(IllegalStateException("Rate limit exceeded. Retry later."))
            } else {
                Result.failure(e)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCombinedTranscript(meetingId: Long): String {
        return transcriptDao.getTranscriptListForMeeting(meetingId)
            .sortedBy { it.chunkIndex }
            .joinToString(separator = " ") { it.text.trim() }
    }

    suspend fun clearTranscriptForMeeting(meetingId: Long) {
        transcriptDao.deleteTranscriptsForMeeting(meetingId)
    }
}