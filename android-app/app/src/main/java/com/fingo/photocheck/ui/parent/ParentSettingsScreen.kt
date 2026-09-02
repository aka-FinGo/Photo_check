package com.fingo.photocheck.ui.parent

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    isScreenPinned: Boolean = false,
    onToggleScreenPinning: (Boolean) -> Unit = {},
    onRequestBiometricAuth: (title: String, onSuccess: () -> Unit) -> Unit = { _, success -> success() },
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
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    var isCheckingUpdate by remember { mutableStateOf(false) }
    var updateInfoState by remember { mutableStateOf<UpdateInfo?>(null) }
    var updateStatusMessage by remember { mutableStateOf<String?>(null) }
    var showUpdateModal by remember { mutableStateOf(false) }

    var showGuideDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    val currentAppVersion = remember {
        try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.0.03"
        } catch (e: Exception) {
            "1.0.03"
        }
    }

    // Group real device albums and compute counts
    val realAlbumsWithCount = remember(mediaList) {
        mediaList.groupBy { it.bucketName }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }
    }

    val whitelistedCount = remember(mediaList, whitelistedAlbums) {
        if (whitelistedAlbums.isEmpty()) mediaList.size
        else mediaList.count { it.bucketName in whitelistedAlbums }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFF0F1420),
                drawerContentColor = Color.White,
                modifier = Modifier.width(310.dp)
            ) {
                // Drawer Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFF1E1B4B), Color(0xFF0F1420))
                            )
                        )
                        .padding(24.dp)
                ) {
                    Column {
                        Text("🎈 PhotoCheck", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFFDE047))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Ota-ona Boshqaruv Markazi", fontSize = 13.sp, color = Color(0xFF94A3B8))
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF38BDF8).copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "v$currentAppVersion • Rasmiy Reliz",
                                color = Color(0xFF38BDF8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                // Drawer Menu Items
                LazyColumn(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item {
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFF38BDF8)) },
                            label = { Text("Asosiy Xavfsizlik Sozlamalari", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                            selected = true,
                            onClick = { coroutineScope.launch { drawerState.close() } },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = Color(0xFF1E293B),
                                selectedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp)
                        )
                    }
                    item {
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.MenuBook, contentDescription = null, tint = Color(0xFFF59E0B)) },
                            label = { Text("Qo'llanma & Yo'riqnoma", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                            selected = false,
                            onClick = {
                                coroutineScope.launch { drawerState.close() }
                                showGuideDialog = true
                            },
                            shape = RoundedCornerShape(14.dp)
                        )
                    }
                    item {
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = Color(0xFF10B981)) },
                            label = { Text("Dastur Yangilanishi", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                            selected = false,
                            onClick = {
                                coroutineScope.launch { drawerState.close() }
                                isCheckingUpdate = true
                                Toast.makeText(context, "Yangilanishlar tekshirilmoqda... ⏳", Toast.LENGTH_SHORT).show()
                                coroutineScope.launch {
                                    val result = UpdateManager.checkForUpdates(context)
                                    isCheckingUpdate = false
                                    result.onSuccess { info ->
                                        updateInfoState = info
                                        showUpdateModal = true
                                    }.onFailure { err ->
                                        Toast.makeText(context, "Xatolik: ${err.localizedMessage ?: "GitHub-ga ulanib bo'lmadi"}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            shape = RoundedCornerShape(14.dp)
                        )
                    }
                    item {
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFA5B4FC)) },
                            label = { Text("Dastur Haqida", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                            selected = false,
                            onClick = {
                                coroutineScope.launch { drawerState.close() }
                                showAboutDialog = true
                            },
                            shape = RoundedCornerShape(14.dp)
                        )
                    }
                    item {
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFF43F5E)) },
                            label = { Text("Donat & Qo'llab-quvvatlash", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                            selected = false,
                            onClick = {
                                coroutineScope.launch { drawerState.close() }
                                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aka-fingo.github.io/Photo_check/#donate"))
                                context.startActivity(browserIntent)
                            },
                            shape = RoundedCornerShape(14.dp)
                        )
                    }
                }
            }
        }
    ) {
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
                        IconButton(onClick = {
                            coroutineScope.launch { drawerState.open() }
                        }) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = "Gamburger Menyusi",
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = onClose) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Yopish",
                                tint = Color(0xFF94A3B8)
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
                // Section 1: Bento Analytics / Stats Card
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Card 1: Whitelisted Photos (Bento Neo-Glass)
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF131928),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.FolderSpecial, contentDescription = null, tint = Color(0xFFFDE047), modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Ruxsat berilgan", fontSize = 11.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("$whitelistedCount ta", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFFDE047))
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Jami: ${mediaList.size} ta fayl", fontSize = 11.sp, color = Color(0xFF64748B))
                            }
                        }

                        // Card 2: Timer Status (Bento Neo-Glass)
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF131928),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.HourglassTop, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Ekran Vaqti", fontSize = 11.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(if (timerLimitMinutes > 0) "$timerLimitMinutes daqiqa" else "Cheksiz", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF38BDF8))
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Taymer chegarasi", fontSize = 11.sp, color = Color(0xFF64748B))
                            }
                        }
                    }
                }

                // Section 2: Kids Safe Mode Switch
                item {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = Color(0xFF141A28),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("👶 Bolalar Rejimi", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    if (isKidsMode) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFF10B981).copy(alpha = 0.2f)
                                        ) {
                                            Text("Faol", color = Color(0xFF10B981), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "O'chirish, tahrirlash va ulashish taqiqlanadi. Faqat ruxsat etilgan albomlar ko'rinadi.",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Switch(
                                checked = isKidsMode,
                                onCheckedChange = onToggleKidsMode,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFFF59E0B),
                                    uncheckedThumbColor = Color(0xFF64748B),
                                    uncheckedTrackColor = Color(0xFF1E293B)
                                )
                            )
                        }
                    }
                }

                // Section 2.5: Screen Pinning (Kiosk Mode)
                item {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = Color(0xFF0F172A),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isScreenPinned) Color(0xFF10B981).copy(alpha = 0.6f) else Color(0xFF334155)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = if (isScreenPinned) Color(0xFF10B981) else Color(0xFF38BDF8))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("📌 Ekran Qadash (Kiosk Rejimi)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                                if (isScreenPinned) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF10B981).copy(alpha = 0.2f)
                                    ) {
                                        Text("Qadalgan", color = Color(0xFF10B981), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Home va Ilovalar ro'yxati (Recents) tugmalarini qulflaydi. Bola ilovadan chiqib ketolmaydi.",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = {
                                    if (isScreenPinned) {
                                        onRequestBiometricAuth("Qadashni Bekor Qilish") {
                                            onToggleScreenPinning(false)
                                        }
                                    } else {
                                        onToggleScreenPinning(true)
                                    }
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isScreenPinned) Color(0xFF7F1D1D) else Color(0xFF0284C7)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    if (isScreenPinned) Icons.Default.LockOpen else Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    if (isScreenPinned) "Qadashni Bekor Qilish 🔓" else "Ilovani Ekranga Qadash 📌",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                // Section 3: Switch to Slidebox Pro Mode
                item {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = Color(0xFF1E1B4B).copy(alpha = 0.7f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Layers, contentDescription = null, tint = Color(0xFF818CF8))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("🎴 1:1 Original Slidebox Sorter", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Rasmlarni tepaga surib savatga tashlash, pastki albomlar paneli orqali saralash va tozalash.",
                                color = Color(0xFFC7D2FE),
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = onOpenClassicMode,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Slidebox Pro Rejimiga O'tish", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Section 4: Screen Time Timer Configuration
                item {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = Color(0xFF141A28),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.HourglassTop, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("⏳ Ekran Vaqti Chegarasi", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                                TextButton(onClick = onResetTimer) {
                                    Text("Qayta Boshlash", color = Color(0xFF38BDF8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))

                            val timerOptions = listOf(15, 30, 45, 60, 0)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                timerOptions.forEach { minutes ->
                                    val isSelected = timerLimitMinutes == minutes
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (isSelected) Color(0xFF38BDF8) else Color(0xFF1E293B),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { onSetTimerLimit(minutes) }
                                    ) {
                                        Text(
                                            text = if (minutes > 0) "$minutes m" else "∞",
                                            color = if (isSelected) Color.Black else Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 10.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Section 5: Whitelisted Albums with Bulk Selection
                item {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = Color(0xFF141A28),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text("📁 Ruxsat Etilgan Albomlar", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("Bolaga faqat tanlangan albomlar ko'rinadi", color = Color(0xFF94A3B8), fontSize = 12.sp)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        // Barchasini tanlash (Select All)
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = Color(0xFF0284C7),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { onSelectAllAlbums() }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                Icon(Icons.Default.DoneAll, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Barchasini Tanlash", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        // Barchasini bekor qilish (Deselect All)
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = Color(0xFF7F1D1D),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { onClearAllAlbums() }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                Icon(Icons.Default.RemoveDone, contentDescription = null, tint = Color(0xFFFCA5A5), modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Bekor Qilish", color = Color(0xFFFCA5A5), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }

                            Spacer(modifier = Modifier.height(10.dp))

                            if (realAlbumsWithCount.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    realAlbumsWithCount.forEach { (albumName, count) ->
                                        val isChecked = whitelistedAlbums.contains(albumName)
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = if (isChecked) Color(0xFF1E293B) else Color(0xFF0F131E),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { onToggleAlbum(albumName) }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Checkbox(
                                                        checked = isChecked,
                                                        onCheckedChange = { onToggleAlbum(albumName) },
                                                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFFF59E0B))
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(albumName, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                                }
                                                Text("$count ta fayl", color = Color(0xFF94A3B8), fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            } else {
                                Text("Qurilmadan hech qanday albom topilmadi.", color = Color(0xFF64748B), fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // In-App Update Modal
    if (showUpdateModal && updateInfoState != null) {
        UpdateDialog(
            updateInfo = updateInfoState!!,
            onDismiss = { showUpdateModal = false }
        )
    }

    // Interactive Guide Dialog
    if (showGuideDialog) {
        AlertDialog(
            onDismissRequest = { showGuideDialog = false },
            containerColor = Color(0xFF141A28),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📖", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("PhotoCheck Qo'llanmasi", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        Text("1. 👶 Bolalar Rejimi:", fontWeight = FontWeight.Bold, color = Color(0xFFFDE047), fontSize = 14.sp)
                        Text("Bola faqat siz ruxsat bergan albomlarni ko'radi. O'chirish va ulashish butunlay yashirilgan. Chiqishda barmoq izi so'raladi.", color = Color(0xFFCBD5E1), fontSize = 12.sp)
                    }
                    item {
                        Text("2. 🔍 Kattalashtirish (Zoom):", fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8), fontSize = 14.sp)
                        Text("Rasmlarni ikki barmoq bilan erkin chimdib kattalashtirish (pinch-zoom) yoki 2 marta tez bosish orqali yaqinlashtirish mumkin.", color = Color(0xFFCBD5E1), fontSize = 12.sp)
                    }
                    item {
                        Text("3. 🎴 1:1 Slidebox Sorter:", fontWeight = FontWeight.Bold, color = Color(0xFF818CF8), fontSize = 14.sp)
                        Text("Rasmni tepaga suring — savatga tashlanadi. Pastdagi albom tugmasini bosing — rasm darhol o'sha albomga saralanadi!", color = Color(0xFFCBD5E1), fontSize = 12.sp)
                    }
                    item {
                        Text("4. ⏳ Ekran Taymeri:", fontWeight = FontWeight.Bold, color = Color(0xFFF43F5E), fontSize = 14.sp)
                        Text("Vaqt tugaganda 'Uxlash vaqti 🌙' qulf ekrani chiqadi va ota-ona barmoq izisiz ochilmaydi.", color = Color(0xFFCBD5E1), fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showGuideDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8))
                ) {
                    Text("Tushunarli", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // About App Dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            containerColor = Color(0xFF141A28),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🎈", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("PhotoCheck Haqida", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Versiya: v$currentAppVersion (Rasmiy Nashr)", fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8), fontSize = 13.sp)
                    Text("Dasturchi: aka-FinGo & Google Antigravity", color = Color.White, fontSize = 13.sp)
                    Text("Texnologiyalar: Jetpack Compose, Kotlin, Material 3, BiometricX, MediaStore", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    Text("Litsenziya: MIT License (Ochiq kodli)", color = Color(0xFF94A3B8), fontSize = 12.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = { showAboutDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8))
                ) {
                    Text("Yopish", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
