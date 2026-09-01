package com.fingo.photocheck.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.fingo.photocheck.data.KidsPreferencesManager
import com.fingo.photocheck.model.MediaItem
import com.fingo.photocheck.model.MediaType
import com.fingo.photocheck.ui.kids.KidsSafeGalleryScreen
import com.fingo.photocheck.ui.parent.ParentSettingsScreen
import com.fingo.photocheck.update.UpdateDialog
import com.fingo.photocheck.update.UpdateInfo
import com.fingo.photocheck.update.UpdateManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

// Undo Action Model for 1:1 Slidebox experience
sealed interface SlideboxAction {
    data class Trashed(val item: MediaItem, val previousIndex: Int) : SlideboxAction
    data class AlbumSorted(val item: MediaItem, val albumName: String, val previousIndex: Int) : SlideboxAction
    data class Favorited(val item: MediaItem, val previousState: Boolean) : SlideboxAction
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoCheckApp(
    mediaList: List<MediaItem>,
    isScreenPinned: Boolean = false,
    onToggleScreenPinning: (Boolean) -> Unit = {},
    onSetImmersiveMode: (Boolean) -> Unit = {},
    onDeleteMediaItems: (List<MediaItem>) -> Unit,
    onRequestBiometricAuth: (title: String, onSuccess: () -> Unit) -> Unit = { _, success -> success() }
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val kidsPrefs = remember { KidsPreferencesManager(context) }

    var isKidsMode by remember { mutableStateOf(kidsPrefs.isKidsMode) }
    var whitelistedAlbums by remember { mutableStateOf(kidsPrefs.whitelistedAlbums) }
    var timerLimitMinutes by remember { mutableIntStateOf(kidsPrefs.timerLimitMinutes) }
    var remainingSeconds by remember { mutableLongStateOf(timerLimitMinutes * 60L) }
    var isTimerExpired by remember { mutableStateOf(false) }

    var showParentSettings by remember { mutableStateOf(false) }
    var isClassicModeActive by remember { mutableStateOf(!kidsPrefs.isKidsMode) }

    // Control Immersive Sticky Fullscreen in Kids Mode
    LaunchedEffect(isKidsMode, isClassicModeActive, showParentSettings) {
        if (isKidsMode && !isClassicModeActive && !showParentSettings) {
            onSetImmersiveMode(true)
        } else {
            onSetImmersiveMode(false)
        }
    }

    // In-App Update State
    var availableUpdate by remember { mutableStateOf<UpdateInfo?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val result = UpdateManager.checkForUpdates(context)
        result.onSuccess { info ->
            if (info.hasUpdate) {
                availableUpdate = info
            }
        }
    }

    // Live timer countdown for Kids Mode
    LaunchedEffect(isKidsMode, isClassicModeActive, timerLimitMinutes) {
        if (isKidsMode && !isClassicModeActive && timerLimitMinutes > 0) {
            remainingSeconds = timerLimitMinutes * 60L
            isTimerExpired = false
            while (remainingSeconds > 0) {
                delay(1000L)
                remainingSeconds--
            }
            isTimerExpired = true
        } else {
            isTimerExpired = false
        }
    }

    // --- 1:1 SLIDEBOX STATE ---
    val favorites = remember { mutableStateListOf<Long>() }
    val trash = remember { mutableStateListOf<Long>() }
    val customAlbums = remember { mutableStateListOf<String>() }
    val photoAlbumAssignments = remember { mutableStateMapOf<Long, String>() }
    val historyStack = remember { mutableStateListOf<SlideboxAction>() }

    var currentIndex by remember { mutableIntStateOf(0) }
    var selectedFilter by remember { mutableStateOf("BARCHA FAYLLAR") }
    var showDropdownMenu by remember { mutableStateOf(false) }
    var showTrashSheet by remember { mutableStateOf(false) }
    var showGridView by remember { mutableStateOf(false) }
    var showNewAlbumDialog by remember { mutableStateOf(false) }
    var newAlbumNameInput by remember { mutableStateOf("") }

    // Real device albums + custom created albums
    val allAlbumsList = remember(mediaList, customAlbums) {
        val deviceAlbums = mediaList.map { it.bucketName }.filter { it.isNotBlank() }.distinct()
        val combined = (deviceAlbums + customAlbums).distinct().sorted()
        if (combined.isEmpty()) listOf("BARCHA FAYLLAR") else listOf("BARCHA FAYLLAR") + combined
    }

    val activeList = remember(mediaList, trash, selectedFilter) {
        mediaList.filter { item ->
            val notInTrash = item.id !in trash
            val matchFilter = if (selectedFilter == "BARCHA FAYLLAR") {
                true
            } else {
                item.bucketName.equals(selectedFilter, ignoreCase = true) ||
                        photoAlbumAssignments[item.id].equals(selectedFilter, ignoreCase = true)
            }
            notInTrash && matchFilter
        }
    }

    val trashedItems = remember(mediaList, trash) {
        mediaList.filter { it.id in trash }
    }

    val currentItem = if (activeList.isNotEmpty()) {
        val safeIndex = currentIndex.coerceIn(0, activeList.size - 1)
        activeList[safeIndex]
    } else null

    // Safe index adjustment when list shrinks
    LaunchedEffect(activeList.size) {
        if (activeList.isNotEmpty() && currentIndex >= activeList.size) {
            currentIndex = activeList.size - 1
        }
    }

    if (showParentSettings) {
        ParentSettingsScreen(
            mediaList = mediaList,
            isKidsMode = isKidsMode,
            whitelistedAlbums = whitelistedAlbums,
            timerLimitMinutes = timerLimitMinutes,
            isScreenPinned = isScreenPinned,
            onToggleScreenPinning = onToggleScreenPinning,
            onRequestBiometricAuth = onRequestBiometricAuth,
            onToggleKidsMode = { enabled ->
                isKidsMode = enabled
                kidsPrefs.isKidsMode = enabled
                if (enabled) isClassicModeActive = false
            },
            onToggleAlbum = { albumName ->
                kidsPrefs.toggleAlbum(albumName)
                whitelistedAlbums = kidsPrefs.whitelistedAlbums
            },
            onSelectAllAlbums = {
                val all = mediaList.map { it.bucketName }.filter { it.isNotBlank() }.distinct()
                kidsPrefs.setAllAlbums(all)
                whitelistedAlbums = kidsPrefs.whitelistedAlbums
            },
            onClearAllAlbums = {
                kidsPrefs.clearAllAlbums()
                whitelistedAlbums = emptySet()
            },
            onSetTimerLimit = { minutes ->
                timerLimitMinutes = minutes
                kidsPrefs.timerLimitMinutes = minutes
                remainingSeconds = minutes * 60L
                isTimerExpired = false
            },
            onResetTimer = {
                remainingSeconds = timerLimitMinutes * 60L
                isTimerExpired = false
            },
            onOpenClassicMode = {
                isClassicModeActive = true
                showParentSettings = false
            },
            onClose = {
                showParentSettings = false
            }
        )
    } else if (isKidsMode && !isClassicModeActive) {
        // 👶 KIDS SAFE GALLERY SCREEN
        KidsSafeGalleryScreen(
            mediaList = mediaList,
            whitelistedAlbums = whitelistedAlbums,
            remainingSeconds = remainingSeconds,
            isTimerExpired = isTimerExpired,
            onOpenParentSettings = {
                onRequestBiometricAuth("Ota-ona Sozlamalari") {
                    showParentSettings = true
                }
            },
            onUnlockTimerRequest = {
                onRequestBiometricAuth("Taymerni Ochish") {
                    remainingSeconds = timerLimitMinutes * 60L
                    isTimerExpired = false
                }
            }
        )
    } else {
        // 🚀 1:1 ORIGINAL SLIDEBOX PRO EXPERIENCE
        Scaffold(
            containerColor = Color(0xFF090A0F),
            topBar = {
                Surface(
                    color = Color(0xFF111420),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Real Album Filter Dropdown
                        Box {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF1E2330))
                                    .clickable { showDropdownMenu = true }
                                    .padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (showGridView) "Galereya Ko'rinishi 🔲" else "$selectedFilter ▼",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            DropdownMenu(
                                expanded = showDropdownMenu,
                                onDismissRequest = { showDropdownMenu = false },
                                modifier = Modifier
                                    .background(Color(0xFF1E2330))
                                    .border(1.dp, Color(0xFF374151), RoundedCornerShape(12.dp))
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (showGridView) "Slidebox Sorter Rejimiga O'tish 🎴" else "Barcha Rasmlar Galereyasi 🔲",
                                            color = Color(0xFF38BDF8),
                                            fontWeight = FontWeight.Bold
                                        )
                                    },
                                    onClick = {
                                        showGridView = !showGridView
                                        showDropdownMenu = false
                                    }
                                )
                                HorizontalDivider(color = Color(0xFF374151))
                                allAlbumsList.forEach { albumName ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                albumName,
                                                color = if (selectedFilter == albumName) Color(0xFF38BDF8) else Color.White,
                                                fontWeight = if (selectedFilter == albumName) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        onClick = {
                                            selectedFilter = albumName
                                            showGridView = false
                                            showDropdownMenu = false
                                            currentIndex = 0
                                        }
                                    )
                                }
                            }
                        }

                        // Right: Trash counter, Kids Lock & Settings
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (availableUpdate?.hasUpdate == true) {
                                Button(
                                    onClick = { showUpdateDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("Yangilash 🚀", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // 🗑️ Top Bar Trash Counter (Tap to view and manage trash)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (trashedItems.isNotEmpty()) Color(0xFF7F1D1D) else Color(0xFF1E2330))
                                    .clickable { showTrashSheet = true }
                                    .padding(horizontal = 10.dp, vertical = 7.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Savat",
                                        tint = if (trashedItems.isNotEmpty()) Color(0xFFFCA5A5) else Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${trashedItems.size}",
                                        color = if (trashedItems.isNotEmpty()) Color.White else Color.Gray,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Switch to Kids Mode
                            Button(
                                onClick = {
                                    isKidsMode = true
                                    kidsPrefs.isKidsMode = true
                                    isClassicModeActive = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("👶 Bolalar", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            IconButton(
                                onClick = { showParentSettings = true },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = "Sozlamalar", tint = Color.White)
                            }
                        }
                    }
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Color(0xFF090A0F))
            ) {
                if (showGridView) {
                    // Optional Grid View Mode
                    SlideboxGridView(
                        mediaList = activeList,
                        onItemClick = { item ->
                            currentIndex = activeList.indexOf(item).coerceAtLeast(0)
                            showGridView = false
                        }
                    )
                } else if (currentItem != null) {
                    // 1:1 SLIDEBOX SORTER
                    SlideboxCardSorterScreen(
                        item = currentItem,
                        currentIndex = currentIndex,
                        totalCount = activeList.size,
                        trashedCount = trashedItems.size,
                        isFavorite = currentItem.id in favorites,
                        canUndo = historyStack.isNotEmpty(),
                        assignedAlbum = photoAlbumAssignments[currentItem.id],
                        allAlbums = allAlbumsList.filter { it != "BARCHA FAYLLAR" },
                        onTrash = {
                            trash.add(currentItem.id)
                            historyStack.add(SlideboxAction.Trashed(currentItem, currentIndex))
                        },
                        onToggleFavorite = {
                            val wasFav = currentItem.id in favorites
                            if (wasFav) {
                                favorites.remove(currentItem.id)
                            } else {
                                favorites.add(currentItem.id)
                            }
                            historyStack.add(SlideboxAction.Favorited(currentItem, wasFav))
                        },
                        onUndo = {
                            if (historyStack.isNotEmpty()) {
                                when (val lastAction = historyStack.removeAt(historyStack.size - 1)) {
                                    is SlideboxAction.Trashed -> {
                                        trash.remove(lastAction.item.id)
                                        currentIndex = lastAction.previousIndex.coerceIn(0, activeList.size)
                                        Toast.makeText(context, "Savatdan qaytarildi ↶", Toast.LENGTH_SHORT).show()
                                    }
                                    is SlideboxAction.AlbumSorted -> {
                                        photoAlbumAssignments.remove(lastAction.item.id)
                                        currentIndex = lastAction.previousIndex.coerceIn(0, activeList.size)
                                        Toast.makeText(context, "Albom saralash bekor qilindi ↶", Toast.LENGTH_SHORT).show()
                                    }
                                    is SlideboxAction.Favorited -> {
                                        if (lastAction.previousState) {
                                            favorites.add(lastAction.item.id)
                                        } else {
                                            favorites.remove(lastAction.item.id)
                                        }
                                    }
                                }
                            }
                        },
                        onNext = {
                            if (currentIndex < activeList.size - 1) {
                                currentIndex++
                            }
                        },
                        onPrevious = {
                            if (currentIndex > 0) {
                                currentIndex--
                            }
                        },
                        onSortToAlbum = { albumName ->
                            photoAlbumAssignments[currentItem.id] = albumName
                            historyStack.add(SlideboxAction.AlbumSorted(currentItem, albumName, currentIndex))
                            Toast.makeText(context, "\"$albumName\" albomiga qo'shildi! 📁", Toast.LENGTH_SHORT).show()
                            if (currentIndex < activeList.size - 1) {
                                currentIndex++
                            }
                        },
                        onAddNewAlbum = {
                            showNewAlbumDialog = true
                        },
                        onShare = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = if (currentItem.mediaType == MediaType.VIDEO) "video/*" else "image/*"
                                putExtra(Intent.EXTRA_STREAM, currentItem.uri)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Ulashish"))
                        }
                    )
                } else {
                    // Empty Sorter state (All files organized!)
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Text("🎉", fontSize = 64.sp)
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                "Barcha rasmlar saralandi!",
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Ushbu filtr bo'yicha boshqa yangi media fayllar qolmadi.",
                                color = Color.Gray,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            if (selectedFilter != "BARCHA FAYLLAR") {
                                Button(
                                    onClick = { selectedFilter = "BARCHA FAYLLAR"; currentIndex = 0 },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text("Barcha Fayllarga Qaytish 📂", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // 🗑️ Trash Management Bottom Sheet
                if (showTrashSheet) {
                    TrashManagementSheet(
                        trashedItems = trashedItems,
                        onDismiss = { showTrashSheet = false },
                        onRestoreItem = { item ->
                            trash.remove(item.id)
                        },
                        onRestoreAll = {
                            trash.clear()
                            showTrashSheet = false
                            Toast.makeText(context, "Barcha rasmlar tiklandi! 🔄", Toast.LENGTH_SHORT).show()
                        },
                        onDeletePermanently = {
                            onDeleteMediaItems(trashedItems)
                            trash.clear()
                            showTrashSheet = false
                        }
                    )
                }

                // 📁 New Album Dialog
                if (showNewAlbumDialog) {
                    AlertDialog(
                        onDismissRequest = {
                            showNewAlbumDialog = false
                            newAlbumNameInput = ""
                        },
                        containerColor = Color(0xFF1E2330),
                        title = { Text("Yangi Albom Yaratish 📁", color = Color.White, fontWeight = FontWeight.Bold) },
                        text = {
                            Column {
                                Text("Albom nomini kiriting:", color = Color.LightGray, fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedTextField(
                                    value = newAlbumNameInput,
                                    onValueChange = { newAlbumNameInput = it },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color(0xFF38BDF8),
                                        unfocusedBorderColor = Color.Gray
                                    ),
                                    placeholder = { Text("Masalan: Ta'til 2026, Oila", color = Color.Gray) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    val trimmed = newAlbumNameInput.trim()
                                    if (trimmed.isNotBlank()) {
                                        if (!customAlbums.contains(trimmed)) {
                                            customAlbums.add(trimmed)
                                        }
                                        if (currentItem != null) {
                                            photoAlbumAssignments[currentItem.id] = trimmed
                                            historyStack.add(SlideboxAction.AlbumSorted(currentItem, trimmed, currentIndex))
                                            Toast.makeText(context, "\"$trimmed\" albomi yaratildi va rasm qo'shildi! 📁", Toast.LENGTH_SHORT).show()
                                            if (currentIndex < activeList.size - 1) {
                                                currentIndex++
                                            }
                                        }
                                    }
                                    showNewAlbumDialog = false
                                    newAlbumNameInput = ""
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8))
                            ) {
                                Text("Yaratish", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                showNewAlbumDialog = false
                                newAlbumNameInput = ""
                            }) {
                                Text("Bekor qilish", color = Color.Gray)
                            }
                        }
                    )
                }

                // In-App Update Dialog
                if (showUpdateDialog && availableUpdate != null) {
                    UpdateDialog(
                        updateInfo = availableUpdate!!,
                        onDismiss = { showUpdateDialog = false }
                    )
                }
            }
        }
    }
}

// 🎴 1:1 SLIDEBOX MAIN CARD SORTER SCREEN
@Composable
fun SlideboxCardSorterScreen(
    item: MediaItem,
    currentIndex: Int,
    totalCount: Int,
    trashedCount: Int,
    isFavorite: Boolean,
    canUndo: Boolean,
    assignedAlbum: String?,
    allAlbums: List<String>,
    onTrash: () -> Unit,
    onToggleFavorite: () -> Unit,
    onUndo: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSortToAlbum: (String) -> Unit,
    onAddNewAlbum: () -> Unit,
    onShare: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }
    val scale = remember { Animatable(1f) }

    val isDraggingUp = offsetY.value < -40f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 8.dp)
    ) {
        // Main Interactive Card Stack
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            // Background shadow card
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp)
                    .padding(top = 10.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF141722))
            )

            // Foreground Active Swipable Card
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black)
                    .graphicsLayer {
                        translationX = offsetX.value
                        translationY = offsetY.value
                        scaleX = scale.value
                        scaleY = scale.value
                    }
                    .pointerInput(item.id) {
                        detectDragGestures(
                            onDragEnd = {
                                if (offsetY.value < -50f && kotlin.math.abs(offsetY.value) > kotlin.math.abs(offsetX.value) * 0.6f) {
                                    // 👆 SWIPE UP TO TRASH
                                    scope.launch {
                                        offsetY.animateTo(-900f, spring())
                                        scale.animateTo(0.2f, spring())
                                        onTrash()
                                        offsetX.snapTo(0f)
                                        offsetY.snapTo(0f)
                                        scale.snapTo(1f)
                                    }
                                } else if (offsetX.value > 90f) {
                                    // 👉 SWIPE RIGHT (PREVIOUS)
                                    scope.launch {
                                        offsetX.animateTo(600f, tween(140))
                                        onPrevious()
                                        offsetX.snapTo(0f)
                                        offsetY.snapTo(0f)
                                    }
                                } else if (offsetX.value < -90f) {
                                    // 👈 SWIPE LEFT (NEXT)
                                    scope.launch {
                                        offsetX.animateTo(-600f, tween(140))
                                        onNext()
                                        offsetX.snapTo(0f)
                                        offsetY.snapTo(0f)
                                    }
                                } else {
                                    // Release back to center
                                    scope.launch {
                                        offsetX.animateTo(0f, spring())
                                        offsetY.animateTo(0f, spring())
                                        scale.animateTo(1f, spring())
                                    }
                                }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                scope.launch {
                                    offsetX.snapTo(offsetX.value + dragAmount.x)
                                    offsetY.snapTo(offsetY.value + dragAmount.y)
                                    if (offsetY.value < 0) {
                                        val s = (1f - (kotlin.math.abs(offsetY.value) / 1000f)).coerceIn(0.75f, 1f)
                                        scale.snapTo(s)
                                    }
                                }
                            }
                        )
                    }
            ) {
                // Media preview
                AsyncImage(
                    model = item.uri,
                    contentDescription = item.displayName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )

                // Trash overlay badge on drag up
                if (isDraggingUp) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 24.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFFEF4444).copy(alpha = 0.9f))
                            .padding(horizontal = 18.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Savatga tashlash 🗑️", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }

                // Assigned Album Tag badge (if tagged)
                if (assignedAlbum != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(14.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0284C7).copy(alpha = 0.85f))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text("📁 $assignedAlbum", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Video duration badge
                if (item.mediaType == MediaType.VIDEO) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(14.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.7f))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFFFDE047), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(item.formattedDuration.ifEmpty { "Video" }, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Action Toolbar (Undo, Prev, Index, Next, Favorite, Share)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Undo Button
            IconButton(
                onClick = onUndo,
                enabled = canUndo,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(if (canUndo) Color(0xFF1E2330) else Color(0xFF141722))
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Ortga qaytarish",
                    tint = if (canUndo) Color(0xFF38BDF8) else Color.DarkGray,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Previous
            IconButton(
                onClick = onPrevious,
                enabled = currentIndex > 0,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E2330))
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Oldingi",
                    tint = if (currentIndex > 0) Color.White else Color.DarkGray,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Counter indicator
            Text(
                text = "${currentIndex + 1} / $totalCount",
                color = Color.LightGray,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )

            // Next
            IconButton(
                onClick = onNext,
                enabled = currentIndex < totalCount - 1,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E2330))
            ) {
                Icon(
                    Icons.Default.ArrowForward,
                    contentDescription = "Keyingi",
                    tint = if (currentIndex < totalCount - 1) Color.White else Color.DarkGray,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Trash Button (Quick Trash)
            IconButton(
                onClick = onTrash,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF7F1D1D).copy(alpha = 0.85f))
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Savatga tashlash",
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Favorite (Heart)
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(if (isFavorite) Color(0xFF7F1D1D) else Color(0xFF1E2330))
            ) {
                Icon(
                    if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Sevimli",
                    tint = if (isFavorite) Color(0xFFEF4444) else Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Share
            IconButton(
                onClick = onShare,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E2330))
            ) {
                Icon(
                    Icons.Default.Share,
                    contentDescription = "Ulashish",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 📁 PASTKI ALBOMLAR PANELI (QUICK ALBUM SORTER TRAY)
        Surface(
            color = Color(0xFF10131D),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(
                    text = "ALBOMLARGA SARALASH (BOSING VA KEYINGISIGA O'TADI)",
                    color = Color.Gray,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // + Yangi Albom tugmasi
                    item {
                        Button(
                            onClick = onAddNewAlbum,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8))
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Yangi Albom", color = Color(0xFF38BDF8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Albomlar ro'yxati
                    items(allAlbums) { albumName ->
                        val isAssigned = assignedAlbum == albumName
                        val icon = when {
                            albumName.contains("Camera", true) || albumName.contains("DCIM", true) -> "📷"
                            albumName.contains("Screenshot", true) -> "📱"
                            albumName.contains("Telegram", true) -> "✈️"
                            albumName.contains("Download", true) -> "📥"
                            albumName.contains("WhatsApp", true) -> "💬"
                            albumName.contains("Cartoon", true) || albumName.contains("multfilm", true) -> "🎬"
                            else -> "📁"
                        }

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isAssigned) Color(0xFF0284C7) else Color(0xFF1E2330)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.clickable { onSortToAlbum(albumName) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(icon, fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = albumName,
                                    color = if (isAssigned) Color.White else Color(0xFFE2E8F0),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// 🗑️ TRASH MANAGEMENT BOTTOM SHEET
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashManagementSheet(
    trashedItems: List<MediaItem>,
    onDismiss: () -> Unit,
    onRestoreItem: (MediaItem) -> Unit,
    onRestoreAll: () -> Unit,
    onDeletePermanently: () -> Unit
) {
    val totalSizeMb = trashedItems.sumOf { it.size } / (1024.0 * 1024.0)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF111420),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.Gray) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "🗑️ Savat Boshqaruvi",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${trashedItems.size} ta fayl (${String.format(Locale.US, "%.1f MB", totalSizeMb)})",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                }

                if (trashedItems.isNotEmpty()) {
                    TextButton(onClick = onRestoreAll) {
                        Text("Barchasini Tiklash ↶", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (trashedItems.isNotEmpty()) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp)
                ) {
                    items(trashedItems, key = { it.id }) { item ->
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1E2330))
                                .clickable { onRestoreItem(item) }
                        ) {
                            AsyncImage(
                                model = item.uri,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )

                            // Quick Restore badge
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(4.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.7f))
                                    .padding(4.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Tiklash", tint = Color(0xFF38BDF8), modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDeletePermanently,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Barchasini Butunlay O'chirish", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Savat bo'sh!", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("Savatga tashlangan fayllar shu yerda paydo bo'ladi.", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

// 🔲 OPTIONAL GRID VIEW
@Composable
fun SlideboxGridView(
    mediaList: List<MediaItem>,
    onItemClick: (MediaItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(mediaList, key = { it.id }) { item ->
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E2330))
                        .clickable { onItemClick(item) }
                ) {
                    AsyncImage(
                        model = item.uri,
                        contentDescription = item.displayName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    if (item.mediaType == MediaType.VIDEO) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(4.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.Black.copy(alpha = 0.7f))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(item.formattedDuration.ifEmpty { "▶" }, color = Color.White, fontSize = 9.sp)
                        }
                    }
                }
            }
        }
    }
}
