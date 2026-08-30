package lk.novalink.zerotrace.ui.screens

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import lk.novalink.zerotrace.data.model.InstalledAppInfo
import lk.novalink.zerotrace.data.model.SplitTunnelMode
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
import java.util.Locale

// Known Sri Lankan Banking & Local Ride-sharing / Delivery package keywords
private val KNOWN_LOCAL_KEYWORDS = listOf(
    "bank", "combank", "boc", "sampath", "hnb", "peoplesbank", "ndb", "nsb", "seylan",
    "pickme", "uber", "daraz", "dialog", "mobitel", "slt", "myaccount", "airtel",
    "payhere", "ezcash", "mcash", "friMi", "qplus", "genie", "ipay", "flash"
)

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
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var filterTab by remember { mutableStateOf(0) } // 0: All, 1: User Apps, 2: Banking & Local
    var isLoading by remember { mutableStateOf(true) }
    val allInstalledApps = remember { mutableStateListOf<InstalledAppInfo>() }

    // Load installed apps asynchronously in background
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            val appList = mutableListOf<InstalledAppInfo>()

            for (pkg in packages) {
                // Don't include self app
                if (pkg.packageName == context.packageName) continue

                val isSystem = (pkg.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val appName = try {
                    pm.getApplicationLabel(pkg).toString()
                } catch (e: Exception) {
                    pkg.packageName
                }

                val lower = (appName + " " + pkg.packageName).lowercase(Locale.ROOT)
                val isLocalOrBank = KNOWN_LOCAL_KEYWORDS.any { lower.contains(it) }

                val iconDrawable = try {
                    pm.getApplicationIcon(pkg)
                } catch (e: Exception) {
                    null
                }

                appList.add(
                    InstalledAppInfo(
                        packageName = pkg.packageName,
                        appName = appName,
                        icon = iconDrawable,
                        isSystemApp = isSystem,
                        isSuggestedBankingOrLocal = isLocalOrBank
                    )
                )
            }

            appList.sortBy { it.appName.lowercase(Locale.ROOT) }

            withContext(Dispatchers.Main) {
                allInstalledApps.clear()
                allInstalledApps.addAll(appList)
                isLoading = false
            }
        }
    }

    val filteredApps = remember(allInstalledApps, searchQuery, filterTab) {
        allInstalledApps.filter { app ->
            val matchesSearch = searchQuery.isBlank() ||
                    app.appName.contains(searchQuery, ignoreCase = true) ||
                    app.packageName.contains(searchQuery, ignoreCase = true)

            val matchesTab = when (filterTab) {
                1 -> !app.isSystemApp
                2 -> app.isSuggestedBankingOrLocal
                else -> true
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
                        placeholder = { Text("Search installed apps...", color = ZtTextFaint, fontSize = 13.sp) },
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

                // Filter Tabs & Quick Action Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterPill(label = "All (${allInstalledApps.size})", isSelected = filterTab == 0, onClick = { filterTab = 0 })
                        FilterPill(label = "User Apps", isSelected = filterTab == 1, onClick = { filterTab = 1 })
                        FilterPill(label = "🏦 Banking & Local", isSelected = filterTab == 2, onClick = { filterTab = 2 })
                    }

                    // Select / Clear All
                    Text(
                        text = if (selectedApps.isNotEmpty()) "Clear (${selectedApps.size})" else "Select All",
                        fontSize = 12.sp,
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
                        items(filteredApps, key = { it.packageName }) { app ->
                            AppItemCard(
                                app = app,
                                isChecked = selectedApps.contains(app.packageName),
                                onToggle = { onToggleApp(app.packageName) }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(24.dp)) }
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
            if (app.icon != null) {
                val bitmap = remember(app.icon) { drawableToBitmap(app.icon) }
                Image(
                    bitmap = bitmap.asImageBitmap(),
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

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = app.appName,
                        fontSize = 13.sp,
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

private fun drawableToBitmap(drawable: Drawable): Bitmap {
    if (drawable is BitmapDrawable && drawable.bitmap != null) {
        return drawable.bitmap
    }
    val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 48
    val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 48
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}
