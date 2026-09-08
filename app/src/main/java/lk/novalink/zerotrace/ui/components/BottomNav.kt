package lk.novalink.zerotrace.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import lk.novalink.zerotrace.ui.theme.ZtAccent
import lk.novalink.zerotrace.ui.theme.ZtAccentSoft
import lk.novalink.zerotrace.ui.theme.ZtBgElevated
import lk.novalink.zerotrace.ui.theme.ZtBorder
import lk.novalink.zerotrace.ui.theme.ZtText
import lk.novalink.zerotrace.ui.theme.ZtTextFaint
import lk.novalink.zerotrace.ui.theme.ZtTextMuted

import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import lk.novalink.zerotrace.ui.theme.LiquidGlassTokens
import lk.novalink.zerotrace.ui.theme.liquidGlass

enum class NavTab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Shield),
    CONFIGS("Configs", Icons.Default.Language),
    STATS("Stats", Icons.Default.BarChart),
    SETTINGS("Settings", Icons.Default.Settings)
}

/**
 * iOS Liquid Glass Floating Navbar
 * Translucent frosted acrylic material with Apple specular top-edge highlights,
 * floating elevation shadow, and spring-animated tab capsules.
 */
@Composable
fun BottomNav(
    activeTab: NavTab,
    onTabSelected: (NavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val navShape = RoundedCornerShape(32.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        // Floating Liquid Glass Island
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .liquidGlass(
                    shape = navShape,
                    elevation = 20.dp,
                    borderWidth = 1.2.dp
                )
                .padding(horizontal = 6.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavTab.entries.forEach { tab ->
                    val isActive = tab == activeTab

                    val iconColor by animateColorAsState(
                        targetValue = if (isActive) ZtAccent else ZtTextMuted,
                        animationSpec = tween(180),
                        label = "tabIconColor"
                    )

                    val textColor by animateColorAsState(
                        targetValue = if (isActive) ZtText else ZtTextFaint,
                        animationSpec = tween(180),
                        label = "tabTextColor"
                    )

                    val iconScale by androidx.compose.animation.core.animateFloatAsState(
                        targetValue = if (isActive) 1.14f else 1.0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "tabScale"
                    )

                    val pillBgColor by animateColorAsState(
                        targetValue = if (isActive) LiquidGlassTokens.ActivePillBg else Color.Transparent,
                        animationSpec = tween(200),
                        label = "pillBgColor"
                    )

                    val pillBorderColor by animateColorAsState(
                        targetValue = if (isActive) LiquidGlassTokens.ActivePillBorder else Color.Transparent,
                        animationSpec = tween(200),
                        label = "pillBorderColor"
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(24.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    if (!isActive) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onTabSelected(tab)
                                    }
                                }
                            )
                            .padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Frosted Capsule Active Indicator
                        Box(
                            modifier = Modifier
                                .size(width = 54.dp, height = 32.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(pillBgColor)
                                .border(1.dp, pillBorderColor, RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label,
                                tint = iconColor,
                                modifier = Modifier
                                    .size(20.dp)
                                    .scale(iconScale)
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = tab.label,
                            fontSize = 11.sp,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                            color = textColor,
                            letterSpacing = (-0.2).sp
                        )
                    }
                }
            }
        }
    }
}
