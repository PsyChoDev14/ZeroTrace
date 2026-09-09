package lk.novalink.zerotrace.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import lk.novalink.zerotrace.ui.theme.liquidGlass

enum class NavTab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Shield),
    CONFIGS("Servers", Icons.Default.Language),
    STATS("Stats", Icons.Default.BarChart),
    SETTINGS("Settings", Icons.Default.Settings)
}

/**
 * iOS Liquid Glass Floating Navbar (Obsidian Dark Cyber Theme • Matched to Desktop Design)
 * - Ultra-frosted acrylic capsule with specular top-edge highlight
 * - Balanced 5-element island: [Home] [Servers] ( + Add Node ) [Stats] [Settings]
 * - Translucent sapphire liquid glass active capsule with zero border flash or layout shift
 * - Center elevated vibrant circular (+) button with smooth rotation, spring bounce & glow
 */
@Composable
fun BottomNav(
    activeTab: NavTab,
    onTabSelected: (NavTab) -> Unit,
    modifier: Modifier = Modifier,
    onAddConfigClick: () -> Unit = {},
    serverCount: Int = 0
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        val navShape = RoundedCornerShape(32.dp)

        // Floating Liquid Glass Island
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 360.dp)
                .liquidGlass(
                    shape = navShape,
                    elevation = 24.dp,
                    borderWidth = 1.dp
                )
                .padding(horizontal = 10.dp, vertical = 7.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Tabs: Home & Servers
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NavTabItem(
                        tab = NavTab.HOME,
                        isActive = activeTab == NavTab.HOME,
                        onClick = { onTabSelected(NavTab.HOME) }
                    )

                    NavTabItem(
                        tab = NavTab.CONFIGS,
                        isActive = activeTab == NavTab.CONFIGS,
                        badgeCount = if (serverCount > 0 && activeTab != NavTab.CONFIGS) serverCount else null,
                        onClick = { onTabSelected(NavTab.CONFIGS) }
                    )
                }

                // Center Elevated Action Button (+)
                CenterAddButton(
                    onClick = onAddConfigClick
                )

                // Right Tabs: Stats & Settings
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NavTabItem(
                        tab = NavTab.STATS,
                        isActive = activeTab == NavTab.STATS,
                        onClick = { onTabSelected(NavTab.STATS) }
                    )

                    NavTabItem(
                        tab = NavTab.SETTINGS,
                        isActive = activeTab == NavTab.SETTINGS,
                        onClick = { onTabSelected(NavTab.SETTINGS) }
                    )
                }
            }
        }
    }
}

@Composable
private fun NavTabItem(
    tab: NavTab,
    isActive: Boolean,
    badgeCount: Int? = null,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val itemShape = RoundedCornerShape(14.dp)

    val iconColor by animateColorAsState(
        targetValue = if (isActive) Color(0xFF60A5FA) else Color(0x8CFFFFFF),
        animationSpec = tween(180),
        label = "tabIconColor"
    )

    val pillBgColor by animateColorAsState(
        targetValue = if (isActive) Color(0x333B82F6) else Color.Transparent,
        animationSpec = tween(180),
        label = "pillBgColor"
    )

    val pillBorderColor by animateColorAsState(
        targetValue = if (isActive) Color(0x6660A5FA) else Color.Transparent,
        animationSpec = tween(180),
        label = "pillBorderColor"
    )

    val iconScale by animateFloatAsState(
        targetValue = if (isActive) 1.10f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "tabScale"
    )

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "pressScale"
    )

    Box(
        modifier = Modifier
            .size(44.dp)
            .scale(pressScale)
            .clip(itemShape)
            .background(pillBgColor)
            .border(1.dp, pillBorderColor, itemShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    if (!isActive) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onClick()
                    }
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = tab.icon,
            contentDescription = tab.label,
            tint = iconColor,
            modifier = Modifier
                .size(21.dp)
                .scale(iconScale)
        )

        // Notification badge for server count
        if (badgeCount != null && badgeCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-2).dp, y = 2.dp)
                    .size(15.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2563EB))
                    .border(1.dp, Color(0x66FFFFFF), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun CenterAddButton(
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "centerButtonScale"
    )

    val rotation by animateFloatAsState(
        targetValue = if (isPressed) 90f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "centerButtonRotation"
    )

    Box(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .size(46.dp)
            .scale(scale)
            .shadow(
                elevation = 14.dp,
                shape = CircleShape,
                ambientColor = Color(0x402563EB),
                spotColor = Color(0x992563EB)
            )
            .clip(CircleShape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF2563EB),
                        Color(0xFF4F46E5),
                        Color(0xFF3B82F6)
                    )
                )
            )
            .border(1.2.dp, Color(0x59FFFFFF), CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Add Server Configuration",
            tint = Color.White,
            modifier = Modifier
                .size(23.dp)
                .rotate(rotation)
        )
    }
}
