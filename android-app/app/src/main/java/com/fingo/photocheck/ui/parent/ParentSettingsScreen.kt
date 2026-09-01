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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fingo.photocheck.model.MediaItem
import com.fingo.photocheck.update.UpdateDialog
import com.fingo.photocheck.update.UpdateInfo
import com.fingo.photocheck.update.UpdateManager
import kotlinx.coroutines.launch

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
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var updateInfoState by remember { mutableStateOf<UpdateInfo?>(null) }
    var updateStatusMessage by remember { mutableStateOf<String?>(null) }
    var showUpdateModal by remember { mutableStateOf(false) }

    val currentAppVersion = remember {
        try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.0.01"
        } catch (e: Exception) {
            "1.0.01"
        }
    }
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
                    Column(modifier = Modifier.weight(1f)) {
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

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = onSelectAllAlbums,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("Barchasi", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = onClearAllAlbums,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("Bekor qilish", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // List of device albums with checkboxes
            items(realAlbumsWithCount) { (albumName, count) ->
                val isChecked = whitelistedAlbums.contains(albumName)
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

            // Section 5: App Updates (In-App Updater)
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🔄", fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        "Dastur Yangilanishi",
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "Joriy versiya: v$currentAppVersion",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            if (updateInfoState?.hasUpdate == true) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF0284C7))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        "Yangi v${updateInfoState?.latestVersion}",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        if (updateStatusMessage != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = updateStatusMessage!!,
                                color = if (updateInfoState?.hasUpdate == true) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        if (updateInfoState?.hasUpdate == true) {
                            Button(
                                onClick = { showUpdateModal = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Hoziroq Yangilash (v${updateInfoState?.latestVersion})", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            OutlinedButton(
                                onClick = {
                                    isCheckingUpdate = true
                                    updateStatusMessage = "Serverdan tekshirilmoqda..."
                                    coroutineScope.launch {
                                        val result = UpdateManager.checkForUpdates(context)
                                        isCheckingUpdate = false
                                        result.onSuccess { info ->
                                            updateInfoState = info
                                            if (info.hasUpdate) {
                                                updateStatusMessage = "Yangi v${info.latestVersion} versiyasi mavjud!"
                                                showUpdateModal = true
                                            } else {
                                                updateStatusMessage = "Sizda eng so'nggi versiya (v${info.currentVersion}) o'rnatilgan ✅"
                                            }
                                        }.onFailure { err ->
                                            updateStatusMessage = "Tekshirishda xatolik: ${err.localizedMessage ?: "Internetni tekshiring"}"
                                        }
                                    }
                                },
                                enabled = !isCheckingUpdate,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (isCheckingUpdate) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = Color(0xFF38BDF8),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Tekshirilmoqda...", fontSize = 12.sp)
                                } else {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Yangilanishlarni Tekshirish", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // In-App Update Modal Dialog
        if (showUpdateModal && updateInfoState != null) {
            UpdateDialog(
                updateInfo = updateInfoState!!,
                onDismiss = { showUpdateModal = false }
            )
        }
    }
}
