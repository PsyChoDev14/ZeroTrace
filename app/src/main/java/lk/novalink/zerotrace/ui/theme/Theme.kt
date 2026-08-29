package lk.novalink.zerotrace.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary          = SapphireCore,
    onPrimary        = TextWhite,
    secondary        = AccentBlue,
    onSecondary      = TextWhite,
    background       = BgDark,
    onBackground     = TextWhite,
    surface          = SurfaceCard,
    onSurface        = TextWhite,
    surfaceVariant   = SurfaceElevated,
    onSurfaceVariant = TextMuted,
    outline          = BorderSubtle
)

@Composable
fun ZeroTraceTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor     = BgDark.toArgb()
            window.navigationBarColor = BgDark.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars     = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography  = Typography,
        content     = content
    )
}
