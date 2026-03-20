package com.example.talknotes.data.repository

import com.example.talknotes.data.local.dao.MeetingDao
import com.example.talknotes.data.local.entity.Meeting
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeetingRepository @Inject constructor(
    private val meetingDao: MeetingDao
) {

    suspend fun createMeeting(title: String): Long {
        val meeting = Meeting(
            title = title,
            startTime = System.currentTimeMillis(),
            status = "RECORDING",
            transcriptionStatus = "PENDING",
            summaryStatus = "PENDING",
            lastChunkIndex = -1
        )
        return meetingDao.insertMeeting(meeting)
    }

    fun getMeetings(): Flow<List<Meeting>> {
        return meetingDao.getMeetings()
    }

    suspend fun updateMeeting(meeting: Meeting) {
        meetingDao.updateMeeting(meeting)
    }

    suspend fun getMeetingById(meetingId: Long): Meeting? {
        return meetingDao.getMeetingById(meetingId)
    }

    suspend fun getActiveRecordings(): List<Meeting> {
        return meetingDao.getActiveRecordings()
    }

    suspend fun stopAllActiveRecordings() {
        val activeMeetings = meetingDao.getActiveRecordings()

        activeMeetings.forEach { meeting ->
            meetingDao.updateMeeting(
                meeting.copy(
                    endTime = System.currentTimeMillis(),
                    status = "STOPPED"
                )
            )
        }
    }

    suspend fun updateMeetingStatus(meetingId: Long, status: String) {
        meetingDao.updateMeetingStatus(meetingId, status)
    }

    suspend fun updateMeetingTranscriptionStatus(meetingId: Long, status: String) {
        meetingDao.updateMeetingTranscriptionStatus(meetingId, status)
    }

    suspend fun updateMeetingSummaryStatus(meetingId: Long, status: String) {
        meetingDao.updateMeetingSummaryStatus(meetingId, status)
    }

    suspend fun updateLastChunkIndex(meetingId: Long, lastChunkIndex: Int) {
        meetingDao.updateLastChunkIndex(meetingId, lastChunkIndex)
    }
}