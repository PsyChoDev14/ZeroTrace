package lk.novalink.zerotrace.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import lk.novalink.zerotrace.ZeroTraceApp
import lk.novalink.zerotrace.data.repository.LiveTrafficData
import lk.novalink.zerotrace.ui.theme.ZtAccent
import lk.novalink.zerotrace.ui.theme.ZtBg
import lk.novalink.zerotrace.ui.theme.ZtBorder
import lk.novalink.zerotrace.ui.theme.ZtSurface
import lk.novalink.zerotrace.ui.theme.ZtSurface2
import lk.novalink.zerotrace.ui.theme.ZtText
import lk.novalink.zerotrace.ui.theme.ZtTextFaint
import lk.novalink.zerotrace.ui.theme.ZtTextMuted
import lk.novalink.zerotrace.ui.theme.ZtTrack
import java.util.Locale

private data class DisplayStats(
    val rangeLabel: String,
    val totalFormatted: Pair<String, String>, // ("14.8", "GB") or ("350", "MB")
    val downloadFormatted: String,
    val uploadFormatted: String,
    val connectedTime: String,
    val chartPoints: List<Float> // 0.1..1.0 scale
)

@Composable
fun StatisticsScreen(
    modifier: Modifier = Modifier
) {
    val statsRepo = ZeroTraceApp.instance.trafficStatsRepository
    val todayData by statsRepo.todayStats.collectAsState()
    val weekData by statsRepo.weekStats.collectAsState()
    val monthData by statsRepo.monthStats.collectAsState()

    var selectedIndex by remember { mutableIntStateOf(1) } // 0=Today, 1=This Week, 2=This Month

    val activeData = when (selectedIndex) {
        0 -> todayData
        1 -> weekData
        else -> monthData
    }

    val rangeLabels = listOf("Today", "This Week", "This Month")
    val currentLabel = rangeLabels[selectedIndex]

    val displayStats = buildDisplayStats(currentLabel, activeData)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ZtBg)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .padding(bottom = 80.dp)
    ) {
        // Header
        Text(
            text = "Usage Statistics",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = ZtText
        )
        Text(
            text = "Measured on this device only",
            fontSize = 12.5.sp,
            color = ZtTextMuted
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Segmented Tab Picker
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(ZtSurface)
                .border(1.dp, ZtBorder, RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            rangeLabels.forEachIndexed { index, label ->
                val isSelected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (isSelected) ZtSurface2 else Color.Transparent)
                        .clickable { selectedIndex = index }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 12.5.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) ZtText else ZtTextMuted
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Animated Total Traffic Display
        AnimatedContent(
            targetState = displayStats,
            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
            label = "totalTrafficStats"
        ) { stats ->
            Column {
                Text(
                    text = "TOTAL TRAFFIC",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.1.sp,
                    color = ZtTextFaint
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = stats.totalFormatted.first,
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = ZtText
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stats.totalFormatted.second,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ZtTextMuted,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                Text(
                    text = "${stats.rangeLabel} · ${stats.connectedTime} connected",
                    fontSize = 12.5.sp,
                    color = ZtTextMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Visual Usage Bar Chart
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(ZtSurface)
                .border(1.dp, ZtBorder, RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val barCount = displayStats.chartPoints.size
                val availableWidth = size.width
                val barWidth = availableWidth / (barCount * 1.8f)
                val spacing = (availableWidth - (barWidth * barCount)) / (barCount - 1)
                val maxHeight = size.height

                displayStats.chartPoints.forEachIndexed { i, fraction ->
                    val x = i * (barWidth + spacing)
                    val barH = (maxHeight * fraction).coerceAtLeast(10f)
                    val y = maxHeight - barH

                    // Background track bar
                    drawRoundRect(
                        color = ZtTrack,
                        topLeft = Offset(x, 0f),
                        size = Size(barWidth, maxHeight),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )

                    // Foreground active data bar
                    drawRoundRect(
                        color = ZtAccent,
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barH),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Breakdown Card (Download, Upload, Connected Time)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(ZtSurface)
                .border(1.dp, ZtBorder, RoundedCornerShape(16.dp))
                .padding(vertical = 4.dp)
        ) {
            Column {
                StatRow(
                    icon = Icons.Default.ArrowDownward,
                    iconTint = ZtAccent,
                    label = "Download",
                    value = displayStats.downloadFormatted
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(ZtBorder)
                )

                StatRow(
                    icon = Icons.Default.ArrowUpward,
                    iconTint = ZtTextMuted,
                    label = "Upload",
                    value = displayStats.uploadFormatted
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(ZtBorder)
                )

                StatRow(
                    icon = Icons.Default.AccessTime,
                    iconTint = ZtTextFaint,
                    label = "Connection time",
                    value = displayStats.connectedTime
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Live Connected Apps Section
        LiveAppTrafficSection()

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Counters are measured locally on device and persisted securely in app storage.",
            fontSize = 11.5.sp,
            color = ZtTextFaint
        )
    }
}

@Composable
private fun LiveAppTrafficSection() {
    var showSheet by remember { mutableStateOf(false) }
    val liveApps by lk.novalink.zerotrace.core.LiveAppTrafficManager.liveApps.collectAsState()
    val activeCount by lk.novalink.zerotrace.core.LiveAppTrafficManager.activeAppsCount.collectAsState()
    val activeOrSessionApps = remember(liveApps) {
        liveApps.filter { it.isActiveNow || it.totalSessionBytes > 0 }.take(4)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "LIVE APP TRAFFIC",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.1.sp,
                    color = ZtTextFaint
                )
                if (activeCount > 0) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0x2635C77B))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "$activeCount ACTIVE",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = ZtSuccess
                        )
                    }
                }
            }

            Text(
                text = "View All (${liveApps.size}) →",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = ZtAccent,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { showSheet = true }
                    .padding(4.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (activeOrSessionApps.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(ZtSurface)
                    .border(1.dp, ZtBorder, RoundedCornerShape(16.dp))
                    .clickable { showSheet = true }
                    .padding(vertical = 18.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No active app bandwidth right now",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = ZtTextMuted
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Tap to inspect all installed apps & tunnel routing",
                        fontSize = 11.sp,
                        color = ZtTextFaint
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(ZtSurface)
                    .border(1.dp, ZtBorder, RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                activeOrSessionApps.forEachIndexed { index, app ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showSheet = true }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (app.iconBitmap != null) {
                            androidx.compose.foundation.Image(
                                bitmap = app.iconBitmap,
                                contentDescription = app.appName,
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(RoundedCornerShape(6.dp))
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(ZtSurface2),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = app.appName.take(1).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = ZtAccent
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = app.appName,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ZtText,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                if (app.isTunneled) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(Color(0x2635C77B))
                                            .padding(horizontal = 3.dp, vertical = 0.5.dp)
                                    ) {
                                        Text("VPN", fontSize = 7.5.sp, fontWeight = FontWeight.Bold, color = ZtSuccess)
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(Color(0x26FF9500))
                                            .padding(horizontal = 3.dp, vertical = 0.5.dp)
                                    ) {
                                        Text("BYPASS", fontSize = 7.5.sp, fontWeight = FontWeight.Bold, color = ZtWarn)
                                    }
                                }
                            }
                            Text(
                                text = if (app.totalSessionBytes > 0) "Session: ${app.formatBytes(app.totalSessionBytes)}" else app.packageName,
                                fontSize = 10.sp,
                                color = ZtTextFaint,
                                maxLines = 1
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            if (app.isActiveNow) {
                                Text(
                                    text = "↓ ${app.formatSpeed(app.downloadSpeed)}",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ZtSuccess
                                )
                            } else {
                                Text(
                                    text = "IDLE",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.5.sp,
                                    color = ZtTextFaint
                                )
                            }
                        }
                    }

                    if (index < activeOrSessionApps.size - 1) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(ZtBorder)
                        )
                    }
                }
            }
        }
    }

    if (showSheet) {
        lk.novalink.zerotrace.ui.components.LiveConnectedAppsSheet(
            onDismiss = { showSheet = false }
        )
    }
}

