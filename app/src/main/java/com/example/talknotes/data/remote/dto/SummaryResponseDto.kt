package com.example.talknotes.data.remote.dto

data class SummaryResponseDto(
    val choices: List<Choice>
)

data class Choice(
    val message: ChoiceMessage
)

data class ChoiceMessage(
    val content: String
)