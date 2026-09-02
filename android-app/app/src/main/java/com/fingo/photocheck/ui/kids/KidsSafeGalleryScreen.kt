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
import androidx.compose.foundation.gestures.*
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
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.fingo.photocheck.model.MediaItem
import com.fingo.photocheck.model.MediaType

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
                                                    videoFrameMillis(1000)
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

// Pinch-to-zoom (2-finger) & Double-tap zoomable image component (allows HorizontalPager swiping when not zoomed)
@Composable
fun KidsZoomableImage(
    model: Any?,
    contentDescription: String?,
    onSingleTap: () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onSingleTap() },
                    onDoubleTap = {
                        if (scale > 1.05f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 2.5f
                            offset = Offset.Zero
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    var zoom = 1f
                    var pan = Offset.Zero
                    var pastTouchSlop = false
                    val touchSlop = viewConfiguration.touchSlop

                    awaitFirstDown(requireUnconsumed = false)

                    do {
                        val event = awaitPointerEvent()
                        val canceled = event.changes.any { it.isConsumed }
                        if (canceled) break

                        val pointerCount = event.changes.size

                        if (pointerCount >= 2) {
                            val zoomChange = event.calculateZoom()
                            val panChange = event.calculatePan()

                            if (!pastTouchSlop) {
                                zoom *= zoomChange
                                pan += panChange

                                val centroidSize = event.calculateCentroidSize(useCurrent = false)
                                val zoomMotion = kotlin.math.abs(1 - zoom) * centroidSize
                                val panMotion = pan.getDistance()

                                if (zoomMotion > touchSlop || panMotion > touchSlop) {
                                    pastTouchSlop = true
                                }
                            }

                            if (pastTouchSlop) {
                                val newScale = (scale * zoomChange).coerceIn(1f, 4.5f)
                                scale = newScale
                                if (scale > 1.05f) {
                                    val maxOffsetX = 800f * (scale - 1f)
                                    val maxOffsetY = 800f * (scale - 1f)
                                    offset = Offset(
                                        x = (offset.x + panChange.x * scale).coerceIn(-maxOffsetX, maxOffsetX),
                                        y = (offset.y + panChange.y * scale).coerceIn(-maxOffsetY, maxOffsetY)
                                    )
                                } else {
                                    scale = 1f
                                    offset = Offset.Zero
                                }
                                event.changes.forEach { it.consume() }
                            }
                        } else if (pointerCount == 1 && scale > 1.05f) {
                            val panChange = event.calculatePan()
                            val maxOffsetX = 800f * (scale - 1f)
                            val maxOffsetY = 800f * (scale - 1f)
                            offset = Offset(
                                x = (offset.x + panChange.x).coerceIn(-maxOffsetX, maxOffsetX),
                                y = (offset.y + panChange.y).coerceIn(-maxOffsetY, maxOffsetY)
                            )
                            event.changes.forEach { it.consume() }
                        } else {
                            // 1 finger and NOT zoomed: do NOT consume events so HorizontalPager swipes freely!
                        }
                    } while (event.changes.any { it.pressed })

                    if (scale <= 1.05f) {
                        scale = 1f
                        offset = Offset.Zero
                    }
                }
            },
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
            .videoFrameMillis(1000)
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
            // Screen Pinning (Kiosk) Alternating Toggle Button with Biometrics
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isScreenPinned) Color(0xFF065F46) else Color(0xFF1E293B),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isScreenPinned) Color(0xFF34D399) else Color(0xFF475569)
                ),
                modifier = Modifier
                    .padding(end = 8.dp)
                    .clickable {
                        val actionTitle = if (isScreenPinned) "Qadashni Bekor Qilish" else "Ilovani Ekranga Qadash"
                        onRequestBiometricAuth(actionTitle) {
                            onToggleScreenPinning(!isScreenPinned)
                        }
                    }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = if (isScreenPinned) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = null,
                        tint = if (isScreenPinned) Color(0xFF34D399) else Color(0xFF94A3B8),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isScreenPinned) "Qadalgan 📌" else "Qadash 🔓",
                        color = if (isScreenPinned) Color(0xFF34D399) else Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
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
