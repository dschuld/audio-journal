package com.audiojournal.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.audiojournal.app.ui.RecorderScreen
import com.audiojournal.app.ui.theme.AudioJournalTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AudioJournalTheme {
                RecorderScreen()
            }
        }
    }
}
