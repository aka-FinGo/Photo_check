package com.fingo.photocheck.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.fingo.photocheck.model.MediaItem
import com.fingo.photocheck.model.MediaType
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoCheckApp(
    mediaList: List<MediaItem>,
    onDeleteMediaItems: (List<MediaItem>) -> Unit
) {
    var activeTab by remember { mutableIntStateOf(0) }
    val favorites = remember { mutableStateListOf<Long>() }
    val trash = remember { mutableStateListOf<Long>() }
    var currentIndex by remember { mutableIntStateOf(0) }
    var selectedFolder by remember { mutableStateOf("Barchasi") }
    var selectedTypeFilter by remember { mutableStateOf("Barchasi") }
    var fullScreenItem by remember { mutableStateOf<MediaItem?>(null) }

    val folderList = remember(mediaList) {
        listOf("Barchasi") + mediaList.map { it.bucketName }.distinct()
    }

    val filteredList = remember(mediaList, trash, selectedFolder, selectedTypeFilter) {
        mediaList.filter { item ->
            val notInTrash = item.id !in trash
            val matchesFolder = selectedFolder == "Barchasi" || item.bucketName == selectedFolder
            val matchesType = when (selectedTypeFilter) {
                "Rasm" -> item.mediaType == MediaType.IMAGE
                "Video" -> item.mediaType == MediaType.VIDEO
                else -> true
            }
            notInTrash && matchesFolder && matchesType
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Photo", fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Check", fontWeight = FontWeight.Bold, color = Color(0xFF6366F1))
                    }
                },
                actions = {
                    Badge(containerColor = Color(0xFF1E1E2E), contentColor = Color.LightGray) {
                        Text(
                            text = if (filteredList.isNotEmpty()) "${currentIndex + 1} / ${filteredList.size}" else "0 / 0",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0A0A0F))
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF0A0A0F)) {
                NavigationBarItem(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Saralash") },
                    label = { Text("Saralash") }
                )
                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    icon = {
                        BadgedBox(badge = {
                            if (favorites.isNotEmpty()) Badge { Text(favorites.size.toString()) }
                        }) {
                            Icon(Icons.Default.Favorite, contentDescription = "Sevimlilar")
                        }
                    },
                    label = { Text("Sevimlilar") }
                )
                NavigationBarItem(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    icon = {
                        BadgedBox(badge = {
                            if (trash.isNotEmpty()) Badge(containerColor = Color(0xFFEF4444)) { Text(trash.size.toString()) }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Savat")
                        }
                    },
                    label = { Text("Savat") }
                )
                NavigationBarItem(
                    selected = activeTab == 3,
                    onClick = { activeTab = 3 },
                    icon = { Icon(Icons.Default.List, contentDescription = "Statistika") },
                    label = { Text("Hisobot") }
                )
            }
        },
        containerColor = Color(0xFF0A0A0F)
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when (activeTab) {
                0 -> SorterScreen(
                    availableMedia = filteredList,
                    folderList = folderList,
                    selectedFolder = selectedFolder,
                    onSelectFolder = { selectedFolder = it; currentIndex = 0 },
                    selectedTypeFilter = selectedTypeFilter,
                    onSelectTypeFilter = { selectedTypeFilter = it; currentIndex = 0 },
                    currentIndex = currentIndex,
                    favorites = favorites,
                    onItemClick = { fullScreenItem = it },
                    onNext = {
                        if (currentIndex < filteredList.size - 1) currentIndex++
                        else if (filteredList.isNotEmpty()) currentIndex = 0
                    },
                    onPrevious = {
                        if (currentIndex > 0) currentIndex--
                        else if (filteredList.isNotEmpty()) currentIndex = filteredList.size - 1
                    },
                    onFavorite = { item ->
                        if (item.id !in favorites) favorites.add(item.id)
                        if (currentIndex < filteredList.size - 1) currentIndex++
                    },
                    onQueueDelete = { item ->
                        if (item.id !in trash) trash.add(item.id)
                        if (currentIndex >= filteredList.size - 1 && currentIndex > 0) currentIndex--
                        scope.launch {
                            val result = snackbarHostState.showSnackbar(
                                message = "${item.displayName} o'chirish navbatiga qo'shildi",
                                actionLabel = "Ortga Qaytarish"
                            )
                            if (result == SnackbarResult.ActionPerformed) trash.remove(item.id)
                        }
                    }
                )
                1 -> FavoritesScreen(
                    mediaList = mediaList.filter { it.id in favorites },
                    onItemClick = { fullScreenItem = it },
                    onRemoveFavorite = { id -> favorites.remove(id) }
                )
                2 -> TrashScreen(
                    mediaList = mediaList.filter { it.id in trash },
                    onItemClick = { fullScreenItem = it },
                    onRestore = { id ->
                        trash.remove(id)
                        scope.launch { snackbarHostState.showSnackbar("Rasm savatdan qaytarildi") }
                    },
                    onDeleteAll = {
                        val itemsToDelete = mediaList.filter { it.id in trash }
                        onDeleteMediaItems(itemsToDelete)
                        trash.clear()
                        scope.launch { snackbarHostState.showSnackbar("Barcha tanlangan fayllar o'chirildi") }
                    }
                )
                3 -> AnalyticsScreen(
                    allMedia = mediaList,
                    favoritesCount = favorites.size,
                    trashItems = mediaList.filter { it.id in trash }
                )
            }

            // Fullscreen viewer dialog
            fullScreenItem?.let { item ->
                FullScreenMediaViewer(
                    item = item,
                    onDismiss = { fullScreenItem = null }
                )
            }
        }
    }
}

