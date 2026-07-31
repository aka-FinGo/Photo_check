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
    val isFavorite: Boolean = false,
    val isQueuedForDeletion: Boolean = false
)
