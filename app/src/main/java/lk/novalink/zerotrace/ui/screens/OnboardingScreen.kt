package lk.novalink.zerotrace.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import lk.novalink.zerotrace.ui.components.ZeroTraceWordmark
import lk.novalink.zerotrace.ui.theme.ZtAccent
import lk.novalink.zerotrace.ui.theme.ZtAccentSoft
import lk.novalink.zerotrace.ui.theme.ZtBg
import lk.novalink.zerotrace.ui.theme.ZtBorder
import lk.novalink.zerotrace.ui.theme.ZtBorderStrong
import lk.novalink.zerotrace.ui.theme.ZtText
import lk.novalink.zerotrace.ui.theme.ZtTextFaint
import lk.novalink.zerotrace.ui.theme.ZtTextMuted

private data class OnboardingSlide(
    val title: String,
    val body: String,
    val visualType: Int // 0 = Shield, 1 = Tap, 2 = Globe
)

/**
 * Native Jetpack Compose implementation of Onboarding.tsx from React design system
 */
@Composable
fun OnboardingScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val slides = listOf(
        OnboardingSlide(
            title = "Privacy without complexity.",
            body = "No dashboards to learn, no complex settings to tune. ZeroTrace hides and encrypts your traffic seamlessly.",
            visualType = 0
        ),
        OnboardingSlide(
            title = "Connect in seconds.",
            body = "One central control, one tap. Select your favourite node or bug host and enjoy unrestricted high-speed internet.",
            visualType = 1
        ),
        OnboardingSlide(
            title = "High-speed Xray tunnel.",
            body = "Engineered with VLESS, VMess & Reality transport — built for extreme speed and bypassing censorship.",
            visualType = 2
        )
    )

    var currentIndex by remember { mutableIntStateOf(0) }
    val currentSlide = slides[currentIndex]
    val isLast = currentIndex == slides.size - 1

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ZtBg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top Header: Wordmark & Skip Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ZeroTraceWordmark(showIcon = true)

            Text(
                text = "Skip",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = ZtTextFaint,
                modifier = Modifier
                    .clickable { onDone() }
                    .padding(8.dp)
            )
        }

        Spacer(modifier = Modifier.weight(0.5f))

        // Visual Graphic Area (Matches ShieldVisual, TapVisual, GlobeVisual)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = currentSlide.visualType,
                transitionSpec = { fadeIn(tween(280)) togetherWith fadeOut(tween(200)) },
                label = "onboardingVisual"
            ) { type ->
                when (type) {
                    0 -> ShieldVisualGraphic()
                    1 -> TapVisualGraphic()
                    else -> GlobeVisualGraphic()
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Title & Description
        AnimatedContent(
            targetState = currentSlide,
            transitionSpec = { fadeIn(tween(260)) togetherWith fadeOut(tween(180)) },
            label = "onboardingText",
            modifier = Modifier.padding(horizontal = 32.dp)
        ) { slide ->
            Column {
                Text(
                    text = slide.title,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = ZtText,
                    lineHeight = 34.sp,
                    letterSpacing = (-0.5).sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = slide.body,
                    fontSize = 14.sp,
                    color = ZtTextMuted,
                    lineHeight = 21.sp
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Bottom Controls: Progress Pills & Continue Button
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 24.dp)
        ) {
            // Animated indicator pills (26dp active vs 6dp inactive)
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 20.dp)
            ) {
                slides.forEachIndexed { index, _ ->
                    val isSelected = index == currentIndex
                    val pillWidth by animateDpAsState(
                        targetValue = if (isSelected) 26.dp else 6.dp,
                        animationSpec = tween(200),
                        label = "pillWidth"
                    )
                    val pillColor by animateColorAsState(
                        targetValue = if (isSelected) ZtAccent else ZtBorderStrong,
                        animationSpec = tween(200),
                        label = "pillColor"
                    )

                    Box(
                        modifier = Modifier
                            .height(4.dp)
                            .width(pillWidth)
                            .clip(CircleShape)
                            .background(pillColor)
                            .clickable { currentIndex = index }
                    )
                }
            }

            // Action Button
            Button(
                onClick = {
                    if (isLast) onDone() else currentIndex++
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ZtAccent,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = if (isLast) "GET STARTED" else "Continue",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = if (isLast) 1.2.sp else 0.sp
                )
            }
        }
    }
}

