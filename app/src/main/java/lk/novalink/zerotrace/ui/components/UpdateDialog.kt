package lk.novalink.zerotrace.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import lk.novalink.zerotrace.core.UpdateManager
import lk.novalink.zerotrace.data.model.AppUpdateInfo
import lk.novalink.zerotrace.data.model.UpdateState
import lk.novalink.zerotrace.ui.theme.ZtAccent
import lk.novalink.zerotrace.ui.theme.ZtAccentSoft
import lk.novalink.zerotrace.ui.theme.ZtBgElevated
import lk.novalink.zerotrace.ui.theme.ZtBorder
import lk.novalink.zerotrace.ui.theme.ZtDanger
import lk.novalink.zerotrace.ui.theme.ZtSuccess
import lk.novalink.zerotrace.ui.theme.ZtSurface
import lk.novalink.zerotrace.ui.theme.ZtSurface2
import lk.novalink.zerotrace.ui.theme.ZtText
import lk.novalink.zerotrace.ui.theme.ZtTextFaint
import lk.novalink.zerotrace.ui.theme.ZtTextMuted
import lk.novalink.zerotrace.ui.theme.ZtTrack
import lk.novalink.zerotrace.ui.theme.ZtWarn
import java.util.Locale

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

    val canDismiss = if (info != null) !info.forceUpdate else true

    Dialog(
        onDismissRequest = {
            if (isDownloading) {
                UpdateManager.cancelDownload()
            }
            if (canDismiss) onDismiss()
        },
        properties = DialogProperties(
            dismissOnBackPress = canDismiss,
            dismissOnClickOutside = canDismiss && !isDownloading,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = ZtBgElevated,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .border(1.5.dp, ZtBorder, RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .padding(22.dp)
                    .fillMaxWidth()
            ) {
                // Header with App Icon and Cancel / Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isError) ZtDanger.copy(alpha = 0.15f)
                                    else if (isReady) ZtSuccess.copy(alpha = 0.15f)
                                    else ZtAccentSoft
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isError) Icons.Default.ErrorOutline
                                else if (isReady) Icons.Default.SystemUpdate
                                else Icons.Default.RocketLaunch,
                                contentDescription = null,
                                tint = if (isError) ZtDanger
                                else if (isReady) ZtSuccess
                                else ZtAccent,
                                modifier = Modifier.size(24.dp)
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
                                color = ZtText
                            )

                            if (info != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    // Version Pill
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(ZtAccentSoft)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "v${info.versionName}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ZtAccent
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))

                                    if (info.releaseDate.isNotBlank()) {
                                        Text(
                                            text = info.releaseDate,
                                            fontSize = 11.sp,
                                            color = ZtTextFaint
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Top-right Close/Cancel button
                    if (canDismiss) {
                        IconButton(
                            onClick = {
                                if (isDownloading) {
                                    UpdateManager.cancelDownload()
                                }
                                onDismiss()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = ZtTextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Interactive Changelog Cards
                if (info != null && info.changelog.isNotBlank()) {
                    Text(
                        text = "WHAT'S NEW",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = ZtTextFaint
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val changelogItems = parseChangelog(info.changelog)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        changelogItems.forEach { item ->
                            InteractiveChangelogCard(item)
                        }
                    }
                }

                // Downloading Progress Bar & Live Stats
                if (updateState is UpdateState.Downloading) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(ZtSurface)
                            .border(1.dp, ZtBorder, RoundedCornerShape(14.dp))
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (updateState.totalBytes > 0) {
                                    "${formatBytes(updateState.downloadedBytes)} / ${formatBytes(updateState.totalBytes)}"
                                } else {
                                    formatBytes(updateState.downloadedBytes)
                                },
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = ZtTextMuted
                            )

                            Text(
                                text = "${(updateState.progress * 100).toInt()}%",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = ZtAccent
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val animatedProgress by animateFloatAsState(
                            targetValue = updateState.progress,
                            animationSpec = tween(150),
                            label = "downloadProgress"
                        )

                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = ZtAccent,
                            trackColor = ZtTrack
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Streaming APK over VPN tunnel...",
                            fontSize = 11.sp,
                            color = ZtTextFaint
                        )
                    }
                } else if (updateState is UpdateState.Error) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(ZtDanger.copy(alpha = 0.1f))
                            .border(1.dp, ZtDanger.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = updateState.message,
                            fontSize = 12.sp,
                            color = ZtDanger
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isDownloading) {
                        // Cancel Download Button
                        OutlinedButton(
                            onClick = {
                                UpdateManager.cancelDownload()
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ZtDanger)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancel",
                                tint = ZtDanger,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Cancel Download",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
                    } else if (updateState is UpdateState.UpdateAvailable) {
                        if (canDismiss) {
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ZtTextMuted)
                            ) {
                                Text(
                                    text = "Later",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Button(
                            onClick = { onStartDownload(updateState.updateInfo) },
                            modifier = Modifier.weight(1.3f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ZtAccent,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Download & Update",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    } else if (updateState is UpdateState.ReadyToInstall) {
                        if (canDismiss) {
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ZtTextMuted)
                            ) {
                                Text("Later", fontSize = 13.sp)
                            }
                        }

                        Button(
                            onClick = { UpdateManager.installApk(context, updateState.apkFile) },
                            modifier = Modifier.weight(1.3f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ZtSuccess,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SystemUpdate,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Install & Restart",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    } else if (isError) {
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = ZtSurface2),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Dismiss", color = ZtText)
                        }
                    }
                }
            }
        }
    }
}

