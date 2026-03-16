package com.example.talknotes

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.talknotes.ui.dashboard.DashboardScreen
import com.example.talknotes.ui.recording.RecordingScreen
import com.example.talknotes.viewmodel.DashboardViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val requestAudioPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            checkNotificationPermission()
        }
    }

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        // optional: later show message if denied
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkAudioPermission()

        setContent {
            TalkNotesRoot()
        }
    }

    private fun checkAudioPermission() {
        val permissionStatus = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        )

        if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
            requestAudioPermission.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            checkNotificationPermission()
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notificationPermissionStatus = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            )

            if (notificationPermissionStatus != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

@Composable
fun TalkNotesRoot() {
    var currentScreen by remember { mutableStateOf("dashboard") }
    var recordingStartTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    val dashboardViewModel: DashboardViewModel = hiltViewModel()
    val activeMeetingStatus by dashboardViewModel.activeMeetingStatus.collectAsState()

    MaterialTheme {
        Surface {
            when (currentScreen) {
                "dashboard" -> {
                    DashboardScreen(
                        onNavigateToRecording = { startTime ->
                            recordingStartTime = startTime
                            currentScreen = "recording"
                        }
                    )
                }

                "recording" -> {
                    RecordingScreen(
                        startTimeMillis = recordingStartTime,
                        meetingStatus = activeMeetingStatus,
                        onBack = {
                            currentScreen = "dashboard"
                        },
                        onStopCompleted = {
                            currentScreen = "dashboard"
                        }
                    )
                }
            }
        }
    }
}