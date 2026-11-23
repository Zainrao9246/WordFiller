package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

private val FillwordsColorScheme = darkColorScheme(
  primary = AccentSky,
  onPrimary = TextPrimary,
  primaryContainer = SlateSurfaceElevated,
  onPrimaryContainer = TextPrimary,
  secondary = AccentGold,
  onSecondary = PillNavyInk,
  secondaryContainer = ColorTokens.GoldMutedSurface,
  onSecondaryContainer = AccentGold,
  tertiary = AccentEmerald,
  onTertiary = PillNavyInk,
  background = DeepNavyBackground,
  onBackground = TextPrimary,
  surface = SlateSurface,
  onSurface = TextPrimary,
  surfaceVariant = UnsolvedTileBg,
  onSurfaceVariant = TextSecondary,
  outline = UnsolvedTileStroke,
  error = AccentCoral,
  onError = TextPrimary,
)

private object ColorTokens {
  val GoldMutedSurface = androidx.compose.ui.graphics.Color(0xFF332D1B)
}

val FillwordsShapes = Shapes(
  small = RoundedCornerShape(10.dp),
  medium = RoundedCornerShape(16.dp),
  large = RoundedCornerShape(24.dp),
  extraLarge = RoundedCornerShape(30.dp),
)

@Composable
fun FillwordsTheme(
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = FillwordsColorScheme,
    typography = FillwordsTypography,
    shapes = FillwordsShapes,
    content = content,
  )
}