@Composable
private fun ShieldVisualGraphic() {
    Canvas(modifier = Modifier.size(200.dp)) {
        val w = size.width
        val h = size.height

        // Outer concentric rings
        drawCircle(color = ZtBorder, radius = 96.dp.toPx() * 0.8f, style = Stroke(1.dp.toPx()))
        drawCircle(color = ZtBorder, radius = 66.dp.toPx() * 0.8f, style = Stroke(1.dp.toPx()))

        // Shield Path
        val shield = Path().apply {
            moveTo(w * 0.5f, h * 0.22f)
            lineTo(w * 0.72f, h * 0.31f)
            lineTo(w * 0.72f, h * 0.49f)
            cubicTo(w * 0.72f, h * 0.62f, w * 0.61f, h * 0.74f, w * 0.5f, h * 0.77f)
            cubicTo(w * 0.39f, h * 0.74f, w * 0.28f, h * 0.62f, w * 0.28f, h * 0.49f)
            lineTo(w * 0.28f, h * 0.31f)
            close()
        }

        drawPath(path = shield, color = ZtAccentSoft)
        drawPath(path = shield, color = ZtAccent, style = Stroke(2.dp.toPx(), join = StrokeJoin.Round))

        // Checkmark inside shield
        val check = Path().apply {
            moveTo(w * 0.43f, h * 0.51f)
            lineTo(w * 0.48f, h * 0.56f)
            lineTo(w * 0.58f, h * 0.45f)
        }
        drawPath(path = check, color = ZtAccent, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
    }
}

@Composable
private fun TapVisualGraphic() {
    val infiniteTransition = rememberInfiniteTransition(label = "tapRotate")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "tapAngle"
    )

    Canvas(modifier = Modifier.size(200.dp)) {
        val w = size.width
        val h = size.height
        val center = Offset(w / 2f, h / 2f)

        drawCircle(color = ZtBorder, radius = 80.dp.toPx(), center = center, style = Stroke(1.dp.toPx()))

        // Rotating dashed accent circle
        drawArc(
            color = ZtAccent,
            startAngle = angle - 90f,
            sweepAngle = 70f,
            useCenter = false,
            topLeft = Offset(center.x - 60.dp.toPx(), center.y - 60.dp.toPx()),
            size = Size(120.dp.toPx(), 120.dp.toPx()),
            style = Stroke(2.dp.toPx(), cap = StrokeCap.Round)
        )

        // Core Power Button circle
        drawCircle(color = ZtAccent, radius = 38.dp.toPx(), center = center)

        // Power symbol line
        drawLine(
            color = Color.White,
            start = Offset(center.x, center.y - 18.dp.toPx()),
            end = Offset(center.x, center.y - 4.dp.toPx()),
            strokeWidth = 2.5.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Power symbol arc
        drawArc(
            color = Color.White,
            startAngle = -45f,
            sweepAngle = 270f,
            useCenter = false,
            topLeft = Offset(center.x - 14.dp.toPx(), center.y - 14.dp.toPx()),
            size = Size(28.dp.toPx(), 28.dp.toPx()),
            style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun GlobeVisualGraphic() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulseDots")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotAlpha"
    )

    Canvas(modifier = Modifier.size(200.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)

        // Concentric globe outline
        drawCircle(color = ZtBorder, radius = 80.dp.toPx(), center = center, style = Stroke(1.dp.toPx()))
        drawCircle(color = ZtBorderStrong, radius = 58.dp.toPx(), center = center, style = Stroke(1.dp.toPx()))
        drawOval(
            color = ZtBorderStrong,
            topLeft = Offset(center.x - 25.dp.toPx(), center.y - 58.dp.toPx()),
            size = Size(50.dp.toPx(), 116.dp.toPx()),
            style = Stroke(1.dp.toPx())
        )

        // Latitude lines
        drawLine(
            color = ZtBorder,
            start = Offset(center.x - 58.dp.toPx(), center.y),
            end = Offset(center.x + 58.dp.toPx(), center.y),
            strokeWidth = 1.dp.toPx()
        )

        // Node points pulsing
        val nodePoints = listOf(
            Offset(center.x - 30.dp.toPx(), center.y - 20.dp.toPx()),
            Offset(center.x + 25.dp.toPx(), center.y - 15.dp.toPx()),
            Offset(center.x, center.y + 25.dp.toPx()),
            Offset(center.x - 18.dp.toPx(), center.y + 15.dp.toPx()),
            Offset(center.x + 35.dp.toPx(), center.y + 18.dp.toPx())
        )

        nodePoints.forEach { pt ->
            drawCircle(color = ZtAccent.copy(alpha = dotAlpha), radius = 4.dp.toPx(), center = pt)
        }
    }
}
