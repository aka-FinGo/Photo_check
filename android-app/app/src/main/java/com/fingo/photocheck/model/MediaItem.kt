package com.fingo.photocheck.model

import android.net.Uri

enum class MediaType {
    IMAGE, VIDEO
}

data class MediaItem(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val size: Long,
    val dateAdded: Long,
    val mediaType: MediaType,
    val bucketName: String = "Galereya",
    val durationMs: Long = 0L,
    val isFavorite: Boolean = false,
    val isQueuedForDeletion: Boolean = false
) {
    val formattedSize: String
        get() {
            val mb = size / (1024.0 * 1024.0)
            return if (mb >= 1000) {
                String.format("%.2f GB", mb / 1024.0)
            } else {
                String.format("%.1f MB", mb)
            }
        }

    val formattedDuration: String
        get() {
            if (durationMs <= 0) return ""
            val seconds = (durationMs / 1000) % 60
            val minutes = (durationMs / (1000 * 60)) % 60
            val hours = durationMs / (1000 * 60 * 60)
            return if (hours > 0) {
                String.format("%d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%d:%02d", minutes, seconds)
            }
        }
}
