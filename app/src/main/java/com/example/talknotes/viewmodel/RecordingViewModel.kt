package com.example.talknotes.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.talknotes.data.local.entity.Meeting
import com.example.talknotes.data.repository.MeetingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class RecordingViewModel @Inject constructor(
    private val meetingRepository: MeetingRepository
) : ViewModel() {

    private val _currentMeetingId = MutableStateFlow<Long?>(null)
    val currentMeetingId: StateFlow<Long?> = _currentMeetingId.asStateFlow()

    private val _recordingStatus = MutableStateFlow("STOPPED")
    val recordingStatus: StateFlow<String> = _recordingStatus.asStateFlow()

    fun startNewMeeting(title: String = "New Meeting") {
        viewModelScope.launch {
            val meetingId = meetingRepository.createMeeting(title)
            _currentMeetingId.value = meetingId
            _recordingStatus.value = "RECORDING"
        }
    }

    fun setRecordingStatus(status: String) {
        _recordingStatus.value = status
    }

    fun stopMeeting() {
        viewModelScope.launch {
            val meetingId = _currentMeetingId.value ?: return@launch
            val existingMeeting = meetingRepository.getMeetingById(meetingId) ?: return@launch

            val updatedMeeting = existingMeeting.copy(
                endTime = System.currentTimeMillis(),
                status = "STOPPED"
            )

            meetingRepository.updateMeeting(updatedMeeting)
            _recordingStatus.value = "STOPPED"
        }
    }
}