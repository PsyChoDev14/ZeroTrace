package lk.novalink.zerotrace.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import lk.novalink.zerotrace.core.UpdateManager
import lk.novalink.zerotrace.data.model.AppUpdateInfo
import lk.novalink.zerotrace.data.model.UpdateState
import lk.novalink.zerotrace.ui.theme.AccentBlue
import lk.novalink.zerotrace.ui.theme.BorderSubtle
import lk.novalink.zerotrace.ui.theme.SapphireCore
import lk.novalink.zerotrace.ui.theme.SapphireLight
import lk.novalink.zerotrace.ui.theme.StatusConnected
import lk.novalink.zerotrace.ui.theme.StatusRed
import lk.novalink.zerotrace.ui.theme.SurfaceDark
import lk.novalink.zerotrace.ui.theme.SurfaceElevated
import lk.novalink.zerotrace.ui.theme.TextDim
import lk.novalink.zerotrace.ui.theme.TextMuted
import lk.novalink.zerotrace.ui.theme.TextWhite

@Composable
fun UpdateDialog(
    updateState: UpdateState,
    onDismiss: () -> Unit,
    onStartDownload: (AppUpdateInfo) -> Unit
) {
    val context = LocalContext.current

    val (info, isDownloading, isReady, isError) = when (updateState) {
        is UpdateState.UpdateAvailable -> Quadruple(updateState.updateInfo, false, false, false)
        is UpdateState.Downloading -> Quadruple(updateState.updateInfo, true, false, false)
        is UpdateState.ReadyToInstall -> Quadruple(updateState.updateInfo, false, true, false)
        is UpdateState.Error -> Quadruple(null, false, false, true)
        else -> Quadruple(null, false, false, false)
    }

    if (info == null && !isError) return

    val canDismiss = if (info != null) !info.forceUpdate && !isDownloading else true

    Dialog(
        onDismissRequest = { if (canDismiss) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = canDismiss,
            dismissOnClickOutside = canDismiss,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = SurfaceDark,
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .border(1.5.dp, BorderSubtle, RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .padding(22.dp)
                    .fillMaxWidth()
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isError) StatusRed.copy(alpha = 0.15f)
                                else if (isReady) StatusConnected.copy(alpha = 0.15f)
                                else SapphireCore.copy(alpha = 0.15f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isError) Icons.Default.ErrorOutline
                            else if (isReady) Icons.Default.SystemUpdate
                            else Icons.Default.NewReleases,
                            contentDescription = null,
                            tint = if (isError) StatusRed
                            else if (isReady) StatusConnected
                            else SapphireLight,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = if (isError) "Update Error"
                            else if (isReady) "Ready to Install"
                            else if (isDownloading) "Downloading Update..."
                            else "New Update Available!",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = TextWhite
                        )
                        if (info != null) {
                            Text(
                                text = "ZeroTrace v${info.versionName} • Build ${info.versionCode}",
                                fontSize = 12.sp,
                                color = AccentBlue
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Changelog or Content
                if (info != null) {
                    if (info.changelog.isNotBlank()) {
                        Text(
                            text = "What's New:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextMuted
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(SurfaceElevated)
                                .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = info.changelog,
                                fontSize = 12.sp,
                                color = TextWhite,
                                lineHeight = 18.sp,
                                modifier = Modifier.verticalScroll(rememberScrollState())
                            )
                        }
                    }

                    // Progress bar during downloading
                    if (updateState is UpdateState.Downloading) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (updateState.totalBytes > 0) {
                                        "${formatBytes(updateState.downloadedBytes)} / ${formatBytes(updateState.totalBytes)}"
                                    } else {
                                        formatBytes(updateState.downloadedBytes)
                                    },
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                                Text(
                                    text = "${(updateState.progress * 100).toInt()}%",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SapphireLight
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            if (updateState.totalBytes > 0) {
                                LinearProgressIndicator(
                                    progress = { updateState.progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = SapphireLight,
                                    trackColor = Color(0xFF1E2D40)
                                )
                            } else {
                                LinearProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = SapphireLight,
                                    trackColor = Color(0xFF1E2D40)
                                )
                            }
                        }
                    }
                } else if (updateState is UpdateState.Error) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(StatusRed.copy(alpha = 0.1f))
                            .border(1.dp, StatusRed.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = updateState.message,
                            fontSize = 12.sp,
                            color = StatusRed
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (canDismiss) {
                        TextButton(onClick = onDismiss) {
                            Text(
                                text = if (isError) "Close" else "Later",
                                color = TextMuted,
                                fontSize = 13.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    if (updateState is UpdateState.UpdateAvailable) {
                        Button(
                            onClick = { onStartDownload(updateState.updateInfo) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SapphireCore,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Update Now",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    } else if (updateState is UpdateState.ReadyToInstall) {
                        Button(
                            onClick = { UpdateManager.installApk(context, updateState.apkFile) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StatusConnected,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SystemUpdate,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Install & Restart",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

private fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
        bytes >= 1024 -> String.format("%.1f KB", bytes / 1024.0)
        else -> "$bytes B"
    }
}
