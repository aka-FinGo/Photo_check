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
        private const val KEY_CUSTOM_ALBUMS = "key_custom_albums"
        private const val KEY_TRASH_IDS = "key_trash_ids"
        private const val KEY_FAVORITE_IDS = "key_favorite_ids"
        private const val KEY_ALBUM_ASSIGNMENTS = "key_album_assignments"
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

    // 🎴 SLIDEBOX PERSISTENCE
    var customAlbums: Set<String>
        get() = prefs.getStringSet(KEY_CUSTOM_ALBUMS, emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_CUSTOM_ALBUMS, value).apply()

    fun addCustomAlbum(name: String) {
        val current = customAlbums.toMutableSet()
        current.add(name)
        customAlbums = current
    }

    var savedTrashIds: Set<Long>
        get() {
            val raw = prefs.getStringSet(KEY_TRASH_IDS, emptySet()) ?: emptySet()
            return raw.mapNotNull { it.toLongOrNull() }.toSet()
        }
        set(value) {
            val raw = value.map { it.toString() }.toSet()
            prefs.edit().putStringSet(KEY_TRASH_IDS, raw).apply()
        }

    var savedFavoriteIds: Set<Long>
        get() {
            val raw = prefs.getStringSet(KEY_FAVORITE_IDS, emptySet()) ?: emptySet()
            return raw.mapNotNull { it.toLongOrNull() }.toSet()
        }
        set(value) {
            val raw = value.map { it.toString() }.toSet()
            prefs.edit().putStringSet(KEY_FAVORITE_IDS, raw).apply()
        }

    fun getAlbumAssignments(): Map<Long, String> {
        val raw = prefs.getStringSet(KEY_ALBUM_ASSIGNMENTS, emptySet()) ?: emptySet()
        val map = mutableMapOf<Long, String>()
        for (entry in raw) {
            val split = entry.split(":::", limit = 2)
            if (split.size == 2) {
                val id = split[0].toLongOrNull()
                if (id != null) {
                    map[id] = split[1]
                }
            }
        }
        return map
    }

    fun saveAlbumAssignment(mediaId: Long, albumName: String) {
        val current = prefs.getStringSet(KEY_ALBUM_ASSIGNMENTS, emptySet())?.toMutableSet() ?: mutableSetOf()
        current.removeAll { it.startsWith("$mediaId:::") }
        current.add("$mediaId:::$albumName")
        prefs.edit().putStringSet(KEY_ALBUM_ASSIGNMENTS, current).apply()
    }

    fun removeAlbumAssignment(mediaId: Long) {
        val current = prefs.getStringSet(KEY_ALBUM_ASSIGNMENTS, emptySet())?.toMutableSet() ?: mutableSetOf()
        current.removeAll { it.startsWith("$mediaId:::") }
        prefs.edit().putStringSet(KEY_ALBUM_ASSIGNMENTS, current).apply()
    }
}
