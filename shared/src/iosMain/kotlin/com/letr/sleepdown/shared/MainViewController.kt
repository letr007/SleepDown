package com.letr.sleepdown.shared

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

fun SleepDownViewController(): UIViewController = ComposeUIViewController {
    SleepDownSharedApp()
}
