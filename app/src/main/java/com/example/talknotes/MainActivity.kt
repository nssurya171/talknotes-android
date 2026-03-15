package com.example.talknotes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            TalkNotesRoot()
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

@Composable
fun DashboardScreen() {
    Text("TalkNotes App Started")
}