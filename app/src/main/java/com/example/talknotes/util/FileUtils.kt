package com.example.talknotes.util

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

object FileUtils {

    fun createAudioPart(file: File): MultipartBody.Part {
        val requestFile = file.asRequestBody("audio/mp4".toMediaTypeOrNull())
        return MultipartBody.Part.createFormData(
            name = "file",
            filename = file.name,
            body = requestFile
        )
    }

    fun createModelPart(model: String): RequestBody {
        return model.toRequestBody("text/plain".toMediaTypeOrNull())
    }
}