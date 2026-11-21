package com.example.model

import com.squareup.moshi.JsonClass
import java.util.UUID

enum class LayerType {
  TEXT,
  LOGO,
}

enum class WatermarkBlendMode(val label: String) {
  NORMAL("Normal"),
  MULTIPLY("Multiply"),
  SCREEN("Screen"),
  OVERLAY("Overlay"),
  SOFT_LIGHT("Soft Light"),
  HARD_LIGHT("Hard Light"),
  DARKEN("Darken"),
  LIGHTEN("Lighten"),
  COLOR_BURN("Color Burn"),
  COLOR_DODGE("Color Dodge"),
  DIFFERENCE("Difference"),
  EXCLUSION("Exclusion"),
  ADD("Add (Linear Dodge)"),
  SUBTRACT("Subtract"),
  HUE("Hue"),
  SATURATION("Saturation"),
  COLOR("Color"),
  LUMINOSITY("Luminosity"),
}

enum class AnchorPreset(val label: String, val xFrac: Float, val yFrac: Float) {
  TOP_LEFT("Top Left", 0f, 0f),
  TOP_CENTER("Top Center", 0.5f, 0f),
  TOP_RIGHT("Top Right", 1f, 0f),
  CENTER_LEFT("Mid Left", 0f, 0.5f),
  CENTER("Center", 0.5f, 0.5f),
  CENTER_RIGHT("Mid Right", 1f, 0.5f),
  BOTTOM_LEFT("Bottom Left", 0f, 1f),
  BOTTOM_CENTER("Bottom Center", 0.5f, 1f),
  BOTTOM_RIGHT("Bottom Right", 1f, 1f),
}

enum class TextAlignOption(val label: String) {
  LEFT("Left"),
  CENTER("Center"),
  RIGHT("Right"),
}

enum class FillMode(val label: String) {
  SOLID("Solid"),
  LINEAR_GRADIENT("Linear"),
  RADIAL_GRADIENT("Radial"),
}

@JsonClass(generateAdapter = true)
data class StrokeEffect(
  val enabled: Boolean = false,
  val colorArgb: Long = 0xFF000000L,
  val widthPx: Float = 3f,
  val opacity: Float = 0.85f,
)

@JsonClass(generateAdapter = true)
data class ShadowEffect(
  val enabled: Boolean = true,
  val colorArgb: Long = 0xFF000000L,
  val blurRadius: Float = 10f,
  val distance: Float = 6f,
  val angleDeg: Float = 135f,
  val opacity: Float = 0.55f,
)

@JsonClass(generateAdapter = true)
data class GlowEffect(
  val enabled: Boolean = false,
  val colorArgb: Long = 0xFF38BDF8L,
  val radius: Float = 14f,
  val opacity: Float = 0.65f,
)

@JsonClass(generateAdapter = true)
data class EmbossEffect(
  val enabled: Boolean = false,
  val azimuthDeg: Float = 135f,
  val depth: Float = 3f,
  val opacity: Float = 0.7f,
)

@JsonClass(generateAdapter = true)
data class BackgroundHighlight(
  val enabled: Boolean = false,
  val colorArgb: Long = 0xFF0D0F14L,
  val opacity: Float = 0.6f,
  val paddingH: Float = 20f,
  val paddingV: Float = 10f,
  val cornerRadius: Float = 10f,
)

@JsonClass(generateAdapter = true)
data class TileSettings(
  val enabled: Boolean = false,
  val spacingX: Float = 180f,
  val spacingY: Float = 140f,
  val rotationDeg: Float = -30f,
  val scale: Float = 0.85f,
  val staggerRows: Boolean = true,
  val opacity: Float = 0.45f,
)

@JsonClass(generateAdapter = true)
data class LayerTransform(
  val xFraction: Float = 0.5f,
  val yFraction: Float = 0.5f,
  val scale: Float = 1.0f,
  val scaleX: Float = 1.0f,
  val scaleY: Float = 1.0f,
  val aspectRatioLocked: Boolean = true,
  val rotationDeg: Float = 0f,
  val anchorPreset: AnchorPreset? = AnchorPreset.CENTER,
  val marginFraction: Float = 0.04f,
)

@JsonClass(generateAdapter = true)
data class TextLayerStyle(
  val text: String = "© 2026 Watermark Studio",
  val fontId: String = "montserrat",
  val fontSize: Float = 48f,
  val fontWeight: Int = 600,
  val isItalic: Boolean = false,
  val letterSpacingEm: Float = 0.04f,
  val wordSpacingPx: Float = 0f,
  val lineHeightMult: Float = 1.2f,
  val textAlign: TextAlignOption = TextAlignOption.CENTER,
  val fillMode: FillMode = FillMode.SOLID,
  val fillColorArgb: Long = 0xFFFFFFFFL,
  val gradientEndColorArgb: Long = 0xFFF59E0BL,
  val gradientAngleDeg: Float = 45f,
  val fillOpacity: Float = 1.0f,
  val stroke: StrokeEffect = StrokeEffect(),
  val shadow: ShadowEffect = ShadowEffect(),
  val glow: GlowEffect = GlowEffect(),
  val emboss: EmbossEffect = EmbossEffect(),
  val background: BackgroundHighlight = BackgroundHighlight(),
)

