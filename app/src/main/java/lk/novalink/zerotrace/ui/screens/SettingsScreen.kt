package lk.novalink.zerotrace.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import lk.novalink.zerotrace.data.model.DnsProviders
import lk.novalink.zerotrace.data.model.DpiBypassMode
import lk.novalink.zerotrace.data.model.SplitTunnelMode
import lk.novalink.zerotrace.data.repository.SettingsRepository
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import lk.novalink.zerotrace.ui.theme.ZtAccent
import lk.novalink.zerotrace.ui.theme.ZtAccentSoft
import lk.novalink.zerotrace.ui.theme.ZtBg
import lk.novalink.zerotrace.ui.theme.ZtBorder
import lk.novalink.zerotrace.ui.theme.ZtDanger
import lk.novalink.zerotrace.ui.theme.ZtSuccess
import lk.novalink.zerotrace.ui.theme.ZtSurface
import lk.novalink.zerotrace.ui.theme.ZtSurface2
import lk.novalink.zerotrace.ui.theme.ZtText
import lk.novalink.zerotrace.ui.theme.ZtTextFaint
import lk.novalink.zerotrace.ui.theme.ZtTextMuted

/**
 * Decluttered, sleek Jetpack Compose Settings Screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    primaryDns: String,
    bypassLan: Boolean,
    sriLankaSni: String,
    dpiBypassMode: DpiBypassMode = DpiBypassMode.SMART_FRAGMENT,
    utlsFingerprint: String = "chrome",
    muxEnabled: Boolean = false,
    splitTunnelMode: SplitTunnelMode = SplitTunnelMode.OFF,
    splitTunnelCount: Int = 0,
    biometricEnabled: Boolean = false,
    onDnsChange: (String) -> Unit,
    onBypassLanChange: (Boolean) -> Unit,
    onSriLankaSniChange: (String) -> Unit,
    onDpiModeChange: (DpiBypassMode) -> Unit = {},
    onUtlsFingerprintChange: (String) -> Unit = {},
    onMuxChange: (Boolean) -> Unit = {},
    onToggleBiometric: (Boolean) -> Unit = {},
    onNavigateToSplitTunneling: () -> Unit = {},
    onCheckUpdatesClick: () -> Unit,
    onShowOnboarding: (() -> Unit)? = null,
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentVersionName = remember(context) { lk.novalink.zerotrace.core.UpdateManager.getCurrentVersionName(context) }
    val currentVersionCode = remember(context) { lk.novalink.zerotrace.core.UpdateManager.getCurrentVersionCode(context) }
    val scrollState = rememberScrollState()

    var searchQuery by remember { mutableStateOf("") }
    var savedItem by remember { mutableStateOf<String?>(null) }
    var showResetAllConfirmDialog by remember { mutableStateOf(false) }
    var showClearLogsConfirmDialog by remember { mutableStateOf(false) }

    var dnsExpanded by remember { mutableStateOf(false) }
    var dpiExpanded by remember { mutableStateOf(false) }
    var utlsExpanded by remember { mutableStateOf(false) }
    var isStealthSectionExpanded by remember { mutableStateOf(false) }
    var showTileGuideDialog by remember { mutableStateOf(false) }
    var showDiagnosticsDialog by remember { mutableStateOf(false) }

    fun notifySaved(item: String) {
        coroutineScope.launch {
            savedItem = item
            delay(1400)
            if (savedItem == item) {
                savedItem = null
            }
        }
    }

    val isDnsModified = primaryDns != "94.140.14.14"
    val isSplitModified = splitTunnelMode != SplitTunnelMode.OFF || splitTunnelCount > 0
    val isBiometricModified = biometricEnabled
    val isLanModified = !bypassLan
    val isDpiModified = dpiBypassMode != DpiBypassMode.OFF
    val isUtlsModified = utlsFingerprint.lowercase() != "chrome"
    val isMuxModified = muxEnabled
    val isStealthModified = isDpiModified || isUtlsModified || isMuxModified
    val activeStealthCount = (if (isDpiModified) 1 else 0) + (if (isUtlsModified) 1 else 0) + (if (isMuxModified) 1 else 0)

    val modifiedCount = listOf(isDnsModified, isSplitModified, isBiometricModified, isLanModified, isDpiModified, isUtlsModified, isMuxModified).count { it }

    val cleanQuery = searchQuery.trim().lowercase()
    fun matches(vararg terms: String): Boolean {
        if (cleanQuery.isEmpty()) return true
        return terms.any { it.lowercase().contains(cleanQuery) }
    }

    val showDnsCard = matches("dns", "ad-blocking", "shield", "cloudflare", "adguard", "google", "quad9", "family", "malware", "custom")
    val showSplitTunnelCard = matches("split", "tunneling", "per-app", "exclude", "include", "banking", "bypass")
    val showBiometricCard = matches("biometric", "app lock", "fingerprint", "face id", "pin", "lock", "security")
    val showBypassLanCard = matches("bypass lan", "lan", "local", "printer", "ip", "routing")
    val showProtectionSection = showDnsCard || showSplitTunnelCard || showBiometricCard || showBypassLanCard

    val showStealthCard = matches("stealth", "dpi", "bypass", "fragment", "tls", "packet", "utls", "browser", "fingerprint", "mux", "multiplexing", "cool", "sni", "engine", "firewall", "deep stealth", "speed")

    val showTileGuideCard = matches("tile", "quick settings", "notification", "panel", "status bar")
    val showUpdatesCard = matches("update", "version", "download", "release", "check")
    val showTelegramCard = matches("telegram", "community", "support", "chat", "help")
    val showWhatsappCard = matches("whatsapp", "support", "chat", "novalink")
    val showDiagnosticsCard = matches("diagnostic", "log", "developer", "report", "debug", "error")
    val showSupportSection = showTileGuideCard || showUpdatesCard || showTelegramCard || showWhatsappCard || showDiagnosticsCard

    val showAboutCard = matches("about", "version", "zerotrace", "nexaura", "nadun", "developer", "website")
    val showDangerZone = matches("danger", "reset", "clear", "logs", "cache", "factory", "defaults", "erase")

    val hasAnyMatch = showProtectionSection || showStealthCard || showSupportSection || showAboutCard || showDangerZone

    val effectiveStealthExpanded = isStealthSectionExpanded || (cleanQuery.isNotEmpty() && showStealthCard)

    if (showTileGuideDialog) {
        lk.novalink.zerotrace.ui.components.QuickSettingsTileGuideDialog(
            onDismiss = { showTileGuideDialog = false }
        )
    }

    if (showDiagnosticsDialog) {
        lk.novalink.zerotrace.ui.components.DiagnosticLogsDialog(
            onDismiss = { showDiagnosticsDialog = false }
        )
    }

    fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ZtBg)
            .statusBarsPadding()
            .verticalScroll(scrollState)
            .padding(bottom = 140.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f, fill = false),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onBackClick != null) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = ZtText
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Column {
                    Text(
                        text = "Settings",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = ZtText
                    )
                    Text(
                        text = "Protection, stealth & application settings",
                        fontSize = 12.sp,
                        color = ZtTextMuted
                    )
                }
            }

            if (modifiedCount > 0) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x245468FF))
                        .border(1.dp, Color(0x4D5468FF), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(ZtAccent)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "$modifiedCount MODIFIED",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = ZtAccent,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }

        // Search Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            SearchSettingsBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (!hasAnyMatch) {
            EmptySearchResult(
                query = searchQuery,
                onClear = { searchQuery = "" }
            )
        } else {

        // SECTION 1: PROTECTION & PRIVACY
        if (showProtectionSection) {
            SectionHeader(title = "PROTECTION & SECURITY")

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // DNS Provider & Built-in Ad-Blocker
                if (showDnsCard) {
                    SettingsCard(isModified = isDnsModified) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f, fill = false),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Dns,
                                        contentDescription = "DNS",
                                        tint = ZtAccent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "DNS & Ad-Blocking Shield",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = ZtText
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    SavedMicroPill(visible = savedItem == "dns")
                                    if (isDnsModified) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        ResetPill(onReset = {
                                            onDnsChange("94.140.14.14")
                                            notifySaved("dns")
                                        })
                                    }
                                    val currentProfile = DnsProviders.findByPrimaryIp(primaryDns)
                                    if (currentProfile?.isAdBlocker == true) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(5.dp))
                                                .background(Color(0x2635C77B))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "SHIELD ON",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = ZtSuccess
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            ExposedDropdownMenuBox(
                                expanded = dnsExpanded,
                                onExpandedChange = { dnsExpanded = !dnsExpanded }
                            ) {
                                val selectedProfile = DnsProviders.findByPrimaryIp(primaryDns)
                                val displayValue = if (selectedProfile != null) {
                                    "${selectedProfile.name} (${selectedProfile.primaryIp})"
                                } else {
                                    "Custom DNS ($primaryDns)"
                                }

                                OutlinedTextField(
                                    value = displayValue,
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dnsExpanded) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ZtAccent,
                                        unfocusedBorderColor = ZtBorder,
                                        focusedTextColor = ZtText,
                                        unfocusedTextColor = ZtText
                                    ),
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = dnsExpanded,
                                    onDismissRequest = { dnsExpanded = false },
                                    modifier = Modifier.background(ZtSurface2)
                                ) {
                                    DnsProviders.ALL_PROFILES.forEach { profile ->
                                        DropdownMenuItem(
                                            text = {
                                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(profile.name, color = ZtText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(4.dp))
                                                                .background(if (profile.isAdBlocker) Color(0x2635C77B) else ZtAccentSoft)
                                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                                        ) {
                                                            Text(
                                                                text = profile.categoryTag,
                                                                fontSize = 8.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (profile.isAdBlocker) ZtSuccess else ZtAccent
                                                            )
                                                        }
                                                    }
                                                    Text(profile.description, color = ZtTextMuted, fontSize = 10.5.sp)
                                                }
                                            },
                                            onClick = {
                                                onDnsChange(profile.primaryIp)
                                                notifySaved("dns")
                                                dnsExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Per-App Split Tunneling Card
                if (showSplitTunnelCard) {
                    SettingsCard(
                        isModified = isSplitModified,
                        modifier = Modifier.clickable(onClick = onNavigateToSplitTunneling)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AltRoute,
                                    contentDescription = "Split Tunneling",
                                    tint = ZtAccent,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Per-App Split Tunneling",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                            color = ZtText
                                        )
                                        if (splitTunnelMode != SplitTunnelMode.OFF) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(ZtAccentSoft)
                                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                                            ) {
                                                Text(
                                                    text = "${splitTunnelCount} APPS",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = ZtAccent
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = when (splitTunnelMode) {
                                            SplitTunnelMode.OFF -> "Off • All apps route through VPN"
                                            SplitTunnelMode.EXCLUDE_SELECTED -> "Bypassing $splitTunnelCount apps (Banking/Local)"
                                            SplitTunnelMode.INCLUDE_ONLY -> "VPN only for $splitTunnelCount selected apps"
                                        },
                                        fontSize = 11.5.sp,
                                        color = if (splitTunnelMode != SplitTunnelMode.OFF) ZtAccent else ZtTextMuted
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Open",
                                tint = ZtTextFaint,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Biometric App Lock Switch
                if (showBiometricCard) {
                    SettingsCard(isModified = isBiometricModified) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = "Biometric Lock",
                                    tint = ZtAccent,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Biometric App Lock",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                            color = ZtText
                                        )
                                        if (biometricEnabled) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(Color(0x2635C77B))
                                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                                            ) {
                                                Text(
                                                    text = "ACTIVE",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = ZtSuccess
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = if (biometricEnabled) "Locked with Fingerprint & Face ID" else "Require Fingerprint/PIN to open ZeroTrace",
                                        fontSize = 11.5.sp,
                                        color = if (biometricEnabled) ZtSuccess else ZtTextMuted
                                    )
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                SavedMicroPill(visible = savedItem == "biometric")
                                if (isBiometricModified) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    ResetPill(onReset = {
                                        onToggleBiometric(false)
                                        notifySaved("biometric")
                                    })
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Switch(
                                    checked = biometricEnabled,
                                    onCheckedChange = {
                                        onToggleBiometric(it)
                                        notifySaved("biometric")
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = ZtAccent,
                                        uncheckedTrackColor = ZtSurface2
                                    )
                                )
                            }
                        }
                    }
                }

                // Bypass LAN Switch
                if (showBypassLanCard) {
                    SettingsCard(isModified = isLanModified) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Route,
                                    contentDescription = "Bypass LAN",
                                    tint = ZtAccent,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Bypass LAN / Local IPs",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = ZtText
                                    )
                                    Text(
                                        text = "Directly route local printers & devices",
                                        fontSize = 11.5.sp,
                                        color = ZtTextMuted
                                    )
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                SavedMicroPill(visible = savedItem == "lan")
                                if (isLanModified) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    ResetPill(onReset = {
                                        onBypassLanChange(true)
                                        notifySaved("lan")
                                    })
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Switch(
                                    checked = bypassLan,
                                    onCheckedChange = {
                                        onBypassLanChange(it)
                                        notifySaved("lan")
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = ZtAccent,
                                        uncheckedTrackColor = ZtSurface2
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // SECTION 2: ADVANCED PROTOCOL & STEALTH ENGINE
        if (showStealthCard) {
            if (showProtectionSection) {
                Spacer(modifier = Modifier.height(20.dp))
            }
            SectionHeader(title = "ADVANCED PROTOCOL & STEALTH")

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                val rotationAngle by animateFloatAsState(
                    targetValue = if (effectiveStealthExpanded) 180f else 0f,
                    animationSpec = tween(200),
                    label = "accordionRotation"
                )

                SettingsCard(
                    isModified = isStealthModified,
                    modifier = Modifier.clickable { isStealthSectionExpanded = !isStealthSectionExpanded }
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = "Stealth Engine",
                                    tint = ZtAccent,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "DPI Bypass & Stealth Engine",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                            color = ZtText
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(if (dpiBypassMode != DpiBypassMode.OFF) Color(0x2635C77B) else ZtSurface2)
                                                .padding(horizontal = 5.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = if (dpiBypassMode == DpiBypassMode.DEEP_STEALTH) "STEALTH MAX" else if (dpiBypassMode != DpiBypassMode.OFF) "ACTIVE" else "OFF",
                                                fontSize = 8.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (dpiBypassMode != DpiBypassMode.OFF) ZtSuccess else ZtTextFaint
                                            )
                                        }
                                    }
                                    Text(
                                        text = "TLS packet fragmentation, uTLS & Mux",
                                        fontSize = 11.5.sp,
                                        color = ZtTextMuted
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                SavedMicroPill(visible = savedItem == "stealth")
                                if (isStealthModified) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(ZtAccentSoft)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "$activeStealthCount ACTIVE",
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ZtAccent
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    ResetPill(onReset = {
                                        onDpiModeChange(DpiBypassMode.OFF)
                                        onUtlsFingerprintChange("chrome")
                                        onMuxChange(false)
                                        notifySaved("stealth")
                                    })
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(ZtSurface2)
                                            .padding(horizontal = 5.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = "DEFAULT",
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ZtTextFaint
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.ExpandMore,
                                    contentDescription = "Expand",
                                    tint = ZtTextMuted,
                                    modifier = Modifier
                                        .size(22.dp)
                                        .rotate(rotationAngle)
                                )
                            }
                        }

                        // Expandable Technical Options
                        AnimatedVisibility(
                            visible = effectiveStealthExpanded,
                            enter = fadeIn(tween(200)) + expandVertically(tween(200)),
                            exit = fadeOut(tween(150)) + shrinkVertically(tween(150))
                        ) {
                            Column(modifier = Modifier.padding(top = 16.dp)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(ZtBorder)
                                )
                                Spacer(modifier = Modifier.height(14.dp))

                                // 1. Anti-DPI Mode Picker
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "ANTI-DPI FIREWALL MODE",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp,
                                        color = ZtTextFaint
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        SavedMicroPill(visible = savedItem == "dpi")
                                        if (isDpiModified) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            ResetPill(onReset = {
                                                onDpiModeChange(DpiBypassMode.OFF)
                                                notifySaved("dpi")
                                            })
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))

                                ExposedDropdownMenuBox(
                                    expanded = dpiExpanded,
                                    onExpandedChange = { dpiExpanded = !dpiExpanded }
                                ) {
                                    OutlinedTextField(
                                        value = dpiBypassMode.title,
                                        onValueChange = {},
                                        readOnly = true,
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dpiExpanded) },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = ZtAccent,
                                            unfocusedBorderColor = ZtBorder,
                                            focusedTextColor = ZtText,
                                            unfocusedTextColor = ZtText
                                        ),
                                        modifier = Modifier
                                            .menuAnchor()
                                            .fillMaxWidth()
                                    )
                                    ExposedDropdownMenu(
                                        expanded = dpiExpanded,
                                        onDismissRequest = { dpiExpanded = false },
                                        modifier = Modifier.background(ZtSurface2)
                                    ) {
                                        DpiBypassMode.values().forEach { mode ->
                                            DropdownMenuItem(
                                                text = {
                                                    Column(modifier = Modifier.padding(vertical = 3.dp)) {
                                                        Text(mode.title, color = ZtText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                                        Text(mode.description, color = ZtTextMuted, fontSize = 10.sp)
                                                    }
                                                },
                                                onClick = {
                                                    onDpiModeChange(mode)
                                                    notifySaved("dpi")
                                                    dpiExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // 2. uTLS Browser Camouflage
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "uTLS BROWSER FINGERPRINT",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp,
                                        color = ZtTextFaint
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        SavedMicroPill(visible = savedItem == "utls")
                                        if (isUtlsModified) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            ResetPill(onReset = {
                                                onUtlsFingerprintChange("chrome")
                                                notifySaved("utls")
                                            })
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))

                                val uTlsProfiles = listOf("chrome", "safari", "firefox", "ios", "randomized")
                                ExposedDropdownMenuBox(
                                    expanded = utlsExpanded,
                                    onExpandedChange = { utlsExpanded = !utlsExpanded }
                                ) {
                                    OutlinedTextField(
                                        value = utlsFingerprint.replaceFirstChar { it.uppercase() },
                                        onValueChange = {},
                                        readOnly = true,
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = utlsExpanded) },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = ZtAccent,
                                            unfocusedBorderColor = ZtBorder,
                                            focusedTextColor = ZtText,
                                            unfocusedTextColor = ZtText
                                        ),
                                        modifier = Modifier
                                            .menuAnchor()
                                            .fillMaxWidth()
                                    )
                                    ExposedDropdownMenu(
                                        expanded = utlsExpanded,
                                        onDismissRequest = { utlsExpanded = false },
                                        modifier = Modifier.background(ZtSurface2)
                                    ) {
                                        uTlsProfiles.forEach { fp ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        text = fp.replaceFirstChar { it.uppercase() },
                                                        color = ZtText,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                },
                                                onClick = {
                                                    onUtlsFingerprintChange(fp)
                                                    notifySaved("utls")
                                                    utlsExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // 3. Mux.Cool Multiplexing
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Mux.Cool Stream Multiplexing",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            color = ZtText
                                        )
                                        Text(
                                            text = "Multiplexes connections to defeat burst inspection",
                                            fontSize = 11.sp,
                                            color = ZtTextMuted
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        SavedMicroPill(visible = savedItem == "mux")
                                        if (isMuxModified) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            ResetPill(onReset = {
                                                onMuxChange(false)
                                                notifySaved("mux")
                                            })
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Switch(
                                            checked = muxEnabled,
                                            onCheckedChange = {
                                                onMuxChange(it)
                                                notifySaved("mux")
                                            },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color.White,
                                                checkedTrackColor = ZtAccent,
                                                uncheckedTrackColor = ZtSurface2
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // SECTION 3: SYSTEM & SUPPORT
        if (showSupportSection) {
            if (showProtectionSection || showStealthCard) {
                Spacer(modifier = Modifier.height(20.dp))
            }
            SectionHeader(title = "UPDATES & SUPPORT")

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Quick Settings Tile Guide
                if (showTileGuideCard) {
                    SettingsCard(modifier = Modifier.clickable { showTileGuideDialog = true }) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PowerSettingsNew,
                                contentDescription = "Quick Settings Tile",
                                tint = ZtAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Notification Bar Tile Guide",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = ZtText
                                )
                                Text(
                                    text = "1-tap connect tile in Android notification panel",
                                    fontSize = 11.5.sp,
                                    color = ZtTextMuted
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Open",
                                tint = ZtTextFaint,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Check for Updates (Dynamic Version)
                if (showUpdatesCard) {
                    SettingsCard(modifier = Modifier.clickable(onClick = onCheckUpdatesClick)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = "Check for Updates",
                                tint = ZtAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Check for Updates",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = ZtText
                                )
                                Text(
                                    text = "v$currentVersionName • Tap to check latest release",
                                    fontSize = 11.5.sp,
                                    color = ZtTextMuted
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Check",
                                tint = ZtTextFaint,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Telegram Customer Support
                if (showTelegramCard) {
                    SettingsCard(modifier = Modifier.clickable { openUrl(SettingsRepository.TELEGRAM_SUPPORT_URL) }) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Telegram",
                                tint = ZtAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Telegram Community & Support",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = ZtText
                                )
                                Text(
                                    text = "Fast updates, VIP configs & community help",
                                    fontSize = 11.5.sp,
                                    color = ZtTextMuted
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = "Open",
                                tint = ZtTextFaint,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // WhatsApp Direct Support
                if (showWhatsappCard) {
                    SettingsCard(modifier = Modifier.clickable { openUrl(SettingsRepository.WHATSAPP_SUPPORT_URL) }) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Chat,
                                contentDescription = "WhatsApp",
                                tint = Color(0xFF25D366),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "WhatsApp Direct Support",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = ZtText
                                )
                                Text(
                                    text = "Chat with NovaLink LK technical team",
                                    fontSize = 11.5.sp,
                                    color = ZtTextMuted
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = "Open",
                                tint = ZtTextFaint,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Diagnostic Logs & Developer Support
                if (showDiagnosticsCard) {
                    SettingsCard(modifier = Modifier.clickable { showDiagnosticsDialog = true }) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.BugReport,
                                contentDescription = "Diagnostic Logs",
                                tint = ZtAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Diagnostic Logs & Support",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = ZtText
                                )
                                Text(
                                    text = "Export connection logs & device diagnostics to developer",
                                    fontSize = 11.5.sp,
                                    color = ZtTextMuted
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Open",
                                tint = ZtTextFaint,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // SECTION 4: ABOUT
        if (showAboutCard) {
            if (showProtectionSection || showStealthCard || showSupportSection) {
                Spacer(modifier = Modifier.height(20.dp))
            }
            SectionHeader(title = "ABOUT")

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                SettingsCard {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "ZeroTrace VPN",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = ZtText
                                )
                                Text(
                                    text = "Version $currentVersionName (Build $currentVersionCode)",
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = ZtTextMuted
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(ZtAccentSoft)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "OFFICIAL",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ZtAccent,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(ZtBorder)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { openUrl(SettingsRepository.WEBSITE_URL) },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Engineered by",
                                    fontSize = 11.sp,
                                    color = ZtTextFaint
                                )
                                Text(
                                    text = "Nexaura Core",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ZtAccent
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = "Website",
                                tint = ZtAccent,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Column {
                            Text(
                                text = "Lead Developer",
                                fontSize = 11.sp,
                                color = ZtTextFaint
                            )
                            Text(
                                text = "Nadun Gawesh",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ZtText
                            )
                        }
                    }
                }
            }
        }

        // SECTION 5: QUARANTINED DANGER ZONE
        if (showDangerZone) {
            Spacer(modifier = Modifier.height(24.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                DangerZoneCard(
                    onResetAll = { showResetAllConfirmDialog = true },
                    onClearLogs = { showClearLogsConfirmDialog = true }
                )
            }
        }
        } // End of else (!hasAnyMatch)
    } // End of Column

    if (showResetAllConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetAllConfirmDialog = false },
            title = {
                Text(
                    text = "Reset All Settings?",
                    fontWeight = FontWeight.Bold,
                    color = ZtText
                )
            },
            text = {
                Text(
                    text = "This will restore DNS, bypass LAN, anti-DPI mode, uTLS camouflage, and multiplexing settings back to their factory defaults.",
                    color = ZtTextMuted,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDnsChange("94.140.14.14")
                        onBypassLanChange(true)
                        onDpiModeChange(DpiBypassMode.OFF)
                        onUtlsFingerprintChange("chrome")
                        onMuxChange(false)
                        onSriLankaSniChange("")
                        showResetAllConfirmDialog = false
                        notifySaved("all")
                    }
                ) {
                    Text("Reset All", color = ZtDanger, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetAllConfirmDialog = false }) {
                    Text("Cancel", color = ZtTextMuted)
                }
            },
            containerColor = ZtSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showClearLogsConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearLogsConfirmDialog = false },
            title = {
                Text(
                    text = "Clear Connection Logs?",
                    fontWeight = FontWeight.Bold,
                    color = ZtText
                )
            },
            text = {
                Text(
                    text = "Permanently deletes the exported diagnostic report and temporary log buffers from local device storage.",
                    color = ZtTextMuted,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        try {
                            val diagFile = java.io.File(context.cacheDir, "ZeroTrace_Diagnostic_Report.txt")
                            if (diagFile.exists()) diagFile.delete()
                            val logDir = java.io.File(context.cacheDir, "logs")
                            if (logDir.exists()) logDir.deleteRecursively()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        showClearLogsConfirmDialog = false
                        notifySaved("logs")
                    }
                ) {
                    Text("Clear Logs", color = ZtDanger, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearLogsConfirmDialog = false }) {
                    Text("Cancel", color = ZtTextMuted)
                }
            },
            containerColor = ZtSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        color = ZtTextFaint,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
    )
}

@Composable
private fun SearchSettingsBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ZtSurface)
            .border(
                width = 1.dp,
                color = if (query.isNotEmpty()) ZtAccent.copy(alpha = 0.6f) else ZtBorder,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 14.dp, vertical = 11.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = if (query.isNotEmpty()) ZtAccent else ZtTextMuted,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        text = "Search settings (DNS, stealth, biometric, mux...)",
                        color = ZtTextMuted,
                        fontSize = 13.sp
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        color = ZtText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    cursorBrush = SolidColor(ZtAccent),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (query.isNotEmpty()) {
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Clear search",
                    tint = ZtTextMuted,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable { onQueryChange("") }
                )
            }
        }
    }
}

@Composable
private fun EmptySearchResult(
    query: String,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.SearchOff,
            contentDescription = null,
            tint = ZtTextMuted,
            modifier = Modifier.size(44.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "No settings matching \"$query\"",
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            color = ZtText
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Try searching for DNS, stealth, split tunneling, or biometric lock",
            fontSize = 12.sp,
            color = ZtTextMuted,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(ZtAccentSoft)
                .clickable(onClick = onClear)
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Clear Search",
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                color = ZtAccent
            )
        }
    }
}

@Composable
private fun SettingsCard(
    modifier: Modifier = Modifier,
    isModified: Boolean = false,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ZtSurface)
            .border(
                width = 1.dp,
                color = if (isModified) ZtAccent.copy(alpha = 0.5f) else ZtBorder,
                shape = RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        content()
    }
}

/**
 * Animated "✓ Saved" micro-pill giving instant feedback for light toggles
 * as featured in "Settings is a system"
 */
