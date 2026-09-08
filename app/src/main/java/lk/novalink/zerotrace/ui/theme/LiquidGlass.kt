package lk.novalink.zerotrace.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Apple Human Interface Guidelines - Liquid Glass Material System
 * Creates a translucent functional layer with specular highlights and physical elevation
 * that floats distinctly above the content layer.
 */
object LiquidGlassTokens {
    // Translucent dark glass acrylic materials
    val GlassBgStart = Color(0xE013151F) // 88% dark slate acrylic
    val GlassBgEnd   = Color(0xF20B0D13) // 95% deep midnight
    val GlassSurface = Color(0xB8141620) // 72% surface for glass cards

    // Apple Specular Top-Edge Highlights (brighter reflection at top edge)
    val SpecularEdgeTop = Color(0x40FFFFFF) // 25% white specular rim
    val SpecularEdgeMid = Color(0x14FFFFFF) // 8% white mid
    val SpecularEdgeBot = Color(0x06FFFFFF) // 2% subtle bottom edge

    // Active Tab Pill / Glass Glow
    val ActivePillBg     = Color(0x285468FF) // 16% Sapphire
    val ActivePillBorder = Color(0x4D5468FF) // 30% Sapphire border
    val ActivePillGlow   = Color(0x335468FF) // 20% Sapphire glow

    // Shadow depth
    val FloatingElevation = 20.dp
}

/**
 * Applies Apple HIG Liquid Glass material styling to any composable:
 * 1. Physical ambient & spot shadow for floating elevation
 * 2. Translucent acrylic gradient fill
 * 3. Specular top-to-bottom edge highlight border
 */
fun Modifier.liquidGlass(
    shape: Shape,
    elevation: Dp = LiquidGlassTokens.FloatingElevation,
    borderWidth: Dp = 1.2.dp,
    bgGradient: Brush = Brush.verticalGradient(
        colors = listOf(LiquidGlassTokens.GlassBgStart, LiquidGlassTokens.GlassBgEnd)
    ),
    borderGradient: Brush = Brush.verticalGradient(
        colors = listOf(
            LiquidGlassTokens.SpecularEdgeTop,
            LiquidGlassTokens.SpecularEdgeMid,
            LiquidGlassTokens.SpecularEdgeBot
        )
    )
): Modifier = this
    .shadow(
        elevation = elevation,
        shape = shape,
        clip = false,
        ambientColor = Color(0x60000000),
        spotColor = Color(0x99000000)
    )
    .clip(shape)
    .background(bgGradient)
    .border(width = borderWidth, brush = borderGradient, shape = shape)
