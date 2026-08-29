package lk.novalink.zerotrace.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import lk.novalink.zerotrace.data.model.ProxyConfig
import lk.novalink.zerotrace.data.model.ProxyProtocol
import lk.novalink.zerotrace.ui.theme.ZtAccent
import lk.novalink.zerotrace.ui.theme.ZtAccentSoft
import lk.novalink.zerotrace.ui.theme.ZtBorder
import lk.novalink.zerotrace.ui.theme.ZtBorderStrong
import lk.novalink.zerotrace.ui.theme.ZtDanger
import lk.novalink.zerotrace.ui.theme.ZtSuccess
import lk.novalink.zerotrace.ui.theme.ZtSurface
import lk.novalink.zerotrace.ui.theme.ZtSurface2
import lk.novalink.zerotrace.ui.theme.ZtText
import lk.novalink.zerotrace.ui.theme.ZtTextFaint
import lk.novalink.zerotrace.ui.theme.ZtTextMuted
import lk.novalink.zerotrace.ui.theme.ZtTrack
import lk.novalink.zerotrace.ui.theme.ZtWarn

/**
 * Native Jetpack Compose implementation of ServerRow.tsx from React design system
 */
@Composable
fun ServerCard(
    config: ProxyConfig,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onPingTest: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) ZtAccent else ZtBorder,
        animationSpec = tween(200),
        label = "serverCardBorder"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ZtSurface)
            .border(if (isSelected) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable { onSelect() }
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Location / Protocol Icon
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) ZtAccentSoft else ZtSurface2),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = null,
                    tint = if (isSelected) ZtAccent else ZtTextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Node info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = config.name,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 14.5.sp,
                        color = ZtText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    ProtocolBadge(protocol = config.protocol)
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = config.displaySubtitle,
                    fontSize = 11.5.sp,
                    color = ZtTextFaint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (config.sni.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "SNI: ${config.sni}",
                        fontSize = 10.5.sp,
                        color = ZtAccent.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Latency & Actions
            Row(verticalAlignment = Alignment.CenterVertically) {
                PingBadge(
                    pingMs = config.pingMs,
                    onPingClick = onPingTest
                )

                Spacer(modifier = Modifier.width(4.dp))

                // Load bars (Matches LoadBars in ServerRow.tsx)
                LoadBars(pingMs = config.pingMs)

                if (onEdit != null) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = ZtTextMuted,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                if (onDelete != null) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete",
                            tint = ZtTextFaint,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Checked indicator circle
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) ZtAccent else Color.Transparent)
                        .border(1.dp, if (isSelected) ZtAccent else ZtBorderStrong, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadBars(pingMs: Long) {
    val level = when {
        pingMs in 1..150 -> 1
        pingMs in 151..300 -> 2
        pingMs > 300 -> 3
        else -> 1
    }
    val color = when (level) {
        1 -> ZtSuccess
        2 -> ZtWarn
        else -> ZtDanger
    }

    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .width(2.5.dp)
                .height(6.dp)
                .clip(CircleShape)
                .background(if (level >= 1) color else ZtTrack)
        )
        Box(
            modifier = Modifier
                .width(2.5.dp)
                .height(9.dp)
                .clip(CircleShape)
                .background(if (level >= 2) color else ZtTrack)
        )
        Box(
            modifier = Modifier
                .width(2.5.dp)
                .height(12.dp)
                .clip(CircleShape)
                .background(if (level >= 3) color else ZtTrack)
        )
    }
}

@Composable
fun ProtocolBadge(protocol: ProxyProtocol) {
    val (bgColor, textColor) = when (protocol) {
        ProxyProtocol.VLESS -> Pair(ZtAccentSoft, ZtAccent)
        ProxyProtocol.VMESS -> Pair(ZtAccentSoft, ZtAccent)
        ProxyProtocol.TROJAN -> Pair(ZtSuccess.copy(alpha = 0.15f), ZtSuccess)
        ProxyProtocol.SHADOWSOCKS -> Pair(ZtWarn.copy(alpha = 0.15f), ZtWarn)
        ProxyProtocol.CUSTOM_JSON -> Pair(ZtSurface2, ZtTextMuted)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .padding(horizontal = 5.dp, vertical = 1.5.dp)
    ) {
        Text(
            text = protocol.displayName.uppercase(),
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            letterSpacing = 0.4.sp
        )
    }
}

@Composable
fun PingBadge(
    pingMs: Long,
    onPingClick: (() -> Unit)? = null
) {
    val (label, color, bg) = when {
        pingMs < 0 -> Triple("Ping", ZtTextFaint, ZtSurface2)
        pingMs in 1..150 -> Triple("${pingMs}ms", ZtSuccess, ZtSuccess.copy(alpha = 0.12f))
        pingMs in 151..350 -> Triple("${pingMs}ms", ZtWarn, ZtWarn.copy(alpha = 0.12f))
        else -> Triple("${pingMs}ms", ZtDanger, ZtDanger.copy(alpha = 0.12f))
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .clickable(enabled = onPingClick != null) { onPingClick?.invoke() }
            .padding(horizontal = 7.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
}
