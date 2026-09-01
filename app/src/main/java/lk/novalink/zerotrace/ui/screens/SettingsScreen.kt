package lk.novalink.zerotrace.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Shield
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import lk.novalink.zerotrace.data.model.DnsProviders
import lk.novalink.zerotrace.data.model.SplitTunnelMode
import lk.novalink.zerotrace.data.repository.SettingsRepository
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

/**
 * Native Jetpack Compose implementation of Settings Screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    primaryDns: String,
    bypassLan: Boolean,
    sriLankaSni: String,
    dpiBypassMode: lk.novalink.zerotrace.data.model.DpiBypassMode = lk.novalink.zerotrace.data.model.DpiBypassMode.SMART_FRAGMENT,
    utlsFingerprint: String = "chrome",
    muxEnabled: Boolean = false,
    splitTunnelMode: SplitTunnelMode = SplitTunnelMode.OFF,
    splitTunnelCount: Int = 0,
    biometricEnabled: Boolean = false,
    onDnsChange: (String) -> Unit,
    onBypassLanChange: (Boolean) -> Unit,
    onSriLankaSniChange: (String) -> Unit,
    onDpiModeChange: (lk.novalink.zerotrace.data.model.DpiBypassMode) -> Unit = {},
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
    val currentVersionName = remember(context) { lk.novalink.zerotrace.core.UpdateManager.getCurrentVersionName(context) }
    val currentVersionCode = remember(context) { lk.novalink.zerotrace.core.UpdateManager.getCurrentVersionCode(context) }
    val scrollState = rememberScrollState()

    var dnsExpanded by remember { mutableStateOf(false) }
    var dpiExpanded by remember { mutableStateOf(false) }
    var utlsExpanded by remember { mutableStateOf(false) }
    var showTileGuideDialog by remember { mutableStateOf(false) }

    if (showTileGuideDialog) {
        lk.novalink.zerotrace.ui.components.QuickSettingsTileGuideDialog(
            onDismiss = { showTileGuideDialog = false }
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
            .padding(bottom = 100.dp)
    ) {
        // Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBackClick != null) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
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
                    text = "VPN core, routing & application settings",
                    fontSize = 12.sp,
                    color = ZtTextMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // SECTION 1: SYSTEM & NETWORK
        SectionHeader(title = "SYSTEM & NETWORK")

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Split Tunneling Card
            SettingsCard(modifier = Modifier.clickable(onClick = onNavigateToSplitTunneling)) {
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
                                    SplitTunnelMode.EXCLUDE_SELECTED -> "Bypassing $splitTunnelCount apps (Banking/PickMe)"
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

            // Bypass LAN Switch
            SettingsCard {
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
                                text = "Directly route local network traffic",
                                fontSize = 11.5.sp,
                                color = ZtTextMuted
                            )
                        }
                    }
                    Switch(
                        checked = bypassLan,
                        onCheckedChange = onBypassLanChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = ZtAccent,
                            uncheckedTrackColor = ZtSurface2
                        )
                    )
                }
            }

            // Biometric App Lock Switch
            SettingsCard {
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
                                text = if (biometricEnabled) "App locked with Fingerprint & Face ID" else "Require Fingerprint/PIN to open ZeroTrace",
                                fontSize = 11.5.sp,
                                color = if (biometricEnabled) ZtSuccess else ZtTextMuted
                            )
                        }
                    }
                    Switch(
                        checked = biometricEnabled,
                        onCheckedChange = onToggleBiometric,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = ZtAccent,
                            uncheckedTrackColor = ZtSurface2
                        )
                    )
                }
            }

            // Primary DNS Provider Selector (With Built-in AdGuard Ad-Blocker)
            SettingsCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Dns,
                                contentDescription = "DNS",
                                tint = ZtAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Primary DNS & Ad-Blocking",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = ZtText
                            )
                        }

                        val currentProfile = DnsProviders.findByPrimaryIp(primaryDns)
                        if (currentProfile?.isAdBlocker == true) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(Color(0x2635C77B))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "AD-BLOCK ON",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ZtSuccess
                                )
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
                                        dnsExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Quick Settings Notification Tile Guide
            SettingsCard(modifier = Modifier.clickable { showTileGuideDialog = true }) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = "Quick Settings Tile",
                        tint = ZtAccent,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Quick Settings Tile",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = ZtText
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(ZtAccentSoft)
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "GUIDE",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ZtAccent
                                )
                            }
                        }
                        Text(
                            text = "Tap here to see guide & 1-tap add to notification bar",
                            fontSize = 11.5.sp,
                            color = ZtTextMuted,
                            lineHeight = 15.sp
                        )
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

        Spacer(modifier = Modifier.height(24.dp))

        // SECTION 2: DPI & STEALTH ENGINE
        SectionHeader(title = "DPI & STEALTH ENGINE")

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // DPI Bypass Mode Card
            SettingsCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "DPI Bypass",
                                tint = ZtAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Anti-DPI / Censorship Bypass",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = ZtText
                            )
                        }

                        if (dpiBypassMode != lk.novalink.zerotrace.data.model.DpiBypassMode.OFF) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(Color(0x2635C77B))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (dpiBypassMode == lk.novalink.zerotrace.data.model.DpiBypassMode.DEEP_STEALTH) "STEALTH MAX" else "ACTIVE",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ZtSuccess
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

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
                            lk.novalink.zerotrace.data.model.DpiBypassMode.values().forEach { mode ->
                                DropdownMenuItem(
                                    text = {
                                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                            Text(mode.title, color = ZtText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            Text(mode.description, color = ZtTextMuted, fontSize = 10.5.sp)
                                        }
                                    },
                                    onClick = {
                                        onDpiModeChange(mode)
                                        dpiExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // uTLS Browser Camouflage Profile
            SettingsCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = "uTLS Camouflage",
                                tint = ZtAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "uTLS Browser Fingerprint",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = ZtText
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

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
                                        utlsExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Mux.Cool Multiplexing Switch
            SettingsCard {
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
                            contentDescription = "Mux Multiplexing",
                            tint = ZtAccent,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Mux.Cool Stream Multiplexing",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = ZtText
                            )
                            Text(
                                text = "Multiplexes 8-16 streams into single TLS pipeline to defeat traffic burst analysis",
                                fontSize = 11.5.sp,
                                color = ZtTextMuted
                            )
                        }
                    }
                    Switch(
                        checked = muxEnabled,
                        onCheckedChange = onMuxChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = ZtAccent,
                            uncheckedTrackColor = ZtSurface2
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // SECTION 3: UPDATES & SUPPORT
        SectionHeader(title = "UPDATES & SUPPORT")

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (onShowOnboarding != null) {
                SupportButton(
                    title = "Introduction & Feature Guide",
                    subtitle = "Review the 3-step onboarding walkthrough",
                    icon = Icons.Default.Shield,
                    iconTint = ZtAccent,
                    onClick = onShowOnboarding
                )
            }

            SupportButton(
                title = "Check for Updates",
                subtitle = "v$currentVersionName • Tap to check latest release",
                icon = Icons.Default.CloudDownload,
                iconTint = ZtAccent,
                onClick = onCheckUpdatesClick
            )

            SupportButton(
                title = "Telegram Customer Support",
                subtitle = "Fast responses, updates & configs",
                icon = Icons.Default.Send,
                iconTint = ZtAccent,
                onClick = { openUrl(SettingsRepository.TELEGRAM_SUPPORT_URL) }
            )

            SupportButton(
                title = "WhatsApp Direct Support",
                subtitle = "Chat with NovaLink LK technical team",
                icon = Icons.Default.Chat,
                iconTint = Color(0xFF25D366),
                onClick = { openUrl(SettingsRepository.WHATSAPP_SUPPORT_URL) }
            )

            SupportButton(
                title = "Official Website & Portal",
                subtitle = "Manage configs & subscription plans",
                icon = Icons.Default.Language,
                iconTint = ZtAccent,
                onClick = { openUrl(SettingsRepository.WEBSITE_URL) }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // SECTION 3: ABOUT
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

                    Spacer(modifier = Modifier.height(14.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(ZtBorder))
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { openUrl("https://nexauracore.com") },
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
                            imageVector = Icons.Default.OpenInNew,
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
private fun SettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ZtSurface)
            .border(1.dp, ZtBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        content()
    }
}

@Composable
private fun SupportButton(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    onClick: () -> Unit
) {
    SettingsCard(modifier = Modifier.clickable(onClick = onClick)) {
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
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = title,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.5.sp,
                        color = ZtText
                    )
                    Text(
                        text = subtitle,
                        fontSize = 11.5.sp,
                        color = ZtTextMuted
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.OpenInNew,
                contentDescription = "Open",
                tint = ZtTextFaint,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
