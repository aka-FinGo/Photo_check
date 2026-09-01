package com.fingo.photocheck

import android.Manifest
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.fingo.photocheck.auth.BiometricAuthManager
import com.fingo.photocheck.data.KidsPreferencesManager
import com.fingo.photocheck.model.MediaItem
import com.fingo.photocheck.repository.MediaRepository
import com.fingo.photocheck.ui.PhotoCheckApp
import com.fingo.photocheck.ui.theme.PhotoCheckTheme
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {

    private lateinit var repository: MediaRepository
    private lateinit var kidsPrefs: KidsPreferencesManager
    private var mediaListState = mutableStateOf<List<MediaItem>>(emptyList())
    private var mediaObserver: ContentObserver? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
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
        kidsPrefs = KidsPreferencesManager(applicationContext)

        // Setup Exit Protection (Kids Lock)
        setupBackPressedProtection()

        // Request storage permissions and observe gallery
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
                        },
                        onRequestBiometricAuth = { title, onSuccess ->
                            BiometricAuthManager.authenticate(
                                activity = this@MainActivity,
                                title = title,
                                subtitle = "Barmoq izi, yuz yoki tizim paroli bilan tasdiqlang",
                                onSuccess = onSuccess,
                                onFailed = {
                                    Toast.makeText(
                                        this@MainActivity,
                                        "Tasdiqlanmadi",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            )
                        }
                    )
                }
            }
        }
    }

    private fun setupBackPressedProtection() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (kidsPrefs.isKidsMode) {
                    // Kids Mode: require parent authentication before exiting app
                    BiometricAuthManager.authenticate(
                        activity = this@MainActivity,
                        title = "Ilovadan Chiqish",
                        subtitle = "Chiqish uchun ota-ona barmoq izi yoki tizim parolini tasdiqlang",
                        onSuccess = {
                            isEnabled = false
                            finish()
                        },
                        onFailed = {
                            Toast.makeText(
                                this@MainActivity,
                                "Ilovadan chiqish uchun ota-ona tasdig'i shart!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                } else {
                    // Pro / Classic Mode: normal exit
                    isEnabled = false
                    finish()
                }
            }
        })
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
                    // Real-time live gallery sync with MIUI / Android System Gallery!
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
