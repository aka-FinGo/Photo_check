package com.fingo.photocheck.ui.kids

import android.net.Uri
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.fingo.photocheck.model.MediaItem
import com.fingo.photocheck.model.MediaType
import kotlinx.coroutines.launch

@Composable
fun KidsSafeGalleryScreen(
    mediaList: List<MediaItem>,
    whitelistedAlbums: Set<String>,
    remainingSeconds: Long,
    isTimerExpired: Boolean,
    isScreenPinned: Boolean = false,
    onToggleScreenPinning: (Boolean) -> Unit = {},
    onRequestBiometricAuth: (title: String, onSuccess: () -> Unit) -> Unit = { _, s -> s() },
    onOpenParentSettings: () -> Unit,
    onUnlockTimerRequest: () -> Unit
) {
    var selectedAlbumFilter by remember { mutableStateOf("BARCHASI") }
    var viewingItemIndex by remember { mutableStateOf<Int?>(null) }
    val gridState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()

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
                onClose = { lastIndex ->
                    viewingItemIndex = null
                    coroutineScope.launch {
                        gridState.scrollToItem(lastIndex.coerceIn(0, (filteredMedia.size - 1).coerceAtLeast(0)))
                    }
                }
            )
        } else {
            // Kids Main Screen (Album Grid)
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Safe Header
                KidsSafeHeader(
                    remainingSeconds = remainingSeconds,
                    isScreenPinned = isScreenPinned,
                    onToggleScreenPinning = onToggleScreenPinning,
                    onRequestBiometricAuth = onRequestBiometricAuth,
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
                        state = gridState,
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
                                    val context = LocalContext.current
                                    val imageRequest = remember(item.uri, item.mediaType) {
                                        ImageRequest.Builder(context)
                                            .data(item.uri)
                                            .apply {
                                                if (item.mediaType == MediaType.VIDEO) {
                                                    videoFrameMillis(1000L)
                                                    decoderFactory(VideoFrameDecoder.Factory())
                                                }
                                            }
                                            .crossfade(true)
                                            .build()
                                    }
                                    AsyncImage(
                                        model = imageRequest,
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
    onClose: (lastViewedIndex: Int) -> Unit
) {
    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, (mediaList.size - 1).coerceAtLeast(0)),
        pageCount = { mediaList.size }
    )
    var showControls by remember { mutableStateOf(true) }

    // Intercept hardware / gesture back button to close fullscreen and preserve gallery scroll position
    BackHandler {
        onClose(pagerState.currentPage)
    }

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
                    // Photo View with Pinch & Double-Tap Smooth Zoom
                    KidsZoomableImage(
                        model = item.uri,
                        contentDescription = item.displayName,
                        onSingleTap = { showControls = !showControls }
                    )
                }
            }
        }

        // Clean Neo-Glass Top Bar (Back button & Photo Index counter)
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
                            colors = listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent)
                        )
                    )
                    .padding(top = 36.dp, bottom = 16.dp, start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onClose(pagerState.currentPage) },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E293B).copy(alpha = 0.85f))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Ortga",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF1E293B).copy(alpha = 0.85f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                    modifier = Modifier.height(34.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${pagerState.currentPage + 1} / ${mediaList.size}",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// Pinch-to-zoom (2-finger) & Smooth Double-tap Zoom In / Zoom Out (allows HorizontalPager swiping when not zoomed)
@Composable
fun KidsZoomableImage(
    model: Any?,
    contentDescription: String?,
    onSingleTap: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
        val newScale = (scale * zoomChange).coerceIn(1f, 4.5f)
        scale = newScale
        if (newScale > 1.05f) {
            val maxOffsetX = 900f * (newScale - 1f)
            val maxOffsetY = 900f * (newScale - 1f)
            offset = Offset(
                x = (offset.x + offsetChange.x * newScale).coerceIn(-maxOffsetX, maxOffsetX),
                y = (offset.y + offsetChange.y * newScale).coerceIn(-maxOffsetY, maxOffsetY)
            )
        } else {
            scale = 1f
            offset = Offset.Zero
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onSingleTap() },
                    onDoubleTap = {
                        coroutineScope.launch {
                            if (scale > 1.15f) {
                                // Smooth Zoom Out to 1f
                                val startScale = scale
                                val startOffsetX = offset.x
                                val startOffsetY = offset.y
                                androidx.compose.animation.core.animate(
                                    initialValue = 0f,
                                    targetValue = 1f,
                                    animationSpec = androidx.compose.animation.core.tween(
                                        durationMillis = 250,
                                        easing = androidx.compose.animation.core.FastOutSlowInEasing
                                    )
                                ) { fraction, _ ->
                                    scale = startScale + (1f - startScale) * fraction
                                    offset = Offset(
                                        x = startOffsetX * (1f - fraction),
                                        y = startOffsetY * (1f - fraction)
                                    )
                                }
                                scale = 1f
                                offset = Offset.Zero
                            } else {
                                // Smooth Zoom In to 2.5f
                                val targetScale = 2.5f
                                val startScale = scale
                                androidx.compose.animation.core.animate(
                                    initialValue = 0f,
                                    targetValue = 1f,
                                    animationSpec = androidx.compose.animation.core.tween(
                                        durationMillis = 250,
                                        easing = androidx.compose.animation.core.FastOutSlowInEasing
                                    )
                                ) { fraction, _ ->
                                    scale = startScale + (targetScale - startScale) * fraction
                                }
                                scale = targetScale
                            }
                        }
                    }
                )
            }
            .transformable(
                state = transformState,
                enabled = true
            ),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = model,
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                }
        )
    }
}

