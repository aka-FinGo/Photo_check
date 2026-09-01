package com.fingo.photocheck.data

import android.content.Context
import android.content.SharedPreferences

class KidsPreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("photocheck_kids_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_IS_KIDS_MODE = "is_kids_mode"
        private const val KEY_WHITELISTED_ALBUMS = "whitelisted_albums"
        private const val KEY_TIMER_LIMIT_MINUTES = "timer_limit_minutes"
        private const val KEY_IS_FIRST_LAUNCH = "is_first_launch"
    }

    var isKidsMode: Boolean
        get() = prefs.getBoolean(KEY_IS_KIDS_MODE, true)
        set(value) = prefs.edit().putBoolean(KEY_IS_KIDS_MODE, value).apply()

    var whitelistedAlbums: Set<String>
        get() = prefs.getStringSet(KEY_WHITELISTED_ALBUMS, emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_WHITELISTED_ALBUMS, value).apply()

    var timerLimitMinutes: Int
        get() = prefs.getInt(KEY_TIMER_LIMIT_MINUTES, 30) // Default 30 min
        set(value) = prefs.edit().putInt(KEY_TIMER_LIMIT_MINUTES, value).apply()

    var isFirstLaunch: Boolean
        get() = prefs.getBoolean(KEY_IS_FIRST_LAUNCH, true)
        set(value) = prefs.edit().putBoolean(KEY_IS_FIRST_LAUNCH, value).apply()

    fun toggleAlbum(albumName: String) {
        val current = whitelistedAlbums.toMutableSet()
        if (current.contains(albumName)) {
            current.remove(albumName)
        } else {
            current.add(albumName)
        }
        whitelistedAlbums = current
    }

    fun setAllAlbums(albums: List<String>) {
        whitelistedAlbums = albums.toSet()
    }

    fun clearAllAlbums() {
        whitelistedAlbums = emptySet()
    }
}
