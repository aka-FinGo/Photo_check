package com.fingo.photocheck.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    var activeTab by remember { mutableIntStateOf(0) } // 0: Sorter, 1: Favorites, 2: Trash
    val favorites = remember { mutableStateListOf<Long>() }
    val trash = remember { mutableStateListOf<Long>() }
    var currentIndex by remember { mutableIntStateOf(0) }

    val availableMedia = mediaList.filter { it.id !in trash }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Photo",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Check",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6366F1)
                        )
                    }
                },
                actions = {
                    Badge(
                        containerColor = Color(0xFF1E1E2E),
                        contentColor = Color.LightGray
                    ) {
                        Text(
                            text = if (availableMedia.isNotEmpty()) "${currentIndex + 1} / ${availableMedia.size}" else "0 / 0",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0A0A0F)
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF0A0A0F)
            ) {
                NavigationBarItem(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    icon = { Icon(Icons.Default.Collections, contentDescription = "Saralash") },
                    label = { Text("Saralash") }
                )
                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    icon = {
                        BadgedBox(badge = {
                            if (favorites.isNotEmpty()) {
                                Badge { Text(favorites.size.toString()) }
                            }
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
                            if (trash.isNotEmpty()) {
                                Badge(containerColor = Color.Red) { Text(trash.size.toString()) }
                            }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Savat")
                        }
                    },
                    label = { Text("Savat") }
                )
            }
        },
        containerColor = Color(0xFF0A0A0F)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (activeTab) {
                0 -> SorterScreen(
                    availableMedia = availableMedia,
                    currentIndex = currentIndex,
                    favorites = favorites,
                    onNext = {
                        if (currentIndex < availableMedia.size - 1) currentIndex++
                        else if (availableMedia.isNotEmpty()) currentIndex = 0
                    },
                    onPrevious = {
                        if (currentIndex > 0) currentIndex--
                        else if (availableMedia.isNotEmpty()) currentIndex = availableMedia.size - 1
                    },
                    onFavorite = { item ->
                        if (item.id !in favorites) favorites.add(item.id)
                        if (currentIndex < availableMedia.size - 1) currentIndex++
                    },
                    onQueueDelete = { item ->
                        if (item.id !in trash) trash.add(item.id)
                        if (currentIndex >= availableMedia.size - 1 && currentIndex > 0) {
                            currentIndex--
                        }
                    }
                )
                1 -> FavoritesScreen(
                    mediaList = mediaList.filter { it.id in favorites },
                    onRemoveFavorite = { id -> favorites.remove(id) }
                )
                2 -> TrashScreen(
                    mediaList = mediaList.filter { it.id in trash },
                    onRestore = { id -> trash.remove(id) },
                    onDeleteAll = {
                        val itemsToDelete = mediaList.filter { it.id in trash }
                        onDeleteMediaItems(itemsToDelete)
                        trash.clear()
                    }
                )
            }
        }
    }
}

@Composable
fun SorterScreen(
    availableMedia: List<MediaItem>,
    currentIndex: Int,
    favorites: List<Long>,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onFavorite: (MediaItem) -> Unit,
    onQueueDelete: (MediaItem) -> Unit
) {
    if (availableMedia.isEmpty() || currentIndex >= availableMedia.size) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF6366F1),
                modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Hamma rasmlar saralandi!",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        return
    }

    val currentItem = availableMedia[currentIndex]
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        // Media Card with Gesture
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .graphicsLayer(
                    translationX = offsetX.value,
                    translationY = offsetY.value,
                    rotationZ = offsetX.value * 0.05f
                )
                .pointerInput(currentItem.id) {
                    detectDragGestures(
                        onDragEnd = {
                            val threshold = 250f
                            val x = offsetX.value
                            val y = offsetY.value

                            scope.launch {
                                if (abs(y) > abs(x) && abs(y) > threshold) {
                                    if (y < 0) {
                                        // Swipe UP -> Favorite
                                        offsetY.animateTo(-1500f, spring())
                                        onFavorite(currentItem)
                                    } else {
                                        // Swipe DOWN -> Trash
                                        offsetY.animateTo(1500f, spring())
                                        onQueueDelete(currentItem)
                                    }
                                } else if (abs(x) > threshold) {
                                    if (x > 0) {
                                        // Swipe RIGHT -> Previous
                                        offsetX.animateTo(1500f, spring())
                                        onPrevious()
                                    } else {
                                        // Swipe LEFT -> Next
                                        offsetX.animateTo(-1500f, spring())
                                        onNext()
                                    }
                                }

                                // Reset position for next card
                                offsetX.snapTo(0f)
                                offsetY.snapTo(0f)
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
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(currentItem.uri)
                        .crossfade(true)
                        .build(),
                    contentDescription = currentItem.displayName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                if (currentItem.mediaType == MediaType.VIDEO) {
                    Icon(
                        imageVector = Icons.Default.PlayCircle,
                        contentDescription = "Video",
                        tint = Color.White,
                        modifier = Modifier
                            .size(64.dp)
                            .align(Alignment.Center)
                    )
                }

                // Bottom Metadata Info
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(Color(0xCC0A0A0F))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = currentItem.displayName,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "${currentItem.size / (1024 * 1024)} MB",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }

                    if (currentItem.id in favorites) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Favorite",
                            tint = Color(0xFFF43F5E)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FavoritesScreen(
    mediaList: List<MediaItem>,
    onRemoveFavorite: (Long) -> Unit
) {
    if (mediaList.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Sevimlilar ro'yxati bo'sh", color = Color.Gray)
        }
    } else {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Sevimlilar", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White)
            Spacer(modifier = Modifier.height(16.dp))
            // Render list
        }
    }
}

@Composable
fun TrashScreen(
    mediaList: List<MediaItem>,
    onRestore: (Long) -> Unit,
    onDeleteAll: () -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("O'chirish navbati (${mediaList.size})", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White)
            Button(
                onClick = onDeleteAll,
                enabled = mediaList.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Text("Barchasini o'chirish")
            }
        }
    }
}
