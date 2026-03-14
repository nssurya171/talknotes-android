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

    val uploaded: Boolean = false
)