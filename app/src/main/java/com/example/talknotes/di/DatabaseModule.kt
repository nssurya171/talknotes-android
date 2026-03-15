package com.example.talknotes.di

import android.content.Context
import androidx.room.Room
import com.example.talknotes.data.local.database.AppDatabase
import com.example.talknotes.data.local.dao.AudioChunkDao
import com.example.talknotes.data.local.dao.MeetingDao
import com.example.talknotes.data.local.dao.SummaryDao
import com.example.talknotes.data.local.dao.TranscriptDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {

        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "talknotes_db"
        ).build()
    }

    @Provides
    fun provideMeetingDao(database: AppDatabase): MeetingDao {
        return database.meetingDao()
    }

    @Provides
    fun provideAudioChunkDao(database: AppDatabase): AudioChunkDao {
        return database.audioChunkDao()
    }

    @Provides
    fun provideTranscriptDao(database: AppDatabase): TranscriptDao {
        return database.transcriptDao()
    }

    @Provides
    fun provideSummaryDao(database: AppDatabase): SummaryDao {
        return database.summaryDao()
    }
}