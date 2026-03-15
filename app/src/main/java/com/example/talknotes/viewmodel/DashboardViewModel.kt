package com.example.talknotes.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.talknotes.data.local.entity.Meeting
import com.example.talknotes.data.repository.MeetingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val meetingRepository: MeetingRepository
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

    fun startNewMeeting(title: String = "New Meeting") {
        viewModelScope.launch {
            val activeMeetings = meetingRepository.getActiveRecordings()

            if (activeMeetings.isEmpty()) {
                meetingRepository.createMeeting(title)
            }
        }
    }

    fun stopAllRecordings() {
        viewModelScope.launch {
            meetingRepository.stopAllActiveRecordings()
        }
    }
}