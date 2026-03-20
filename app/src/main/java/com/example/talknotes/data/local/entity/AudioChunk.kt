package com.example.talknotes.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audio_chunks")
data class AudioChunk(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val meetingId: Long,

    val chunkIndex: Int,

    val filePath: String,

    val uploaded: Boolean = false,

    val transcriptionStatus: String = "PENDING", // PENDING, PROCESSING, DONE, FAILED

    val retryCount: Int = 0,

    val errorMessage: String? = null,

    val startTimeMs: Long? = null,

    val endTimeMs: Long? = null,

    val createdAt: Long = System.currentTimeMillis()
)