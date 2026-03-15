package com.example.talknotes.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.talknotes.data.local.entity.Meeting
import com.example.talknotes.data.local.entity.Transcript
import com.example.talknotes.data.repository.AudioChunkRepository
import com.example.talknotes.data.repository.MeetingRepository
import com.example.talknotes.data.repository.TranscriptRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val meetingRepository: MeetingRepository,
    private val audioChunkRepository: AudioChunkRepository,
    private val transcriptRepository: TranscriptRepository
) : ViewModel() {

    val meetings: StateFlow<List<Meeting>> = meetingRepository
        .getMeetings()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val isRecording: StateFlow<Boolean> = meetingRepository
        .getMeetings()
        .map { meetings ->
            meetings.any { it.status == "RECORDING" }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false
        )

    val activeMeetingStartTime: StateFlow<Long?> = meetingRepository
        .getMeetings()
        .map { meetings ->
            meetings.firstOrNull { it.status == "RECORDING" }?.startTime
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    suspend fun createNewMeetingIfPossible(title: String = "New Meeting"): Long? {
        val activeMeetings = meetingRepository.getActiveRecordings()

        return if (activeMeetings.isEmpty()) {
            meetingRepository.createMeeting(title)
        } else {
            null
        }
    }

    fun stopAllRecordings() {
        viewModelScope.launch {
            meetingRepository.stopAllActiveRecordings()
        }
    }

    fun getChunkCountForMeeting(meetingId: Long): Flow<Int> {
        return audioChunkRepository.getChunkCountForMeeting(meetingId)
    }

    fun getTranscriptForMeeting(meetingId: Long): Flow<List<Transcript>> {
        return transcriptRepository.getTranscript(meetingId)
    }

    fun generateMockTranscript(meetingId: Long) {
        viewModelScope.launch {
            val chunks = audioChunkRepository.getChunksForMeeting(meetingId)

            transcriptRepository.clearTranscriptForMeeting(meetingId)

            chunks.forEach { chunk ->
                transcriptRepository.saveTranscript(
                    Transcript(
                        meetingId = meetingId,
                        chunkIndex = chunk.chunkIndex,
                        text = "Mock transcript generated for chunk ${chunk.chunkIndex}"
                    )
                )
            }
        }
    }
}