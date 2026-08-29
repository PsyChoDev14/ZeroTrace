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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Dns
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import lk.novalink.zerotrace.data.repository.SettingsRepository
import lk.novalink.zerotrace.ui.theme.ZtAccent
import lk.novalink.zerotrace.ui.theme.ZtAccentSoft
import lk.novalink.zerotrace.ui.theme.ZtBg
import lk.novalink.zerotrace.ui.theme.ZtBorder
import lk.novalink.zerotrace.ui.theme.ZtBorderStrong
import lk.novalink.zerotrace.ui.theme.ZtSurface
import lk.novalink.zerotrace.ui.theme.ZtSurface2
import lk.novalink.zerotrace.ui.theme.ZtText
import lk.novalink.zerotrace.ui.theme.ZtTextFaint
import lk.novalink.zerotrace.ui.theme.ZtTextMuted

/**
 * Native Jetpack Compose implementation of Settings.tsx from React design system
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    primaryDns: String,
    bypassLan: Boolean,
    sriLankaSni: String,
    onDnsChange: (String) -> Unit,
    onBypassLanChange: (Boolean) -> Unit,
    onSriLankaSniChange: (String) -> Unit,
    onCheckUpdatesClick: () -> Unit,
    onShowOnboarding: (() -> Unit)? = null,
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val dnsOptions = listOf(
        Pair("1.1.1.1", "Cloudflare DNS (1.1.1.1)"),
        Pair("8.8.8.8", "Google DNS (8.8.8.8)"),
        Pair("94.140.14.14", "AdGuard Ad-Blocking DNS"),
        Pair("9.9.9.9", "Quad9 Secure DNS")
    )
    var dnsExpanded by remember { mutableStateOf(false) }

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
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = ZtText
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = "Settings",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = ZtText
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // SECTION 1: CONNECTION
        SectionHeader(title = "CONNECTION")

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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

            // Primary DNS Selector
            SettingsCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Dns,
                            contentDescription = "DNS",
                            tint = ZtAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Primary DNS Provider",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = ZtText
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    ExposedDropdownMenuBox(
                        expanded = dnsExpanded,
                        onExpandedChange = { dnsExpanded = !dnsExpanded }
                    ) {
                        OutlinedTextField(
                            value = dnsOptions.firstOrNull { it.first == primaryDns }?.second ?: primaryDns,
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
                            dnsOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.second, color = ZtText, fontSize = 13.sp) },
                                    onClick = {
                                        onDnsChange(option.first)
                                        dnsExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // SECTION 2: UPDATES & SUPPORT
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
                subtitle = "v1.0.0 • Tap to check latest release",
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
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "ZeroTrace",
                        tint = ZtAccent,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "ZeroTrace VPN",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = ZtText
                    )
                    Text(
                        text = "Version 1.0.0 · Xray Core · NovaLink LK Edition",
                        fontSize = 12.sp,
                        color = ZtTextMuted
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { openUrl("https://nexauracore.com") }
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "Engineered by Nexaura Core",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ZtAccent
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = "Website",
                            tint = ZtAccent,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Lead by Nadun Gawesh",
                        fontSize = 11.5.sp,
                        color = ZtTextMuted
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "We keep no connection logs, no bandwidth logs, and no IP records.",
            fontSize = 11.5.sp,
            color = ZtTextFaint,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
        color = ZtTextFaint,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
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
            .padding(14.dp)
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ZtSurface)
            .border(1.dp, ZtBorder, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(iconTint.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = title,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
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
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
