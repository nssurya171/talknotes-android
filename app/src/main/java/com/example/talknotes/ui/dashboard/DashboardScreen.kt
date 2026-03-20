package com.example.talknotes.ui.dashboard

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.talknotes.service.RecordingService
import com.example.talknotes.viewmodel.DashboardViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.talknotes.ui.recording.formatElapsedTime

@Composable
fun DashboardScreen(
    onNavigateToRecording: (Long) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val meetings by viewModel.meetings.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val activeMeetingStartTime by viewModel.activeMeetingStartTime.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var currentTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(isRecording) {
        while (isRecording) {
            currentTimeMillis = System.currentTimeMillis()
            delay(1000)
        }
    }

    val elapsedMillis = if (isRecording && activeMeetingStartTime != null) {
        currentTimeMillis - activeMeetingStartTime!!
    } else {
        0L
    }

    val timerText = formatElapsedTime(elapsedMillis)

    Scaffold(
        floatingActionButton = {
            if (!isRecording) {
                FloatingActionButton(
                    onClick = {
                        val permissionGranted = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED

                        android.util.Log.d("TalkNotes", "FAB clicked, permissionGranted=$permissionGranted")

                        if (!permissionGranted) {
                            android.util.Log.d("TalkNotes", "Returning because RECORD_AUDIO not granted")
                            return@FloatingActionButton
                        }

                        coroutineScope.launch {
                            android.util.Log.d("TalkNotes", "Trying to create meeting")
                            val meetingId = viewModel.createNewMeetingIfPossible()
                            android.util.Log.d("TalkNotes", "meetingId=$meetingId")
                            android.util.Log.d("TalkNotes", "Starting recording service for meetingId=$meetingId")

                            if (meetingId != null) {
                                val startTime = System.currentTimeMillis()
                                val intent = Intent(context, RecordingService::class.java).apply {
                                    action = RecordingService.ACTION_START
                                    putExtra(
                                        RecordingService.EXTRA_MEETING_ID,
                                        meetingId.toLong()
                                    )
                                }
                                ContextCompat.startForegroundService(context, intent)
                                android.util.Log.d("TalkNotes", "Started service, navigating to recording")
                                onNavigateToRecording(startTime)
                            }
                        }
                    }
                ) {
                    Text("+")
                }
            }
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "TalkNotes",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isRecording) {
                Text(
                    text = timerText,
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        viewModel.stopAllRecordings()

                        val intent = Intent(context, RecordingService::class.java).apply {
                            action = RecordingService.ACTION_STOP
                        }
                        context.startService(intent)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Stop Recording")
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            if (meetings.isEmpty()) {
                Text("No meetings recorded yet")
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(meetings) { meeting ->
                        MeetingItem(
                            meetingId = meeting.id,
                            title = meeting.title,
                            status = meeting.status,
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MeetingItem(
    meetingId: Long,
    title: String,
    status: String,
    viewModel: DashboardViewModel
) {
    val context = LocalContext.current
    val chunkCount by viewModel.getChunkCountForMeeting(meetingId).collectAsState(initial = 0)
    val transcriptList by viewModel.getTranscriptForMeeting(meetingId).collectAsState(initial = emptyList())
    val summary by viewModel.getSummaryForMeeting(meetingId).collectAsState(initial = null)
    val displayStatus = when (status) {
        "RECORDING" -> "Recording..."
        "STOPPED" -> "Stopped"
        "PAUSED_AUDIO_FOCUS" -> "Paused - Audio focus lost"
        else -> status
    }
    val summaryStatusText = when (summary?.status) {
        "PROCESSING" -> "Summary: Generating..."
        "DONE" -> "Summary: Available"
        "FAILED" -> "Summary: Failed"
        else -> "Summary: Not generated"
    }
    val transcriptPreview = transcriptList
        .take(2)
        .joinToString(" ") { it.text }
        .ifBlank { "No transcript generated yet" }
    val summaryData = summary

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Status: $displayStatus",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Chunks: $chunkCount",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Transcript lines: ${transcriptList.size}",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Transcript Preview",
                style = MaterialTheme.typography.titleSmall
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = transcriptPreview,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = summaryStatusText,
                style = MaterialTheme.typography.bodyMedium
            )


            if ((status == "STOPPED" || status == "PAUSED_AUDIO_FOCUS") && transcriptList.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        viewModel.generateRealSummary(context, meetingId)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Generate Summary")
                }
            }


            if (summaryData != null && summaryData.status == "DONE") {
                Spacer(modifier = Modifier.height(12.dp))

                Text("Title", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(4.dp))
                Text(summaryData.title, style = MaterialTheme.typography.bodyMedium)

                Spacer(modifier = Modifier.height(8.dp))

                Text("Summary", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(4.dp))
                Text(summaryData.summary, style = MaterialTheme.typography.bodyMedium)

                Spacer(modifier = Modifier.height(8.dp))

                Text("Action Items", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(4.dp))
                Text(summaryData.actionItems, style = MaterialTheme.typography.bodyMedium)

                Spacer(modifier = Modifier.height(8.dp))

                Text("Key Points", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(4.dp))
                Text(summaryData.keyPoints, style = MaterialTheme.typography.bodyMedium)
            }


            if (summaryData?.status == "FAILED") {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = summaryData.errorMessage ?: "Summary generation failed",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