@JsonClass(generateAdapter = true)
data class LogoLayerStyle(
  val assetId: String = "builtin_aperture",
  val assetName: String = "Aperture Crest",
  val isSvg: Boolean = true,
  val baseWidthFraction: Float = 0.22f,
  val logoOpacity: Float = 1.0f,
  val brightness: Float = 0f,
  val contrast: Float = 1.0f,
  val saturation: Float = 1.0f,
  val grayscale: Boolean = false,
  val monochrome: Boolean = false,
  val tintEnabled: Boolean = false,
  val tintColorArgb: Long = 0xFFFFFFFFL,
  val tintIntensity: Float = 1.0f,
  val stroke: StrokeEffect = StrokeEffect(),
  val shadow: ShadowEffect = ShadowEffect(enabled = false),
  val glow: GlowEffect = GlowEffect(),
  val emboss: EmbossEffect = EmbossEffect(),
)

@JsonClass(generateAdapter = true)
data class WatermarkLayer(
  val id: String = UUID.randomUUID().toString(),
  val name: String = "Watermark Layer",
  val type: LayerType = LayerType.TEXT,
  val visible: Boolean = true,
  val locked: Boolean = false,
  val opacity: Float = 0.9f,
  val blendMode: WatermarkBlendMode = WatermarkBlendMode.NORMAL,
  val transform: LayerTransform = LayerTransform(),
  val tile: TileSettings = TileSettings(),
  val textStyle: TextLayerStyle? = null,
  val logoStyle: LogoLayerStyle? = null,
)

data class ImageAsset(
  val id: String = UUID.randomUUID().toString(),
  val name: String,
  val uriString: String? = null,
  val sampleDrawableRes: Int? = null,
  val width: Int,
  val height: Int,
  val formatLabel: String,
  val fileSizeBytes: Long,
  val hasAlpha: Boolean,
  val hasExif: Boolean,
  val iccProfileName: String = "sRGB IEC61966-2.1",
)

@JsonClass(generateAdapter = true)
data class WatermarkPreset(
  val id: String,
  val name: String,
  val description: String,
  val category: String,
  val isBuiltIn: Boolean = false,
  val layers: List<WatermarkLayer>,
)

enum class ExportFormat(val extension: String, val mimeType: String, val label: String) {
  JPEG("jpg", "image/jpeg", "JPEG"),
  PNG("png", "image/png", "PNG"),
  WEBP("webp", "image/webp", "WebP"),
  TIFF("tif", "image/tiff", "TIFF"),
}

enum class ResizeMode(val label: String) {
  ORIGINAL("Original Size"),
  SCALE_PERCENT("Scale %"),
  FIT_LONG_EDGE("Max Long Edge"),
}

enum class IccProfileMode(val label: String) {
  PRESERVE_OR_SRGB("Preserve / sRGB"),
  EMBED_SRGB("Embed sRGB"),
  STRIP("Strip Profile"),
}

@JsonClass(generateAdapter = true)
data class ExportSettings(
  val format: ExportFormat = ExportFormat.JPEG,
  val quality: Int = 92,
  val webpLossless: Boolean = false,
  val resizeMode: ResizeMode = ResizeMode.ORIGINAL,
  val scalePercent: Int = 100,
  val maxLongEdgePx: Int = 2048,
  val preserveMetadata: Boolean = true,
  val iccProfileMode: IccProfileMode = IccProfileMode.PRESERVE_OR_SRGB,
)

@JsonClass(generateAdapter = true)
data class BatchSettings(
  val namingTemplate: String = "{name}_watermarked",
  val autoScaleToImage: Boolean = true,
  val exportSettings: ExportSettings = ExportSettings(),
)

data class BatchError(
  val fileName: String,
  val reason: String,
)

data class BatchJobProgress(
  val isRunning: Boolean = false,
  val isFinished: Boolean = false,
  val total: Int = 0,
  val completed: Int = 0,
  val succeeded: Int = 0,
  val failed: Int = 0,
  val currentFileName: String = "",
  val errors: List<BatchError> = emptyList(),
  val cancelled: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class SerializedFontEntry(
  val id: String,
  val displayName: String,
  val fileName: String,
)

@JsonClass(generateAdapter = true)
data class SerializedLogoEntry(
  val id: String,
  val displayName: String,
  val fileName: String,
  val isSvg: Boolean,
)

@JsonClass(generateAdapter = true)
data class WatermarkProjectFile(
  val version: Int = 1,
  val projectName: String = "Untitled Project",
  val savedAtMillis: Long = System.currentTimeMillis(),
  val activeImageName: String? = null,
  val activeSampleResName: String? = null,
  val activeImageUri: String? = null,
  val embeddedImageFileName: String? = null,
  val layers: List<WatermarkLayer> = emptyList(),
  val customFonts: List<SerializedFontEntry> = emptyList(),
  val customLogos: List<SerializedLogoEntry> = emptyList(),
  val exportSettings: ExportSettings = ExportSettings(),
)

enum class InspectorTab(val label: String) {
  CONTENT("Content"),
  STYLE("Fill & FX"),
  BLEND("Compositing"),
  LAYOUT("Position"),
  TILE("Tiling"),
}

enum class LeftSidebarTab(val label: String) {
  IMAGES("Images"),
  LAYERS("Layers"),
  PRESETS("Presets"),
}
