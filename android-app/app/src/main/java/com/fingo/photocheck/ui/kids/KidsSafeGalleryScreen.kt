package com.fingo.photocheck.ui.kids

import android.net.Uri
import android.widget.VideoView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.fingo.photocheck.model.MediaItem
import com.fingo.photocheck.model.MediaType

@Composable
fun KidsSafeGalleryScreen(
    mediaList: List<MediaItem>,
    whitelistedAlbums: Set<String>,
    remainingSeconds: Long,
    isTimerExpired: Boolean,
    onOpenParentSettings: () -> Unit,
    onUnlockTimerRequest: () -> Unit
) {
    var selectedAlbumFilter by remember { mutableStateOf("BARCHASI") }
    var viewingItemIndex by remember { mutableStateOf<Int?>(null) }

    // Filter media items to only those in whitelisted albums
    val filteredMedia = remember(mediaList, whitelistedAlbums, selectedAlbumFilter) {
        val allowed = if (whitelistedAlbums.isEmpty()) {
            mediaList
        } else {
            mediaList.filter { it.bucketName in whitelistedAlbums }
        }

        if (selectedAlbumFilter == "BARCHASI") {
            allowed
        } else {
            allowed.filter { it.bucketName.equals(selectedAlbumFilter, ignoreCase = true) }
        }
    }

    // Available whitelisted album tabs
    val availableTabs = remember(mediaList, whitelistedAlbums) {
        val albums = if (whitelistedAlbums.isEmpty()) {
            mediaList.map { it.bucketName }.distinct().sorted()
        } else {
            whitelistedAlbums.sorted()
        }
        listOf("BARCHASI") + albums
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF1E1B4B),
                        Color(0xFF0F172A)
                    )
                )
            )
    ) {
        if (isTimerExpired) {
            // Sleep / Timer Locked Screen
            KidsSleepLockedScreen(
                onUnlock = onUnlockTimerRequest
            )
        } else if (viewingItemIndex != null && filteredMedia.isNotEmpty()) {
            // 1:1 System Gallery (Mi Gallery Style) Fullscreen Pager Viewer with Zoom
            val initialIndex = viewingItemIndex!!.coerceIn(0, filteredMedia.size - 1)
            KidsSystemGalleryViewer(
                mediaList = filteredMedia,
                initialIndex = initialIndex,
                onClose = { viewingItemIndex = null }
            )
        } else {
            // Kids Main Screen (Album Grid)
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Safe Header
                KidsSafeHeader(
                    remainingSeconds = remainingSeconds,
                    onOpenSettings = onOpenParentSettings
                )

                // Whitelisted Album Filter Chips
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(availableTabs) { tab ->
                        val isSelected = tab.equals(selectedAlbumFilter, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) Color(0xFFF59E0B) else Color(0xFF1E293B),
                            modifier = Modifier.clickable { selectedAlbumFilter = tab }
                        ) {
                            Text(
                                text = if (tab == "BARCHASI") "🌈 Barchasi" else "📁 $tab",
                                color = if (isSelected) Color.Black else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                // Grid View of Safe Photos & Videos
                if (filteredMedia.isNotEmpty()) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        itemsIndexed(filteredMedia, key = { _, item -> item.id }) { index, item ->
                            Card(
                                shape = RoundedCornerShape(22.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(22.dp))
                                    .clickable { viewingItemIndex = index }
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    AsyncImage(
                                        model = item.uri,
                                        contentDescription = item.displayName,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )

                                    // Video indicator & duration
                                    if (item.mediaType == MediaType.VIDEO) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(8.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Color.Black.copy(alpha = 0.7f))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Default.PlayArrow,
                                                    contentDescription = null,
                                                    tint = Color(0xFFFDE047),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text(
                                                    text = item.formattedDuration.ifEmpty { "Video" },
                                                    color = Color.White,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Empty whitelisted state
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Text("🧸", fontSize = 56.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Hozircha rasm yoki videolar yo'q",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Ota-onangiz ruxsat bergan albomlar shu yerda chiqadi.",
                                color = Color(0xFF94A3B8),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

// 1:1 System Gallery (Mi Gallery Style) Swipeable Fullscreen Viewer with Zoom
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KidsSystemGalleryViewer(
    mediaList: List<MediaItem>,
    initialIndex: Int,
    onClose: () -> Unit
) {
    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, (mediaList.size - 1).coerceAtLeast(0)),
        pageCount = { mediaList.size }
    )
    var showControls by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Fullscreen Horizontal Pager (Mi Gallery Swipe Left / Right)
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            key = { page -> mediaList[page].id }
        ) { page ->
            val item = mediaList[page]
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (item.mediaType == MediaType.VIDEO) {
                    // Video Player
                    KidsVideoPlayer(uri = item.uri)
                } else {
                    // Photo View with Pinch & Double-Tap Zoom
                    KidsZoomableImage(
                        model = item.uri,
                        contentDescription = item.displayName,
                        onSingleTap = { showControls = !showControls }
                    )
                }
            }
        }

        // Clean Top Bar (Back button & Photo Index counter)
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)
                        )
                    )
                    .padding(top = 36.dp, bottom = 20.dp, start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Ortga",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${pagerState.currentPage + 1} / ${mediaList.size}",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// Pinch-to-zoom & Double-tap zoomable image component (allows HorizontalPager swiping when not zoomed)
@Composable
fun KidsZoomableImage(
    model: Any?,
    contentDescription: String?,
    onSingleTap: () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val isZoomed = scale > 1.05f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onSingleTap() },
                    onDoubleTap = {
                        scale = if (scale > 1f) 1f else 2.5f
                        offset = Offset.Zero
                    }
                )
            }
            .then(
                if (isZoomed) {
                    Modifier.pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 4f)
                            if (scale > 1.05f) {
                                offset = Offset(
                                    x = offset.x + pan.x,
                                    y = offset.y + pan.y
                                )
                            } else {
                                scale = 1f
                                offset = Offset.Zero
                            }
                        }
                    }
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = model,
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
        )
    }
}

// Built-in Native Android Video Player for Kids
@Composable
fun KidsVideoPlayer(uri: Uri) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { ctx ->
                VideoView(ctx).apply {
                    setVideoURI(uri)
                    setOnPreparedListener { mp ->
                        mp.isLooping = true
                        start()
                        isPlaying = true
                    }
                    setOnCompletionListener {
                        isPlaying = false
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun KidsSafeHeader(
    remainingSeconds: Long,
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🎈", fontSize = 24.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                "PhotoCheck Kids",
                color = Color(0xFFFDE047),
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            // Live Countdown Timer Badge
            val mins = remainingSeconds / 60
            val secs = remainingSeconds % 60
            val formattedTime = String.format("%02d:%02d", mins, secs)

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF312E81),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF818CF8)),
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        Icons.Default.HourglassBottom,
                        contentDescription = null,
                        tint = Color(0xFFA5B4FC),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formattedTime,
                        color = Color(0xFFA5B4FC),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Parental Lock Shield Button
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E293B))
            ) {
                Icon(
                    Icons.Default.Security,
                    contentDescription = "Ota-ona sozlamalari",
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun KidsSleepLockedScreen(
    onUnlock: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070913))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("🌙", fontSize = 72.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Uxlash va dam olish vaqti! ✨",
                color = Color(0xFFFDE047),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Bugungi tomosha vaqti tugadi.\nKo'zlaringizni dam oldiring! 🧸",
                color = Color(0xFF94A3B8),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onUnlock,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8))
            ) {
                Icon(Icons.Default.Fingerprint, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ota-ona uchun ochish 🔑", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}
