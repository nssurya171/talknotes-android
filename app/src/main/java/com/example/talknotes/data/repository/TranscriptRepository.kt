package com.example.talknotes.data.repository

import com.example.talknotes.data.local.dao.TranscriptDao
import com.example.talknotes.data.local.entity.Transcript
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranscriptRepository @Inject constructor(
    private val transcriptDao: TranscriptDao
) {

    suspend fun saveTranscript(transcript: Transcript) {
        transcriptDao.insertTranscript(transcript)
    }

    fun getTranscript(meetingId: Long): Flow<List<Transcript>> {
        return transcriptDao.getTranscriptForMeeting(meetingId)
    }

    suspend fun clearTranscriptForMeeting(meetingId: Long) {
        transcriptDao.deleteTranscriptsForMeeting(meetingId)
    }
}