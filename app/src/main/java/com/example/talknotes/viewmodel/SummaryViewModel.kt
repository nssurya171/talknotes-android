package com.example.talknotes.viewmodel

import androidx.lifecycle.ViewModel
import com.example.talknotes.data.local.entity.Summary
import com.example.talknotes.data.local.entity.Transcript
import com.example.talknotes.data.repository.SummaryRepository
import com.example.talknotes.data.repository.TranscriptRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

@HiltViewModel
class SummaryViewModel @Inject constructor(
    private val transcriptRepository: TranscriptRepository,
    private val summaryRepository: SummaryRepository
) : ViewModel() {

    fun getTranscript(meetingId: Long): Flow<List<Transcript>> {
        return transcriptRepository.getTranscript(meetingId)
    }

    fun getSummary(meetingId: Long): Flow<Summary?> {
        return summaryRepository.getSummary(meetingId)
    }
}