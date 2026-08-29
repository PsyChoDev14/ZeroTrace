package lk.novalink.zerotrace.ui.theme

import androidx.compose.ui.graphics.Color

// ── Theme Colors from React Design System ─────────────────────

// Backgrounds
val ZtBg            = Color(0xFF08080A)   // --zt-bg (deep dark)
val ZtBgElevated    = Color(0xFF0E0E11)   // --zt-bg-elevated
val ZtSurface       = Color(0xFF141417)   // --zt-surface
val ZtSurface2      = Color(0xFF1B1B20)   // --zt-surface-2

// Borders & Lines
val ZtBorder        = Color(0xFF26262C)   // --zt-border
val ZtBorderStrong  = Color(0xFF33333B)   // --zt-border-strong
val ZtTrack         = Color(0x12FFFFFF)   // --zt-track (rgba 255,255,255, 0.07)

// Text
val ZtText          = Color(0xFFF4F4F6)   // --zt-text (ink)
val ZtTextMuted     = Color(0xFF8C8C97)   // --zt-text-muted
val ZtTextFaint     = Color(0xFF5C5C66)   // --zt-text-faint

// Accent (Indigo / Sapphire)
val ZtAccent        = Color(0xFF5468FF)   // --zt-accent
val ZtAccentHover   = Color(0xFF6A7BFF)   // --zt-accent-hover
val ZtAccentSoft    = Color(0x245468FF)   // --zt-accent-soft (rgba 84, 104, 255, 0.14)
val ZtAccentRing    = Color(0x525468FF)   // --zt-accent-ring (rgba 84, 104, 255, 0.32)

// Status
val ZtSuccess       = Color(0xFF35C77B)   // --zt-success
val ZtSuccessSoft   = Color(0x2435C77B)   // --zt-success-soft
val ZtWarn          = Color(0xFFE5A33C)   // --zt-warn
val ZtWarnSoft      = Color(0x24E5A33C)   // --zt-warn-soft
val ZtDanger        = Color(0xFFF0533D)   // --zt-danger
val ZtDangerSoft    = Color(0x24F0533D)   // --zt-danger-soft

// Legacy Aliases for backwards-compatibility
val BgDark          = ZtBg
val SurfaceDark     = ZtBgElevated
val SurfaceCard     = ZtSurface
val SurfaceElevated = ZtSurface2
val BorderSubtle    = ZtBorder
val BorderStrong    = ZtBorderStrong
val TextWhite       = ZtText
val TextMuted       = ZtTextMuted
val TextDim         = ZtTextFaint
val SapphireCore    = ZtAccent
val SapphireLight   = ZtAccent
val SapphireDim     = Color(0xFF3D4EDB)
val SapphireGlow    = ZtAccentRing
val SapphireRing    = ZtAccentRing
val AccentBlue      = ZtAccent
val StatusConnected = ZtSuccess
val StatusYellow    = ZtWarn
val StatusRed       = ZtDanger
