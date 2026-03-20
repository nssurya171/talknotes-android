package com.example.talknotes.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "summaries")
data class Summary(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val meetingId: Long,

    val title: String = "",

    val summary: String = "",

    val actionItems: String = "",

    val keyPoints: String = "",

    val status: String = "PENDING", // PENDING, PROCESSING, DONE, FAILED

    val errorMessage: String? = null,

    val updatedAt: Long = System.currentTimeMillis()
)