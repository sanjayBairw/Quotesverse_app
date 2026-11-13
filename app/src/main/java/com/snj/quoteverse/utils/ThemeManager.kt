package com.snj.quoteverse.utils

import androidx.appcompat.app.AppCompatDelegate

/**
 * Applies the persisted light, dark, or system theme mode across the application.
 */
object ThemeManager {

    fun applyTheme(mode: ThemeMode) {
        AppCompatDelegate.setDefaultNightMode(mode.nightMode)
    }
}

