package com.letr.sleepdown

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import androidx.core.app.LocaleManagerCompat

/** Applies the app-selected locale to contexts used outside AppCompat activities. */
fun Context.withSleepDownAppLocales(): Context {
    val languageTags = LocaleManagerCompat.getApplicationLocales(this).toLanguageTags()
    if (languageTags.isBlank()) return this
    val configuration = Configuration(resources.configuration)
    configuration.setLocales(LocaleList.forLanguageTags(languageTags))
    return createConfigurationContext(configuration)
}
