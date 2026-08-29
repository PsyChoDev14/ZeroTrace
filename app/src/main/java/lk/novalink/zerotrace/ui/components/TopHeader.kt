package lk.novalink.zerotrace.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import lk.novalink.zerotrace.ui.theme.AccentBlue
import lk.novalink.zerotrace.ui.theme.SapphireCore
import lk.novalink.zerotrace.ui.theme.SurfaceElevated
import lk.novalink.zerotrace.ui.theme.TextMuted
import lk.novalink.zerotrace.ui.theme.TextWhite

@Composable
fun TopHeader(
    onAddClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Logo + Title
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SapphireCore),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "ZT",
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = "ZeroTrace",
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp,
                    color = TextWhite
                )
                Text(
                    text = "by NovaLink LK",
                    fontWeight = FontWeight.Normal,
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
        }

        // Action icons
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(
                onClick = onAddClick,
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceElevated)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Config",
                    tint = AccentBlue,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceElevated)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