@Composable
private fun StatRow(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = label,
                fontSize = 13.5.sp,
                color = ZtTextMuted
            )
        }

        Text(
            text = value,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
            color = ZtText
        )
    }
}

private fun buildDisplayStats(label: String, data: LiveTrafficData): DisplayStats {
    val totalBytes = data.downloadBytes + data.uploadBytes
    val totalPair = formatBytesPair(totalBytes)

    // Normalize daily history for bar chart
    val maxBytes = (data.dailyHistory.maxOrNull() ?: 1L).coerceAtLeast(1024 * 1024L) // min 1MB baseline
    val chartPoints = data.dailyHistory.map {
        if (it == 0L) 0.12f else (it.toFloat() / maxBytes.toFloat()).coerceIn(0.12f, 1.0f)
    }

    return DisplayStats(
        rangeLabel = label,
        totalFormatted = totalPair,
        downloadFormatted = formatBytesString(data.downloadBytes),
        uploadFormatted = formatBytesString(data.uploadBytes),
        connectedTime = formatSeconds(data.connectionSeconds),
        chartPoints = chartPoints
    )
}

private fun formatBytesPair(bytes: Long): Pair<String, String> {
    if (bytes <= 0) return Pair("0.0", "MB")
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1024.0) {
        val gb = mb / 1024.0
        Pair(String.format(Locale.US, "%.1f", gb), "GB")
    } else {
        Pair(String.format(Locale.US, "%.1f", mb), "MB")
    }
}

private fun formatBytesString(bytes: Long): String {
    val pair = formatBytesPair(bytes)
    return "${pair.first} ${pair.second}"
}

private fun formatSeconds(seconds: Long): String {
    if (seconds <= 0) return "0m"
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    return if (hours > 0) {
        "${hours}h ${minutes}m"
    } else {
        "${minutes}m"
    }
}
