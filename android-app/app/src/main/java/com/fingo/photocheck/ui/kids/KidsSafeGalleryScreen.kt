package com.fingo.photocheck.ui.kids

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
            mediaList // if no albums specifically restricted yet, show list
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
            // Fullscreen Child Media Viewer
            val safeIndex = viewingItemIndex!!.coerceIn(0, filteredMedia.size - 1)
            val currentItem = filteredMedia[safeIndex]

            KidsFullscreenViewer(
                item = currentItem,
                currentIndex = safeIndex,
                totalCount = filteredMedia.size,
                onClose = { viewingItemIndex = null },
                onNext = {
                    if (safeIndex < filteredMedia.size - 1) {
                        viewingItemIndex = safeIndex + 1
                    }
                },
                onPrevious = {
                    if (safeIndex > 0) {
                        viewingItemIndex = safeIndex - 1
                    }
                }
            )
        } else {
            // Main Kids Safe Gallery Grid
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp)
            ) {
                // Header: Playful title, timer badge, and discreet parent shield
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "🎈 Bolalar Galereyasi",
                            color = Color(0xFFFDE047),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Timer countdown pill
                        if (remainingSeconds > 0) {
                            val mins = remainingSeconds / 60
                            val secs = remainingSeconds % 60
                            val formattedTime = String.format("%02d:%02d", mins, secs)

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(0xFF312E81))
                                    .border(1.dp, Color(0xFF818CF8), RoundedCornerShape(20.dp))
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("⏳", fontSize = 12.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = formattedTime,
                                        color = Color(0xFFA5B4FC),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Parent Shield Button (requires biometrics)
                        IconButton(
                            onClick = onOpenParentSettings,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E293B))
                        ) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = "Ota-ona sozlamalari",
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Whitelisted Album Filter Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(availableTabs) { tab ->
                        val isSelected = selectedAlbumFilter.equals(tab, ignoreCase = true)
                        val icon = when {
                            tab == "BARCHASI" -> "🌈"
                            tab.contains("multfilm", true) || tab.contains("cartoon", true) -> "🎬"
                            tab.contains("video", true) || tab.contains("mov", true) -> "🎥"
                            tab.contains("oila", true) || tab.contains("family", true) -> "👨‍👩‍👧"
                            tab.contains("hayvon", true) || tab.contains("animal", true) -> "🐱"
                            tab.contains("bolalar", true) || tab.contains("kids", true) -> "🧸"
                            else -> "📁"
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(
                                    if (isSelected) Color(0xFFF59E0B) else Color(0xFF1E293B)
                                )
                                .clickable { selectedAlbumFilter = tab }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(icon, fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = tab,
                                    color = if (isSelected) Color.Black else Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Media Grid
                if (filteredMedia.isNotEmpty()) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(filteredMedia) { item ->
                            val index = filteredMedia.indexOf(item)
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
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
                                                .background(Color.Black.copy(alpha = 0.65f))
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

// Fullscreen Viewer with zero destructive actions
@Composable
fun KidsFullscreenViewer(
    item: MediaItem,
    currentIndex: Int,
    totalCount: Int,
    onClose: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Main Image / Video Preview
        AsyncImage(
            model = item.uri,
            contentDescription = item.displayName,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )

        // Top Navigation Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp, start = 16.dp, end = 16.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Ortga",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "${currentIndex + 1} / $totalCount",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Bottom Joyful Navigation Buttons for Kids
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp, start = 24.dp, end = 24.dp)
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onPrevious,
                enabled = currentIndex > 0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6366F1),
                    disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.height(52.dp)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Oldingi", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Button(
                onClick = onNext,
                enabled = currentIndex < totalCount - 1,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF59E0B),
                    disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.height(52.dp)
            ) {
                Text("Keyingi", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                Spacer(modifier = Modifier.width(6.dp))
                Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color.Black)
            }
        }
    }
}

// Screen Time Sleep & Locked Screen
@Composable
fun KidsSleepLockedScreen(
    onUnlock: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090A15))
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
                "Uxlash va dam olish vaqti bo'ldi! ✨",
                color = Color(0xFFFDE047),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Bugungi tomosha vaqti tugadi. Ko'zlaringizni dam oldiring! 🧸",
                color = Color(0xFF94A3B8),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(36.dp))

            Button(
                onClick = onUnlock,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(54.dp)
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Ota-ona uchun ochish 🔑",
                    color = Color.Black,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
