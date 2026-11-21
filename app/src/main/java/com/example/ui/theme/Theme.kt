package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val StudioDarkColorScheme = darkColorScheme(
  primary = StudioAmber,
  onPrimary = StudioObsidian,
  primaryContainer = StudioAmberDark,
  onPrimaryContainer = StudioAmber,
  secondary = StudioCyan,
  onSecondary = StudioObsidian,
  secondaryContainer = StudioCyanDark,
  onSecondaryContainer = StudioCyan,
  tertiary = StudioEmerald,
  background = StudioObsidian,
  onBackground = StudioTextPrimary,
  surface = StudioPanel,
  onSurface = StudioTextPrimary,
  surfaceVariant = StudioSurface,
  onSurfaceVariant = StudioTextSecondary,
  surfaceContainerLowest = StudioObsidian,
  surfaceContainerLow = StudioPanel,
  surfaceContainer = StudioSurface,
  surfaceContainerHigh = StudioSurfaceElevated,
  outline = StudioBorder,
  outlineVariant = StudioBorderSubtle,
  error = StudioCoral,
)

private val StudioShapes = Shapes(
  extraSmall = RoundedCornerShape(4.dp),
  small = RoundedCornerShape(6.dp),
  medium = RoundedCornerShape(10.dp),
  large = RoundedCornerShape(14.dp),
  extraLarge = RoundedCornerShape(20.dp),
)

@Composable
fun WatermarkStudioTheme(
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = StudioDarkColorScheme,
    typography = StudioTypography,
    shapes = StudioShapes,
    content = content,
  )
}
