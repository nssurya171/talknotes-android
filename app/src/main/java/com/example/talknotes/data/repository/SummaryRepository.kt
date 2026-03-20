package com.example.talknotes.data.repository

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.talknotes.data.local.dao.SummaryDao
import com.example.talknotes.data.local.entity.Summary
import com.example.talknotes.data.remote.api.SummaryApi
import com.example.talknotes.data.remote.dto.Message
import com.example.talknotes.data.remote.dto.SummaryRequestDto
import com.example.talknotes.util.Constants
import com.example.talknotes.worker.SummaryWorker
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SummaryRepository @Inject constructor(
    private val summaryDao: SummaryDao,
    private val summaryApi: SummaryApi
) {

    suspend fun saveSummary(summary: Summary): Long {
        return summaryDao.insertSummary(summary)
    }

    fun getSummary(meetingId: Long): Flow<Summary?> {
        return summaryDao.getSummary(meetingId)
    }

    suspend fun getSummaryNow(meetingId: Long): Summary? {
        return summaryDao.getSummaryNow(meetingId)
    }

    suspend fun saveProcessingSummary(meetingId: Long) {
        summaryDao.insertSummary(
            Summary(
                meetingId = meetingId,
                status = Constants.STATUS_PROCESSING,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun saveFailedSummary(meetingId: Long, errorMessage: String?) {
        summaryDao.insertSummary(
            Summary(
                meetingId = meetingId,
                status = Constants.STATUS_FAILED,
                errorMessage = errorMessage,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun clearSummaryForMeeting(meetingId: Long) {
        summaryDao.deleteSummaryForMeeting(meetingId)
    }

    suspend fun generateSummaryFromTranscript(
        transcript: String
    ): Result<String> {
        return try {
            val prompt = """
            Summarize the following meeting transcript.

            Return the answer in this exact format:

            Title:
            <short title>

            Summary:
            <2-5 sentence summary>

            Action Items:
            - item 1
            - item 2

            Key Points:
            - point 1
            - point 2

            Transcript:
            $transcript
        """.trimIndent()

            val request = SummaryRequestDto(
                model = Constants.MODEL_SUMMARY,
                messages = listOf(
                    Message(
                        role = "user",
                        content = prompt
                    )
                )
            )

            val response = summaryApi.generateSummary(request)
            val resultText = response.choices.firstOrNull()?.message?.content
                ?: "No summary generated"

            Result.success(resultText)
        } catch (e: retrofit2.HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            Result.failure(
                IllegalStateException("HTTP ${e.code()}: $errorBody")
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun enqueueSummary(
        context: Context,
        meetingId: Long,
        forceRegenerate: Boolean = true
    ) {
        val request = OneTimeWorkRequestBuilder<SummaryWorker>()
            .setInputData(
                workDataOf(
                    Constants.WORK_INPUT_MEETING_ID to meetingId,
                    Constants.WORK_INPUT_FORCE_REGENERATE to forceRegenerate
                )
            )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                15,
                TimeUnit.SECONDS
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "summary_$meetingId",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}