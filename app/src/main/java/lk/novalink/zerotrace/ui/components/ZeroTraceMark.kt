package lk.novalink.zerotrace.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import lk.novalink.zerotrace.ui.theme.ZtAccent
import lk.novalink.zerotrace.ui.theme.ZtText

/**
 * ZERO TRACE mark: a shield-negative "zero" with a severed trace line.
 * Matches ZeroTraceMark.tsx exactly.
 */
@Composable
fun ZeroTraceMark(
    size: Dp = 24.dp,
    tint: Color = ZtAccent,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val strokeWidth = 2.dp.toPx()

        // Shield outer path
        val shieldPath = Path().apply {
            moveTo(w * 0.5f, h * 0.073f)
            lineTo(w * 0.865f, h * 0.204f)
            lineTo(w * 0.865f, h * 0.483f)
            cubicTo(
                w * 0.865f, h * 0.70f,
                w * 0.721f, h * 0.892f,
                w * 0.5f, h * 0.938f
            )
            cubicTo(
                w * 0.279f, h * 0.892f,
                w * 0.135f, h * 0.70f,
                w * 0.135f, h * 0.483f
            )
            lineTo(w * 0.135f, h * 0.204f)
            close()
        }

        drawPath(
            path = shieldPath,
            color = tint,
            style = Stroke(width = strokeWidth, join = StrokeJoin.Round)
        )

        // Center ellipse
        drawOval(
            color = tint,
            topLeft = Offset(w * 0.344f, h * 0.281f),
            size = Size(w * 0.312f, h * 0.396f),
            style = Stroke(width = strokeWidth)
        )

        // Severed trace diagonal line
        drawLine(
            color = tint,
            start = Offset(w * 0.271f, h * 0.708f),
            end = Offset(w * 0.729f, h * 0.25f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

/**
 * Matches ZeroTraceWordmark.tsx: "ZERO TRACE" in uppercase tracking-widest with bold "ZERO" and light "TRACE"
 */
@Composable
fun ZeroTraceWordmark(
    modifier: Modifier = Modifier,
    textColor: Color = ZtText,
    showIcon: Boolean = true
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        if (showIcon) {
            ZeroTraceMark(size = 22.dp, tint = ZtAccent)
            Spacer(modifier = Modifier.width(8.dp))
        }

        Text(
            text = buildAnnotatedString {
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append("ZERO")
                }
                withStyle(style = SpanStyle(fontWeight = FontWeight.Light)) {
                    append("TRACE")
                }
            },
            fontSize = 14.sp,
            letterSpacing = 2.5.sp,
            color = textColor
        )
    }
}
