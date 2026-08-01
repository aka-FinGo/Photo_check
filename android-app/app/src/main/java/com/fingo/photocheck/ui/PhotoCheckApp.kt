package com.fingo.photocheck.ui

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.fingo.photocheck.model.MediaItem
import com.fingo.photocheck.model.MediaType
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoCheckApp(
    mediaList: List<MediaItem>,
    onDeleteMediaItems: (List<MediaItem>) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var activeTab by remember { mutableIntStateOf(0) } // 0: Organize, 1: Albums, 2: Trash, 3: Hisobot
    val favorites = remember { mutableStateListOf<Long>() }
    val trash = remember { mutableStateListOf<Long>() }
    val actionHistory = remember { mutableStateListOf<Pair<Long, String>>() } // (id, "trash" or "fav")

    var currentIndex by remember { mutableIntStateOf(0) }
    var selectedFolder by remember { mutableStateOf("RECENT") }
    var showFolderDropdown by remember { mutableStateOf(false) }

    val folderList = remember(mediaList) {
        listOf("RECENT") + mediaList.map { it.bucketName }.distinct()
    }

    val activeList = remember(mediaList, trash, selectedFolder) {
        mediaList.filter { item ->
            val notInTrash = item.id !in trash
            val matchFolder = selectedFolder == "RECENT" || item.bucketName == selectedFolder
            notInTrash && matchFolder
        }
    }

    val trashedItems = remember(mediaList, trash) {
        mediaList.filter { it.id in trash }
    }

    val favoriteItems = remember(mediaList, favorites) {
        mediaList.filter { it.id in favorites }
    }

    // Ensure index bounds
    val currentItem = if (activeList.isNotEmpty()) {
        val safeIndex = currentIndex.coerceIn(0, activeList.size - 1)
        activeList[safeIndex]
    } else null

    Scaffold(
        containerColor = Color(0xFF07070A),
        topBar = {
            if (activeTab == 0 || activeTab == 1) {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF07070A)),
                    title = {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            // Center: Folder Selector Pill
                            Row(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(0xFF1C1C24))
                                    .clickable { showFolderDropdown = !showFolderDropdown }
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedFolder,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                            }

                            DropdownMenu(
                                expanded = showFolderDropdown,
                                onDismissRequest = { showFolderDropdown = false },
                                modifier = Modifier.background(Color(0xFF1C1C24))
                            ) {
                                folderList.forEach { folder ->
                                    DropdownMenuItem(
                                        text = { Text(folder, color = Color.White) },
                                        onClick = {
                                            selectedFolder = folder
                                            currentIndex = 0
                                            showFolderDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    },
                    actions = {
                        // Top-Right Trash Bin Icon with Red Badge
                        Box(
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .clickable { activeTab = 2 }
                        ) {
                            IconButton(onClick = { activeTab = 2 }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Trash",
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            if (trashedItems.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(18.dp)
                                        .background(Color(0xFFEF4444), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${trashedItems.size}",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                )
            } else if (activeTab == 2) {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF07070A)),
                    title = { Text("Savatcha (${trashedItems.size})", color = Color.White, fontWeight = FontWeight.Bold) },
                    actions = {
                        if (trashedItems.isNotEmpty()) {
                            TextButton(onClick = { onDeleteMediaItems(trashedItems) }) {
                                Text("Tozalash", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                )
            } else {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF07070A)),
                    title = { Text("Hisobot", color = Color.White, fontWeight = FontWeight.Bold) }
                )
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF0F0F16),
                contentColor = Color.White
            ) {
                NavigationBarItem(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Organize") },
                    label = { Text("Saralash") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        indicatorColor = Color(0xFF6366F1)
                    )
                )
                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    icon = { Icon(Icons.Default.Star, contentDescription = "Sevimlilar") },
                    label = { Text("Sevimlilar (${favoriteItems.size})") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        indicatorColor = Color(0xFF6366F1)
                    )
                )
                NavigationBarItem(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    icon = { Icon(Icons.Default.Delete, contentDescription = "Savat") },
                    label = { Text("Savat (${trashedItems.size})") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        indicatorColor = Color(0xFF6366F1)
                    )
                )
                NavigationBarItem(
                    selected = activeTab == 3,
                    onClick = { activeTab = 3 },
                    icon = { Icon(Icons.Default.List, contentDescription = "Hisobot") },
                    label = { Text("Hisobot") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        indicatorColor = Color(0xFF6366F1)
                    )
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFF07070A))
        ) {
            when (activeTab) {
                0 -> {
                    // Slidebox-Style Swipe Organize View
                    if (currentItem != null) {
                        SlideboxOrganizeView(
                            item = currentItem,
                            totalCount = activeList.size,
                            currentIndex = currentIndex,
                            onSwipeTrash = {
                                trash.add(currentItem.id)
                                actionHistory.add(Pair(currentItem.id, "trash"))
                            },
                            onNext = {
                                if (currentIndex < activeList.size - 1) {
                                    currentIndex++
                                }
                            },
                            onUndo = {
                                if (actionHistory.isNotEmpty()) {
                                    val lastAction = actionHistory.removeAt(actionHistory.size - 1)
                                    trash.remove(lastAction.first)
                                    favorites.remove(lastAction.first)
                                }
                            },
                            onShare = {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = if (currentItem.mediaType == MediaType.VIDEO) "video/*" else "image/*"
                                    putExtra(Intent.EXTRA_STREAM, currentItem.uri)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Ulashish"))
                            },
                            folderList = folderList,
                            onMoveToFolder = { newFolder ->
                                // Move action indicator
                            }
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(54.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Barcha media saralandi!", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                1 -> {
                    // Favorites Grid
                    if (favoriteItems.isNotEmpty()) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            contentPadding = PaddingValues(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(favoriteItems, key = { it.id }) { item ->
                                Box(
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF151522))
                                ) {
                                    AsyncImage(
                                        model = item.uri,
                                        contentDescription = item.displayName,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    IconButton(
                                        onClick = { favorites.remove(item.id) },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                            .size(24.dp)
                                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Sevimli fayllar yo'q", color = Color.Gray, fontSize = 16.sp)
                        }
                    }
                }
                2 -> {
                    // System Trash View
                    if (trashedItems.isNotEmpty()) {
                        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "O'chirilishi kutilayotgan fayllar: ${trashedItems.size} ta",
                                    color = Color.Gray,
                                    fontSize = 14.sp
                                )
                                TextButton(onClick = { trash.clear() }) {
                                    Text("Barchasini tiklash", color = Color(0xFF6366F1))
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                items(trashedItems, key = { it.id }) { item ->
                                    Box(
                                        modifier = Modifier
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFF151522))
                                    ) {
                                        AsyncImage(
                                            model = item.uri,
                                            contentDescription = item.displayName,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        IconButton(
                                            onClick = { trash.remove(item.id) },
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(4.dp)
                                                .size(28.dp)
                                                .background(Color(0xFF6366F1), CircleShape)
                                        ) {
                                            Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Savatcha bo'sh", color = Color.Gray, fontSize = 16.sp)
                        }
                    }
                }
                3 -> {
                    // Hisobot / Stats Clean View (NO top counter!)
                    val totalSizeMb = mediaList.sumOf { it.size } / (1024.0 * 1024.0)
                    val trashSizeMb = trashedItems.sumOf { it.size } / (1024.0 * 1024.0)

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            "Xotira va Statistika Hisoboti",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF13131D)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text("Umumiy Egallangan Xotira", color = Color.Gray, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    String.format(Locale.US, "%.1f MB", totalSizeMb),
                                    color = Color(0xFF818CF8),
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Divider(color = Color(0xFF222232), modifier = Modifier.padding(vertical = 12.dp))
                                Text(
                                    String.format(Locale.US, "O'chirish navbatidagi joy: %.1f MB", trashSizeMb),
                                    color = Color(0xFFEF4444),
                                    fontSize = 13.sp
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF13131D)),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Icon(Icons.Default.Home, contentDescription = null, tint = Color(0xFF818CF8))
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text("${mediaList.size}", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                    Text("Jami Fayllar", color = Color.Gray, fontSize = 12.sp)
                                }
                            }

                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF13131D)),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFF43F5E))
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text("${favoriteItems.size}", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                    Text("Sevimlilar", color = Color.Gray, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SlideboxOrganizeView(
    item: MediaItem,
    totalCount: Int,
    currentIndex: Int,
    onSwipeTrash: () -> Unit,
    onNext: () -> Unit,
    onUndo: () -> Unit,
    onShare: () -> Unit,
    folderList: List<String>,
    onMoveToFolder: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }
    val scale = remember { Animatable(1f) }

    val formattedDate = remember(item.dateAdded) {
        val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
        sdf.format(Date(item.dateAdded * 1000))
    }

    val fileSizeMb = remember(item.size) {
        String.format(Locale.US, "%.1f MB", item.size / (1024.0 * 1024.0))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Subtitle Counter & Date
        Text(
            text = "${currentIndex + 1} / $totalCount • $formattedDate",
            color = Color.Gray,
            fontSize = 13.sp,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Main Slidebox Media Card with SWIPE UP / TOP-RIGHT TO TRASH Gesture
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF12121A))
                .pointerInput(item.id) {
                    detectDragGestures(
                        onDragEnd = {
                            // Check if dragged UP or TOP-RIGHT towards Trash icon
                            if (offsetY.value < -180f || (offsetY.value < -80f && offsetX.value > 80f)) {
                                scope.launch {
                                    // Fly up towards top-right trash icon animation
                                    offsetX.animateTo(300f, spring())
                                    offsetY.animateTo(-600f, spring())
                                    scale.animateTo(0.2f, spring())
                                    onSwipeTrash()
                                    // Reset offsets for next card
                                    offsetX.snapTo(0f)
                                    offsetY.snapTo(0f)
                                    scale.snapTo(1f)
                                }
                            } else {
                                // Snap back to center
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
                                // Scale down slightly when dragging up/top-right
                                if (offsetY.value < 0) {
                                    val newScale = (1f - (kotlin.math.abs(offsetY.value) / 1000f)).coerceIn(0.7f, 1f)
                                    scale.snapTo(newScale)
                                }
                            }
                        }
                    )
                }
                .graphicsLayer {
                    translationX = offsetX.value
                    translationY = offsetY.value
                    scaleX = scale.value
                    scaleY = scale.value
                }
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(item.uri)
                    .crossfade(true)
                    .build(),
                contentDescription = item.displayName,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )

            if (item.mediaType == MediaType.VIDEO) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(64.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play Video",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            // Bottom File Info Banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color(0xCC0D0D14))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.displayName,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "📁 ${item.bucketName} • $fileSizeMb",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Middle Slidebox Control Buttons (NEXT, UNDO, SHARE, TRASH)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // NEXT BUTTON
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onNext() }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("KEYINGI", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            // UNDO BUTTON
            IconButton(onClick = { onUndo() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Undo", tint = Color.White, modifier = Modifier.size(22.dp))
            }

            // SHARE BUTTON
            IconButton(onClick = { onShare() }) {
                Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White, modifier = Modifier.size(22.dp))
            }

            // TRASH BUTTON (Manual tap to trash)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onSwipeTrash() }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("SAVATGA", color = Color(0xFFEF4444), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Bottom MOVE TO ALBUM Panel
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF12121A))
                .padding(12.dp)
        ) {
            Text(
                "ALBUMGA KO'CHIRISH...",
                color = Color.Gray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(folderList.filter { it != "RECENT" }) { folder ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1E1E2C))
                            .clickable { onMoveToFolder(folder) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(folder, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}
