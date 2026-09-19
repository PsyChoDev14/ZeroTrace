package lk.novalink.zerotrace.ui.components

import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import lk.novalink.zerotrace.ui.theme.ZtAccent
import lk.novalink.zerotrace.ui.theme.ZtAccentRing
import lk.novalink.zerotrace.ui.theme.ZtAccentSoft
import java.net.HttpURLConnection
import java.net.URL

private val avatarCache = LruCache<String, ImageBitmap>(8)

private fun downloadAvatar(url: String): ImageBitmap? = try {
    avatarCache.get(url) ?: (URL(url).openConnection() as HttpURLConnection).run {
        connectTimeout = 10_000
        readTimeout = 10_000
        try {
            inputStream.use { BitmapFactory.decodeStream(it) }?.asImageBitmap()?.also { avatarCache.put(url, it) }
        } finally {
            disconnect()
        }
    }
} catch (e: Exception) {
    null
}

/**
 * Round profile picture. Falls back to the user's initial when there's no picture or it fails to
 * load, and to a person icon when [name] is null (signed out). Only https URLs are fetched.
 */
@Composable
fun AccountAvatar(
    name: String?,
    url: String?,
    size: Dp,
    modifier: Modifier = Modifier,
    active: Boolean = false
) {
    val bitmap by produceState<ImageBitmap?>(initialValue = null, url) {
        value = if (url != null && url.startsWith("https://")) {
            withContext(Dispatchers.IO) { downloadAvatar(url) }
        } else null
    }
    val initial = name?.trim()?.firstOrNull()?.uppercaseChar()

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(ZtAccentSoft)
            .border(1.dp, if (active) ZtAccent else ZtAccentRing, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        val image = bitmap
        when {
            image != null -> Image(
                bitmap = image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size)
            )
            initial != null -> Text(
                text = initial.toString(),
                color = ZtAccent,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.42f).sp
            )
            else -> Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Account",
                tint = if (active) ZtAccent else Color(0xFF8C8C97),
                modifier = Modifier.size(size * 0.55f)
            )
        }
    }
}
