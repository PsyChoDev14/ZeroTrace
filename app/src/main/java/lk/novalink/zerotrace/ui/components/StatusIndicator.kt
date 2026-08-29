package lk.novalink.zerotrace.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import lk.novalink.zerotrace.core.VpnState
import lk.novalink.zerotrace.ui.theme.SapphireLight
import lk.novalink.zerotrace.ui.theme.StatusConnected
import lk.novalink.zerotrace.ui.theme.StatusRed
import lk.novalink.zerotrace.ui.theme.StatusYellow
import lk.novalink.zerotrace.ui.theme.SurfaceElevated
import lk.novalink.zerotrace.ui.theme.TextMuted

@Composable
fun StatusIndicator(
    state: VpnState,
    modifier: Modifier = Modifier
) {
    val isConnecting = state is VpnState.Connecting || state is VpnState.Stopping

    // Pulsing dot only when connecting
    val infiniteTransition = rememberInfiniteTransition(label = "statusPulse")
    val dotScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue  = if (isConnecting) 1.6f else 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotScale"
    )

    val dotColor by animateColorAsState(
        targetValue = when (state) {
            is VpnState.Connected    -> StatusConnected
            is VpnState.Connecting   -> SapphireLight
            is VpnState.Stopping     -> StatusYellow
            is VpnState.Error        -> StatusRed
            is VpnState.Disconnected -> TextMuted.copy(alpha = 0.5f)
        },
        animationSpec = tween(500),
        label = "dotColor"
    )

    val label = when (state) {
        is VpnState.Connected    -> "Connected"
        is VpnState.Connecting   -> "Connecting"
        is VpnState.Stopping     -> "Stopping"
        is VpnState.Error        -> "Error"
        is VpnState.Disconnected -> "Not Connected"
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceElevated)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        // Pulsing status dot
        Box(contentAlignment = Alignment.Center) {
            // Glow behind dot
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .scale(dotScale)
                    .clip(CircleShape)
                    .background(dotColor.copy(alpha = 0.25f))
            )
            // Solid dot
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text  = label,
            color = dotColor,
            fontWeight = FontWeight.SemiBold,
            fontSize   = 13.sp
        )
    }
}
