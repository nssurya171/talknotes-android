package com.example.talknotes.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.talknotes.data.local.dao.AudioChunkDao
import com.example.talknotes.data.local.dao.MeetingDao
import com.example.talknotes.data.local.dao.SummaryDao
import com.example.talknotes.data.local.dao.TranscriptDao
import com.example.talknotes.data.local.entity.AudioChunk
import com.example.talknotes.data.local.entity.Meeting
import com.example.talknotes.data.local.entity.Summary
import com.example.talknotes.data.local.entity.Transcript

@Database(
    entities = [
        Meeting::class,
        AudioChunk::class,
        Transcript::class,
        Summary::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun meetingDao(): MeetingDao

    abstract fun audioChunkDao(): AudioChunkDao

    abstract fun transcriptDao(): TranscriptDao

    abstract fun summaryDao(): SummaryDao
}