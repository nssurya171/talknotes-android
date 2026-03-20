package com.example.talknotes.data.remote.api

import com.example.talknotes.data.remote.dto.SummaryRequestDto
import com.example.talknotes.data.remote.dto.SummaryResponseDto
import retrofit2.http.Body
import retrofit2.http.POST

interface SummaryApi {
    @POST("chat/completions")
    suspend fun generateSummary(
        @Body request: SummaryRequestDto
    ): SummaryResponseDto
}