private data class ChangelogEntry(
    val categoryTag: String,
    val tagColor: Color,
    val tagBg: Color,
    val description: String
)

@Composable
private fun InteractiveChangelogCard(entry: ChangelogEntry) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ZtSurface)
            .border(1.dp, ZtBorder, RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // Category Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(5.dp))
                    .background(entry.tagBg)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = entry.categoryTag,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = entry.tagColor,
                    letterSpacing = 0.4.sp
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = entry.description,
                fontSize = 12.sp,
                color = ZtText,
                lineHeight = 16.5.sp,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

private fun parseChangelog(rawText: String): List<ChangelogEntry> {
    val lines = rawText.split("\n", "\\n").map { it.trim().removePrefix("•").removePrefix("-").trim() }.filter { it.isNotBlank() }

    return lines.map { line ->
        val lower = line.lowercase(Locale.ROOT)
        val (tag, tagColor, tagBg) = when {
            lower.contains("speed") || lower.contains("performance") || lower.contains("bbr") || lower.contains("mtu") || lower.contains("buffer") ->
                Triple("⚡ SPEED", ZtSuccess, ZtSuccess.copy(alpha = 0.15f))
            lower.contains("size") || lower.contains("smaller") || lower.contains("r8") || lower.contains("shrink") ->
                Triple("📦 COMPACT", Color(0xFF64B5F6), Color(0x2664B5F6))
            lower.contains("qr") || lower.contains("share") || lower.contains("scanner") || lower.contains("camera") ->
                Triple("📷 QR CODE", Color(0xFFBA68C8), Color(0x26BA68C8))
            lower.contains("fix") || lower.contains("bug") || lower.contains("crash") || lower.contains("timeout") ->
                Triple("🐛 FIX", ZtWarn, ZtWarn.copy(alpha = 0.15f))
            lower.contains("ui") || lower.contains("design") || lower.contains("theme") ->
                Triple("🎨 UI/UX", Color(0xFFFF80AB), Color(0x26FF80AB))
            else ->
                Triple("✨ UPDATE", ZtAccent, ZtAccentSoft)
        }
        ChangelogEntry(tag, tagColor, tagBg, line)
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

private fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1024 * 1024 -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
        bytes >= 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
        else -> "$bytes B"
    }
}
