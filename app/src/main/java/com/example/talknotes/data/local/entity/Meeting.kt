package com.example.talknotes.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meetings")
data class Meeting(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val title: String,

    val startTime: Long,

    val endTime: Long? = null,

    val status: String
)