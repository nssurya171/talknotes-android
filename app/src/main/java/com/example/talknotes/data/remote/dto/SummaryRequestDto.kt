package com.example.talknotes.data.remote.dto

data class SummaryRequestDto(
    val model: String,
    val messages: List<Message>
)

data class Message(
    val role: String,
    val content: String
)