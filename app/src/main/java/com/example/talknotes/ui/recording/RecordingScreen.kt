package com.example.talknotes.ui.recording

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.talknotes.service.RecordingService
import kotlinx.coroutines.delay
import androidx.compose.foundation.clickable

@Composable
fun RecordingScreen(
    startTimeMillis: Long,
    meetingStatus: String?,
    onBack: () -> Unit,
    onStopCompleted: () -> Unit
) {
    val context = LocalContext.current
    var elapsedMillis by remember { mutableLongStateOf(0L) }
    var lastTickTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(meetingStatus) {
        while (true) {
            val now = System.currentTimeMillis()

            if (meetingStatus == "RECORDING") {
                elapsedMillis += (now - lastTickTime)
            }

            lastTickTime = now
            delay(1000)
        }
    }
    val timerText = formatElapsedTime(elapsedMillis)
    val statusText = when (meetingStatus) {
        "RECORDING" -> "Recording..."
        "PAUSED_AUDIO_FOCUS" -> "Paused - Audio focus lost"
        else -> "Preparing..."
    }

    Surface(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = "Back",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.clickable {
                    val intent = Intent(context, RecordingService::class.java).apply {
                        action = RecordingService.ACTION_STOP
                    }
                    context.startService(intent)
                    onBack()
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("🔴")
                Text(
                    text = timerText,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(16.dp))


            Text(
                text = "Listening and taking notes...",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Text(
                    text = "Questions",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Notes",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Transcript",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "Live Transcript",
                        style = MaterialTheme.typography.headlineSmall
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Your conversation will appear here as chunks are processed. For now this is a TwinMind-style placeholder screen.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Chat")
                }

                Button(
                    onClick = {
                        val intent = Intent(context, RecordingService::class.java).apply {
                            action = RecordingService.ACTION_STOP
                        }
                        context.startService(intent)
                        onStopCompleted()
                    },
                    modifier = Modifier.weight(2f)
                ) {
                    Text("$timerText  Stop")
                }
            }
        }
    }
}

fun formatElapsedTime(elapsedMillis: Long): String {
    val totalSeconds = elapsedMillis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}