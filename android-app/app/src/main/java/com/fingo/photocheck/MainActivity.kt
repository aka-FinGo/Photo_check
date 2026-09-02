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
    private var isScreenPinnedState = mutableStateOf(false)
    private var mediaObserver: ContentObserver? = null
    private var shouldLockOnResume = false

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

    fun startScreenPinning() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                startLockTask()
                isScreenPinnedState.value = true
                Toast.makeText(this, "Ilova ekranga qadandi (Kiosk Rejimi) 📌", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Ekranni qadash imkoni bo'lmadi: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun stopScreenPinning() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                stopLockTask()
                isScreenPinnedState.value = false
                Toast.makeText(this, "Ekranni qadash bekor qilindi 🔓", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            // ignore
        }
    }

    fun setImmersiveMode(enabled: Boolean) {
        try {
            val windowInsetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
            if (enabled) {
                windowInsetsController.systemBarsBehavior =
                    androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                windowInsetsController.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            } else {
                windowInsetsController.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            }
        } catch (e: Exception) {
            // fallback
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = MediaRepository(applicationContext)
        kidsPrefs = KidsPreferencesManager(applicationContext)

        // Setup Exit & Back Protection (Always requires Fingerprint/PIN)
        setupBackPressedProtection()

        // Configure Coil to decode video thumbnails seamlessly with VideoFrameDecoder
        val imageLoader = coil.ImageLoader.Builder(applicationContext)
            .components {
                add(coil.decode.VideoFrameDecoder.Factory())
            }
            .crossfade(true)
            .build()
        coil.Coil.setImageLoader(imageLoader)

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
                        isScreenPinned = isScreenPinnedState.value,
                        onToggleScreenPinning = { shouldPin ->
                            if (shouldPin) {
                                startScreenPinning()
                            } else {
                                stopScreenPinning()
                            }
                        },
                        onSetImmersiveMode = { enabled ->
                            setImmersiveMode(enabled)
                        },
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
                // Any back press that could exit or change state asks for biometrics in Kids Mode
                if (kidsPrefs.isKidsMode) {
                    BiometricAuthManager.authenticate(
                        activity = this@MainActivity,
                        title = "Ilovadan Chiqish",
                        subtitle = "Dasturdan chiqish uchun barmoq izi, yuz yoki tizim parolingizni tasdiqlang",
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
                    isEnabled = false
                    finish()
                }
            }
        })
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // When home button is pressed or app is minimized in Kids Mode
        if (kidsPrefs.isKidsMode) {
            shouldLockOnResume = true
        }
    }

    override fun onResume() {
        super.onResume()
        loadMedia()

        // Require Biometric authentication if app was backgrounded
        if (shouldLockOnResume && kidsPrefs.isKidsMode) {
            shouldLockOnResume = false
            BiometricAuthManager.authenticate(
                activity = this,
                title = "PhotoCheck Kids Qulfi",
                subtitle = "Ilovaga qaytish uchun barmoq izi yoki tizim parolini tasdiqlang",
                onSuccess = {
                    // Success unlocked
                },
                onFailed = {
                    // Lock retained
                    Toast.makeText(this, "Ilova qulflangan", Toast.LENGTH_SHORT).show()
                }
            )
        }
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
