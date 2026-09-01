package com.fingo.photocheck.update

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

sealed interface UpdateDialogState {
    data class Available(val info: UpdateInfo) : UpdateDialogState
    data class Downloading(
        val info: UpdateInfo,
        val progress: Float,
        val downloadedBytes: Long,
        val totalBytes: Long
    ) : UpdateDialogState
    data class ReadyToInstall(val info: UpdateInfo, val apkFile: File) : UpdateDialogState
    data class Error(val info: UpdateInfo, val errorMessage: String) : UpdateDialogState
}

@Composable
fun UpdateDialog(
    updateInfo: UpdateInfo,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var state by remember {
        mutableStateOf<UpdateDialogState>(UpdateDialogState.Available(updateInfo))
    }

    Dialog(
        onDismissRequest = {
            if (state !is UpdateDialogState.Downloading) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = state !is UpdateDialogState.Downloading,
            dismissOnClickOutside = state !is UpdateDialogState.Downloading
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF374151))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            when (state) {
                                is UpdateDialogState.ReadyToInstall -> Color(0xFF065F46)
                                is UpdateDialogState.Error -> Color(0xFF7F1D1D)
                                else -> Color(0xFF1E3A8A)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (state) {
                            is UpdateDialogState.ReadyToInstall -> Icons.Default.CheckCircle
                            is UpdateDialogState.Error -> Icons.Default.Warning
                            is UpdateDialogState.Downloading -> Icons.Default.Download
                            else -> Icons.Default.SystemUpdate
                        },
                        contentDescription = null,
                        tint = when (state) {
                            is UpdateDialogState.ReadyToInstall -> Color(0xFF34D399)
                            is UpdateDialogState.Error -> Color(0xFFF87171)
                            else -> Color(0xFF38BDF8)
                        },
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Title
                Text(
                    text = when (state) {
                        is UpdateDialogState.ReadyToInstall -> "Yangilanish Tayyor!"
                        is UpdateDialogState.Downloading -> "Yuklab Olinmoqda..."
                        is UpdateDialogState.Error -> "Yuklashda Xatolik"
                        else -> "Yangi Versiya Mavjud 🚀"
                    },
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Version comparison pills
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF1F2937))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Joriy: v${updateInfo.currentVersion}",
                            color = Color(0xFF9CA3AF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Text("➔", color = Color(0xFF38BDF8), fontSize = 12.sp)

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0C4A6E))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Yangi: v${updateInfo.latestVersion}",
                            color = Color(0xFF38BDF8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (updateInfo.apkSize > 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    val mbSize = String.format(Locale.US, "%.1f MB", updateInfo.apkSize / (1024.0 * 1024.0))
                    Text(
                        text = "Hajmi: $mbSize",
                        color = Color(0xFF6B7280),
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Content / Changelog Box
                when (val currentState = state) {
                    is UpdateDialogState.Available -> {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 160.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2937))
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = null,
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "O'zgarishlar va yangiliklar:",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = updateInfo.changelog.ifBlank { "Tizim barqarorligi va tezligi oshirildi." },
                                    color = Color(0xFFD1D5DB),
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF9CA3AF))
                            ) {
                                Text("Keyinroq", fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    state = UpdateDialogState.Downloading(updateInfo, 0f, 0L, updateInfo.apkSize)
                                    coroutineScope.launch {
                                        val result = UpdateManager.downloadApk(
                                            context = context,
                                            downloadUrl = updateInfo.downloadUrl,
                                            onProgress = { progress, downloaded, total ->
                                                state = UpdateDialogState.Downloading(updateInfo, progress, downloaded, total)
                                            }
                                        )
                                        result.onSuccess { file ->
                                            state = UpdateDialogState.ReadyToInstall(updateInfo, file)
                                            UpdateManager.installApk(context, file)
                                        }.onFailure { err ->
                                            state = UpdateDialogState.Error(
                                                updateInfo,
                                                err.localizedMessage ?: "Noma'lum xatolik"
                                            )
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1.3f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                            ) {
                                Text("Yangilash ⬇️", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    is UpdateDialogState.Downloading -> {
                        Spacer(modifier = Modifier.height(8.dp))
                        val percent = (currentState.progress * 100).toInt().coerceIn(0, 100)
                        val downloadedMB = String.format(Locale.US, "%.1f", currentState.downloadedBytes / (1024.0 * 1024.0))
                        val totalMB = String.format(Locale.US, "%.1f", currentState.totalBytes / (1024.0 * 1024.0))

                        LinearProgressIndicator(
                            progress = { currentState.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = Color(0xFF38BDF8),
                            trackColor = Color(0xFF374151)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "$percent%",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "$downloadedMB MB / $totalMB MB",
                                color = Color(0xFF9CA3AF),
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Ilova yuklanmoqda, iltimos kuting...",
                            color = Color(0xFF6B7280),
                            fontSize = 11.sp
                        )
                    }

                    is UpdateDialogState.ReadyToInstall -> {
                        Text(
                            "PhotoCheck v${updateInfo.latestVersion} muvaffaqiyatli yuklandi!",
                            color = Color(0xFF34D399),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Button(
                            onClick = {
                                UpdateManager.installApk(context, currentState.apkFile)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                        ) {
                            Text("O'rnatishni Davom Ettirish 📦", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    is UpdateDialogState.Error -> {
                        Text(
                            text = currentState.errorMessage,
                            color = Color(0xFFF87171),
                            fontSize = 12.sp
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Yopish")
                            }

                            Button(
                                onClick = {
                                    state = UpdateDialogState.Available(updateInfo)
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                            ) {
                                Text("Qayta Urinish 🔄")
                            }
                        }
                    }
                }
            }
        }
    }
}
