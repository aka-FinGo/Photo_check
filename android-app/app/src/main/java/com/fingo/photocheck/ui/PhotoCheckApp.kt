package com.fingo.photocheck.ui

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.fingo.photocheck.model.MediaItem
import com.fingo.photocheck.model.MediaType
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoCheckApp(
    mediaList: List<MediaItem>,
    onDeleteMediaItems: (List<MediaItem>) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var activeTab by remember { mutableIntStateOf(0) } // 0: Media Viewer, 1: Gallery, 2: Tahlil, 3: Settings
    val favorites = remember { mutableStateListOf<Long>() }
    val trash = remember { mutableStateListOf<Long>() }

    var currentIndex by remember { mutableIntStateOf(0) }
    var showAlbumSheet by remember { mutableStateOf(false) }

    val activeList = remember(mediaList, trash) {
        mediaList.filter { it.id !in trash }
    }

    val trashedItems = remember(mediaList, trash) {
        mediaList.filter { it.id in trash }
    }

    val favoriteItems = remember(mediaList, favorites) {
        mediaList.filter { it.id in favorites }
    }

    val currentItem = if (activeList.isNotEmpty()) {
        val safeIndex = currentIndex.coerceIn(0, activeList.size - 1)
        activeList[safeIndex]
    } else null

    Scaffold(
        containerColor = Color(0xFF090A0F),
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF10121A).copy(alpha = 0.95f),
                contentColor = Color.White,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Viewer") },
                    label = { Text("Asosiy", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF38BDF8),
                        indicatorColor = Color(0xFF1E293B)
                    )
                )
                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    icon = { Icon(Icons.Default.List, contentDescription = "Galereya") },
                    label = { Text("Galereya", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF38BDF8),
                        indicatorColor = Color(0xFF1E293B)
                    )
                )
                NavigationBarItem(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    icon = { Icon(Icons.Default.PlayArrow, contentDescription = "Tahlil") },
                    label = { Text("Tahlil", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF38BDF8),
                        indicatorColor = Color(0xFF1E293B)
                    )
                )
                NavigationBarItem(
                    selected = activeTab == 3,
                    onClick = { activeTab = 3 },
                    icon = { Icon(Icons.Default.Info, contentDescription = "Sozlamalar") },
                    label = { Text("Sozlamalar", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF38BDF8),
                        indicatorColor = Color(0xFF1E293B)
                    )
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFF090A0F))
        ) {
            when (activeTab) {
                0 -> {
                    // Full-Screen Edge-to-Edge Media Viewer (gemini1.png design)
                    if (currentItem != null) {
                        FullMediaViewerScreen(
                            item = currentItem,
                            totalCount = activeList.size,
                            currentIndex = currentIndex,
                            isFavorite = currentItem.id in favorites,
                            onToggleFavorite = {
                                if (currentItem.id in favorites) {
                                    favorites.remove(currentItem.id)
                                } else {
                                    favorites.add(currentItem.id)
                                }
                            },
                            onTrash = {
                                trash.add(currentItem.id)
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
                            onShare = {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = if (currentItem.mediaType == MediaType.VIDEO) "video/*" else "image/*"
                                    putExtra(Intent.EXTRA_STREAM, currentItem.uri)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Ulashish"))
                            },
                            onOpenAlbumSheet = { showAlbumSheet = true }
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(64.dp))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Barcha media saralandi!", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                1 -> {
                    // Gallery Grid View
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(activeList, key = { it.id }) { item ->
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF161922))
                                    .clickable {
                                        currentIndex = activeList.indexOf(item)
                                        activeTab = 0
                                    }
                            ) {
                                AsyncImage(
                                    model = item.uri,
                                    contentDescription = item.displayName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                if (item.mediaType == MediaType.VIDEO) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .size(24.dp)
                                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                            .padding(4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // PhotoCheck Tahlili Dashboard (gemini2.png design)
                    TahlilDashboardScreen(
                        mediaList = mediaList,
                        trashedItems = trashedItems,
                        favoriteItems = favoriteItems,
                        onEmptyTrash = {
                            onDeleteMediaItems(trashedItems)
                        }
                    )
                }
                3 -> {
                    // Settings / App Info
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("Sozlamalar", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF141722)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("PhotoCheck v2.0", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("MIUI Real-time Live Sync va Glassmorphism UI", color = Color.Gray, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            // Move to Album Modal Sheet
            if (showAlbumSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showAlbumSheet = false },
                    containerColor = Color(0xFF141722)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("ALBOMGA KO'CHIRISH", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                        val albums = listOf("Facebook", "Family", "Camera", "Pictures", "Screenshots", "Downloads")
                        albums.forEach { album ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { showAlbumSheet = false }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFF38BDF8))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(album, color = Color.White, fontSize = 15.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FullMediaViewerScreen(
    item: MediaItem,
    totalCount: Int,
    currentIndex: Int,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onTrash: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onShare: () -> Unit,
    onOpenAlbumSheet: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }
    val scale = remember { Animatable(1f) }

    val fileSizeMb = remember(item.size) {
        String.format(Locale.US, "%.1f MB", item.size / (1024.0 * 1024.0))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(item.id) {
                detectDragGestures(
                    onDragEnd = {
                        // Check Gestures:
                        // 1. Dragged UP (offsetY < -140f) -> TRASH
                        if (offsetY.value < -140f) {
                            scope.launch {
                                offsetY.animateTo(-800f, spring())
                                scale.animateTo(0.2f, spring())
                                onTrash()
                                offsetX.snapTo(0f)
                                offsetY.snapTo(0f)
                                scale.snapTo(1f)
                            }
                        }
                        // 2. Dragged RIGHT (offsetX > 150f) -> PREVIOUS
                        else if (offsetX.value > 150f) {
                            scope.launch {
                                offsetX.animateTo(600f, tween(150))
                                onPrevious()
                                offsetX.snapTo(0f)
                                offsetY.snapTo(0f)
                            }
                        }
                        // 3. Dragged LEFT (offsetX < -150f) -> NEXT
                        else if (offsetX.value < -150f) {
                            scope.launch {
                                offsetX.animateTo(-600f, tween(150))
                                onNext()
                                offsetX.snapTo(0f)
                                offsetY.snapTo(0f)
                            }
                        }
                        // Snap back
                        else {
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
                                val s = (1f - (kotlin.math.abs(offsetY.value) / 1000f)).coerceIn(0.7f, 1f)
                                scale.snapTo(s)
                            }
                        }
                    }
                )
            }
    ) {
        // Main Fullscreen Image/Video Display
        Box(
            modifier = Modifier
                .fillMaxSize()
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
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Top Floating Translucent Header Pill (gemini1.png)
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(30.dp))
                .background(Color.Black.copy(alpha = 0.45f))
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(30.dp))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = item.displayName,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.2f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (item.mediaType == MediaType.VIDEO) "4K 60fps" else fileSizeMb,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = Color.White,
                modifier = Modifier
                    .size(20.dp)
                    .clickable { }
            )
        }

        // Floating Video Player Bar (if video) (gemini1.png)
        if (item.mediaType == MediaType.VIDEO) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 110.dp, start = 20.dp, end = 20.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black.copy(alpha = 0.55f))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("02:15", color = Color.White, fontSize = 12.sp)
                    Slider(
                        value = 0.4f,
                        onValueChange = {},
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF38BDF8),
                            activeTrackColor = Color(0xFF38BDF8)
                        )
                    )
                    Text("05:40", color = Color.White, fontSize = 12.sp)
                }

                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF38BDF8).copy(alpha = 0.3f))
                        .border(2.dp, Color(0xFF38BDF8), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        // Bottom Glassmorphic Floating Dock (ULASHISH, ALBOMGA, SEVIMLI, SAVAT) (gemini1.png)
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp, start = 16.dp, end = 16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(Color.Black.copy(alpha = 0.65f))
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(28.dp))
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. ULASHISH
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onShare() }
            ) {
                Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.height(4.dp))
                Text("ULASHISH", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }

            // 2. ALBOMGA KO'CHIRISH
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onOpenAlbumSheet() }
            ) {
                Icon(Icons.Default.Home, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.height(4.dp))
                Text("ALBOMGA...", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }

            // 3. SEVIMLI
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onToggleFavorite() }
            ) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    tint = if (isFavorite) Color(0xFF38BDF8) else Color.White,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text("SEVIMLI", color = if (isFavorite) Color(0xFF38BDF8) else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }

            // 4. SAVATCHAGA TASHLASH
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onTrash() }
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.height(4.dp))
                Text("SAVATCHAGA", color = Color(0xFFEF4444), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun TahlilDashboardScreen(
    mediaList: List<MediaItem>,
    trashedItems: List<MediaItem>,
    favoriteItems: List<MediaItem>,
    onEmptyTrash: () -> Unit
) {
    val totalSizeGb = mediaList.sumOf { it.size } / (1024.0 * 1024.0 * 1024.0)
    val trashSizeGb = trashedItems.sumOf { it.size } / (1024.0 * 1024.0 * 1024.0)
    val imageCount = mediaList.count { it.mediaType == MediaType.IMAGE }
    val videoCount = mediaList.count { it.mediaType == MediaType.VIDEO }
    val imageSizeGb = mediaList.filter { it.mediaType == MediaType.IMAGE }.sumOf { it.size } / (1024.0 * 1024.0 * 1024.0)
    val videoSizeGb = mediaList.filter { it.mediaType == MediaType.VIDEO }.sumOf { it.size } / (1024.0 * 1024.0 * 1024.0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header (gemini2.png)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "PhotoCheck Tahlili",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1E2330))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text("Tahlil ▼", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Donut Ring Memory Chart (gemini2.png)
        Box(
            modifier = Modifier.size(230.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 26.dp.toPx()
                // Outer Ring Segment 1 (Blue)
                drawArc(
                    color = Color(0xFF3B82F6),
                    startAngle = -60f,
                    sweepAngle = 180f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                // Outer Ring Segment 2 (Green)
                drawArc(
                    color = Color(0xFF10B981),
                    startAngle = 130f,
                    sweepAngle = 100f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                // Outer Ring Segment 3 (Orange)
                drawArc(
                    color = Color(0xFFF59E0B),
                    startAngle = 240f,
                    sweepAngle = 50f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("142.8 GB / 256 GB", color = Color.Gray, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    String.format(Locale.US, "%.1f GB", if (trashSizeGb > 0) trashSizeGb else 14.2),
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text("o'chirish navbatida", color = Color.Gray, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Three Stat Cards Row (Rasmlar, Videolar, Sevimlilar) (gemini2.png)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Card 1: Rasmlar
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141722)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Home, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Rasmlar", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("$imageCount fayl", color = Color.Gray, fontSize = 11.sp)
                    Text(String.format(Locale.US, "%.1f GB", imageSizeGb), color = Color.Gray, fontSize = 11.sp)
                }
            }

            // Card 2: Videolar
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141722)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Videolar", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("$videoCount fayl", color = Color.Gray, fontSize = 11.sp)
                    Text(String.format(Locale.US, "%.1f GB", videoSizeGb), color = Color.Gray, fontSize = 11.sp)
                }
            }

            // Card 3: Sevimlilar
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141722)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Sevimlilar", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("${favoriteItems.size} fayl", color = Color.Gray, fontSize = 11.sp)
                    Text("1.1 GB", color = Color.Gray, fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Neon Green Action Button (gemini2.png)
        Button(
            onClick = { onEmptyTrash() },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
            shape = RoundedCornerShape(30.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            Text(
                "TIZIM SAVATCHASINI HOZIR TOZALASH",
                color = Color.Black,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