// Built-in Native Android Video Player with Interactive Timeline for Kids
@Composable
fun KidsVideoPlayer(uri: Uri) {
    val context = LocalContext.current
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var totalDurationMs by remember { mutableLongStateOf(0L) }
    var showControls by remember { mutableStateOf(true) }

    val previewRequest = remember(uri) {
        ImageRequest.Builder(context)
            .data(uri)
            .videoFrameMillis(1000L)
            .decoderFactory(VideoFrameDecoder.Factory())
            .crossfade(true)
            .build()
    }

    // Live progress tracker loop
    LaunchedEffect(videoViewRef, isPlaying) {
        while (true) {
            videoViewRef?.let { vv ->
                try {
                    currentPositionMs = vv.currentPosition.toLong().coerceAtLeast(0L)
                    val dur = vv.duration.toLong()
                    if (dur > 0) totalDurationMs = dur
                    isPlaying = vv.isPlaying
                } catch (e: Exception) {
                    // ignore
                }
            }
            kotlinx.coroutines.delay(250L)
        }
    }

    // Auto-hide controls after 3.5s
    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying) {
            kotlinx.coroutines.delay(3500L)
            showControls = false
        }
    }

    fun formatTime(ms: Long): String {
        val totalSec = ms / 1000
        val m = totalSec / 60
        val s = totalSec % 60
        return String.format("%02d:%02d", m, s)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable { showControls = !showControls },
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { ctx ->
                VideoView(ctx).apply {
                    setVideoURI(uri)
                    setOnPreparedListener { mp ->
                        mp.isLooping = true
                        totalDurationMs = duration.toLong()
                        if (isPlaying) {
                            start()
                        }
                    }
                    setOnCompletionListener {
                        isPlaying = false
                    }
                    videoViewRef = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Video Preview Backdrop until video is actively playing
        if (!isPlaying) {
            AsyncImage(
                model = previewRequest,
                contentDescription = "Video Preview",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Center Play/Pause Overlay Button (Neon Style)
        AnimatedVisibility(
            visible = showControls || !isPlaying,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            IconButton(
                onClick = {
                    videoViewRef?.let { vv ->
                        if (vv.isPlaying) {
                            vv.pause()
                            isPlaying = false
                        } else {
                            vv.start()
                            isPlaying = true
                        }
                        showControls = true
                    }
                },
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0F172A).copy(alpha = 0.85f))
                    .border(2.dp, Color(0xFF38BDF8), CircleShape)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "To'xtatish" else "Ijro",
                    tint = Color(0xFFFDE047),
                    modifier = Modifier.size(42.dp)
                )
            }
        }

        // Bottom Interactive Timeline & Controls Bar
        AnimatedVisibility(
            visible = showControls || !isPlaying,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp, start = 16.dp, end = 16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF0F1420).copy(alpha = 0.88f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                    // Timeline Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatTime(currentPositionMs),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Slider(
                            value = if (totalDurationMs > 0) currentPositionMs.toFloat() else 0f,
                            onValueChange = { newPos ->
                                currentPositionMs = newPos.toLong()
                                videoViewRef?.seekTo(newPos.toInt())
                            },
                            valueRange = 0f..(totalDurationMs.toFloat().coerceAtLeast(1f)),
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF38BDF8),
                                activeTrackColor = Color(0xFF38BDF8),
                                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = formatTime(totalDurationMs),
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Quick action buttons (Rewind 10s, Play/Pause, Forward 10s)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                videoViewRef?.let { vv ->
                                    val newPos = (vv.currentPosition - 10000).coerceAtLeast(0)
                                    vv.seekTo(newPos)
                                }
                            }
                        ) {
                            Icon(Icons.Default.Replay10, contentDescription = "10s ortga", tint = Color.White)
                        }

                        IconButton(
                            onClick = {
                                videoViewRef?.let { vv ->
                                    if (vv.isPlaying) {
                                        vv.pause()
                                        isPlaying = false
                                    } else {
                                        vv.start()
                                        isPlaying = true
                                    }
                                }
                            }
                        ) {
                            Icon(
                                if (isPlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                                contentDescription = "Play/Pause",
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                videoViewRef?.let { vv ->
                                    val newPos = (vv.currentPosition + 10000).coerceAtMost(vv.duration)
                                    vv.seekTo(newPos)
                                }
                            }
                        ) {
                            Icon(Icons.Default.Forward10, contentDescription = "10s oldinga", tint = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KidsSafeHeader(
    remainingSeconds: Long,
    isScreenPinned: Boolean = false,
    onToggleScreenPinning: (Boolean) -> Unit = {},
    onRequestBiometricAuth: (title: String, onSuccess: () -> Unit) -> Unit = { _, s -> s() },
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0F172A).copy(alpha = 0.95f),
                        Color(0xFF0A0E17).copy(alpha = 0.85f)
                    )
                )
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Compact Brand Pill
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFFFDE047), Color(0xFFF59E0B))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🎈", fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        "PhotoCheck",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.2).sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isScreenPinned) Color(0xFF34D399) else Color(0xFFFDE047))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            if (isScreenPinned) "XAVFSIZ QADALGAN" else "BOLALAR REJIMI",
                            color = if (isScreenPinned) Color(0xFF34D399) else Color(0xFFFDE047),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.4.sp
                        )
                    }
                }
            }

            // Right Action Controls (Pill Bar)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Screen Pinning (Kiosk) Mode Toggle Chip
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isScreenPinned) Color(0xFF065F46) else Color(0xFF1E293B).copy(alpha = 0.85f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isScreenPinned) Color(0xFF34D399) else Color(0xFF38BDF8).copy(alpha = 0.4f)
                    ),
                    modifier = Modifier
                        .height(32.dp)
                        .clickable {
                            val actionTitle = if (isScreenPinned) "Qadashni Bekor Qilish" else "Ilovani Ekranga Qadash"
                            onRequestBiometricAuth(actionTitle) {
                                onToggleScreenPinning(!isScreenPinned)
                            }
                        }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 9.dp)
                    ) {
                        Icon(
                            imageVector = if (isScreenPinned) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = null,
                            tint = if (isScreenPinned) Color(0xFFE6FFFA) else Color(0xFF38BDF8),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isScreenPinned) "Qadalgan" else "Qadash",
                            color = if (isScreenPinned) Color(0xFFE6FFFA) else Color(0xFF38BDF8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Live Countdown Timer Badge
                val mins = remainingSeconds / 60
                val secs = remainingSeconds % 60
                val formattedTime = String.format("%02d:%02d", mins, secs)

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (remainingSeconds < 180) Color(0xFF7F1D1D).copy(alpha = 0.85f) else Color(0xFF312E81).copy(alpha = 0.7f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (remainingSeconds < 180) Color(0xFFEF4444) else Color(0xFF818CF8).copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.height(32.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.HourglassBottom,
                            contentDescription = null,
                            tint = if (remainingSeconds < 180) Color(0xFFFCA5A5) else Color(0xFFA5B4FC),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = formattedTime,
                            color = if (remainingSeconds < 180) Color(0xFFFCA5A5) else Color(0xFFA5B4FC),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Parental Settings Shield Button
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E293B).copy(alpha = 0.85f))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape)
                ) {
                    Icon(
                        Icons.Default.Security,
                        contentDescription = "Ota-ona sozlamalari",
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Sub-header Kiosk Alert Ribbon when pinned
        AnimatedVisibility(
            visible = isScreenPinned,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Surface(
                color = Color(0xFF065F46).copy(alpha = 0.35f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF34D399).copy(alpha = 0.35f)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF34D399))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Ekran qadalgan: Chiqish barmoq izi bilan himoyalangan 🔒",
                            color = Color(0xFFA7F3D0),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF047857),
                        modifier = Modifier.clickable {
                            onRequestBiometricAuth("Qadashni Bekor Qilish") {
                                onToggleScreenPinning(false)
                            }
                        }
                    ) {
                        Text(
                            "Yechish 🔓",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun KidsSleepLockedScreen(
    onUnlock: () -> Unit
) {
    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "sleepMoonPulse")
    val moonScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(2400, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "moonScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF070A14),
                        Color(0xFF191438),
                        Color(0xFF0A0F1F)
                    )
                )
            )
            .padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Atmospheric Glowing Moon Orb
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .graphicsLayer {
                        scaleX = moonScale
                        scaleY = moonScale
                    }
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFDE047).copy(alpha = 0.28f),
                                Color(0xFF6366F1).copy(alpha = 0.15f),
                                Color.Transparent
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E1B4B).copy(alpha = 0.8f))
                        .border(1.5.dp, Color(0xFFFDE047).copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🌙", fontSize = 42.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Uxlash va dam olish vaqti! ✨",
                color = Color(0xFFFDE047),
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                letterSpacing = (-0.3).sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                "Bugungi tomosha vaqti tugadi.\nKo'zlaringizni dam oldiring! 🧸",
                color = Color(0xFF94A3B8),
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Neo-Glass Biometric Unlock Button
            Surface(
                onClick = onUnlock,
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF1E293B).copy(alpha = 0.9f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f)),
                shadowElevation = 8.dp,
                modifier = Modifier.height(48.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Fingerprint,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "Ota-ona uchun ochish 🔑",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
