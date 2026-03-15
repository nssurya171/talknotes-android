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
            status = "RECORDING"
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
}