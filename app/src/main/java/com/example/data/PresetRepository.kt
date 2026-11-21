package com.example.data

import android.content.Context
import com.example.model.AnchorPreset
import com.example.model.BackgroundHighlight
import com.example.model.EmbossEffect
import com.example.model.FillMode
import com.example.model.GlowEffect
import com.example.model.LayerTransform
import com.example.model.LayerType
import com.example.model.LogoLayerStyle
import com.example.model.ShadowEffect
import com.example.model.StrokeEffect
import com.example.model.TextAlignOption
import com.example.model.TextLayerStyle
import com.example.model.TileSettings
import com.example.model.WatermarkBlendMode
import com.example.model.WatermarkLayer
import com.example.model.WatermarkPreset
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File
import java.util.UUID

class PresetRepository(context: Context) {

  private val presetsDir = File(context.filesDir, "user_presets").apply { mkdirs() }
  private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
  private val presetAdapter = moshi.adapter(WatermarkPreset::class.java)

  fun getBuiltInPresets(): List<WatermarkPreset> = listOf(
    WatermarkPreset(
      id = "preset_minimal_copyright",
      name = "Minimal Copyright",
      description = "Clean bottom-right editorial mark with soft drop shadow",
      category = "Photography",
      isBuiltIn = true,
      layers = listOf(
        WatermarkLayer(
          id = "l_min_1",
          name = "Copyright Line",
          type = LayerType.TEXT,
          opacity = 0.85f,
          blendMode = WatermarkBlendMode.NORMAL,
          transform = LayerTransform(
            xFraction = 0.94f,
            yFraction = 0.93f,
            scale = 1.0f,
            anchorPreset = AnchorPreset.BOTTOM_RIGHT,
            marginFraction = 0.05f,
          ),
          textStyle = TextLayerStyle(
            text = "© {YEAR} STUDIO ARCHIVE",
            fontId = "montserrat",
            fontSize = 26f,
            fontWeight = 500,
            letterSpacingEm = 0.14f,
            textAlign = TextAlignOption.RIGHT,
            fillColorArgb = 0xFFFFFFFFL,
            shadow = ShadowEffect(enabled = true, blurRadius = 8f, distance = 3f, opacity = 0.55f),
          ),
        )
      ),
    ),
    WatermarkPreset(
      id = "preset_photographer_signature",
      name = "Photographer Signature",
      description = "Script signature paired with fine-tracked photography credit",
      category = "Signature",
      isBuiltIn = true,
      layers = listOf(
        WatermarkLayer(
          id = "l_sig_1",
          name = "Script Signature",
          type = LayerType.TEXT,
          opacity = 0.92f,
          blendMode = WatermarkBlendMode.SCREEN,
          transform = LayerTransform(
            xFraction = 0.5f,
            yFraction = 0.84f,
            scale = 1.05f,
            anchorPreset = AnchorPreset.BOTTOM_CENTER,
          ),
          textStyle = TextLayerStyle(
            text = "Elena Vance",
            fontId = "dancing_script",
            fontSize = 62f,
            fontWeight = 600,
            letterSpacingEm = 0.02f,
            textAlign = TextAlignOption.CENTER,
            fillColorArgb = 0xFFFDF8EDL,
            shadow = ShadowEffect(enabled = true, blurRadius = 10f, distance = 4f, opacity = 0.5f),
          ),
        ),
        WatermarkLayer(
          id = "l_sig_2",
          name = "Sub Caption",
          type = LayerType.TEXT,
          opacity = 0.78f,
          blendMode = WatermarkBlendMode.NORMAL,
          transform = LayerTransform(
            xFraction = 0.5f,
            yFraction = 0.92f,
            scale = 1.0f,
            anchorPreset = AnchorPreset.BOTTOM_CENTER,
          ),
          textStyle = TextLayerStyle(
            text = "FINE ART PHOTOGRAPHY  •  {YEAR}",
            fontId = "montserrat",
            fontSize = 18f,
            fontWeight = 500,
            letterSpacingEm = 0.28f,
            textAlign = TextAlignOption.CENTER,
            fillColorArgb = 0xFFE2E8F0L,
            shadow = ShadowEffect(enabled = true, blurRadius = 6f, distance = 2f, opacity = 0.45f),
          ),
        ),
      ),
    ),
    WatermarkPreset(
      id = "preset_studio_branding",
      name = "Studio Branding",
      description = "Aperture emblem crest with editorial serif wordmark & gradient",
      category = "Branding",
      isBuiltIn = true,
      layers = listOf(
        WatermarkLayer(
          id = "l_brand_logo",
          name = "Aperture Crest",
          type = LayerType.LOGO,
          opacity = 0.88f,
          blendMode = WatermarkBlendMode.OVERLAY,
          transform = LayerTransform(
            xFraction = 0.5f,
            yFraction = 0.42f,
            scale = 0.85f,
            scaleX = 0.85f,
            scaleY = 0.85f,
            anchorPreset = AnchorPreset.CENTER,
          ),
          logoStyle = LogoLayerStyle(
            assetId = "builtin_aperture",
            assetName = "Aperture Crest",
            isSvg = true,
            baseWidthFraction = 0.18f,
            shadow = ShadowEffect(enabled = true, blurRadius = 14f, distance = 6f, opacity = 0.45f),
          ),
        ),
        WatermarkLayer(
          id = "l_brand_text",
          name = "Studio Wordmark",
          type = LayerType.TEXT,
          opacity = 0.9f,
          blendMode = WatermarkBlendMode.NORMAL,
          transform = LayerTransform(
            xFraction = 0.5f,
            yFraction = 0.57f,
            scale = 1.0f,
            anchorPreset = AnchorPreset.CENTER,
          ),
          textStyle = TextLayerStyle(
            text = "ATELIER LUMEN\nVISUAL STUDIO",
            fontId = "playfair_display",
            fontSize = 42f,
            fontWeight = 700,
            letterSpacingEm = 0.12f,
            lineHeightMult = 1.15f,
            textAlign = TextAlignOption.CENTER,
            fillMode = FillMode.LINEAR_GRADIENT,
            fillColorArgb = 0xFFFFFFFFL,
            gradientEndColorArgb = 0xFFF59E0BL,
            gradientAngleDeg = 20f,
            shadow = ShadowEffect(enabled = true, blurRadius = 12f, distance = 5f, opacity = 0.6f),
          ),
        ),
      ),
    ),
    WatermarkPreset(
      id = "preset_repeated_diagonal",
      name = "Repeated Diagonal Copyright",
      description = "Full-canvas staggered -30° protection pattern with Screen blend",
      category = "Protection",
      isBuiltIn = true,
      layers = listOf(
        WatermarkLayer(
          id = "l_tile_1",
          name = "Diagonal Tile Pattern",
          type = LayerType.TEXT,
          opacity = 0.65f,
          blendMode = WatermarkBlendMode.SCREEN,
          transform = LayerTransform(xFraction = 0.5f, yFraction = 0.5f),
          tile = TileSettings(
            enabled = true,
            spacingX = 160f,
            spacingY = 125f,
            rotationDeg = -30f,
            scale = 0.85f,
            staggerRows = true,
            opacity = 0.42f,
          ),
          textStyle = TextLayerStyle(
            text = "© PROOF ONLY • DO NOT DISTRIBUTE",
            fontId = "space_grotesk",
            fontSize = 24f,
            fontWeight = 600,
            letterSpacingEm = 0.1f,
            fillColorArgb = 0xFFFFFFFFL,
            shadow = ShadowEffect(enabled = false),
            emboss = EmbossEffect(enabled = true, depth = 1.5f, opacity = 0.4f),
          ),
        )
      ),
    ),
    WatermarkPreset(
      id = "preset_confidential",
      name = "Confidential",
      description = "High-visibility agency stamp with outline & soft highlight",
      category = "Protection",
      isBuiltIn = true,
      layers = listOf(
        WatermarkLayer(
          id = "l_conf_1",
          name = "Confidential Stamp",
          type = LayerType.TEXT,
          opacity = 0.82f,
          blendMode = WatermarkBlendMode.HARD_LIGHT,
          transform = LayerTransform(
            xFraction = 0.5f,
            yFraction = 0.5f,
            rotationDeg = -22f,
            scale = 1.15f,
            anchorPreset = AnchorPreset.CENTER,
          ),
          textStyle = TextLayerStyle(
            text = "CONFIDENTIAL\nCLIENT PREVIEW",
            fontId = "oswald",
            fontSize = 60f,
            fontWeight = 700,
            letterSpacingEm = 0.16f,
            lineHeightMult = 1.05f,
            textAlign = TextAlignOption.CENTER,
            fillColorArgb = 0xFFEF4444L,
            fillOpacity = 0.85f,
            stroke = StrokeEffect(enabled = true, colorArgb = 0xFFFFFFFFL, widthPx = 2.5f, opacity = 0.9f),
            background = BackgroundHighlight(
              enabled = true,
              colorArgb = 0xFF0D0F14L,
              opacity = 0.45f,
              paddingH = 34f,
              paddingV = 18f,
              cornerRadius = 12f,
            ),
            shadow = ShadowEffect(enabled = true, blurRadius = 14f, distance = 8f, opacity = 0.6f),
          ),
        )
      ),
    ),
    WatermarkPreset(
      id = "preset_social_media",
      name = "Social Media Branding",
      description = "Modern corner badge with frosted pill and studio handle",
      category = "Social",
      isBuiltIn = true,
      layers = listOf(
        WatermarkLayer(
          id = "l_soc_1",
          name = "Social Pill Badge",
          type = LayerType.TEXT,
          opacity = 0.95f,
          blendMode = WatermarkBlendMode.NORMAL,
          transform = LayerTransform(
            xFraction = 0.06f,
            yFraction = 0.92f,
            scale = 1.0f,
            anchorPreset = AnchorPreset.BOTTOM_LEFT,
          ),
          textStyle = TextLayerStyle(
            text = "@WATERMARK.STUDIO  •  {RES}",
            fontId = "space_grotesk",
            fontSize = 24f,
            fontWeight = 600,
            letterSpacingEm = 0.06f,
            textAlign = TextAlignOption.LEFT,
            fillColorArgb = 0xFFF8FAFC,
            background = BackgroundHighlight(
              enabled = true,
              colorArgb = 0xFF0F172AL,
              opacity = 0.72f,
              paddingH = 22f,
              paddingV = 12f,
              cornerRadius = 24f,
            ),
            glow = GlowEffect(enabled = true, colorArgb = 0xFF38BDF8L, radius = 8f, opacity = 0.35f),
            shadow = ShadowEffect(enabled = true, blurRadius = 10f, distance = 4f, opacity = 0.45f),
          ),
        )
      ),
    ),
  )

  fun getUserPresets(): List<WatermarkPreset> {
    return presetsDir.listFiles()
      ?.filter { it.isFile && it.extension == "json" }
      ?.sortedByDescending { it.lastModified() }
      ?.mapNotNull { file ->
        runCatching { presetAdapter.fromJson(file.readText()) }.getOrNull()
      } ?: emptyList()
  }

  fun saveUserPreset(name: String, description: String, layers: List<WatermarkLayer>): WatermarkPreset {
    val id = "user_${UUID.randomUUID().toString().take(8)}"
    val preset = WatermarkPreset(
      id = id,
      name = name.trim().ifBlank { "Custom Preset" },
      description = description.trim().ifBlank { "${layers.size} layer composition" },
      category = "Custom",
      isBuiltIn = false,
      layers = layers,
    )
    val json = presetAdapter.toJson(preset)
    File(presetsDir, "$id.json").writeText(json)
    return preset
  }

  fun deleteUserPreset(id: String) {
    File(presetsDir, "$id.json").delete()
  }
}
