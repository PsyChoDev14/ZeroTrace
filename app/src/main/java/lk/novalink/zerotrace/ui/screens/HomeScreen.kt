package lk.novalink.zerotrace.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import kotlinx.coroutines.delay
import lk.novalink.zerotrace.core.VpnState
import lk.novalink.zerotrace.data.model.ProxyConfig
import lk.novalink.zerotrace.ui.components.ConnectionDial
import lk.novalink.zerotrace.ui.components.PingBadge
import lk.novalink.zerotrace.ui.components.ProtocolBadge
import lk.novalink.zerotrace.ui.components.ZeroTraceWordmark
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
 * Native Jetpack Compose implementation of clean, decluttered Home Screen
 */
@Composable
fun HomeScreen(
    vpnState: VpnState,
    selectedConfig: ProxyConfig?,
    downloadSpeed: Long,
    uploadSpeed: Long,
    onConnectToggle: () -> Unit,
    onNavigateToConfigs: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onAddConfigClick: () -> Unit,
    onEditActiveConfig: (() -> Unit)? = null,
    onPingTest: (ProxyConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    var uptimeSeconds by remember { mutableLongStateOf(0L) }
    LaunchedEffect(vpnState) {
        if (vpnState is VpnState.Connected) {
            uptimeSeconds = 0L
            while (true) {
                delay(1000L)
                uptimeSeconds++
            }
        } else {
            uptimeSeconds = 0L
        }
    }

    val isConnected = vpnState is VpnState.Connected
    val isConnecting = vpnState is VpnState.Connecting || vpnState is VpnState.Stopping
    val isError = vpnState is VpnState.Error

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ZtBg)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Bar: ZeroTrace Wordmark & Action Icons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ZeroTraceWordmark(showIcon = true)

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IconButton(
                    onClick = onAddConfigClick,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(ZtSurface)
                        .border(1.dp, ZtBorder, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Config",
                        tint = ZtAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onNavigateToSettings,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(ZtSurface)
                        .border(1.dp, ZtBorder, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = ZtTextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Minimal Status Pill
        StatusPill(state = vpnState)

        Spacer(modifier = Modifier.height(24.dp))

        // Central Connection Dial
        ConnectionDial(
            state = vpnState,
            hasConfig = selectedConfig != null,
            onClick = onConnectToggle
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Center Status Subtitle Area with Progressive Disclosure
        AnimatedContent(
            targetState = vpnState,
            transitionSpec = {
                (fadeIn(tween(220)) + slideInVertically(tween(220)) { 6 })
                    .togetherWith(fadeOut(tween(180)) + slideOutVertically(tween(180)) { -6 })
            },
            label = "statusSubtitle"
        ) { state ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.height(58.dp)
            ) {
                when {
                    state is VpnState.Connected -> {
                        Text(
                            text = formatDuration(uptimeSeconds),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = (-0.5).sp,
                            color = ZtText
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = ZtSuccess,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Traffic Encrypted • ",
                                fontSize = 12.sp,
                                color = ZtTextMuted
                            )
                            Text(
                                text = selectedConfig?.server ?: "Encrypted",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ZtSuccess
                            )
                        }
                    }
                    state is VpnState.Connecting -> {
                        Text(
                            text = "Securing Tunnel…",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ZtAccent
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Handshaking with ${selectedConfig?.name ?: "server"}",
                            fontSize = 12.sp,
                            color = ZtTextMuted
                        )
                    }
                    state is VpnState.Error -> {
                        Text(
                            text = "Connection Failed",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ZtDanger
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Tap dial to retry or pick another node",
                            fontSize = 12.sp,
                            color = ZtTextMuted
                        )
                    }
                    else -> {
                        Text(
                            text = if (selectedConfig != null) "Ready to Connect" else "No Server Configured",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ZtText
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (selectedConfig != null) "Tap the power button to protect traffic" else "Tap below to add an Xray config",
                            fontSize = 12.sp,
                            color = ZtTextMuted
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Selected Server Card (Minimal, Clean Pill)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            if (selectedConfig != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(ZtSurface)
                        .border(1.dp, ZtBorder, RoundedCornerShape(16.dp))
                        .clickable { onNavigateToConfigs() }
                        .padding(horizontal = 16.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(ZtSurface2),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = ZtAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ProtocolBadge(protocol = selectedConfig.protocol)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = selectedConfig.name,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = ZtText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = selectedConfig.displaySubtitle,
                            fontSize = 11.sp,
                            color = ZtTextFaint,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PingBadge(
                            pingMs = selectedConfig.pingMs,
                            onPingClick = { onPingTest(selectedConfig) }
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = null,
                            tint = ZtTextFaint,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            } else {
                // Empty server placeholder
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(ZtSurface)
                        .border(1.dp, ZtBorder, RoundedCornerShape(16.dp))
                        .clickable { onAddConfigClick() }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(ZtAccentSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = ZtAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Add First Server",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = ZtText
                        )
                        Text(
                            text = "Tap to paste an Xray/VLESS link",
                            fontSize = 11.5.sp,
                            color = ZtAccent
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        tint = ZtTextFaint,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            // Real-Time Speed Bar: ONLY shown when connected to eliminate clutter!
            AnimatedVisibility(
                visible = isConnected,
                enter = fadeIn(tween(250)) + expandVertically(tween(250)),
                exit = fadeOut(tween(200)) + shrinkVertically(tween(200))
            ) {
                Column {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(ZtSurface)
                            .border(1.dp, ZtBorder, RoundedCornerShape(14.dp))
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Download
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = null,
                                tint = ZtSuccess,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = formatSpeed(downloadSpeed),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ZtText
                            )
                        }

                        Box(
                            modifier = Modifier
                                .height(14.dp)
                                .width(1.dp)
                                .background(ZtBorder)
                        )

                        // Upload
                        Row(
                            modifier = Modifier.weight(1f).padding(start = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = null,
                                tint = ZtAccent,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = formatSpeed(uploadSpeed),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ZtText
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(90.dp))
    }
}

@Composable
private fun StatusPill(state: VpnState) {
    val isConnected = state is VpnState.Connected
    val isConnecting = state is VpnState.Connecting || state is VpnState.Stopping
    val isError = state is VpnState.Error

    val (label, dotColor) = when {
        isConnected -> Pair("PROTECTED", ZtSuccess)
        isConnecting -> Pair("CONNECTING", ZtAccent)
        isError -> Pair("FAILED", ZtDanger)
        else -> Pair("NOT PROTECTED", ZtTextFaint)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "dotPulse")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isConnected || isConnecting) 0.35f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotAlpha"
    )

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(ZtSurface)
            .border(1.dp, ZtBorder, RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .scale(if (isConnected || isConnecting) dotAlpha else 1f)
                .clip(CircleShape)
                .background(dotColor)
        )

        Spacer(modifier = Modifier.width(7.dp))

        Text(
            text = label,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = if (isConnected) ZtSuccess else if (isConnecting) ZtAccent else if (isError) ZtDanger else ZtTextMuted
        )
    }
}

private fun formatSpeed(bytes: Long): String = when {
    bytes >= 1024 * 1024 -> "%.1f MB/s".format(bytes / (1024.0 * 1024.0))
    bytes >= 1024 -> "%.1f KB/s".format(bytes / 1024.0)
    else -> "$bytes B/s"
}

private fun formatDuration(seconds: Long): String {
    val hrs = seconds / 3600
    val mins = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hrs > 0) {
        String.format("%02d:%02d:%02d", hrs, mins, secs)
    } else {
        String.format("%02d:%02d", mins, secs)
    }
}