@Composable
private fun SavedMicroPill(visible: Boolean) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(150)) + scaleIn(tween(150)),
        exit = fadeOut(tween(300)) + scaleOut(tween(300))
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0x2635C77B))
                .border(1.dp, Color(0x4D35C77B), RoundedCornerShape(6.dp))
                .padding(horizontal = 7.dp, vertical = 2.dp)
        ) {
            Text(
                text = "✓ Saved",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = ZtSuccess
            )
        }
    }
}

/**
 * 1-Click "Reset to Default" chip for modified settings
 * as featured in "Settings is a system"
 */
@Composable
private fun ResetPill(onReset: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0x245468FF))
            .border(1.dp, Color(0x4D5468FF), RoundedCornerShape(6.dp))
            .clickable(onClick = onReset)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = "Reset to default",
            tint = ZtAccent,
            modifier = Modifier.size(11.dp)
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = "Default",
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            color = ZtAccent
        )
    }
}

/**
 * Quarantined Danger Zone at the bottom of Settings
 * Isolates high-stakes destructive actions with friction scaling
 */
@Composable
private fun DangerZoneCard(
    onResetAll: () -> Unit,
    onClearLogs: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x12F0533D))
            .border(1.dp, Color(0x40F0533D), RoundedCornerShape(14.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Danger Zone",
                    tint = ZtDanger,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "DANGER ZONE",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp,
                        color = ZtDanger
                    )
                    Text(
                        text = "Destructive actions live in quarantine behind friction",
                        fontSize = 11.5.sp,
                        color = ZtTextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0x26F0533D))
            )
            Spacer(modifier = Modifier.height(14.dp))

            // Action 1: Reset All Settings to Defaults
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Reset All Settings",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = ZtText
                    )
                    Text(
                        text = "Reverts DNS, stealth, bypass & tweak configurations",
                        fontSize = 11.sp,
                        color = ZtTextMuted
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(7.dp))
                        .background(Color(0x24F0533D))
                        .border(1.dp, Color(0x66F0533D), RoundedCornerShape(7.dp))
                        .clickable(onClick = onResetAll)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Reset All",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ZtDanger
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0x26F0533D))
            )
            Spacer(modifier = Modifier.height(14.dp))

            // Action 2: Clear Logs & Cache
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Clear Connection Logs & Cache",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = ZtText
                    )
                    Text(
                        text = "Permanently removes local diagnostics and core reports",
                        fontSize = 11.sp,
                        color = ZtTextMuted
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(7.dp))
                        .background(Color(0x24F0533D))
                        .border(1.dp, Color(0x66F0533D), RoundedCornerShape(7.dp))
                        .clickable(onClick = onClearLogs)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Clear Logs",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ZtDanger
                    )
                }
            }
        }
    }
}