@Composable
fun SorterScreen(
    availableMedia: List<MediaItem>,
    folderList: List<String>,
    selectedFolder: String,
    onSelectFolder: (String) -> Unit,
    selectedTypeFilter: String,
    onSelectTypeFilter: (String) -> Unit,
    currentIndex: Int,
    favorites: List<Long>,
    onItemClick: (MediaItem) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onFavorite: (MediaItem) -> Unit,
    onQueueDelete: (MediaItem) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                folderList.take(3).forEach { folder ->
                    FilterChip(
                        selected = selectedFolder == folder,
                        onClick = { onSelectFolder(folder) },
                        label = { Text(folder, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF6366F1),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = { onSelectTypeFilter("Barchasi") }) {
                    Icon(Icons.Default.Home, contentDescription = null, tint = if (selectedTypeFilter == "Barchasi") Color(0xFF6366F1) else Color.Gray)
                }
                IconButton(onClick = { onSelectTypeFilter("Rasm") }) {
                    Icon(Icons.Default.Image, contentDescription = null, tint = if (selectedTypeFilter == "Rasm") Color(0xFF6366F1) else Color.Gray)
                }
                IconButton(onClick = { onSelectTypeFilter("Video") }) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = if (selectedTypeFilter == "Video") Color(0xFF6366F1) else Color.Gray)
                }
            }
        }

        if (availableMedia.isEmpty() || currentIndex >= availableMedia.size) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF6366F1), modifier = Modifier.size(72.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Tanlangan albomda saralanmagan fayllar qolmadi!", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            return
        }

        val currentItem = availableMedia[currentIndex]
        val offsetX = remember { Animatable(0f) }
        val offsetY = remember { Animatable(0f) }
        val scope = rememberCoroutineScope()

        Box(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .graphicsLayer(
                        translationX = offsetX.value,
                        translationY = offsetY.value,
                        rotationZ = offsetX.value * 0.05f
                    )
                    .clickable { onItemClick(currentItem) }
                    .pointerInput(currentItem.id) {
                        detectDragGestures(
                            onDragEnd = {
                                val threshold = 250f
                                val x = offsetX.value
                                val y = offsetY.value
                                scope.launch {
                                    if (abs(y) > abs(x) && abs(y) > threshold) {
                                        if (y < 0) { offsetY.animateTo(-1500f, spring()); onFavorite(currentItem) }
                                        else { offsetY.animateTo(1500f, spring()); onQueueDelete(currentItem) }
                                    } else if (abs(x) > threshold) {
                                        if (x > 0) { offsetX.animateTo(1500f, spring()); onPrevious() }
                                        else { offsetX.animateTo(-1500f, spring()); onNext() }
                                    }
                                    offsetX.snapTo(0f); offsetY.snapTo(0f)
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
                    },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF151522))
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current).data(currentItem.uri).crossfade(true).build(),
                        contentDescription = currentItem.displayName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    if (currentItem.mediaType == MediaType.VIDEO) {
                        Surface(
                            modifier = Modifier.align(Alignment.Center).clip(CircleShape),
                            color = Color.Black.copy(alpha = 0.6f)
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
                                if (currentItem.formattedDuration.isNotEmpty()) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(currentItem.formattedDuration, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).background(Color(0xCC0A0A0F)).padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(currentItem.displayName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("📁 ${currentItem.bucketName}", color = Color.LightGray, fontSize = 12.sp)
                                Text("• ${currentItem.formattedSize}", color = Color.Gray, fontSize = 12.sp)
                            }
                        }

                        if (currentItem.id in favorites) {
                            Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFF43F5E), modifier = Modifier.size(28.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FavoritesScreen(
    mediaList: List<MediaItem>,
    onItemClick: (MediaItem) -> Unit,
    onRemoveFavorite: (Long) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Sevimlilar Ro'yxati (${mediaList.size})", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White)
        Spacer(modifier = Modifier.height(16.dp))

        if (mediaList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Hozircha hech qanday fayl sevimlilarga qo'shilmadi.", color = Color.Gray)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(mediaList, key = { it.id }) { item ->
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF151522))
                            .clickable { onItemClick(item) }
                    ) {
                        AsyncImage(model = item.uri, contentDescription = item.displayName, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        IconButton(
                            onClick = { onRemoveFavorite(item.id) },
                            modifier = Modifier.align(Alignment.TopRight).padding(4.dp).size(28.dp).background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        ) {
                            Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFF43F5E), modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TrashScreen(
    mediaList: List<MediaItem>,
    onItemClick: (MediaItem) -> Unit,
    onRestore: (Long) -> Unit,
    onDeleteAll: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("O'chirish Navbati", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White)
                Text("${mediaList.size} ta fayl", color = Color.Gray, fontSize = 13.sp)
            }
            Button(
                onClick = onDeleteAll,
                enabled = mediaList.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
            ) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Butunlay O'chirish")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (mediaList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("O'chirish navbati bo'sh.", color = Color.Gray)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(mediaList, key = { it.id }) { item ->
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF151522))
                            .clickable { onItemClick(item) }
                    ) {
                        AsyncImage(model = item.uri, contentDescription = item.displayName, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        IconButton(
                            onClick = { onRestore(item.id) },
                            modifier = Modifier.align(Alignment.BottomRight).padding(4.dp).size(32.dp).background(Color(0xFF6366F1), CircleShape)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FullScreenMediaViewer(
    item: MediaItem,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = item.uri,
                contentDescription = item.displayName,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(24.dp)
                    .size(44.dp)
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Yopish", tint = Color.White)
            }

            // Bottom Info Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color(0xCC000000))
                    .padding(20.dp)
            ) {
                Text(item.displayName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Papka: ${item.bucketName} • Haçmi: ${item.formattedSize}", color = Color.LightGray, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun AnalyticsScreen(
    allMedia: List<MediaItem>,
    favoritesCount: Int,
    trashItems: List<MediaItem>
) {
    val totalSizeMb = remember(allMedia) { allMedia.sumOf { it.size } / (1024.0 * 1024.0) }
    val trashSizeMb = remember(trashItems) { trashItems.sumOf { it.size } / (1024.0 * 1024.0) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Xotira va Statistika Hisoboti", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White)
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF151522)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Umumiy Egallangan Xotira", color = Color.Gray, fontSize = 13.sp)
                Text(
                    text = String.format("%.1f MB", totalSizeMb),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6366F1)
                )
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = if (totalSizeMb > 0) (trashSizeMb / totalSizeMb).toFloat() else 0f,
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = Color(0xFFEF4444),
                    trackColor = Color(0xFF262636)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "O'chirish navbatidagi joy: " + String.format("%.1f MB", trashSizeMb),
                    color = Color(0xFFEF4444),
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF151522)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFF6366F1))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("${allMedia.size}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Jami Fayllar", fontSize = 12.sp, color = Color.Gray)
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF151522)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFF43F5E))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("$favoritesCount", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Sevimlilar", fontSize = 12.sp, color = Color.Gray)
                }
            }
        }
    }
}
