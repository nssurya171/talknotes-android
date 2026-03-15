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
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager


@AndroidEntryPoint
class RecordingService : Service() {

    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var hasAudioFocus: Boolean = false
    private var isPausedByAudioFocus: Boolean = false

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

    private fun updateNotification(contentText: String) {
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, buildNotification(contentText))
    }

    private suspend fun handleAudioFocusLoss() {
        if (currentMeetingId == -1L || isPausedByAudioFocus) return

        isPausedByAudioFocus = true

        stopChunkingLoopAndSavePartialChunk()
        meetingRepository.updateMeetingStatus(currentMeetingId, "PAUSED_AUDIO_FOCUS")
        updateNotification("Paused - Audio focus lost")
    }

    private suspend fun handleAudioFocusGain() {
        if (currentMeetingId == -1L || !isPausedByAudioFocus) return

        isPausedByAudioFocus = false

        meetingRepository.updateMeetingStatus(currentMeetingId, "RECORDING")
        updateNotification("Recording...")

        if (mediaRecorder == null) {
            startChunkingLoop()
        }
    }

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                serviceScope.launch {
                    handleAudioFocusLoss()
                }
            }

            AudioManager.AUDIOFOCUS_GAIN -> {
                serviceScope.launch {
                    handleAudioFocusGain()
                }
            }
        }
    }

    private fun requestAudioFocus(): Boolean {
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build()

            audioFocusRequest = request
            val result = audioManager?.requestAudioFocus(request)
            hasAudioFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            hasAudioFocus
        } else {
            @Suppress("DEPRECATION")
            val result = audioManager?.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
            hasAudioFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            hasAudioFocus
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { request ->
                audioManager?.abandonAudioFocusRequest(request)
            }
        } else {
            @Suppress("DEPRECATION")
            audioManager?.abandonAudioFocus(audioFocusChangeListener)
        }
        hasAudioFocus = false
    }

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

                val focusGranted = requestAudioFocus()
                if (!focusGranted) {
                    serviceScope.launch {
                        meetingRepository.updateMeetingStatus(currentMeetingId, "PAUSED_AUDIO_FOCUS")
                        updateNotification("Paused - Audio focus lost")
                    }
                    return START_STICKY
                }

                isPausedByAudioFocus = false

                if (mediaRecorder == null) {
                    currentChunkIndex = 0
                    startChunkingLoop()
                }
            }

            ACTION_STOP -> {
                serviceScope.launch {
                    meetingRepository.stopAllActiveRecordings()
                    stopChunkingLoopAndSavePartialChunk()
                    abandonAudioFocus()
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        chunkingJob?.cancel()
        chunkingJob = null
        abandonAudioFocus()
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

    private suspend fun stopChunkingLoopAndSavePartialChunk() {
        chunkingJob?.cancel()
        chunkingJob = null

        stopCurrentChunkAndSaveIfNeeded()
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
        outputFilePath = null
    }

    private suspend fun stopCurrentChunkAndSaveIfNeeded() {
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
            val file = File(savedPath)

            if (file.exists() && file.length() > 0L) {
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

        outputFilePath = null
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