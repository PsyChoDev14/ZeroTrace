package lk.novalink.zerotrace.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import lk.novalink.zerotrace.core.LiveAppTrafficManager
import lk.novalink.zerotrace.data.model.AppTrafficStats
import lk.novalink.zerotrace.ui.theme.ZtAccent
import lk.novalink.zerotrace.ui.theme.ZtAccentSoft
import lk.novalink.zerotrace.ui.theme.ZtBg
import lk.novalink.zerotrace.ui.theme.ZtBgElevated
import lk.novalink.zerotrace.ui.theme.ZtBorder
import lk.novalink.zerotrace.ui.theme.ZtSuccess
import lk.novalink.zerotrace.ui.theme.ZtSurface
import lk.novalink.zerotrace.ui.theme.ZtSurface2
import lk.novalink.zerotrace.ui.theme.ZtText
import lk.novalink.zerotrace.ui.theme.ZtTextFaint
import lk.novalink.zerotrace.ui.theme.ZtTextMuted
import lk.novalink.zerotrace.ui.theme.ZtWarn

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveConnectedAppsSheet(
    onDismiss: () -> Unit,
    onNavigateToSplitTunneling: (() -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val liveApps by LiveAppTrafficManager.liveApps.collectAsState()
    val activeCount by LiveAppTrafficManager.activeAppsCount.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var filterTab by remember { mutableIntStateOf(0) } // 0: Active Now, 1: Tunneled, 2: Bypassed, 3: All

    val filteredApps = remember(liveApps, searchQuery, filterTab) {
        liveApps.filter { app ->
            val matchesSearch = searchQuery.isBlank() ||
                    app.appName.contains(searchQuery, ignoreCase = true) ||
                    app.packageName.contains(searchQuery, ignoreCase = true)

            val matchesTab = when (filterTab) {
                0 -> app.isActiveNow || app.totalSessionBytes > 0
                1 -> app.isTunneled
                2 -> !app.isTunneled
                else -> true
            }

            matchesSearch && matchesTab
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ZtBgElevated,
        contentColor = ZtText,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(bottom = 24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Pulsing green radar dot
                    val pulse = rememberInfiniteTransition(label = "radar")
                    val alpha by pulse.animateFloat(
                        initialValue = 0.3f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(800, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "radarAlpha"
                    )

                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(ZtSuccess.copy(alpha = alpha))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Live Connected Apps",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = ZtText
                        )
                        Text(
                            text = if (activeCount > 0) "$activeCount apps transmitting live via VPN" else "Monitoring per-app network traffic",
                            fontSize = 11.5.sp,
                            color = if (activeCount > 0) ZtSuccess else ZtTextMuted
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = ZtTextMuted)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                placeholder = {
                    Text("Search active apps or packages...", fontSize = 13.sp, color = ZtTextFaint)
                },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = ZtTextMuted, modifier = Modifier.size(18.dp))
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ZtAccent,
                    unfocusedBorderColor = ZtBorder,
                    focusedContainerColor = ZtSurface,
                    unfocusedContainerColor = ZtSurface,
                    focusedTextColor = ZtText,
                    unfocusedTextColor = ZtText
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Filter Tabs Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val activeOnlyCount = liveApps.count { it.isActiveNow || it.totalSessionBytes > 0 }
                FilterPill(label = "Active ($activeOnlyCount)", isSelected = filterTab == 0, onClick = { filterTab = 0 })
                FilterPill(label = "Tunneled", isSelected = filterTab == 1, onClick = { filterTab = 1 })
                FilterPill(label = "Bypassed", isSelected = filterTab == 2, onClick = { filterTab = 2 })
                FilterPill(label = "All (${liveApps.size})", isSelected = filterTab == 3, onClick = { filterTab = 3 })
            }

            Spacer(modifier = Modifier.height(12.dp))

            // App List
            if (filteredApps.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = ZtTextFaint,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (filterTab == 0) "No active app traffic right now" else "No matching apps found",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = ZtTextMuted
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Launch any app to see real-time bandwidth & routing",
                            fontSize = 12.sp,
                            color = ZtTextFaint
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(
                        items = filteredApps,
                        key = { it.packageName },
                        contentType = { "app_traffic_row" }
                    ) { app ->
                        LiveAppCard(app = app)
                    }
                }
            }

            // Bottom Split Tunneling shortcut
            if (onNavigateToSplitTunneling != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            onDismiss()
                            onNavigateToSplitTunneling()
                        },
                    color = ZtSurface
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = ZtAccent, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Configure Per-App Split Tunneling", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = ZtText)
                        }
                        Text("Manage →", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ZtAccent)
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveAppCard(app: AppTrafficStats) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, if (app.isActiveNow) ZtAccent.copy(alpha = 0.5f) else ZtBorder, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = ZtSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Icon
            if (app.iconBitmap != null) {
                Image(
                    bitmap = app.iconBitmap,
                    contentDescription = app.appName,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(ZtSurface2),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = app.appName.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = ZtAccent
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // App Name & Package
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = app.appName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ZtText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))

                    // Route Status Badge
                    if (app.isTunneled) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0x2635C77B))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "ENCRYPTED",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = ZtSuccess
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0x26FF9500))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "BYPASS",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = ZtWarn
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = if (app.totalSessionBytes > 0) "Session: ${app.formatBytes(app.totalSessionBytes)}" else app.packageName,
                    fontSize = 10.5.sp,
                    color = ZtTextFaint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Live Transfer Speed
            Column(horizontalAlignment = Alignment.End) {
                if (app.isActiveNow) {
                    Text(
                        text = "↓ ${app.formatSpeed(app.downloadSpeed)}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = ZtSuccess
                    )
                    Text(
                        text = "↑ ${app.formatSpeed(app.uploadSpeed)}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = ZtTextMuted
                    )
                } else if (app.totalSessionBytes > 0) {
                    Text(
                        text = "IDLE",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = ZtTextFaint
                    )
                } else {
                    Text(
                        text = "0 B/s",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = ZtTextFaint
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) ZtAccent else ZtSurface)
            .border(1.dp, if (isSelected) ZtAccent else ZtBorder, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else ZtTextMuted
        )
    }
}
