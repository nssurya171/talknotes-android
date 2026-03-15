package com.example.talknotes.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.MediaRecorder
import android.os.Build
import android.os.Environment
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.talknotes.R
import com.example.talknotes.data.local.entity.AudioChunk
import com.example.talknotes.data.repository.AudioChunkRepository
import com.example.talknotes.data.repository.MeetingRepository
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RecordingService : Service() {

    @Inject
    lateinit var meetingRepository: MeetingRepository

    @Inject
    lateinit var audioChunkRepository: AudioChunkRepository

    private var mediaRecorder: MediaRecorder? = null
    private var outputFilePath: String? = null

    private var currentMeetingId: Long = -1L
    private var currentChunkIndex: Int = 0
    private var chunkingJob: Job? = null

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                currentMeetingId = intent.getLongExtra(EXTRA_MEETING_ID, -1L)

                if (currentMeetingId == -1L) {
                    stopSelf()
                    return START_NOT_STICKY
                }

                startForeground(NOTIFICATION_ID, buildNotification("Recording..."))

                if (mediaRecorder == null) {
                    currentChunkIndex = 0
                    startChunkingLoop()
                }
            }

            ACTION_STOP -> {
                serviceScope.launch {
                    meetingRepository.stopAllActiveRecordings()
                    stopChunkingLoop()
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        stopChunkingLoop()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startChunkingLoop() {
        chunkingJob?.cancel()

        chunkingJob = serviceScope.launch {
            while (true) {
                currentChunkIndex++

                startRecordingChunk()
                delay(CHUNK_DURATION_MS)
                stopRecordingChunkAndSaveMetadata()
            }
        }
    }

    private fun stopChunkingLoop() {
        chunkingJob?.cancel()
        chunkingJob = null

        stopRecordingSilently()
    }

    private fun startRecordingChunk() {
        val baseDir = getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: filesDir
        val audioDir = File(baseDir, "recordings")

        if (!audioDir.exists()) {
            audioDir.mkdirs()
        }

        outputFilePath = File(
            audioDir,
            "meeting_${currentMeetingId}_chunk_${currentChunkIndex}.m4a"
        ).absolutePath

        android.util.Log.d("TalkNotes", "Recording file path: $outputFilePath")

        try {
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                MediaRecorder()
            }

            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setOutputFile(outputFilePath)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
        }
    }

    private suspend fun stopRecordingChunkAndSaveMetadata() {
        val savedPath = outputFilePath

        try {
            mediaRecorder?.apply {
                stop()
                reset()
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaRecorder = null
        }

        if (!savedPath.isNullOrBlank()) {
            audioChunkRepository.saveChunk(
                AudioChunk(
                    meetingId = currentMeetingId,
                    chunkIndex = currentChunkIndex,
                    filePath = savedPath,
                    uploaded = false
                )
            )
        }
    }

    private fun stopRecordingSilently() {
        try {
            mediaRecorder?.apply {
                stop()
                reset()
                release()
            }
        } catch (_: Exception) {
        } finally {
            mediaRecorder = null
        }
    }

    private fun buildNotification(contentText: String): Notification {
        val stopIntent = Intent(this, RecordingService::class.java).apply {
            action = ACTION_STOP
        }

        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("TalkNotes")
            .setContentText(contentText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .addAction(
                android.R.drawable.ic_media_pause,
                "Stop",
                stopPendingIntent
            )
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Recording Service",
                NotificationManager.IMPORTANCE_LOW
            )

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "recording_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "ACTION_START_RECORDING"
        const val ACTION_STOP = "ACTION_STOP_RECORDING"

        const val EXTRA_MEETING_ID = "extra_meeting_id"

        const val CHUNK_DURATION_MS = 30_000L
    }
}