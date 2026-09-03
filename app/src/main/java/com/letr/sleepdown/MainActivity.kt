package com.letr.sleepdown

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.letr.sleepdown.ui.TimetableApp
import com.letr.sleepdown.ui.TimetableTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = AppContainer.repo(this)
        setContent {
            TimetableTheme {
                TimetableApp(repository)
            }
        }
    }
}
