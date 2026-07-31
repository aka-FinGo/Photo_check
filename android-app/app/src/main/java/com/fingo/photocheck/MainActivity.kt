package com.fingo.photocheck

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
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
                            // Deletion logic
                        }
                    )
                }
            }
        }
    }

    private fun requestStoragePermissions() {
        val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        permissionLauncher.launch(permissionsToRequest)
    }

    private fun loadMedia() {
        lifecycleScope.launch {
            val items = repository.fetchMediaItems()
            mediaListState.value = items
        }
    }
}
