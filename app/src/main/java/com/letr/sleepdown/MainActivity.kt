package com.letr.sleepdown

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.letr.sleepdown.ui.TimetableApp
import com.letr.sleepdown.ui.TimetableTheme
import com.letr.sleepdown.widget.WIDGET_TABLE_ID_EXTRA

class MainActivity : ComponentActivity() {
    private var incomingFileUri by mutableStateOf<Uri?>(null)
    private var incomingTableId by mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureSystemBars()
        applyLaunchIntent(intent)
        val repository = AppContainer.repo(this)
        setContent {
            TimetableTheme {
                TimetableApp(
                    repository,
                    initialTableId = incomingTableId,
                    initialImportUri = incomingFileUri,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        applyLaunchIntent(intent)
    }

    private fun applyLaunchIntent(source: Intent) {
        incomingFileUri = source.takeIf { it.action == Intent.ACTION_VIEW }?.data
        incomingTableId = source.getLongExtra(WIDGET_TABLE_ID_EXTRA, 0L).takeIf { it > 0L }
    }

    private fun configureSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
    }
}
