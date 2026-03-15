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

@Composable
fun DashboardScreen(
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

                        if (!permissionGranted) {
                            return@FloatingActionButton
                        }

                        coroutineScope.launch {
                            val meetingId = viewModel.createNewMeetingIfPossible()

                            if (meetingId != null) {
                                val intent = Intent(context, RecordingService::class.java).apply {
                                    action = RecordingService.ACTION_START
                                    putExtra(
                                        RecordingService.EXTRA_MEETING_ID,
                                        meetingId.toLong()
                                    )
                                }
                                ContextCompat.startForegroundService(context, intent)
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
                            title = meeting.title,
                            status = meeting.status
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MeetingItem(
    title: String,
    status: String
) {
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
                text = "Status: $status",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

fun formatElapsedTime(elapsedMillis: Long): String {
    val totalSeconds = elapsedMillis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}