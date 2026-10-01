package com.fingo.photocheck.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.fingo.photocheck.data.KidsPreferencesManager
import com.fingo.photocheck.model.MediaItem
import com.fingo.photocheck.model.MediaType
import com.fingo.photocheck.repository.MediaRepository
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
    data class BatchTrashed(val items: List<MediaItem>, val previousIndex: Int) : SlideboxAction
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
    val repository = remember { MediaRepository(context) }

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
    LaunchedEffect(isKidsMode, isClassicModeActive, showParentSettings, timerLimitMinutes, isTimerExpired) {
        if (isKidsMode && !isClassicModeActive && !showParentSettings && timerLimitMinutes > 0 && !isTimerExpired) {
            while (remainingSeconds > 0) {
                delay(1000L)
                remainingSeconds--
            }
            isTimerExpired = true
        }
    }

    // --- 1:1 SLIDEBOX STATE WITH LOCAL PERSISTENCE ---
    val favorites = remember {
        mutableStateListOf<Long>().apply {
            addAll(kidsPrefs.savedFavoriteIds)
        }
    }
    val trash = remember {
        mutableStateListOf<Long>().apply {
            addAll(kidsPrefs.savedTrashIds)
        }
    }
    val customAlbums = remember {
        mutableStateListOf<String>().apply {
            addAll(kidsPrefs.customAlbums)
        }
    }
    val photoAlbumAssignments = remember {
        mutableStateMapOf<Long, String>().apply {
            putAll(kidsPrefs.getAlbumAssignments())
        }
    }
    val historyStack = remember { mutableStateListOf<SlideboxAction>() }

    // Sync state changes with persistence
    LaunchedEffect(trash.toList()) {
        kidsPrefs.savedTrashIds = trash.toSet()
    }
    LaunchedEffect(favorites.toList()) {
        kidsPrefs.savedFavoriteIds = favorites.toSet()
    }
    LaunchedEffect(customAlbums.toList()) {
        kidsPrefs.customAlbums = customAlbums.toSet()
    }

    var currentIndex by remember { mutableIntStateOf(0) }
    var selectedFilter by remember { mutableStateOf("BARCHA FAYLLAR") }
    var showDropdownMenu by remember { mutableStateOf(false) }
    var showTrashSheet by remember { mutableStateOf(false) }
    var showGridView by remember { mutableStateOf(false) }
    var showNewAlbumDialog by remember { mutableStateOf(false) }
    var newAlbumNameInput by remember { mutableStateOf("") }
    var isExportingAlbums by remember { mutableStateOf(false) }

    val trashSnapshot = trash.toList()
    val favoritesSnapshot = favorites.toList()
    val assignmentsSnapshot = photoAlbumAssignments.toMap()

    val favoriteItems = remember(mediaList, favoritesSnapshot, trashSnapshot) {
        mediaList.filter { it.id in favorites && it.id !in trash }
    }

    val trashedItems = remember(mediaList, trashSnapshot) {
        mediaList.filter { it.id in trash }
    }

    // Real device albums + custom created albums + favorites
    val allAlbumsList = remember(mediaList, customAlbums.toList(), favoriteItems.size) {
        val deviceAlbums = mediaList.map { it.bucketName }.filter { it.isNotBlank() }.distinct()
        val combined = (deviceAlbums + customAlbums).distinct().sorted()
        listOf("BARCHA FAYLLAR", "❤️ SEVIMLILAR") + combined
    }

    val activeList = remember(mediaList, trashSnapshot, selectedFilter, assignmentsSnapshot, favoritesSnapshot) {
        mediaList.filter { item ->
            val notInTrash = item.id !in trash
            val matchFilter = when (selectedFilter) {
                "BARCHA FAYLLAR" -> true
                "❤️ SEVIMLILAR" -> item.id in favorites
                else -> {
                    item.bucketName.equals(selectedFilter, ignoreCase = true) ||
                            photoAlbumAssignments[item.id].equals(selectedFilter, ignoreCase = true)
                }
            }
            notInTrash && matchFilter
        }
    }

    val currentItem = if (activeList.isNotEmpty()) {
        val safeIndex = currentIndex.coerceIn(0, activeList.size - 1)
        activeList[safeIndex]
    } else null

    val nextItem = if (activeList.isNotEmpty() && currentIndex < activeList.size - 1) {
        activeList[currentIndex + 1]
    } else null

    val trashedTotalBytes = remember(trashedItems) {
        trashedItems.sumOf { it.size }
    }

    fun formatBytes(bytes: Long): String {
        val mb = bytes / (1024.0 * 1024.0)
        return if (mb >= 1000) String.format(Locale.US, "%.2f GB", mb / 1024.0) else String.format(Locale.US, "%.1f MB", mb)
    }

    val similarPhotos = remember(currentItem, activeList) {
        if (currentItem == null) emptyList()
        else {
            activeList.filter { other ->
                other.bucketName.equals(currentItem.bucketName, ignoreCase = true) &&
                        kotlin.math.abs(other.dateAdded - currentItem.dateAdded) <= 45L
            }.sortedBy { it.dateAdded }
        }
    }

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
            timerLimitMinutes = timerLimitMinutes,
            isScreenPinned = isScreenPinned,
            onToggleScreenPinning = onToggleScreenPinning,
            onRequestBiometricAuth = onRequestBiometricAuth,
            onOpenParentSettings = {
                onRequestBiometricAuth("Ota-ona Sozlamalari") {
                    showParentSettings = true
                }
            },
            onUnlockTimerRequest = {
                onRequestBiometricAuth("Taymerni Ochish") {
                    remainingSeconds = (if (timerLimitMinutes > 0) timerLimitMinutes else 30) * 60L
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
                    color = Color(0xFF0B0F19),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Real Album Filter Dropdown (Neo-Glass Pill)
                        Box {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFF1E293B).copy(alpha = 0.85f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.35f)),
                                modifier = Modifier
                                    .height(32.dp)
                                    .clickable { showDropdownMenu = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (showGridView) "Galereya 🔲" else selectedFilter,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showDropdownMenu,
                                onDismissRequest = { showDropdownMenu = false },
                                modifier = Modifier
                                    .background(Color(0xFF161E2E))
                                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
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
                                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                                allAlbumsList.forEach { albumName ->
                                    val isFav = albumName == "❤️ SEVIMLILAR"
                                    val label = if (isFav) "❤️ Sevimlilar (${favoriteItems.size})" else albumName
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                label,
                                                color = if (selectedFilter == albumName) (if (isFav) Color(0xFFFB7185) else Color(0xFF38BDF8)) else Color.White,
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

                        // Right: Unified 32dp Action Pills
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (availableUpdate?.hasUpdate == true) {
                                Surface(
                                    onClick = { showUpdateDialog = true },
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color(0xFF0284C7).copy(alpha = 0.85f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8)),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 9.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Yangilash 🚀", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // ❤️ Top Bar Favorites Pill
                            Surface(
                                onClick = {
                                    selectedFilter = if (selectedFilter == "❤️ SEVIMLILAR") "BARCHA FAYLLAR" else "❤️ SEVIMLILAR"
                                    showGridView = false
                                    currentIndex = 0
                                },
                                shape = RoundedCornerShape(16.dp),
                                color = if (selectedFilter == "❤️ SEVIMLILAR") Color(0xFFBE185D).copy(alpha = 0.9f) else if (favoriteItems.isNotEmpty()) Color(0xFF831843).copy(alpha = 0.65f) else Color(0xFF1E293B).copy(alpha = 0.85f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (selectedFilter == "❤️ SEVIMLILAR") Color(0xFFF472B6) else if (favoriteItems.isNotEmpty()) Color(0xFFFB7185).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.12f)),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Favorite,
                                        contentDescription = "Sevimlilar",
                                        tint = if (selectedFilter == "❤️ SEVIMLILAR" || favoriteItems.isNotEmpty()) Color(0xFFFB7185) else Color(0xFF94A3B8),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${favoriteItems.size}",
                                        color = if (selectedFilter == "❤️ SEVIMLILAR" || favoriteItems.isNotEmpty()) Color.White else Color(0xFF94A3B8),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // 🗑️ Top Bar Trash Pill
                            Surface(
                                onClick = { showTrashSheet = true },
                                shape = RoundedCornerShape(16.dp),
                                color = if (trashedItems.isNotEmpty()) Color(0xFF7F1D1D).copy(alpha = 0.75f) else Color(0xFF1E293B).copy(alpha = 0.85f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (trashedItems.isNotEmpty()) Color(0xFFEF4444).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.12f)),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Savat",
                                        tint = if (trashedItems.isNotEmpty()) Color(0xFFFCA5A5) else Color(0xFF94A3B8),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (trashedItems.isNotEmpty()) "${trashedItems.size} (${formatBytes(trashedTotalBytes)})" else "0",
                                        color = if (trashedItems.isNotEmpty()) Color.White else Color(0xFF94A3B8),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Switch to Kids Mode Pill
                            Surface(
                                onClick = {
                                    isKidsMode = true
                                    kidsPrefs.isKidsMode = true
                                    isClassicModeActive = false
                                },
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFF1E293B).copy(alpha = 0.85f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.6f)),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = Color(0xFFFDE047),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text("Bolalar", color = Color(0xFFFDE047), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Settings Gear Pill
                            IconButton(
                                onClick = { showParentSettings = true },
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1E293B).copy(alpha = 0.85f))
                                    .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape)
                            ) {
                                Icon(
                                    Icons.Default.Settings,
                                    contentDescription = "Sozlamalar",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
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
                        nextItem = nextItem,
                        similarPhotos = similarPhotos,
                        onSelectSimilarPhoto = { photo ->
                            val idx = activeList.indexOf(photo)
                            if (idx >= 0) currentIndex = idx
                        },
                        onKeepOnlyCurrentInSimilar = {
                            val othersToTrash = similarPhotos.filter { it.id != currentItem.id }
                            if (othersToTrash.isNotEmpty()) {
                                othersToTrash.forEach { trash.add(it.id) }
                                historyStack.add(SlideboxAction.BatchTrashed(othersToTrash, currentIndex))
                                Toast.makeText(context, "${othersToTrash.size} ta o'xshash kadr savatga tashlandi ⚡", Toast.LENGTH_SHORT).show()
                            }
                        },
                        currentIndex = currentIndex,
                        totalCount = activeList.size,
                        trashedCount = trashedItems.size,
                        trashedFormattedSize = formatBytes(trashedTotalBytes),
                        isFavorite = currentItem.id in favorites,
                        canUndo = historyStack.isNotEmpty(),
                        assignedAlbum = photoAlbumAssignments[currentItem.id],
                        allAlbums = allAlbumsList.filter { it != "BARCHA FAYLLAR" },
                        onTrash = {
                            trash.add(currentItem.id)
                            kidsPrefs.savedTrashIds = trash.toSet()
                            historyStack.add(SlideboxAction.Trashed(currentItem, currentIndex))
                            Toast.makeText(context, "Savatga tashlandi 🗑️", Toast.LENGTH_SHORT).show()
                        },
                        onToggleFavorite = {
                            val wasFav = currentItem.id in favorites
                            if (wasFav) {
                                favorites.remove(currentItem.id)
                            } else {
                                favorites.add(currentItem.id)
                            }
                            kidsPrefs.savedFavoriteIds = favorites.toSet()
                            historyStack.add(SlideboxAction.Favorited(currentItem, wasFav))
                            Toast.makeText(context, if (wasFav) "Sevimlilardan olib tashlandi 💔" else "Sevimlilarga qo'shildi ❤️", Toast.LENGTH_SHORT).show()
                        },
                        onUndo = {
                            if (historyStack.isNotEmpty()) {
                                when (val lastAction = historyStack.removeAt(historyStack.size - 1)) {
                                    is SlideboxAction.Trashed -> {
                                        trash.remove(lastAction.item.id)
                                        kidsPrefs.savedTrashIds = trash.toSet()
                                        currentIndex = lastAction.previousIndex.coerceIn(0, activeList.size)
                                        Toast.makeText(context, "Savatdan qaytarildi ↶", Toast.LENGTH_SHORT).show()
                                    }
                                    is SlideboxAction.BatchTrashed -> {
                                        lastAction.items.forEach { trash.remove(it.id) }
                                        kidsPrefs.savedTrashIds = trash.toSet()
                                        currentIndex = lastAction.previousIndex.coerceIn(0, activeList.size)
                                        Toast.makeText(context, "${lastAction.items.size} ta kadr savatdan qaytarildi ↶", Toast.LENGTH_SHORT).show()
                                    }
                                    is SlideboxAction.AlbumSorted -> {
                                        photoAlbumAssignments.remove(lastAction.item.id)
                                        kidsPrefs.removeAlbumAssignment(lastAction.item.id)
                                        currentIndex = lastAction.previousIndex.coerceIn(0, activeList.size)
                                        Toast.makeText(context, "Albom saralash bekor qilindi ↶", Toast.LENGTH_SHORT).show()
                                    }
                                    is SlideboxAction.Favorited -> {
                                        if (lastAction.previousState) {
                                            favorites.add(lastAction.item.id)
                                        } else {
                                            favorites.remove(lastAction.item.id)
                                        }
                                        kidsPrefs.savedFavoriteIds = favorites.toSet()
                                        Toast.makeText(context, "Sevimli holati qaytarildi ↶", Toast.LENGTH_SHORT).show()
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
                            kidsPrefs.saveAlbumAssignment(currentItem.id, albumName)
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
                    // 🎉 SLIDEBOX BENTO COMPLETION SCREEN
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val isFavoritesFilter = selectedFilter == "❤️ SEVIMLILAR"
                            Surface(
                                shape = CircleShape,
                                color = if (isFavoritesFilter) Color(0xFFBE185D).copy(alpha = 0.15f) else Color(0xFF0284C7).copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, if (isFavoritesFilter) Color(0xFFFB7185).copy(alpha = 0.5f) else Color(0xFF38BDF8).copy(alpha = 0.4f)),
                                modifier = Modifier.size(80.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(if (isFavoritesFilter) "❤️" else "🎉", fontSize = 42.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = if (isFavoritesFilter) "Sevimli rasmlar hali yo'q" else "Barcha rasmlar saralandi!",
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = if (isFavoritesFilter) "Rasmlarni sevimlilarga qo'shish uchun pastga suring yoki yurakcha ❤️ tugmasini bosing." else "Filtr: \"$selectedFilter\" bo'yicha saralash yakunlandi.",
                                color = Color(0xFF94A3B8),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            // Bento Statistics Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Stat 1: Total Processed
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color(0xFF131826),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("👀 Ko'rildi", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("${mediaList.size - trashedItems.size}", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                // Stat 2: In Trash
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color(0xFF131826),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("🗑️ Savatda", color = Color(0xFFFCA5A5), fontSize = 11.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(formatBytes(trashedTotalBytes), color = Color(0xFFEF4444), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                // Stat 3: Assigned to Albums
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color(0xFF131826),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.3f)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("📁 Albomda", color = Color(0xFF7DD3FC), fontSize = 11.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("${photoAlbumAssignments.size}", color = Color(0xFF38BDF8), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(28.dp))

                            // Action: Empty Trash if items exist
                            if (trashedItems.isNotEmpty()) {
                                Button(
                                    onClick = {
                                        onDeleteMediaItems(trashedItems)
                                        trash.clear()
                                        kidsPrefs.savedTrashIds = emptySet()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                ) {
                                    Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Savatni Tizimdan Tozalash (${formatBytes(trashedTotalBytes)})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                            }

                            // Action: Export Sorted Albums to Real Storage
                            if (photoAlbumAssignments.isNotEmpty()) {
                                OutlinedButton(
                                    onClick = {
                                        isExportingAlbums = true
                                        scope.launch {
                                            var copiedCount = 0
                                            photoAlbumAssignments.forEach { (id, albumName) ->
                                                val media = mediaList.find { it.id == id }
                                                if (media != null) {
                                                    val res = repository.copyMediaToAlbum(media, albumName)
                                                    if (res != null) copiedCount++
                                                }
                                            }
                                            isExportingAlbums = false
                                            Toast.makeText(context, "$copiedCount ta rasm qurilma albomlariga ko'chirildi! 📁", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    enabled = !isExportingAlbums,
                                    shape = RoundedCornerShape(16.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        if (isExportingAlbums) "Nusxalanmoqda..." else "📁 Albomlarni Qurilma Xotirasiga Saqlash",
                                        color = Color(0xFF10B981),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                            }

                            // Action: Reset filter
                            if (selectedFilter != "BARCHA FAYLLAR") {
                                OutlinedButton(
                                    onClick = { selectedFilter = "BARCHA FAYLLAR"; currentIndex = 0 },
                                    shape = RoundedCornerShape(16.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                ) {
                                    Text("Barcha Fayllarga Qaytish 📂", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
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
                            kidsPrefs.savedTrashIds = trash.toSet()
                            Toast.makeText(context, "Rasm savatdan chiqarildi! ↶", Toast.LENGTH_SHORT).show()
                        },
                        onRestoreAll = {
                            trash.clear()
                            kidsPrefs.savedTrashIds = emptySet()
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
    nextItem: MediaItem?,
    similarPhotos: List<MediaItem>,
    onSelectSimilarPhoto: (MediaItem) -> Unit,
    onKeepOnlyCurrentInSimilar: () -> Unit,
    currentIndex: Int,
    totalCount: Int,
    trashedCount: Int,
    trashedFormattedSize: String,
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
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }
    val scale = remember { Animatable(1f) }

    var isZoomed by remember { mutableStateOf(false) }
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var zoomOffset by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(item.id) {
        offsetX.snapTo(0f)
        offsetY.snapTo(0f)
        scale.snapTo(1f)
        isZoomed = false
        zoomScale = 1f
        zoomOffset = Offset.Zero
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 6.dp)
    ) {
        // Main Interactive Card Stack (2-Layer 3D Physical Deck)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            // 1. UNDERLYING NEXT CARD (PEEK DECK)
            if (nextItem != null) {
                val dragDist = kotlin.math.sqrt(offsetX.value * offsetX.value + offsetY.value * offsetY.value)
                val deckProgress = (dragDist / 350f).coerceIn(0f, 1f)
                val peekScale = 0.93f + (0.07f * deckProgress)
                val peekOffsetY = (14.dp * (1f - deckProgress))

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset(y = peekOffsetY)
                        .graphicsLayer {
                            scaleX = peekScale
                            scaleY = peekScale
                        }
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF141724))
                ) {
                    AsyncImage(
                        model = nextItem.uri,
                        contentDescription = "Keyingi rasm",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(0.6f + (0.4f * deckProgress))
                    )
                }
            }

            // 2. FOREGROUND ACTIVE SWIPABLE CARD
            val rotationZ = (offsetX.value / 45f).coerceIn(-16f, 16f)
            val dynamicScale = (scale.value * (1f - (kotlin.math.abs(offsetY.value) / 1600f))).coerceIn(0.2f, 1f)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = offsetX.value
                        translationY = offsetY.value
                        this.rotationZ = rotationZ
                        scaleX = dynamicScale
                        scaleY = dynamicScale
                    }
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black)
                    .border(
                        width = 1.dp,
                        color = when {
                            offsetY.value < -40f -> Color(0xFFEF4444).copy(alpha = (kotlin.math.abs(offsetY.value) / 120f).coerceIn(0.2f, 0.9f))
                            offsetY.value > 50f -> Color(0xFFEC4899).copy(alpha = (offsetY.value / 120f).coerceIn(0.2f, 0.9f))
                            offsetX.value < -40f -> Color(0xFF10B981).copy(alpha = (kotlin.math.abs(offsetX.value) / 120f).coerceIn(0.2f, 0.9f))
                            offsetX.value > 40f -> Color(0xFF38BDF8).copy(alpha = (offsetX.value / 120f).coerceIn(0.2f, 0.9f))
                            else -> Color.White.copy(alpha = 0.1f)
                        },
                        shape = RoundedCornerShape(24.dp)
                    )
                    .pointerInput(item.id, isZoomed) {
                        if (isZoomed) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                zoomOffset = Offset(
                                    x = (zoomOffset.x + dragAmount.x).coerceIn(-600f, 600f),
                                    y = (zoomOffset.y + dragAmount.y).coerceIn(-600f, 600f)
                                )
                            }
                        } else {
                            detectDragGestures(
                                onDragEnd = {
                                    val dragX = offsetX.value
                                    val dragY = offsetY.value
                                    if (dragY < -75f && kotlin.math.abs(dragY) > kotlin.math.abs(dragX) * 0.7f) {
                                        // 👆 SWIPE UP TO TRASH
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        scope.launch {
                                            offsetY.animateTo(-1200f, spring(dampingRatio = 0.8f, stiffness = 500f))
                                            scale.animateTo(0.2f)
                                            onTrash()
                                            offsetX.snapTo(0f)
                                            offsetY.snapTo(0f)
                                            scale.snapTo(1f)
                                        }
                                    } else if (dragX < -85f) {
                                        // 👈 SWIPE LEFT (NEXT)
                                        scope.launch {
                                            offsetX.animateTo(-900f, tween(120))
                                            onNext()
                                            offsetX.snapTo(0f)
                                            offsetY.snapTo(0f)
                                        }
                                    } else if (dragX > 85f) {
                                        // 👉 SWIPE RIGHT (PREVIOUS)
                                        scope.launch {
                                            offsetX.animateTo(900f, tween(120))
                                            onPrevious()
                                            offsetX.snapTo(0f)
                                            offsetY.snapTo(0f)
                                        }
                                    } else if (dragY > 80f && dragY > kotlin.math.abs(dragX) * 0.7f) {
                                        // 👇 SWIPE DOWN (FAVORITE)
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        scope.launch {
                                            onToggleFavorite()
                                            offsetY.animateTo(0f, spring(dampingRatio = 0.7f, stiffness = 400f))
                                        }
                                    } else {
                                        // Snap back to center
                                        scope.launch {
                                            offsetX.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = 400f))
                                            offsetY.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = 400f))
                                            scale.animateTo(1f, spring())
                                        }
                                    }
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    scope.launch {
                                        offsetX.snapTo(offsetX.value + dragAmount.x)
                                        offsetY.snapTo(offsetY.value + dragAmount.y)
                                    }
                                }
                            )
                        }
                    }
                    .pointerInput(item.id) {
                        detectTapGestures(
                            onDoubleTap = {
                                isZoomed = !isZoomed
                                zoomScale = if (isZoomed) 2.2f else 1f
                                if (!isZoomed) zoomOffset = Offset.Zero
                            }
                        )
                    }
            ) {
                // Media preview (Image or Video)
                AsyncImage(
                    model = item.uri,
                    contentDescription = item.displayName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = zoomScale
                            scaleY = zoomScale
                            translationX = zoomOffset.x
                            translationY = zoomOffset.y
                        }
                )

                // DYNAMIC BADGE STAMPS (Mutually exclusive dominant axis)
                val absX = kotlin.math.abs(offsetX.value)
                val absY = kotlin.math.abs(offsetY.value)
                val isVerticalDominant = absY > (absX * 0.75f)

                if (isVerticalDominant) {
                    // 1. Trash Stamp (Swipe UP)
                    if (offsetY.value < -35f) {
                        val alpha = (kotlin.math.abs(offsetY.value) / 130f).coerceIn(0f, 1f)
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 28.dp)
                                .graphicsLayer { this.alpha = alpha }
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFFDC2626).copy(alpha = 0.92f))
                                .border(1.5.dp, Color(0xFFFCA5A5), RoundedCornerShape(20.dp))
                                .padding(horizontal = 20.dp, vertical = 9.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("SAVATGA TASHLASH 🗑️", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
                            }
                        }
                    }
                    // 4. Favorite Stamp (Swipe DOWN)
                    else if (offsetY.value > 40f) {
                        val alpha = (offsetY.value / 130f).coerceIn(0f, 1f)
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 28.dp)
                                .graphicsLayer { this.alpha = alpha }
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFFDB2777).copy(alpha = 0.92f))
                                .border(1.5.dp, Color(0xFFF472B6), RoundedCornerShape(20.dp))
                                .padding(horizontal = 20.dp, vertical = 9.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (isFavorite) Icons.Default.FavoriteBorder else Icons.Default.Favorite,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (isFavorite) "SEVIMLILARDAN CHIQARISH 💔" else "SEVIMLI ❤️", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
                            }
                        }
                    }
                } else {
                    // 2. Next Stamp (Swipe LEFT)
                    if (offsetX.value < -35f) {
                        val alpha = (kotlin.math.abs(offsetX.value) / 130f).coerceIn(0f, 1f)
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 20.dp)
                                .graphicsLayer { this.alpha = alpha }
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color(0xFF059669).copy(alpha = 0.92f))
                                .border(1.5.dp, Color(0xFF6EE7B7), RoundedCornerShape(18.dp))
                                .padding(horizontal = 16.dp, vertical = 9.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("KEYINGISI", color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                    // 3. Previous Stamp (Swipe RIGHT)
                    else if (offsetX.value > 35f) {
                        val alpha = (offsetX.value / 130f).coerceIn(0f, 1f)
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(start = 20.dp)
                                .graphicsLayer { this.alpha = alpha }
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color(0xFF0284C7).copy(alpha = 0.92f))
                                .border(1.5.dp, Color(0xFF7DD3FC), RoundedCornerShape(18.dp))
                                .padding(horizontal = 16.dp, vertical = 9.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("OLDINGI", color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp)
                            }
                        }
                    }
                }

                // Top-left Info Pills: Bucket & Size
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Black.copy(alpha = 0.7f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                    ) {
                        Text(
                            text = item.bucketName,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Black.copy(alpha = 0.7f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                    ) {
                        Text(
                            text = item.formattedSize,
                            color = Color(0xFF38BDF8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Assigned Album Tag badge
                if (assignedAlbum != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(14.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF0284C7).copy(alpha = 0.9f))
                            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("📁", fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(assignedAlbum, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Video duration badge
                if (item.mediaType == MediaType.VIDEO) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(14.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.75f))
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

        // 📸 BURST & SIMILAR PHOTOS COMPARE BAR
        if (similarPhotos.size >= 2) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF131724),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("📸", fontSize = 14.sp)
                        Text(
                            "${similarPhotos.size} ta kadr:",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        similarPhotos.forEach { photo ->
                            val isSelected = photo.id == item.id
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { onSelectSimilarPhoto(photo) }
                            ) {
                                AsyncImage(
                                    model = photo.uri,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }

                    TextButton(
                        onClick = onKeepOnlyCurrentInSimilar,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("⚡ Boshqalarini savatga", color = Color(0xFFF87171), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Action Toolbar (Ergonomic Cluster)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Group: Undo & Navigation
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Undo Button Pill
                Surface(
                    onClick = onUndo,
                    enabled = canUndo,
                    shape = RoundedCornerShape(16.dp),
                    color = if (canUndo) Color(0xFF1E293B) else Color(0xFF111622),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (canUndo) Color(0xFF38BDF8).copy(alpha = 0.4f) else Color.White.copy(alpha = 0.05f)),
                    modifier = Modifier.height(36.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Ortga",
                            tint = if (canUndo) Color(0xFF38BDF8) else Color.DarkGray,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ortga", color = if (canUndo) Color.White else Color.DarkGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Previous
                IconButton(
                    onClick = onPrevious,
                    enabled = currentIndex > 0,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E293B).copy(alpha = 0.8f))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Oldingi",
                        tint = if (currentIndex > 0) Color.White else Color.DarkGray,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Counter Badge
                Text(
                    text = "${currentIndex + 1} / $totalCount",
                    color = Color.LightGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                // Next
                IconButton(
                    onClick = onNext,
                    enabled = currentIndex < totalCount - 1,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E293B).copy(alpha = 0.8f))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(
                        Icons.Default.ArrowForward,
                        contentDescription = "Keyingi",
                        tint = if (currentIndex < totalCount - 1) Color.White else Color.DarkGray,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Right Group: Trash, Favorite, Share
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Quick Trash Button
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        scope.launch {
                            offsetY.animateTo(-1200f, spring(dampingRatio = 0.8f, stiffness = 500f))
                            scale.animateTo(0.2f)
                            onTrash()
                            offsetX.snapTo(0f)
                            offsetY.snapTo(0f)
                            scale.snapTo(1f)
                        }
                    },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF7F1D1D).copy(alpha = 0.75f))
                        .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.6f), CircleShape)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Savatga tashlash",
                        tint = Color(0xFFFCA5A5),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Favorite Heart Button
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (isFavorite) Color(0xFF7F1D1D).copy(alpha = 0.85f) else Color(0xFF1E293B).copy(alpha = 0.8f))
                        .border(1.dp, if (isFavorite) Color(0xFFEF4444) else Color.White.copy(alpha = 0.12f), CircleShape)
                ) {
                    Icon(
                        if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Sevimli",
                        tint = if (isFavorite) Color(0xFFEF4444) else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Share Button
                IconButton(
                    onClick = onShare,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E293B).copy(alpha = 0.8f))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = "Ulashish",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 📁 PASTKI ALBOMLAR PANELI (QUICK ALBUM SORTER TRAY)
        Surface(
            color = Color(0xFF0F1422),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(
                    text = "📁 Albomga saralash (bosing va pastga sirpanib keyingisiga o'tadi):",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
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
                            modifier = Modifier.clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                scope.launch {
                                    offsetY.animateTo(900f, tween(140))
                                    onSortToAlbum(albumName)
                                    offsetY.snapTo(0f)
                                }
                            }
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
    val totalBytes = trashedItems.sumOf { it.size }
    val formattedTotalSize = run {
        val mb = totalBytes / (1024.0 * 1024.0)
        if (mb >= 1000) String.format(Locale.US, "%.2f GB", mb / 1024.0) else String.format(Locale.US, "%.1f MB", mb)
    }

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
                        "${trashedItems.size} ta fayl ($formattedTotalSize bo'shatiladi)",
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

                            // Quick Restore banner
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .background(Color.Black.copy(alpha = 0.78f))
                                    .padding(vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Tiklash", tint = Color(0xFF38BDF8), modifier = Modifier.size(11.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Tiklash ↶", color = Color(0xFF38BDF8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
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
                    Text("Tizim Savatchasiga Ko'chirish ($formattedTotalSize)", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
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
