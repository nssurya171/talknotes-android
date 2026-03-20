package com.example.talknotes.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.talknotes.data.local.entity.Summary
import com.example.talknotes.data.repository.MeetingRepository
import com.example.talknotes.data.repository.SummaryRepository
import com.example.talknotes.data.repository.TranscriptRepository
import com.example.talknotes.util.Constants
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SummaryWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val transcriptRepository: TranscriptRepository,
    private val summaryRepository: SummaryRepository,
    private val meetingRepository: MeetingRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val meetingId = inputData.getLong(Constants.WORK_INPUT_MEETING_ID, -1L)
        val forceRegenerate = inputData.getBoolean(Constants.WORK_INPUT_FORCE_REGENERATE, true)

        if (meetingId == -1L) return Result.failure()

        return try {
            meetingRepository.updateMeetingSummaryStatus(
                meetingId,
                Constants.STATUS_PROCESSING
            )

            val existingSummary = summaryRepository.getSummaryNow(meetingId)
            if (existingSummary != null &&
                existingSummary.status == Constants.STATUS_DONE &&
                !forceRegenerate
            ) {
                return Result.success()
            }

            summaryRepository.saveProcessingSummary(meetingId)

            val transcriptText = transcriptRepository.getCombinedTranscript(meetingId)

            if (transcriptText.isBlank()) {
                summaryRepository.saveFailedSummary(
                    meetingId = meetingId,
                    errorMessage = "Transcript is empty. Cannot generate summary."
                )
                meetingRepository.updateMeetingSummaryStatus(
                    meetingId,
                    Constants.STATUS_FAILED
                )
                return Result.failure()
            }

            val result = summaryRepository.generateSummaryFromTranscript(transcriptText)

            result.fold(
                onSuccess = { rawSummary ->
                    val parsed = parseSummarySections(rawSummary)

                    summaryRepository.saveSummary(
                        Summary(
                            meetingId = meetingId,
                            title = parsed.title,
                            summary = parsed.summary,
                            actionItems = parsed.actionItems,
                            keyPoints = parsed.keyPoints,
                            status = Constants.STATUS_DONE,
                            errorMessage = null,
                            updatedAt = System.currentTimeMillis()
                        )
                    )

                    meetingRepository.updateMeetingSummaryStatus(
                        meetingId,
                        Constants.STATUS_DONE
                    )

                    Result.success()
                },
                onFailure = { error ->
                    summaryRepository.saveFailedSummary(
                        meetingId = meetingId,
                        errorMessage = error.message ?: "Summary generation failed"
                    )

                    meetingRepository.updateMeetingSummaryStatus(
                        meetingId,
                        Constants.STATUS_FAILED
                    )

                    if (runAttemptCount >= 2) Result.failure() else Result.retry()
                }
            )
        } catch (e: Exception) {
            summaryRepository.saveFailedSummary(
                meetingId = meetingId,
                errorMessage = e.message ?: "Unexpected summary error"
            )

            meetingRepository.updateMeetingSummaryStatus(
                meetingId,
                Constants.STATUS_FAILED
            )

            if (runAttemptCount >= 2) Result.failure() else Result.retry()
        }
    }

    private fun parseSummarySections(rawText: String): ParsedSummary {
        val lines = rawText.lines()

        var currentSection = ""
        val titleBuilder = StringBuilder()
        val summaryBuilder = StringBuilder()
        val actionItemsBuilder = StringBuilder()
        val keyPointsBuilder = StringBuilder()

        for (line in lines) {
            val trimmed = line.trim()

            when {
                trimmed.startsWith("Title:", ignoreCase = true) -> {
                    currentSection = "TITLE"
                    titleBuilder.append(trimmed.removePrefix("Title:").trim())
                }

                trimmed.startsWith("Summary:", ignoreCase = true) -> {
                    currentSection = "SUMMARY"
                    summaryBuilder.append(trimmed.removePrefix("Summary:").trim())
                }

                trimmed.startsWith("Action Items:", ignoreCase = true) -> {
                    currentSection = "ACTION_ITEMS"
                    actionItemsBuilder.append(trimmed.removePrefix("Action Items:").trim())
                }

                trimmed.startsWith("Key Points:", ignoreCase = true) -> {
                    currentSection = "KEY_POINTS"
                    keyPointsBuilder.append(trimmed.removePrefix("Key Points:").trim())
                }

                else -> {
                    when (currentSection) {
                        "TITLE" -> {
                            if (trimmed.isNotBlank()) {
                                if (titleBuilder.isNotEmpty()) titleBuilder.append("\n")
                                titleBuilder.append(trimmed)
                            }
                        }
                        "SUMMARY" -> {
                            if (trimmed.isNotBlank()) {
                                if (summaryBuilder.isNotEmpty()) summaryBuilder.append("\n")
                                summaryBuilder.append(trimmed)
                            }
                        }
                        "ACTION_ITEMS" -> {
                            if (trimmed.isNotBlank()) {
                                if (actionItemsBuilder.isNotEmpty()) actionItemsBuilder.append("\n")
                                actionItemsBuilder.append(trimmed)
                            }
                        }
                        "KEY_POINTS" -> {
                            if (trimmed.isNotBlank()) {
                                if (keyPointsBuilder.isNotEmpty()) keyPointsBuilder.append("\n")
                                keyPointsBuilder.append(trimmed)
                            }
                        }
                    }
                }
            }
        }

        return ParsedSummary(
            title = titleBuilder.toString().ifBlank { "Meeting Summary" },
            summary = summaryBuilder.toString().ifBlank { rawText },
            actionItems = actionItemsBuilder.toString(),
            keyPoints = keyPointsBuilder.toString()
        )
    }

    private data class ParsedSummary(
        val title: String,
        val summary: String,
        val actionItems: String,
        val keyPoints: String
    )
}