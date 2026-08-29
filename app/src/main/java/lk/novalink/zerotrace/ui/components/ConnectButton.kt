package lk.novalink.zerotrace.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import lk.novalink.zerotrace.core.VpnState
import lk.novalink.zerotrace.ui.theme.AccentBlue
import lk.novalink.zerotrace.ui.theme.BorderSubtle
import lk.novalink.zerotrace.ui.theme.SapphireCore
import lk.novalink.zerotrace.ui.theme.SapphireDim
import lk.novalink.zerotrace.ui.theme.SapphireGlow
import lk.novalink.zerotrace.ui.theme.SapphireLight
import lk.novalink.zerotrace.ui.theme.SapphireRing
import lk.novalink.zerotrace.ui.theme.StatusConnected
import lk.novalink.zerotrace.ui.theme.SurfaceElevated
import lk.novalink.zerotrace.ui.theme.TextMuted
import lk.novalink.zerotrace.ui.theme.TextWhite

import androidx.compose.material.icons.filled.Add

@Composable
fun ConnectButton(
    state: VpnState,
    hasConfig: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isConnected  = state is VpnState.Connected
    val isConnecting = state is VpnState.Connecting || state is VpnState.Stopping

    // Only pulse when actively connecting — saves battery when idle/connected
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue  = if (isConnecting) 1.12f else if (isConnected) 1.06f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation  = tween(
                durationMillis = if (isConnecting) 900 else 2000,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // Smooth color transitions
    val ringColor by animateColorAsState(
        targetValue = when {
            isConnected  -> StatusConnected.copy(alpha = 0.18f)
            isConnecting -> SapphireLight.copy(alpha = 0.22f)
            else         -> Color.Transparent
        },
        animationSpec = tween(600),
        label = "ringColor"
    )

    val coreGradient = when {
        isConnected  -> Brush.radialGradient(listOf(StatusConnected, Color(0xFF16A34A)))
        isConnecting -> Brush.radialGradient(listOf(SapphireLight, SapphireDim))
        else         -> Brush.radialGradient(listOf(SurfaceElevated, Color(0xFF0A1221)))
    }

    val borderColor by animateColorAsState(
        targetValue = when {
            isConnected  -> StatusConnected.copy(alpha = 0.7f)
            isConnecting -> SapphireLight.copy(alpha = 0.8f)
            else         -> BorderSubtle
        },
        animationSpec = tween(500),
        label = "borderColor"
    )

    val iconTint by animateColorAsState(
        targetValue = when {
            isConnected || isConnecting -> Color.White
            else                        -> AccentBlue
        },
        animationSpec = tween(400),
        label = "iconTint"
    )

    Box(
        modifier = modifier.size(210.dp),
        contentAlignment = Alignment.Center
    ) {
        // Soft ambient glow ring (only visible when active)
        Box(
            modifier = Modifier
                .size(210.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(ringColor)
        )

        // Thin accent border ring
        Box(
            modifier = Modifier
                .size(176.dp)
                .clip(CircleShape)
                .border(1.dp, borderColor, CircleShape)
                .background(Color(0xFF090E1A))
        )

        // Core button
        Box(
            modifier = Modifier
                .size(144.dp)
                .clip(CircleShape)
                .background(coreGradient)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = rememberRipple(bounded = false, color = SapphireLight),
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = if (!hasConfig) Icons.Default.Add else Icons.Default.PowerSettingsNew,
                    contentDescription = "Toggle VPN",
                    tint = iconTint,
                    modifier = Modifier.size(if (!hasConfig) 44.dp else 48.dp)
                )
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = when {
                        !hasConfig   -> "ADD CONFIG"
                        isConnected  -> "DISCONNECT"
                        isConnecting -> "CONNECTING"
                        else         -> "CONNECT"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize   = 10.sp,
                    letterSpacing = 1.2.sp,
                    color = when {
                        isConnected || isConnecting -> Color.White.copy(alpha = 0.9f)
                        !hasConfig                  -> SapphireLight
                        else                        -> TextMuted
                    }
                )
            }
        }
    }
}
