package com.example.talknotes.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transcripts")
data class Transcript(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val meetingId: Long,

    val chunkIndex: Int,

    val text: String
)