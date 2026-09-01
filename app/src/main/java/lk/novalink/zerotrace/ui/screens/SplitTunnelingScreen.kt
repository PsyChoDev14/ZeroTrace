package lk.novalink.zerotrace.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import lk.novalink.zerotrace.ZeroTraceApp
import lk.novalink.zerotrace.data.model.InstalledAppInfo
import lk.novalink.zerotrace.data.model.SplitTunnelMode
import lk.novalink.zerotrace.ui.theme.ZtAccent
import lk.novalink.zerotrace.ui.theme.ZtAccentSoft
import lk.novalink.zerotrace.ui.theme.ZtBg
import lk.novalink.zerotrace.ui.theme.ZtBorder
import lk.novalink.zerotrace.ui.theme.ZtSuccess
import lk.novalink.zerotrace.ui.theme.ZtSurface
import lk.novalink.zerotrace.ui.theme.ZtSurface2
import lk.novalink.zerotrace.ui.theme.ZtText
import lk.novalink.zerotrace.ui.theme.ZtTextFaint
import lk.novalink.zerotrace.ui.theme.ZtTextMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitTunnelingScreen(
    currentMode: SplitTunnelMode,
    selectedApps: Set<String>,
    onModeChange: (SplitTunnelMode) -> Unit,
    onToggleApp: (String) -> Unit,
    onSelectAll: (List<String>) -> Unit,
    onDeselectAll: () -> Unit,
    onBackClick: () -> Unit
) {
    val appsRepo = ZeroTraceApp.instance.installedAppsRepository
    val allInstalledApps by appsRepo.installedApps.collectAsState()
    val isLoading by appsRepo.isLoading.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var filterTab by remember { mutableIntStateOf(0) } // 0: User Apps, 1: Banking & Local, 2: Selected, 3: System Apps, 4: All

    val userAppsCount = remember(allInstalledApps.size) { allInstalledApps.count { !it.isSystemApp } }
    val bankAppsCount = remember(allInstalledApps.size) { allInstalledApps.count { it.isSuggestedBankingOrLocal } }
    val systemAppsCount = remember(allInstalledApps.size) { allInstalledApps.count { it.isSystemApp } }

    val filteredApps = remember(allInstalledApps.size, searchQuery, filterTab, selectedApps) {
        allInstalledApps.filter { app ->
            val matchesSearch = searchQuery.isBlank() ||
                    app.appName.contains(searchQuery, ignoreCase = true) ||
                    app.packageName.contains(searchQuery, ignoreCase = true)

            val matchesTab = when (filterTab) {
                0 -> !app.isSystemApp // User Apps (Installed + Launchable + Store Apps)
                1 -> app.isSuggestedBankingOrLocal // Banking & Local
                2 -> selectedApps.contains(app.packageName) // Selected Only
                3 -> app.isSystemApp // Background OS Daemons & Frameworks
                else -> true // All
            }

            matchesSearch && matchesTab
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ZtBg)
            .statusBarsPadding()
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = ZtText
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Split Tunneling",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = ZtText
                )
                Text(
                    text = when (currentMode) {
                        SplitTunnelMode.OFF -> "All device apps use VPN"
                        SplitTunnelMode.EXCLUDE_SELECTED -> "Bypassing ${selectedApps.size} selected apps"
                        SplitTunnelMode.INCLUDE_ONLY -> "VPN only for ${selectedApps.size} selected apps"
                    },
                    fontSize = 11.5.sp,
                    color = if (currentMode != SplitTunnelMode.OFF) ZtAccent else ZtTextMuted
                )
            }

            IconButton(onClick = { appsRepo.refreshInstalledApps() }) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh Apps",
                    tint = ZtTextMuted
                )
            }
        }

        // Mode Selection Segment
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ModeOptionCard(
                title = "All Apps (Standard VPN)",
                subtitle = "All applications on your phone route through the encrypted VPN tunnel.",
                isSelected = currentMode == SplitTunnelMode.OFF,
                onClick = { onModeChange(SplitTunnelMode.OFF) }
            )

            ModeOptionCard(
                title = "Bypass Selected Apps (Recommended)",
                subtitle = "VPN is active for everything EXCEPT selected apps (Bypass Banking, PickMe, Uber, SLT).",
                isSelected = currentMode == SplitTunnelMode.EXCLUDE_SELECTED,
                onClick = { onModeChange(SplitTunnelMode.EXCLUDE_SELECTED) }
            )

            ModeOptionCard(
                title = "VPN Only for Selected Apps",
                subtitle = "Only checked apps will use the VPN. All other apps connect to regular ISP.",
                isSelected = currentMode == SplitTunnelMode.INCLUDE_ONLY,
                onClick = { onModeChange(SplitTunnelMode.INCLUDE_ONLY) }
            )
        }

        // Show App Selection List when mode is not OFF
        AnimatedVisibility(visible = currentMode != SplitTunnelMode.OFF) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.height(16.dp))

                // Search Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, ZtBorder, RoundedCornerShape(12.dp)),
                        placeholder = { Text("Search apps or package...", color = ZtTextFaint, fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = ZtTextMuted, modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = ZtTextMuted, modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = ZtSurface,
                            unfocusedContainerColor = ZtSurface,
                            focusedTextColor = ZtText,
                            unfocusedTextColor = ZtText,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Filter Tabs & Quick Action Row (Horizontally Scrollable)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterPill(label = "User Apps ($userAppsCount)", isSelected = filterTab == 0, onClick = { filterTab = 0 })
                        FilterPill(label = "🏦 Banking ($bankAppsCount)", isSelected = filterTab == 1, onClick = { filterTab = 1 })
                        if (selectedApps.isNotEmpty()) {
                            FilterPill(label = "Selected (${selectedApps.size})", isSelected = filterTab == 2, onClick = { filterTab = 2 })
                        }
                        FilterPill(label = "⚙️ System ($systemAppsCount)", isSelected = filterTab == 3, onClick = { filterTab = 3 })
                        FilterPill(label = "All (${allInstalledApps.size})", isSelected = filterTab == 4, onClick = { filterTab = 4 })
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Select / Clear All
                    Text(
                        text = if (selectedApps.isNotEmpty()) "Clear (${selectedApps.size})" else "Select All",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = ZtAccent,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                if (selectedApps.isNotEmpty()) {
                                    onDeselectAll()
                                } else {
                                    onSelectAll(filteredApps.map { it.packageName })
                                }
                            }
                            .padding(4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = ZtAccent, modifier = Modifier.size(32.dp))
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(
                            items = filteredApps,
                            key = { it.packageName },
                            contentType = { "app_row" }
                        ) { app ->
                            AppItemCard(
                                app = app,
                                isChecked = selectedApps.contains(app.packageName),
                                onToggle = { onToggleApp(app.packageName) }
                            )
                        }
                        item(contentType = "bottom_spacer") { Spacer(modifier = Modifier.height(24.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeOptionCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) ZtAccentSoft else ZtSurface)
            .border(1.dp, if (isSelected) ZtAccent else ZtBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = ZtAccent,
                    unselectedColor = ZtTextFaint
                )
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    color = if (isSelected) ZtAccent else ZtText
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.5.sp,
                    color = ZtTextMuted,
                    lineHeight = 15.sp
                )
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
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) ZtAccent else ZtSurface2)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else ZtTextMuted
        )
    }
}

@Composable
private fun AppItemCard(
    app: InstalledAppInfo,
    isChecked: Boolean,
    onToggle: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isChecked) ZtAccentSoft.copy(alpha = 0.08f) else ZtSurface)
            .border(1.dp, if (isChecked) ZtAccent.copy(alpha = 0.4f) else ZtBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Icon
            if (app.iconBitmap != null) {
                Image(
                    bitmap = app.iconBitmap,
                    contentDescription = app.appName,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(ZtSurface2),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = app.appName.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = ZtAccent
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = app.appName,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ZtText,
                        maxLines = 1
                    )
                    if (app.isSuggestedBankingOrLocal) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0x2635C77B))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "LOCAL/BANK",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = ZtSuccess
                            )
                        }
                    }
                }

                Text(
                    text = app.packageName,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.5.sp,
                    color = ZtTextFaint,
                    maxLines = 1
                )
            }

            // Checkbox
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isChecked) ZtAccent else Color.Transparent)
                    .border(1.5.dp, if (isChecked) ZtAccent else ZtBorder, RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (isChecked) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
