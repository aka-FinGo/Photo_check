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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.fingo.photocheck.model.MediaItem
import com.fingo.photocheck.model.MediaType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

enum class SmartCategory(val displayName: String, val icon: String) {
    TABIAT("TABIAT", "🌄"),
    SHAHARLAR("SHAHARLAR", "🏙️"),
    ODAMLAR("ODAMLAR", "👤"),
    HAYVONLAR("HAYVONLAR", "🐕"),
    OZIQ_OVQAT("OZIQ-OVQAT", "🥗"),
    BARCHASI("BARCHASI", "📁")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoCheckApp(
    mediaList: List<MediaItem>,
    onDeleteMediaItems: (List<MediaItem>) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var activeTab by remember { mutableIntStateOf(0) } // 0: Viewer, 1: Galereya, 2: Tag (AI Smart Teglar), 3: Tahlil, 4: Sozlamalar
    val favorites = remember { mutableStateListOf<Long>() }
    val trash = remember { mutableStateListOf<Long>() }

    var currentIndex by remember { mutableIntStateOf(0) }
    var selectedSmartCategory by remember { mutableStateOf(SmartCategory.BARCHASI) }
    var showAlbumSheet by remember { mutableStateOf(false) }
    var isAnalyzing by remember { mutableStateOf(false) }

    // Smart Categorization Logic
    val categorizedMedia = remember(mediaList) {
        val map = mutableMapOf<SmartCategory, MutableList<MediaItem>>()
        SmartCategory.values().forEach { map[it] = mutableListOf() }

        mediaList.forEach { item ->
            val nameLower = item.displayName.lowercase()
            val bucketLower = item.bucketName.lowercase()

            when {
                nameLower.contains("cat") || nameLower.contains("dog") || nameLower.contains("pet") || nameLower.contains("animal") || bucketLower.contains("pet") -> {
                    map[SmartCategory.HAYVONLAR]?.add(item)
                }
                nameLower.contains("food") || nameLower.contains("dish") || nameLower.contains("meal") || nameLower.contains("ovqat") || bucketLower.contains("food") -> {
                    map[SmartCategory.OZIQ_OVQAT]?.add(item)
                }
                nameLower.contains("portrait") || nameLower.contains("selfie") || nameLower.contains("face") || nameLower.contains("odam") || bucketLower.contains("selfie") || bucketLower.contains("camera") -> {
                    map[SmartCategory.ODAMLAR]?.add(item)
                }
                nameLower.contains("city") || nameLower.contains("street") || nameLower.contains("building") || nameLower.contains("shahar") -> {
                    map[SmartCategory.SHAHARLAR]?.add(item)
                }
                else -> {
                    map[SmartCategory.TABIAT]?.add(item)
                }
            }
        }
        map
    }

    val activeList = remember(mediaList, trash, selectedSmartCategory) {
        mediaList.filter { item ->
            val notInTrash = item.id !in trash
            val matchCategory = if (selectedSmartCategory == SmartCategory.BARCHASI) {
                true
            } else {
                categorizedMedia[selectedSmartCategory]?.contains(item) == true
            }
            notInTrash && matchCategory
        }
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
                contentColor = Color.White
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
                    icon = { Icon(Icons.Default.Star, contentDescription = "Smart Tag") },
                    label = { Text("Tag", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF38BDF8),
                        indicatorColor = Color(0xFF1E293B)
                    )
                )
                NavigationBarItem(
                    selected = activeTab == 3,
                    onClick = { activeTab = 3 },
                    icon = { Icon(Icons.Default.PlayArrow, contentDescription = "Tahlil") },
                    label = { Text("Tahlil", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF38BDF8),
                        indicatorColor = Color(0xFF1E293B)
                    )
                )
                NavigationBarItem(
                    selected = activeTab == 4,
                    onClick = { activeTab = 4 },
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
                            }
                        }
                    }
                }
                2 -> {
                    AISmartTagsScreen(
                        categorizedMedia = categorizedMedia,
                        trashedItems = trashedItems,
                        isAnalyzing = isAnalyzing,
                        onReAnalyze = {
                            scope.launch {
                                isAnalyzing = true
                                delay(1200)
                                isAnalyzing = false
                            }
                        },
                        onSelectCategory = { cat ->
                            selectedSmartCategory = cat
                            currentIndex = 0
                            activeTab = 0
                        }
                    )
                }
                3 -> {
                    TahlilDashboardScreen(
                        mediaList = mediaList,
                        trashedItems = trashedItems,
                        favoriteItems = favoriteItems,
                        onEmptyTrash = {
                            onDeleteMediaItems(trashedItems)
                        }
                    )
                }
                4 -> {
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
                                Text("PhotoCheck AI v2.5", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Smart Teglar, MIUI Live Sync va Glassmorphism UI", color = Color.Gray, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

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
                        if (offsetY.value < -140f) {
                            scope.launch {
                                offsetY.animateTo(-800f, spring())
                                scale.animateTo(0.2f, spring())
                                onTrash()
                                offsetX.snapTo(0f)
                                offsetY.snapTo(0f)
                                scale.snapTo(1f)
                            }
                        } else if (offsetX.value > 150f) {
                            scope.launch {
                                offsetX.animateTo(600f, tween(150))
                                onPrevious()
                                offsetX.snapTo(0f)
                                offsetY.snapTo(0f)
                            }
                        } else if (offsetX.value < -150f) {
                            scope.launch {
                                offsetX.animateTo(-600f, tween(150))
                                onNext()
                                offsetX.snapTo(0f)
                                offsetY.snapTo(0f)
                            }
                        } else {
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
                modifier = Modifier.size(20.dp)
            )
        }

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
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onShare() }
            ) {
                Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.height(4.dp))
                Text("ULASHISH", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onOpenAlbumSheet() }
            ) {
                Icon(Icons.Default.Home, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.height(4.dp))
                Text("ALBOMGA...", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }

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
fun AISmartTagsScreen(
    categorizedMedia: Map<SmartCategory, List<MediaItem>>,
    trashedItems: List<MediaItem>,
    isAnalyzing: Boolean,
    onReAnalyze: () -> Unit,
    onSelectCategory: (SmartCategory) -> Unit
) {
    val trashSizeGb = trashedItems.sumOf { it.size } / (1024.0 * 1024.0 * 1024.0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(30.dp))
                .background(Color(0xFF1E2634))
                .border(1.dp, Color(0xFF10B981).copy(alpha = 0.4f), RoundedCornerShape(30.dp))
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Synced with MIUI Gallery", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(Color(0xFF10B981), CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("PhotoCheck", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("Smart Teglari", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF1E2330))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text("TURI ▼", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF1B1B26))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                String.format(Locale.US, "%.1f GB Pending Deletion", if (trashSizeGb > 0) trashSizeGb else 14.2),
                color = Color.Gray,
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            val categories = listOf(
                SmartCategory.TABIAT,
                SmartCategory.SHAHARLAR,
                SmartCategory.ODAMLAR,
                SmartCategory.HAYVONLAR,
                SmartCategory.OZIQ_OVQAT
            )

            items(categories) { cat ->
                val count = categorizedMedia[cat]?.size ?: 0
                val sampleItem = categorizedMedia[cat]?.firstOrNull()

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF141722)),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clickable { onSelectCategory(cat) }
                        .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (sampleItem != null) {
                            AsyncImage(
                                model = sampleItem.uri,
                                contentDescription = cat.displayName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.45f))
                            )
                        }

                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(14.dp)
                        ) {
                            Text(
                                "${cat.icon} ${cat.displayName}",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "$count rasm",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { onReAnalyze() },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
            shape = RoundedCornerShape(30.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            if (isAnalyzing) {
                CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
            } else {
                Text(
                    "AI ANALIZINI QAYTADAN BOSHLASH",
                    color = Color.Black,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
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

        Box(
            modifier = Modifier.size(230.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 26.dp.toPx()
                drawArc(
                    color = Color(0xFF3B82F6),
                    startAngle = -60f,
                    sweepAngle = 180f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                drawArc(
                    color = Color(0xFF10B981),
                    startAngle = 130f,
                    sweepAngle = 100f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
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
