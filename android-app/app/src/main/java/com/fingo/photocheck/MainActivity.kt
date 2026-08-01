package com.fingo.photocheck

import android.Manifest
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.lifecycleScope
import com.fingo.photocheck.model.MediaItem
import com.fingo.photocheck.repository.MediaRepository
import com.fingo.photocheck.ui.PhotoCheckApp
import com.fingo.photocheck.ui.theme.PhotoCheckTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var repository: MediaRepository
    private var mediaListState = mutableStateOf<List<MediaItem>>(emptyList())
    private var mediaObserver: ContentObserver? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Load media regardless of individual permission granularity
        loadMedia()
        registerMediaObserver()
    }

    private val deleteLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            loadMedia()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = MediaRepository(applicationContext)

        requestStoragePermissions()

        setContent {
            PhotoCheckTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0A0A0F)
                ) {
                    PhotoCheckApp(
                        mediaList = mediaListState.value,
                        onDeleteMediaItems = { itemsToDelete ->
                            deleteMediaItems(itemsToDelete)
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadMedia()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaObserver?.let {
            contentResolver.unregisterContentObserver(it)
        }
    }

    private fun requestStoragePermissions() {
        val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        permissionLauncher.launch(permissionsToRequest)
    }

    private fun registerMediaObserver() {
        if (mediaObserver == null) {
            mediaObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean, uri: Uri?) {
                    super.onChange(selfChange, uri)
                    loadMedia()
                }
            }
            contentResolver.registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                true,
                mediaObserver!!
            )
            contentResolver.registerContentObserver(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                true,
                mediaObserver!!
            )
        }
    }

    private fun loadMedia() {
        lifecycleScope.launch {
            val items = repository.fetchMediaItems()
            mediaListState.value = items
        }
    }

    private fun deleteMediaItems(items: List<MediaItem>) {
        if (items.isEmpty()) return
        val uris = items.map { it.uri }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val pendingIntent = MediaStore.createTrashRequest(contentResolver, uris, true)
                deleteLauncher.launch(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
            } catch (e: Exception) {
                try {
                    val pendingIntent = MediaStore.createDeleteRequest(contentResolver, uris)
                    deleteLauncher.launch(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
        } else {
            for (uri in uris) {
                try {
                    contentResolver.delete(uri, null, null)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            loadMedia()
        }
    }
}
