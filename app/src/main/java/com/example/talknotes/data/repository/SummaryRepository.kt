package com.example.talknotes.data.repository

import com.example.talknotes.data.local.dao.SummaryDao
import com.example.talknotes.data.local.entity.Summary
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SummaryRepository @Inject constructor(
    private val summaryDao: SummaryDao
) {

    suspend fun saveSummary(summary: Summary) {
        summaryDao.insertSummary(summary)
    }

    fun getSummary(meetingId: Long): Flow<Summary?> {
        return summaryDao.getSummary(meetingId)
    }
}