package com.example.talknotes

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.core.content.ContextCompat
import com.example.talknotes.ui.dashboard.DashboardScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val requestAudioPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // later we can show UI message if denied
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
        }
    }
}

@Composable
fun TalkNotesRoot() {
    MaterialTheme {
        Surface {
            DashboardScreen()
        }
    }
}