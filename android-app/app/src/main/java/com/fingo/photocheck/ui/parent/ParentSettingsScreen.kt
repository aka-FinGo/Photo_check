package com.fingo.photocheck.ui.parent

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fingo.photocheck.model.MediaItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentSettingsScreen(
    mediaList: List<MediaItem>,
    isKidsMode: Boolean,
    whitelistedAlbums: Set<String>,
    timerLimitMinutes: Int,
    onToggleKidsMode: (Boolean) -> Unit,
    onToggleAlbum: (String) -> Unit,
    onSelectAllAlbums: () -> Unit,
    onClearAllAlbums: () -> Unit,
    onSetTimerLimit: (Int) -> Unit,
    onResetTimer: () -> Unit,
    onOpenClassicMode: () -> Unit,
    onClose: () -> Unit
) {
    // Group real device albums and compute counts
    val realAlbumsWithCount = remember(mediaList) {
        mediaList.groupBy { it.bucketName }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }
    }

    Scaffold(
        containerColor = Color(0xFF0A0D14),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "🛡️ Ota-ona Boshqaruvi",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Yopish",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF111622)
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Kids Mode Toggle
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161C2C)),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "👶 Bolalar Xavfsiz Rejimi",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "O'chirish, tahrirlash va begonalarga yuborish to'liq bloklanadi.",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp
                            )
                        }
                        Switch(
                            checked = isKidsMode,
                            onCheckedChange = { onToggleKidsMode(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF38BDF8)
                            )
                        )
                    }
                }
            }

            // Section 2: Switch to Classic PhotoCheck Pro
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🚀", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "PhotoCheck Pro (Klassik Rejim)",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Slidebox saralash, Dublikatlarni tozalash, Media siqish va Tahlil dashboardiga o'tish.",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = onOpenClassicMode,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Klassik Rejimga O'tish", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Section 3: Screen Time Limit Settings
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161C2C)),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "⏳ Ekran Vaqti Taymeri",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(onClick = onResetTimer) {
                                Text("Qayta Boshlash", color = Color(0xFF38BDF8), fontSize = 12.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        val timerOptions = listOf(
                            15 to "15 daq",
                            30 to "30 daq",
                            45 to "45 daq",
                            60 to "1 soat",
                            0 to "Cheksiz"
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            timerOptions.forEach { (minutes, label) ->
                                val isSelected = timerLimitMinutes == minutes
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isSelected) Color(0xFF38BDF8) else Color(0xFF1E293B)
                                        )
                                        .clickable { onSetTimerLimit(minutes) }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) Color.Black else Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Section 4: Whitelisted Albums Management
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "📁 Ruxsat Berilgan Albomlar",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Bolaga faqat tanlangan papkalar ko'rsatiladi.",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                    }

                    Row {
                        TextButton(onClick = onSelectAllAlbums) {
                            Text("Barchasi", color = Color(0xFF38BDF8), fontSize = 11.sp)
                        }
                        TextButton(onClick = onClearAllAlbums) {
                            Text("Tozalash", color = Color(0xFFEF4444), fontSize = 11.sp)
                        }
                    }
                }
            }

            // List of device albums with checkboxes
            items(realAlbumsWithCount) { (albumName, count) ->
                val isChecked = whitelistedAlbums.contains(albumName) || whitelistedAlbums.isEmpty()
                val icon = when {
                    albumName.contains("Camera", true) || albumName.contains("DCIM", true) -> "📷"
                    albumName.contains("Screenshot", true) -> "📱"
                    albumName.contains("Telegram", true) -> "✈️"
                    albumName.contains("Download", true) -> "📥"
                    albumName.contains("WhatsApp", true) -> "💬"
                    albumName.contains("Cartoon", true) || albumName.contains("multfilm", true) -> "🎬"
                    else -> "🖼️"
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161C2C)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleAlbum(albumName) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(icon, fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = albumName,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "$count ta media fayl",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { onToggleAlbum(albumName) },
                            colors = CheckboxDefaults.colors(
                                checkedColor = Color(0xFF38BDF8),
                                checkmarkColor = Color.Black
                            )
                        )
                    }
                }
            }
        }
    }
}
