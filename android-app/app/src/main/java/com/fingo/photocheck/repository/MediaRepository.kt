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

        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_ADDED,
            MediaStore.Files.FileColumns.MEDIA_TYPE,
            MediaStore.MediaColumns.BUCKET_DISPLAY_NAME,
            MediaStore.Video.VideoColumns.DURATION
        )

        val selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE}=? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE}=?"
        val selectionArgs = arrayOf(
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
        )

        val sortOrder = "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"
        val queryUri = MediaStore.Files.getContentUri("external")

        try {
            context.contentResolver.query(
                queryUri,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndex(MediaStore.Files.FileColumns._ID)
                val nameColumn = cursor.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val sizeColumn = cursor.getColumnIndex(MediaStore.Files.FileColumns.SIZE)
                val dateColumn = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATE_ADDED)
                val typeColumn = cursor.getColumnIndex(MediaStore.Files.FileColumns.MEDIA_TYPE)
                val bucketColumn = cursor.getColumnIndex(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
                val durationColumn = cursor.getColumnIndex(MediaStore.Video.VideoColumns.DURATION)

                while (cursor.moveToNext()) {
                    val id = if (idColumn != -1) cursor.getLong(idColumn) else 0L
                    val name = if (nameColumn != -1) cursor.getString(nameColumn) ?: "Media_$id" else "Media_$id"
                    val size = if (sizeColumn != -1) cursor.getLong(sizeColumn) else 0L
                    val date = if (dateColumn != -1) cursor.getLong(dateColumn) else 0L
                    val typeInt = if (typeColumn != -1) cursor.getInt(typeColumn) else MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
                    val bucketName = if (bucketColumn != -1) cursor.getString(bucketColumn) ?: "Asosiy" else "Asosiy"
                    val duration = if (durationColumn != -1) cursor.getLong(durationColumn) else 0L

                    val mediaType = if (typeInt == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO) {
                        MediaType.VIDEO
                    } else {
                        MediaType.IMAGE
                    }

                    val contentUri = if (mediaType == MediaType.VIDEO) {
                        ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                    } else {
                        ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                    }

                    mediaList.add(
                        MediaItem(
                            id = id,
                            uri = contentUri,
                            displayName = name,
                            size = size,
                            dateAdded = date,
                            mediaType = mediaType,
                            bucketName = bucketName,
                            durationMs = duration
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return@withContext mediaList
    }
}
