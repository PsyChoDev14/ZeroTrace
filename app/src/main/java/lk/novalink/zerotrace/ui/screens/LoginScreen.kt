package lk.novalink.zerotrace.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import lk.novalink.zerotrace.R
import lk.novalink.zerotrace.core.UpdateManager
import lk.novalink.zerotrace.ui.components.ZeroTraceMark
import lk.novalink.zerotrace.ui.theme.ZtAccent
import lk.novalink.zerotrace.ui.theme.ZtBg
import lk.novalink.zerotrace.ui.theme.ZtDanger
import lk.novalink.zerotrace.ui.theme.ZtDangerSoft
import lk.novalink.zerotrace.ui.theme.ZtText
import lk.novalink.zerotrace.ui.theme.ZtTextMuted

private enum class LoginPhase(val heading: String, val sub: String, val hint: String) {
    IDLE(
        "Sign in to continue",
        "Link this device to your ZeroTrace account to sync configs and your license.",
        "We store no browsing data. Sign-in identifies your license — nothing else."
    ),
    OPENING(
        "Opening your browser",
        "Finish signing in with Google, then come back to this window.",
        "We store no browsing data. Sign-in identifies your license — nothing else."
    ),
    WAITING(
        "Waiting for your browser",
        "Approve the sign-in in the tab that just opened. This screen continues on its own.",
        "Nothing opened? Your default browser may be blocked by the active tunnel."
    ),
    ERROR(
        "Sign-in failed",
        "ZeroTrace could not complete the handshake with Google.",
        "Still failing? Message support on WhatsApp and include your diagnostics log."
    )
}

/**
 * Sign-in page: "Continue with Google" plus an offline escape hatch. Used as the launch gate
 * (onOffline != null) and embedded in the Account screen when signed out.
 */
@Composable
fun LoginScreen(
    loading: Boolean,
    error: String?,
    onLogin: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    onOffline: (() -> Unit)? = null,
    showFooter: Boolean = false
) {
    var opening by remember { mutableStateOf(false) }
    LaunchedEffect(opening) {
        if (opening) {
            delay(800)
            opening = false
        }
    }
    val phase = when {
        opening -> LoginPhase.OPENING
        loading -> LoginPhase.WAITING
        error != null -> LoginPhase.ERROR
        else -> LoginPhase.IDLE
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ZtBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            HeroOrb(spinning = phase == LoginPhase.OPENING)

            Spacer(Modifier.height(16.dp))
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold, color = ZtText)) { append("ZERO") }
                    withStyle(SpanStyle(fontWeight = FontWeight.Light, color = ZtTextMuted)) { append("TRACE") }
                },
                fontSize = 14.sp,
                letterSpacing = 3.4.sp
            )

            Spacer(Modifier.height(20.dp))
            Text(phase.heading, color = ZtText, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(
                phase.sub,
                color = ZtTextMuted,
                fontSize = 13.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 290.dp)
            )

            if (phase == LoginPhase.ERROR && error != null) {
                Spacer(Modifier.height(18.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(ZtDangerSoft)
                        .border(1.dp, ZtDanger.copy(alpha = 0.26f), RoundedCornerShape(18.dp))
                        .padding(horizontal = 13.dp, vertical = 11.dp),
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = ZtDanger, modifier = Modifier.size(16.dp))
                    Text(error, color = ZtDanger, fontSize = 12.sp, lineHeight = 18.sp, modifier = Modifier.weight(1f, fill = false))
                }
            }

            Spacer(Modifier.height(24.dp))

            GoogleButton(
                label = when (phase) {
                    LoginPhase.OPENING -> "Opening browser…"
                    LoginPhase.ERROR -> "Try again with Google"
                    else -> "Continue with Google"
                },
                busy = phase == LoginPhase.OPENING,
                onClick = {
                    if (!opening) {
                        opening = true
                        onLogin()
                    }
                }
            )

            if (phase == LoginPhase.WAITING) {
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Open the link again", color = ZtAccent, fontSize = 12.sp, fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable { opening = true; onLogin() }
                    )
                    Box(Modifier.width(1.dp).height(10.dp).background(Color(0x17FFFFFF)))
                    Text(
                        "Cancel", color = ZtTextMuted, fontSize = 12.sp, fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable { onCancel() }
                    )
                }
            }

            if (phase == LoginPhase.IDLE && onOffline != null) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0x0BFFFFFF))
                        .border(1.dp, Color(0x17FFFFFF), RoundedCornerShape(18.dp))
                        .clickable { onOffline() },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = ZtTextMuted, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Use this device offline", color = ZtTextMuted, fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                phase.hint,
                color = ZtTextMuted,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 280.dp)
            )
        }

        if (showFooter) {
            val context = LocalContext.current
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Support", color = ZtAccent, fontSize = 11.sp,
                    modifier = Modifier.clickable {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/94788385465")))
                        }
                    }
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Android, contentDescription = null, tint = ZtTextMuted, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("v${UpdateManager.getCurrentVersionName(context)}", color = ZtTextMuted, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun GoogleButton(label: String, busy: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0x26FFFFFF), Color(0x0FFFFFFF))))
            .border(1.dp, Brush.verticalGradient(listOf(Color(0x33FFFFFF), Color(0x14FFFFFF))), shape)
            .clickable(enabled = !busy, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (busy) {
                CircularProgressIndicator(color = ZtText, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
            } else {
                Image(painterResource(R.drawable.ic_google_g), contentDescription = null, modifier = Modifier.size(18.dp))
            }
            Text(label, color = ZtText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** Glowing shield orb; the ring spins while the browser is being opened. */
@Composable
private fun HeroOrb(spinning: Boolean) {
    val transition = rememberInfiniteTransition(label = "hero")
    val pulse by transition.animateFloat(
        initialValue = 0.96f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    val spin by transition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)),
        label = "spin"
    )

    Box(
        modifier = Modifier
            .size(104.dp)
            .drawBehind {
                val center = Offset(size.width / 2, size.height / 2)
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color(0x6600E5FF), Color(0x335468FF), Color.Transparent),
                        center = center, radius = size.minDimension * 0.75f * pulse
                    ),
                    radius = size.minDimension * 0.75f * pulse, center = center
                )
                if (spinning) {
                    val stroke = 1.5.dp.toPx()
                    val inset = stroke / 2
                    val arcSize = Size(size.width - stroke, size.height - stroke)
                    drawCircle(Color(0x295468FF), radius = size.minDimension / 2 - inset, style = Stroke(stroke))
                    rotate(spin, pivot = center) {
                        drawArc(
                            color = Color(0xFF00E5FF), startAngle = -90f, sweepAngle = 90f, useCenter = false,
                            topLeft = Offset(inset, inset), size = arcSize,
                            style = Stroke(stroke, cap = StrokeCap.Round)
                        )
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(78.dp)
                .clip(CircleShape)
                .background(Brush.verticalGradient(listOf(Color(0x21FFFFFF), Color(0x05FFFFFF))))
                .border(1.dp, Color(0x26FFFFFF), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            ZeroTraceMark(size = 52.dp, tint = Color(0xFF36C8FF))
        }
    }
}
