package lk.novalink.zerotrace.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import lk.novalink.zerotrace.core.VpnState
import lk.novalink.zerotrace.ui.theme.ZtAccent
import lk.novalink.zerotrace.ui.theme.ZtAccentRing
import lk.novalink.zerotrace.ui.theme.ZtBorder
import lk.novalink.zerotrace.ui.theme.ZtBorderStrong
import lk.novalink.zerotrace.ui.theme.ZtDanger
import lk.novalink.zerotrace.ui.theme.ZtSuccess
import lk.novalink.zerotrace.ui.theme.ZtSurface
import lk.novalink.zerotrace.ui.theme.ZtText
import lk.novalink.zerotrace.ui.theme.ZtTextFaint
import lk.novalink.zerotrace.ui.theme.ZtTextMuted
import lk.novalink.zerotrace.ui.theme.ZtTrack
import lk.novalink.zerotrace.ui.theme.ZtWarn

/**
 * Native Jetpack Compose implementation of the React ConnectionDial.tsx
 */
@Composable
fun ConnectionDial(
    state: VpnState,
    hasConfig: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isConnected = state is VpnState.Connected
    val isConnecting = state is VpnState.Connecting || state is VpnState.Stopping
    val isError = state is VpnState.Error

    // Ambient breathing halo while connected
    val infiniteTransition = rememberInfiniteTransition(label = "dialBreath")
    val haloScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = if (isConnected) 1.05f else 0.94f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "haloScale"
    )
    val haloAlpha by infiniteTransition.animateFloat(
        initialValue = 0.12f,
        targetValue = if (isConnected) 0.38f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "haloAlpha"
    )

    // Rotating spinner angle when connecting
    val spinnerAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spinnerAngle"
    )

    // Outer ring stroke progress
    val strokeProgress by animateFloatAsState(
        targetValue = if (isConnected) 1f else 0f,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "strokeProgress"
    )

    // Dynamic colors
    val ringColor by animateColorAsState(
        targetValue = when {
            isConnected -> ZtAccent
            isConnecting -> ZtAccent
            isError -> ZtDanger
            else -> ZtBorder
        },
        animationSpec = tween(300),
        label = "ringColor"
    )

    val buttonBgColor by animateColorAsState(
        targetValue = when {
            isConnected -> ZtAccent
            else -> ZtSurface
        },
        animationSpec = tween(300),
        label = "buttonBgColor"
    )

    val buttonBorderColor by animateColorAsState(
        targetValue = when {
            isConnected -> ZtAccent
            isConnecting -> ZtAccent
            isError -> ZtDanger
            else -> ZtBorderStrong
        },
        animationSpec = tween(300),
        label = "buttonBorderColor"
    )

    val contentColor by animateColorAsState(
        targetValue = when {
            isConnected -> Color.White
            isConnecting -> ZtAccent
            isError -> ZtDanger
            !hasConfig -> ZtAccent
            else -> ZtText
        },
        animationSpec = tween(200),
        label = "contentColor"
    )

    Box(
        modifier = modifier.size(268.dp),
        contentAlignment = Alignment.Center
    ) {
        // Ambient soft blur halo
        if (isConnected) {
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .scale(haloScale)
                    .clip(CircleShape)
                    .background(ZtAccentRing.copy(alpha = haloAlpha))
            )
        }

        // Circular Dial Canvas Track & Arcs
        Canvas(modifier = Modifier.size(268.dp)) {
            val strokeW = 2.dp.toPx()
            val radius = 118.dp.toPx()
            val centerOffset = Offset(size.width / 2f, size.height / 2f)
            val arcTopLeft = Offset(centerOffset.x - radius, centerOffset.y - radius)
            val arcSize = Size(radius * 2f, radius * 2f)

            // Static background track
            drawCircle(
                color = ZtTrack,
                radius = radius,
                center = centerOffset,
                style = Stroke(width = strokeW)
            )

            // Connected progress arc
            if (strokeProgress > 0f) {
                drawArc(
                    color = ringColor,
                    startAngle = -90f,
                    sweepAngle = 360f * strokeProgress,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = strokeW, cap = StrokeCap.Round)
                )
            }

            // Connecting rotating spinner arc
            if (isConnecting) {
                drawArc(
                    color = ZtAccent,
                    startAngle = spinnerAngle - 90f,
                    sweepAngle = 80f,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = strokeW, cap = StrokeCap.Round)
                )
            }
        }

        // Inner static depth border ring (212dp)
        Box(
            modifier = Modifier
                .size(212.dp)
                .clip(CircleShape)
                .border(1.dp, ZtBorder, CircleShape)
        )

        // Core Interactive Action Button (172dp)
        Box(
            modifier = Modifier
                .size(172.dp)
                .clip(CircleShape)
                .background(buttonBgColor)
                .border(1.5.dp, buttonBorderColor, CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = rememberRipple(bounded = false, color = ZtAccent),
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = when {
                        !hasConfig -> Icons.Default.Add
                        isError -> Icons.Default.Refresh
                        isConnected -> Icons.Default.Shield
                        else -> Icons.Default.PowerSettingsNew
                    },
                    contentDescription = "Connection Toggle",
                    tint = contentColor,
                    modifier = Modifier.size(28.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = when {
                        !hasConfig -> "ADD CONFIG"
                        isConnected -> "CONNECTED"
                        isConnecting -> "CONNECTING"
                        isError -> "RETRY"
                        else -> "CONNECT"
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 2.sp,
                    color = contentColor
                )
            }
        }
    }
}
