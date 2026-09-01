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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import lk.novalink.zerotrace.core.VpnState
import lk.novalink.zerotrace.ui.theme.ZtAccent
import lk.novalink.zerotrace.ui.theme.ZtAccentRing
import lk.novalink.zerotrace.ui.theme.ZtAccentSoft
import lk.novalink.zerotrace.ui.theme.ZtBgElevated
import lk.novalink.zerotrace.ui.theme.ZtBorder
import lk.novalink.zerotrace.ui.theme.ZtBorderStrong
import lk.novalink.zerotrace.ui.theme.ZtDanger
import lk.novalink.zerotrace.ui.theme.ZtSuccess
import lk.novalink.zerotrace.ui.theme.ZtSuccessSoft
import lk.novalink.zerotrace.ui.theme.ZtSurface
import lk.novalink.zerotrace.ui.theme.ZtSurface2
import lk.novalink.zerotrace.ui.theme.ZtText
import lk.novalink.zerotrace.ui.theme.ZtTextFaint
import lk.novalink.zerotrace.ui.theme.ZtTextMuted
import lk.novalink.zerotrace.ui.theme.ZtTrack
import lk.novalink.zerotrace.ui.theme.ZtWarn

import androidx.compose.ui.graphics.drawscope.rotate

/**
 * Modern luxury cyber/Apple-style VPN Connection Dial with obsidian glass core,
 * ambient glowing breathing halo, and high-precision telemetry ring.
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

    // Ambient breathing aura transition
    val infiniteTransition = rememberInfiniteTransition(label = "dialAnimations")
    val haloScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = if (isConnected) 1.06f else if (isConnecting) 1.04f else 0.98f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isConnecting) 900 else 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "haloScale"
    )
    val haloAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = if (isConnected) 0.38f else if (isConnecting) 0.32f else 0.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isConnecting) 900 else 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "haloAlpha"
    )

    // Ultra-smooth rotating laser comet angle (continuous 360 deg)
    val spinnerAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spinnerAngle"
    )

    // Ambient gentle orbital slow rotation for connected state (subtle 8s continuous loop)
    val ambientSlowAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ambientSlowAngle"
    )

    // Outer progress transition
    val strokeProgress by animateFloatAsState(
        targetValue = if (isConnected) 1f else 0f,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "strokeProgress"
    )

    // Dynamic state colors
    val primaryColor by animateColorAsState(
        targetValue = when {
            isConnected -> ZtSuccess
            isConnecting -> ZtAccent
            isError -> ZtDanger
            !hasConfig -> ZtAccent
            else -> ZtAccent
        },
        animationSpec = tween(300),
        label = "primaryColor"
    )

    val coreBorderColor by animateColorAsState(
        targetValue = when {
            isConnected -> ZtSuccess.copy(alpha = 0.85f)
            isConnecting -> ZtAccent.copy(alpha = 0.85f)
            isError -> ZtDanger.copy(alpha = 0.85f)
            else -> ZtBorderStrong
        },
        animationSpec = tween(300),
        label = "coreBorderColor"
    )

    // Luxury dark glass core gradient (Obsidian dark glass with state tint)
    val coreGradient = when {
        isConnected -> Brush.radialGradient(
            listOf(
                Color(0xFF0C241B), // Subtle emerald glow center
                Color(0xFF07150F),
                Color(0xFF040A07)
            )
        )
        isConnecting -> Brush.radialGradient(
            listOf(
                Color(0xFF121B35), // Sapphire glow center
                Color(0xFF0A0F20),
                Color(0xFF050810)
            )
        )
        isError -> Brush.radialGradient(
            listOf(
                Color(0xFF260D0B),
                Color(0xFF140706),
                Color(0xFF0A0303)
            )
        )
        else -> Brush.radialGradient(
            listOf(
                Color(0xFF161A24),
                Color(0xFF0E1118),
                Color(0xFF08090D)
            )
        )
    }

    Box(
        modifier = modifier.size(260.dp),
        contentAlignment = Alignment.Center
    ) {
        // 1. Ambient Soft Breathing Glow Halo
        Box(
            modifier = Modifier
                .size(240.dp)
                .scale(haloScale)
                .clip(CircleShape)
                .background(
                    when {
                        isConnected -> ZtSuccessSoft.copy(alpha = haloAlpha)
                        isConnecting -> ZtAccentRing.copy(alpha = haloAlpha)
                        else -> Color.Transparent
                    }
                )
        )

        // 2. High-Tech Precision Outer Arc Track Canvas
        Canvas(modifier = Modifier.size(260.dp)) {
            val strokeW = 3.dp.toPx()
            val radius = 114.dp.toPx()
            val centerOffset = Offset(size.width / 2f, size.height / 2f)
            val arcTopLeft = Offset(centerOffset.x - radius, centerOffset.y - radius)
            val arcSize = Size(radius * 2f, radius * 2f)

            // Static background circular track
            drawCircle(
                color = ZtTrack,
                radius = radius,
                center = centerOffset,
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Connected complete neon ring with subtle ambient continuous drift
            if (strokeProgress > 0f) {
                rotate(degrees = ambientSlowAngle, pivot = centerOffset) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                ZtSuccess.copy(alpha = 0.35f),
                                ZtSuccess,
                                Color(0xFF38BDF8),
                                ZtSuccess,
                                ZtSuccess.copy(alpha = 0.35f)
                            ),
                            center = centerOffset
                        ),
                        startAngle = 0f,
                        sweepAngle = 360f * strokeProgress,
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = arcSize,
                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                    )
                }
            }

            // Connecting rotating smooth laser comet with glowing head
            if (isConnecting) {
                rotate(degrees = spinnerAngle, pivot = centerOffset) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                Color.Transparent,
                                ZtAccent.copy(alpha = 0.05f),
                                ZtAccent.copy(alpha = 0.4f),
                                ZtAccent,
                                Color(0xFF93C5FD)
                            ),
                            center = centerOffset
                        ),
                        startAngle = 0f,
                        sweepAngle = 100f,
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = arcSize,
                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                    )

                    // Glowing orbital particle at leading edge of comet (angle = 100 deg)
                    val rad = Math.toRadians(100.0)
                    val headX = centerOffset.x + radius * Math.cos(rad).toFloat()
                    val headY = centerOffset.y + radius * Math.sin(rad).toFloat()

                    // Ambient glow around head
                    drawCircle(
                        color = Color(0xFF93C5FD).copy(alpha = 0.45f),
                        radius = 6.dp.toPx(),
                        center = Offset(headX, headY)
                    )
                    // Core bright head dot
                    drawCircle(
                        color = Color.White,
                        radius = 2.5.dp.toPx(),
                        center = Offset(headX, headY)
                    )
                }
            }
        }

        // 3. Subtle Inner Concentric Ring
        Box(
            modifier = Modifier
                .size(208.dp)
                .clip(CircleShape)
                .border(1.dp, ZtBorder.copy(alpha = 0.6f), CircleShape)
        )

        // 4. Obsidian Dark Glass Core Interactive Button
        Box(
            modifier = Modifier
                .size(174.dp)
                .clip(CircleShape)
                .background(coreGradient)
                .border(1.5.dp, coreBorderColor, CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = rememberRipple(bounded = false, color = primaryColor),
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Main State Icon with Ambient Glow
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(52.dp)
                ) {
                    if (isConnected) {
                        // Soft glow behind shield
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(ZtSuccess.copy(alpha = 0.18f))
                        )
                    }

                    Icon(
                        imageVector = when {
                            !hasConfig -> Icons.Default.Add
                            isError -> Icons.Default.Refresh
                            isConnected -> Icons.Default.Shield
                            isConnecting -> Icons.Default.Lock
                            else -> Icons.Default.PowerSettingsNew
                        },
                        contentDescription = "Connection Toggle",
                        tint = when {
                            isConnected -> ZtSuccess
                            isConnecting -> ZtAccent
                            isError -> ZtDanger
                            !hasConfig -> ZtAccent
                            else -> ZtText
                        },
                        modifier = Modifier.size(34.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Primary Status Title
                Text(
                    text = when {
                        !hasConfig -> "ADD CONFIG"
                        isConnected -> "PROTECTED"
                        isConnecting -> "SECURING"
                        isError -> "RETRY"
                        else -> "CONNECT"
                    },
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    color = when {
                        isConnected -> ZtSuccess
                        isConnecting -> ZtAccent
                        isError -> ZtDanger
                        else -> ZtText
                    }
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Action Hint / Secondary text
                Text(
                    text = when {
                        isConnected -> "Tap to Disconnect"
                        isConnecting -> "Please wait…"
                        isError -> "Check Network"
                        !hasConfig -> "Import Server"
                        else -> "Tap to Start"
                    },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Normal,
                    color = ZtTextFaint
                )
            }
        }
    }
}
