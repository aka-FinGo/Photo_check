package com.fingo.photocheck.repository

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.fingo.photocheck.model.MediaItem
import com.fingo.photocheck.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaRepository(private val context: Context) {

    suspend fun fetchMediaItems(): List<MediaItem> = withContext(Dispatchers.IO) {
        val mediaList = mutableListOf<MediaItem>()

        // 1. Fetch Images from MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val imageProjection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME
        )

        try {
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                imageProjection,
                null,
                null,
                "${MediaStore.Images.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndex(MediaStore.Images.Media._ID)
                val nameColumn = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
                val sizeColumn = cursor.getColumnIndex(MediaStore.Images.Media.SIZE)
                val dateColumn = cursor.getColumnIndex(MediaStore.Images.Media.DATE_ADDED)
                val bucketColumn = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)

                while (cursor.moveToNext()) {
                    val id = if (idColumn != -1) cursor.getLong(idColumn) else 0L
                    val name = if (nameColumn != -1) cursor.getString(nameColumn) ?: "Image_$id" else "Image_$id"
                    val size = if (sizeColumn != -1) cursor.getLong(sizeColumn) else 0L
                    val date = if (dateColumn != -1) cursor.getLong(dateColumn) else 0L
                    val bucketName = if (bucketColumn != -1) cursor.getString(bucketColumn) ?: "Kamera" else "Kamera"

                    val contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)

                    mediaList.add(
                        MediaItem(
                            id = id,
                            uri = contentUri,
                            displayName = name,
                            size = size,
                            dateAdded = date,
                            mediaType = MediaType.IMAGE,
                            bucketName = bucketName,
                            durationMs = 0L
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Fetch Videos from MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val videoProjection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.VideoColumns.DURATION
        )

        try {
            context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                videoProjection,
                null,
                null,
                "${MediaStore.Video.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndex(MediaStore.Video.Media._ID)
                val nameColumn = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
                val sizeColumn = cursor.getColumnIndex(MediaStore.Video.Media.SIZE)
                val dateColumn = cursor.getColumnIndex(MediaStore.Video.Media.DATE_ADDED)
                val bucketColumn = cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
                val durationColumn = cursor.getColumnIndex(MediaStore.Video.VideoColumns.DURATION)

                while (cursor.moveToNext()) {
                    val id = if (idColumn != -1) cursor.getLong(idColumn) else 0L
                    val name = if (nameColumn != -1) cursor.getString(nameColumn) ?: "Video_$id" else "Video_$id"
                    val size = if (sizeColumn != -1) cursor.getLong(sizeColumn) else 0L
                    val date = if (dateColumn != -1) cursor.getLong(dateColumn) else 0L
                    val bucketName = if (bucketColumn != -1) cursor.getString(bucketColumn) ?: "Videolar" else "Videolar"
                    val duration = if (durationColumn != -1) cursor.getLong(durationColumn) else 0L

                    val contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)

                    mediaList.add(
                        MediaItem(
                            id = id,
                            uri = contentUri,
                            displayName = name,
                            size = size,
                            dateAdded = date,
                            mediaType = MediaType.VIDEO,
                            bucketName = bucketName,
                            durationMs = duration
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Sort combined list by date added (newest first)
        return@withContext mediaList.sortedByDescending { it.dateAdded }
    }
}
