package com.example.talknotes.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.talknotes.data.local.entity.Transcript
import com.example.talknotes.data.repository.AudioChunkRepository
import com.example.talknotes.data.repository.MeetingRepository
import com.example.talknotes.data.repository.TranscriptRepository
import com.example.talknotes.util.Constants
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class TranscriptionWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val audioChunkRepository: AudioChunkRepository,
    private val transcriptRepository: TranscriptRepository,
    private val meetingRepository: MeetingRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {

        val chunkId = inputData.getLong(Constants.WORK_INPUT_CHUNK_ID, -1L)
        val meetingId = inputData.getLong(Constants.WORK_INPUT_MEETING_ID, -1L)

        if (chunkId == -1L || meetingId == -1L) {
            return Result.failure()
        }

        val chunk = audioChunkRepository.getChunkById(chunkId) ?: return Result.failure()

        return try {
            meetingRepository.updateMeetingTranscriptionStatus(
                meetingId,
                Constants.STATUS_PROCESSING
            )

            audioChunkRepository.markChunkProcessing(
                chunkId = chunkId,
                retryCount = runAttemptCount
            )
            android.util.Log.d(
                "TalkNotes",
                "TranscriptionWorker started for chunkId=$chunkId meetingId=$meetingId"
            )



            val result = transcriptRepository.transcribeAudioFile(chunk.filePath)

            result.fold(
                onSuccess = { text ->
                    transcriptRepository.saveTranscript(
                        Transcript(
                            meetingId = meetingId,
                            chunkId = chunk.id,
                            chunkIndex = chunk.chunkIndex,
                            text = text
                        )

                    )
                    android.util.Log.d(
                        "TalkNotes",
                        "Transcription success for chunkId=$chunkId text=${text.take(80)}"
                    )

                    audioChunkRepository.markChunkDone(
                        chunkId = chunkId,
                        retryCount = runAttemptCount
                    )

                    val incompleteCount = audioChunkRepository.countIncompleteChunks(meetingId)
                    if (incompleteCount == 0) {
                        meetingRepository.updateMeetingTranscriptionStatus(
                            meetingId,
                            Constants.STATUS_DONE
                        )
                    }

                    Result.success()
                },
                onFailure = { error ->
                    audioChunkRepository.markChunkFailed(
                        chunkId = chunkId,
                        retryCount = runAttemptCount,
                        errorMessage = error.message
                    )

                    meetingRepository.updateMeetingTranscriptionStatus(
                        meetingId,
                        Constants.STATUS_FAILED
                    )

                    if (runAttemptCount >= 2) {
                        Result.failure()
                    } else {
                        Result.retry()
                    }
                }
            )
        } catch (e: Exception) {
            audioChunkRepository.markChunkFailed(
                chunkId = chunkId,
                retryCount = runAttemptCount,
                errorMessage = e.message
            )

            meetingRepository.updateMeetingTranscriptionStatus(
                meetingId,
                Constants.STATUS_FAILED
            )

            if (runAttemptCount >= 5) {
                Result.failure()
            } else {
                Result.retry()
            }
        }
    }
}