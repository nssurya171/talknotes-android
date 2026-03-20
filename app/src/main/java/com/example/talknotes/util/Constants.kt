package com.example.talknotes.util

object Constants {
    const val OPENAI_BASE_URL = "https://api.openai.com/"
    const val OPENROUTER_BASE_URL = "https://openrouter.ai/api/v1/"

    const val MODEL_SUMMARY = "openrouter/free"
    const val WHISPER_MODEL = "whisper-1"

    const val WORK_INPUT_CHUNK_ID = "chunk_id"
    const val WORK_INPUT_MEETING_ID = "meeting_id"
    const val WORK_INPUT_FORCE_REGENERATE = "force_regenerate"

    const val STATUS_PENDING = "PENDING"
    const val STATUS_PROCESSING = "PROCESSING"
    const val STATUS_DONE = "DONE"
    const val STATUS_FAILED = "FAILED"

    const val MEETING_STATUS_RECORDING = "RECORDING"
    const val MEETING_STATUS_PAUSED = "PAUSED"
    const val MEETING_STATUS_STOPPED = "STOPPED"
}