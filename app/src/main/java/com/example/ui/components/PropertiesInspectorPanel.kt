package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.FormatAlignRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.engine.LayerRenderBounds
import com.example.engine.LogoAsset
import com.example.engine.StudioFont
import com.example.model.AnchorPreset
import com.example.model.FillMode
import com.example.model.ImageAsset
import com.example.model.InspectorTab
import com.example.model.LayerType
import com.example.model.LogoLayerStyle
import com.example.model.TextAlignOption
import com.example.model.TextLayerStyle
import com.example.model.WatermarkBlendMode
import com.example.model.WatermarkLayer
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioAmberDark
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.StudioObsidian
import com.example.ui.theme.StudioPanel
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.StudioTextMuted
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import com.example.viewmodel.ColorPickerTarget
import kotlin.math.roundToInt

@Composable
fun PropertiesInspectorPanel(
  selectedLayer: WatermarkLayer?,
  selectedBounds: LayerRenderBounds?,
  activeAsset: ImageAsset?,
  activeTab: InspectorTab,
  onTabChange: (InspectorTab) -> Unit,
  availableFonts: List<StudioFont>,
  availableLogos: List<LogoAsset>,
  onImportFont: () -> Unit,
  onImportLogo: () -> Unit,
  onRecordUndo: () -> Unit,
  onUpdateLayer: ((WatermarkLayer) -> WatermarkLayer) -> Unit,
  onApplyAnchorPreset: (AnchorPreset) -> Unit,
  onOpenColorPicker: (ColorPickerTarget) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .fillMaxHeight()
      .background(StudioPanel),
  ) {
    // Tab Row
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState())
        .padding(horizontal = 8.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      InspectorTab.entries.forEach { tab ->
        val selected = tab == activeTab
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) StudioSurfaceElevated else StudioSurface)
            .border(1.dp, if (selected) StudioAmber else StudioBorder, RoundedCornerShape(6.dp))
            .clickable { onTabChange(tab) }
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag("inspector_tab_${tab.name.lowercase()}"),
        ) {
          Text(
            text = tab.label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) StudioAmber else StudioTextSecondary,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
          )
        }
      }
    }

    HorizontalDivider(color = StudioBorder)

    if (selectedLayer == null) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(20.dp),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = "Select or add a watermark layer to inspect and edit its properties.",
          style = MaterialTheme.typography.bodySmall,
          color = StudioTextSecondary,
        )
      }
      return
    }

    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(10.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      when (activeTab) {
        InspectorTab.CONTENT -> {
          if (selectedLayer.type == LayerType.TEXT && selectedLayer.textStyle != null) {
            TextContentSection(
              style = selectedLayer.textStyle,
              availableFonts = availableFonts,
              onImportFont = onImportFont,
              onRecordUndo = onRecordUndo,
              onChange = { nextStyle ->
                onUpdateLayer { it.copy(textStyle = nextStyle) }
              },
            )
          } else if (selectedLayer.type == LayerType.LOGO && selectedLayer.logoStyle != null) {
            LogoContentSection(
              layer = selectedLayer,
              style = selectedLayer.logoStyle,
              availableLogos = availableLogos,
              onImportLogo = onImportLogo,
              onRecordUndo = onRecordUndo,
              onChangeLayer = onUpdateLayer,
            )
          }
        }
        InspectorTab.STYLE -> {
          StyleAndEffectsSection(
            layer = selectedLayer,
            onRecordUndo = onRecordUndo,
            onUpdateLayer = onUpdateLayer,
            onOpenColorPicker = onOpenColorPicker,
          )
        }
        InspectorTab.BLEND -> {
          CompositingSection(
            layer = selectedLayer,
            onRecordUndo = onRecordUndo,
            onUpdateLayer = onUpdateLayer,
          )
        }
        InspectorTab.LAYOUT -> {
          PositioningSection(
            layer = selectedLayer,
            bounds = selectedBounds,
            activeAsset = activeAsset,
            onRecordUndo = onRecordUndo,
            onUpdateLayer = onUpdateLayer,
            onApplyAnchorPreset = onApplyAnchorPreset,
          )
        }
        InspectorTab.TILE -> {
          TilingSection(
            layer = selectedLayer,
            onRecordUndo = onRecordUndo,
            onUpdateLayer = onUpdateLayer,
          )
        }
      }
      Spacer(Modifier.height(24.dp))
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TextContentSection(
  style: TextLayerStyle,
  availableFonts: List<StudioFont>,
  onImportFont: () -> Unit,
  onRecordUndo: () -> Unit,
  onChange: (TextLayerStyle) -> Unit,
) {
  var showFontMenu by remember { mutableStateOf(false) }
  val activeFont = availableFonts.find { it.id == style.fontId }

  InspectorCard(title = "TEXT & TEMPLATES") {
    OutlinedTextField(
      value = style.text,
      onValueChange = { onChange(style.copy(text = it)) },
      label = { Text("Watermark Copy (Multiline & Unicode)") },
      minLines = 2,
      maxLines = 4,
      colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = StudioAmber,
        unfocusedBorderColor = StudioBorder,
        focusedTextColor = StudioTextPrimary,
        unfocusedTextColor = StudioTextPrimary,
      ),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("inspector_text_input"),
    )
    Spacer(Modifier.height(6.dp))
    FlowRow(
      horizontalArrangement = Arrangement.spacedBy(4.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      listOf("©", "™", "®", "{YEAR}", "{DATE}", "{FILE}", "{RES}").forEach { token ->
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(StudioSurfaceElevated)
            .border(1.dp, StudioBorder, RoundedCornerShape(5.dp))
            .clickable {
              onRecordUndo()
              val sep = if (style.text.isEmpty() || style.text.endsWith(" ") || style.text.endsWith("\n")) "" else " "
              onChange(style.copy(text = style.text + sep + token))
            }
            .padding(horizontal = 7.dp, vertical = 4.dp),
        ) {
          Text(token, style = MaterialTheme.typography.labelSmall, color = StudioCyan)
        }
      }
    }
  }

  InspectorCard(title = "TYPOGRAPHY") {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Box(modifier = Modifier.weight(1f)) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(StudioSurfaceElevated)
            .border(1.dp, StudioBorder, RoundedCornerShape(6.dp))
            .clickable { showFontMenu = true }
            .padding(horizontal = 10.dp, vertical = 8.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = activeFont?.displayName ?: style.fontId,
            style = MaterialTheme.typography.bodyMedium,
            color = StudioTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text("▼", style = MaterialTheme.typography.labelSmall, color = StudioTextSecondary)
        }
        DropdownMenu(
          expanded = showFontMenu,
          onDismissRequest = { showFontMenu = false },
          containerColor = StudioSurfaceElevated,
        ) {
          availableFonts.forEach { font ->
            DropdownMenuItem(
              text = {
                Column {
                  Text(font.displayName, color = StudioTextPrimary)
                  Text(font.category, style = MaterialTheme.typography.labelSmall, color = StudioTextSecondary)
                }
              },
              onClick = {
                showFontMenu = false
                onRecordUndo()
                onChange(style.copy(fontId = font.id))
              },
            )
          }
        }
      }

      Row(
        modifier = Modifier
          .clip(RoundedCornerShape(6.dp))
          .background(StudioAmberDark)
          .border(1.dp, StudioAmber.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
          .clickable(onClick = onImportFont)
          .padding(horizontal = 8.dp, vertical = 8.dp)
          .testTag("import_font_btn"),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(Icons.Default.Add, null, tint = StudioAmber, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(2.dp))
        Text("TTF/OTF", style = MaterialTheme.typography.labelSmall, color = StudioAmber)
      }
    }

    Spacer(Modifier.height(8.dp))

    StudioSlider(
      label = "Font Size",
      value = style.fontSize,
      range = 10f..180f,
      valueText = "${style.fontSize.roundToInt()} pt",
      onValueChange = { onChange(style.copy(fontSize = it)) },
      onFinished = onRecordUndo,
    )

    StudioSlider(
      label = "Weight",
      value = style.fontWeight.toFloat(),
      range = 100f..900f,
      valueText = "${(style.fontWeight / 100) * 100}",
      onValueChange = { onChange(style.copy(fontWeight = (it.roundToInt() / 100) * 100)) },
      onFinished = onRecordUndo,
    )

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        TextAlignOption.entries.forEach { align ->
          val active = style.textAlign == align
          Box(
            modifier = Modifier
              .size(32.dp)
              .clip(RoundedCornerShape(6.dp))
              .background(if (active) StudioAmber else StudioSurfaceElevated)
              .border(1.dp, if (active) StudioAmber else StudioBorder, RoundedCornerShape(6.dp))
              .clickable {
                onRecordUndo()
                onChange(style.copy(textAlign = align))
              },
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = when (align) {
                TextAlignOption.LEFT -> Icons.AutoMirrored.Filled.FormatAlignLeft
                TextAlignOption.CENTER -> Icons.Default.FormatAlignCenter
                TextAlignOption.RIGHT -> Icons.AutoMirrored.Filled.FormatAlignRight
              },
              contentDescription = align.label,
              tint = if (active) StudioObsidian else StudioTextPrimary,
              modifier = Modifier.size(16.dp),
            )
          }
        }
      }

      Row(
        modifier = Modifier
          .clip(RoundedCornerShape(6.dp))
          .background(if (style.isItalic) StudioAmber else StudioSurfaceElevated)
          .border(1.dp, if (style.isItalic) StudioAmber else StudioBorder, RoundedCornerShape(6.dp))
          .clickable {
            onRecordUndo()
            onChange(style.copy(isItalic = !style.isItalic))
          }
          .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(
          Icons.Default.FormatItalic,
          contentDescription = "Italic",
          tint = if (style.isItalic) StudioObsidian else StudioTextPrimary,
          modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
          "Italic",
          style = MaterialTheme.typography.labelSmall,
          color = if (style.isItalic) StudioObsidian else StudioTextPrimary,
        )
      }
    }

    Spacer(Modifier.height(6.dp))

    StudioSlider(
      label = "Char Spacing",
      value = style.letterSpacingEm,
      range = -0.05f..0.6f,
      valueText = "%.2f em".format(style.letterSpacingEm),
      onValueChange = { onChange(style.copy(letterSpacingEm = it)) },
      onFinished = onRecordUndo,
    )

    StudioSlider(
      label = "Word Spacing",
      value = style.wordSpacingPx,
      range = 0f..48f,
      valueText = "${style.wordSpacingPx.roundToInt()} px",
      onValueChange = { onChange(style.copy(wordSpacingPx = it)) },
      onFinished = onRecordUndo,
    )

    StudioSlider(
      label = "Line Height",
      value = style.lineHeightMult,
      range = 0.7f..2.5f,
      valueText = "%.2f×".format(style.lineHeightMult),
      onValueChange = { onChange(style.copy(lineHeightMult = it)) },
      onFinished = onRecordUndo,
    )
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LogoContentSection(
  layer: WatermarkLayer,
  style: LogoLayerStyle,
  availableLogos: List<LogoAsset>,
  onImportLogo: () -> Unit,
  onRecordUndo: () -> Unit,
  onChangeLayer: ((WatermarkLayer) -> WatermarkLayer) -> Unit,
) {
  InspectorCard(title = "LOGO & GRAPHIC ASSET") {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = style.assetName,
        style = MaterialTheme.typography.bodyMedium,
        color = StudioTextPrimary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.weight(1f),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Row(
        modifier = Modifier
          .clip(RoundedCornerShape(6.dp))
          .background(StudioAmberDark)
          .border(1.dp, StudioAmber.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
          .clickable(onClick = onImportLogo)
          .padding(horizontal = 8.dp, vertical = 5.dp)
          .testTag("import_logo_btn"),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(Icons.Default.Add, null, tint = StudioAmber, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(3.dp))
        Text("Import PNG/SVG", style = MaterialTheme.typography.labelSmall, color = StudioAmber)
      }
    }

    Spacer(Modifier.height(8.dp))

    FlowRow(
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      availableLogos.forEach { logo ->
        val active = logo.id == style.assetId
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (active) StudioAmber else StudioSurfaceElevated)
            .border(1.dp, if (active) StudioAmber else StudioBorder, RoundedCornerShape(6.dp))
            .clickable {
              onRecordUndo()
              onChangeLayer {
                it.copy(
                  logoStyle = style.copy(
                    assetId = logo.id,
                    assetName = logo.displayName,
                    isSvg = logo.isSvg,
                  )
                )
              }
            }
            .padding(horizontal = 8.dp, vertical = 5.dp),
        ) {
          Text(
            text = logo.displayName,
            style = MaterialTheme.typography.labelSmall,
            color = if (active) StudioObsidian else StudioTextPrimary,
          )
        }
      }
    }
  }

  InspectorCard(title = "LOGO DIMENSIONS & ASPECT") {
    StudioSlider(
      label = "Base Size",
      value = style.baseWidthFraction * 100f,
      range = 5f..80f,
      valueText = "${(style.baseWidthFraction * 100).roundToInt()}%",
      onValueChange = { pct ->
        onChangeLayer { it.copy(logoStyle = style.copy(baseWidthFraction = pct / 100f)) }
      },
      onFinished = onRecordUndo,
    )

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text("Lock Aspect Ratio", style = MaterialTheme.typography.bodySmall, color = StudioTextSecondary)
      Row(
        modifier = Modifier
          .clip(RoundedCornerShape(6.dp))
          .background(if (layer.transform.aspectRatioLocked) StudioAmberDark else StudioSurfaceElevated)
          .border(1.dp, StudioBorder, RoundedCornerShape(6.dp))
          .clickable {
            onRecordUndo()
            val locked = !layer.transform.aspectRatioLocked
            onChangeLayer {
              it.copy(
                transform = it.transform.copy(
                  aspectRatioLocked = locked,
                  scaleY = if (locked) it.transform.scaleX else it.transform.scaleY,
                )
              )
            }
          }
          .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(
          if (layer.transform.aspectRatioLocked) Icons.Default.Lock else Icons.Default.LockOpen,
          contentDescription = null,
          tint = if (layer.transform.aspectRatioLocked) StudioAmber else StudioTextSecondary,
          modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
          if (layer.transform.aspectRatioLocked) "Locked" else "Unlocked",
          style = MaterialTheme.typography.labelSmall,
          color = StudioTextPrimary,
        )
      }
    }

    if (!layer.transform.aspectRatioLocked) {
      StudioSlider(
        label = "Stretch X",
        value = layer.transform.scaleX,
        range = 0.25f..3f,
        valueText = "%.2f×".format(layer.transform.scaleX),
        onValueChange = { sx ->
          onChangeLayer { it.copy(transform = it.transform.copy(scaleX = sx)) }
        },
        onFinished = onRecordUndo,
      )
      StudioSlider(
        label = "Stretch Y",
        value = layer.transform.scaleY,
        range = 0.25f..3f,
        valueText = "%.2f×".format(layer.transform.scaleY),
        onValueChange = { sy ->
          onChangeLayer { it.copy(transform = it.transform.copy(scaleY = sy)) }
        },
        onFinished = onRecordUndo,
      )
    }
  }

  InspectorCard(title = "COLOR & TONE ADJUSTMENTS") {
    StudioSlider(
      label = "Logo Opacity",
      value = style.logoOpacity * 100f,
      range = 0f..100f,
      valueText = "${(style.logoOpacity * 100).roundToInt()}%",
      onValueChange = { p -> onChangeLayer { it.copy(logoStyle = style.copy(logoOpacity = p / 100f)) } },
      onFinished = onRecordUndo,
    )
    StudioSlider(
      label = "Brightness",
      value = style.brightness,
      range = -0.6f..0.6f,
      valueText = "${(style.brightness * 100).roundToInt()}",
      onValueChange = { b -> onChangeLayer { it.copy(logoStyle = style.copy(brightness = b)) } },
      onFinished = onRecordUndo,
    )
    StudioSlider(
      label = "Contrast",
      value = style.contrast,
      range = 0.2f..2.5f,
      valueText = "%.2f×".format(style.contrast),
      onValueChange = { c -> onChangeLayer { it.copy(logoStyle = style.copy(contrast = c)) } },
      onFinished = onRecordUndo,
    )
    StudioSlider(
      label = "Saturation",
      value = style.saturation,
      range = 0f..2f,
      valueText = "${(style.saturation * 100).roundToInt()}%",
      onValueChange = { s -> onChangeLayer { it.copy(logoStyle = style.copy(saturation = s)) } },
      onFinished = onRecordUndo,
    )
    StudioToggleRow(
      label = "Grayscale",
      checked = style.grayscale,
      onCheckedChange = { g ->
        onRecordUndo()
        onChangeLayer { it.copy(logoStyle = style.copy(grayscale = g)) }
      },
    )
    StudioToggleRow(
      label = "High-Contrast Monochrome",
      checked = style.monochrome,
      onCheckedChange = { m ->
        onRecordUndo()
        onChangeLayer { it.copy(logoStyle = style.copy(monochrome = m)) }
      },
    )
  }
}

@Composable
private fun StyleAndEffectsSection(
  layer: WatermarkLayer,
  onRecordUndo: () -> Unit,
  onUpdateLayer: ((WatermarkLayer) -> WatermarkLayer) -> Unit,
  onOpenColorPicker: (ColorPickerTarget) -> Unit,
) {
  val textStyle = layer.textStyle
  val logoStyle = layer.logoStyle

  if (layer.type == LayerType.TEXT && textStyle != null) {
    InspectorCard(title = "TEXT FILL & GRADIENT") {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        FillMode.entries.forEach { mode ->
          val active = textStyle.fillMode == mode
          Box(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(6.dp))
              .background(if (active) StudioAmber else StudioSurfaceElevated)
              .border(1.dp, if (active) StudioAmber else StudioBorder, RoundedCornerShape(6.dp))
              .clickable {
                onRecordUndo()
                onUpdateLayer { it.copy(textStyle = textStyle.copy(fillMode = mode)) }
              }
              .padding(vertical = 6.dp),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              mode.label,
              style = MaterialTheme.typography.labelSmall,
              color = if (active) StudioObsidian else StudioTextPrimary,
            )
          }
        }
      }
      Spacer(Modifier.height(8.dp))
      ColorSwatchRow(
        label = if (textStyle.fillMode == FillMode.SOLID) "Fill Color" else "Gradient Start",
        colorArgb = textStyle.fillColorArgb,
        onClick = { onOpenColorPicker(ColorPickerTarget.TEXT_FILL) },
      )
      if (textStyle.fillMode != FillMode.SOLID) {
        ColorSwatchRow(
          label = "Gradient End",
          colorArgb = textStyle.gradientEndColorArgb,
          onClick = { onOpenColorPicker(ColorPickerTarget.TEXT_GRADIENT_END) },
        )
        if (textStyle.fillMode == FillMode.LINEAR_GRADIENT) {
          StudioSlider(
            label = "Gradient Angle",
            value = textStyle.gradientAngleDeg,
            range = 0f..360f,
            valueText = "${textStyle.gradientAngleDeg.roundToInt()}°",
            onValueChange = { a -> onUpdateLayer { it.copy(textStyle = textStyle.copy(gradientAngleDeg = a)) } },
            onFinished = onRecordUndo,
          )
        }
      }
      StudioSlider(
        label = "Text Fill Opacity",
        value = textStyle.fillOpacity * 100f,
        range = 0f..100f,
        valueText = "${(textStyle.fillOpacity * 100).roundToInt()}%",
        onValueChange = { p -> onUpdateLayer { it.copy(textStyle = textStyle.copy(fillOpacity = p / 100f)) } },
        onFinished = onRecordUndo,
      )
    }

    // Background highlight
    InspectorCard(title = "BACKGROUND HIGHLIGHT") {
      val bg = textStyle.background
      StudioToggleRow(
        label = "Enable Highlight Box",
        checked = bg.enabled,
        onCheckedChange = { en ->
          onRecordUndo()
          onUpdateLayer { it.copy(textStyle = textStyle.copy(background = bg.copy(enabled = en))) }
        },
      )
      if (bg.enabled) {
        ColorSwatchRow("Highlight Color", bg.colorArgb) { onOpenColorPicker(ColorPickerTarget.BACKGROUND_HIGHLIGHT) }
        StudioSlider("Background Opacity", bg.opacity * 100f, 0f..100f, "${(bg.opacity * 100).roundToInt()}%", { p ->
          onUpdateLayer { it.copy(textStyle = textStyle.copy(background = bg.copy(opacity = p / 100f))) }
        }, onRecordUndo)
        StudioSlider("Padding H", bg.paddingH, 0f..64f, "${bg.paddingH.roundToInt()} px", { v ->
          onUpdateLayer { it.copy(textStyle = textStyle.copy(background = bg.copy(paddingH = v))) }
        }, onRecordUndo)
        StudioSlider("Corner Radius", bg.cornerRadius, 0f..40f, "${bg.cornerRadius.roundToInt()} px", { v ->
          onUpdateLayer { it.copy(textStyle = textStyle.copy(background = bg.copy(cornerRadius = v))) }
        }, onRecordUndo)
      }
    }
  } else if (layer.type == LayerType.LOGO && logoStyle != null) {
    InspectorCard(title = "LOGO TINTING") {
      StudioToggleRow(
        label = "Custom Color Tint",
        checked = logoStyle.tintEnabled,
        onCheckedChange = { en ->
          onRecordUndo()
          onUpdateLayer { it.copy(logoStyle = logoStyle.copy(tintEnabled = en)) }
        },
      )
      if (logoStyle.tintEnabled) {
        ColorSwatchRow("Tint Color", logoStyle.tintColorArgb) { onOpenColorPicker(ColorPickerTarget.LOGO_TINT) }
        StudioSlider("Tint Intensity", logoStyle.tintIntensity * 100f, 0f..100f, "${(logoStyle.tintIntensity * 100).roundToInt()}%", { p ->
          onUpdateLayer { it.copy(logoStyle = logoStyle.copy(tintIntensity = p / 100f)) }
        }, onRecordUndo)
      }
    }
  }

  val stroke = textStyle?.stroke ?: logoStyle?.stroke
  val shadow = textStyle?.shadow ?: logoStyle?.shadow
  val glow = textStyle?.glow ?: logoStyle?.glow
  val emboss = textStyle?.emboss ?: logoStyle?.emboss

  if (stroke != null) {
    InspectorCard(title = "OUTLINE / STROKE") {
      StudioToggleRow("Enable Outline", stroke.enabled) { en ->
        onRecordUndo()
        val next = stroke.copy(enabled = en)
        onUpdateLayer {
          if (it.type == LayerType.TEXT) it.copy(textStyle = it.textStyle?.copy(stroke = next))
          else it.copy(logoStyle = it.logoStyle?.copy(stroke = next))
        }
      }
      if (stroke.enabled) {
        ColorSwatchRow("Outline Color", stroke.colorArgb) { onOpenColorPicker(ColorPickerTarget.STROKE) }
        StudioSlider("Thickness", stroke.widthPx, 0.5f..18f, "%.1f px".format(stroke.widthPx), { w ->
          val next = stroke.copy(widthPx = w)
          onUpdateLayer {
            if (it.type == LayerType.TEXT) it.copy(textStyle = it.textStyle?.copy(stroke = next))
            else it.copy(logoStyle = it.logoStyle?.copy(stroke = next))
          }
        }, onRecordUndo)
        StudioSlider("Outline Opacity", stroke.opacity * 100f, 0f..100f, "${(stroke.opacity * 100).roundToInt()}%", { p ->
          val next = stroke.copy(opacity = p / 100f)
          onUpdateLayer {
            if (it.type == LayerType.TEXT) it.copy(textStyle = it.textStyle?.copy(stroke = next))
            else it.copy(logoStyle = it.logoStyle?.copy(stroke = next))
          }
        }, onRecordUndo)
      }
    }
  }

  if (shadow != null) {
    InspectorCard(title = "DROP SHADOW") {
      StudioToggleRow("Enable Shadow", shadow.enabled) { en ->
        onRecordUndo()
        val next = shadow.copy(enabled = en)
        onUpdateLayer {
          if (it.type == LayerType.TEXT) it.copy(textStyle = it.textStyle?.copy(shadow = next))
          else it.copy(logoStyle = it.logoStyle?.copy(shadow = next))
        }
      }
      if (shadow.enabled) {
        ColorSwatchRow("Shadow Color", shadow.colorArgb) { onOpenColorPicker(ColorPickerTarget.SHADOW) }
        StudioSlider("Blur", shadow.blurRadius, 0f..40f, "${shadow.blurRadius.roundToInt()} px", { v ->
          val next = shadow.copy(blurRadius = v)
          onUpdateLayer {
            if (it.type == LayerType.TEXT) it.copy(textStyle = it.textStyle?.copy(shadow = next))
            else it.copy(logoStyle = it.logoStyle?.copy(shadow = next))
          }
        }, onRecordUndo)
        StudioSlider("Distance", shadow.distance, 0f..40f, "${shadow.distance.roundToInt()} px", { v ->
          val next = shadow.copy(distance = v)
          onUpdateLayer {
            if (it.type == LayerType.TEXT) it.copy(textStyle = it.textStyle?.copy(shadow = next))
            else it.copy(logoStyle = it.logoStyle?.copy(shadow = next))
          }
        }, onRecordUndo)
        StudioSlider("Direction", shadow.angleDeg, 0f..360f, "${shadow.angleDeg.roundToInt()}°", { v ->
          val next = shadow.copy(angleDeg = v)
          onUpdateLayer {
            if (it.type == LayerType.TEXT) it.copy(textStyle = it.textStyle?.copy(shadow = next))
            else it.copy(logoStyle = it.logoStyle?.copy(shadow = next))
          }
        }, onRecordUndo)
        StudioSlider("Shadow Opacity", shadow.opacity * 100f, 0f..100f, "${(shadow.opacity * 100).roundToInt()}%", { p ->
          val next = shadow.copy(opacity = p / 100f)
          onUpdateLayer {
            if (it.type == LayerType.TEXT) it.copy(textStyle = it.textStyle?.copy(shadow = next))
            else it.copy(logoStyle = it.logoStyle?.copy(shadow = next))
          }
        }, onRecordUndo)
      }
    }
  }

  if (glow != null) {
    InspectorCard(title = "OUTER GLOW") {
      StudioToggleRow("Enable Glow", glow.enabled) { en ->
        onRecordUndo()
        val next = glow.copy(enabled = en)
        onUpdateLayer {
          if (it.type == LayerType.TEXT) it.copy(textStyle = it.textStyle?.copy(glow = next))
          else it.copy(logoStyle = it.logoStyle?.copy(glow = next))
        }
      }
      if (glow.enabled) {
        ColorSwatchRow("Glow Color", glow.colorArgb) { onOpenColorPicker(ColorPickerTarget.GLOW) }
        StudioSlider("Glow Radius", glow.radius, 1f..48f, "${glow.radius.roundToInt()} px", { r ->
          val next = glow.copy(radius = r)
          onUpdateLayer {
            if (it.type == LayerType.TEXT) it.copy(textStyle = it.textStyle?.copy(glow = next))
            else it.copy(logoStyle = it.logoStyle?.copy(glow = next))
          }
        }, onRecordUndo)
        StudioSlider("Glow Opacity", glow.opacity * 100f, 0f..100f, "${(glow.opacity * 100).roundToInt()}%", { p ->
          val next = glow.copy(opacity = p / 100f)
          onUpdateLayer {
            if (it.type == LayerType.TEXT) it.copy(textStyle = it.textStyle?.copy(glow = next))
            else it.copy(logoStyle = it.logoStyle?.copy(glow = next))
          }
        }, onRecordUndo)
      }
    }
  }

  if (emboss != null) {
    InspectorCard(title = "EMBOSS RELIEF") {
      StudioToggleRow("Enable Emboss", emboss.enabled) { en ->
        onRecordUndo()
        val next = emboss.copy(enabled = en)
        onUpdateLayer {
          if (it.type == LayerType.TEXT) it.copy(textStyle = it.textStyle?.copy(emboss = next))
          else it.copy(logoStyle = it.logoStyle?.copy(emboss = next))
        }
      }
      if (emboss.enabled) {
        StudioSlider("Depth", emboss.depth, 0.5f..8f, "%.1f px".format(emboss.depth), { d ->
          val next = emboss.copy(depth = d)
          onUpdateLayer {
            if (it.type == LayerType.TEXT) it.copy(textStyle = it.textStyle?.copy(emboss = next))
            else it.copy(logoStyle = it.logoStyle?.copy(emboss = next))
          }
        }, onRecordUndo)
        StudioSlider("Light Angle", emboss.azimuthDeg, 0f..360f, "${emboss.azimuthDeg.roundToInt()}°", { a ->
          val next = emboss.copy(azimuthDeg = a)
          onUpdateLayer {
            if (it.type == LayerType.TEXT) it.copy(textStyle = it.textStyle?.copy(emboss = next))
            else it.copy(logoStyle = it.logoStyle?.copy(emboss = next))
          }
        }, onRecordUndo)
        StudioSlider("Emboss Opacity", emboss.opacity * 100f, 0f..100f, "${(emboss.opacity * 100).roundToInt()}%", { p ->
          val next = emboss.copy(opacity = p / 100f)
          onUpdateLayer {
            if (it.type == LayerType.TEXT) it.copy(textStyle = it.textStyle?.copy(emboss = next))
            else it.copy(logoStyle = it.logoStyle?.copy(emboss = next))
          }
        }, onRecordUndo)
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CompositingSection(
  layer: WatermarkLayer,
  onRecordUndo: () -> Unit,
  onUpdateLayer: ((WatermarkLayer) -> WatermarkLayer) -> Unit,
) {
  InspectorCard(title = "LAYER COMPOSITING") {
    StudioSlider(
      label = "Overall Layer Opacity",
      value = layer.opacity * 100f,
      range = 0f..100f,
      valueText = "${(layer.opacity * 100).roundToInt()}%",
      onValueChange = { p -> onUpdateLayer { it.copy(opacity = p / 100f) } },
      onFinished = onRecordUndo,
    )
  }

  InspectorCard(title = "BLEND MODE (REAL COMPOSITING)") {
    FlowRow(
      horizontalArrangement = Arrangement.spacedBy(5.dp),
      verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
      WatermarkBlendMode.entries.forEach { mode ->
        val active = layer.blendMode == mode
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (active) StudioAmber else StudioSurfaceElevated)
            .border(1.dp, if (active) StudioAmber else StudioBorder, RoundedCornerShape(6.dp))
            .clickable {
              onRecordUndo()
              onUpdateLayer { it.copy(blendMode = mode) }
            }
            .padding(horizontal = 8.dp, vertical = 5.dp)
            .testTag("blend_mode_${mode.name.lowercase()}"),
        ) {
          Text(
            text = mode.label,
            style = MaterialTheme.typography.labelSmall,
            color = if (active) StudioObsidian else StudioTextPrimary,
          )
        }
      }
    }
  }
}

@Composable
private fun PositioningSection(
  layer: WatermarkLayer,
  bounds: LayerRenderBounds?,
  activeAsset: ImageAsset?,
  onRecordUndo: () -> Unit,
  onUpdateLayer: ((WatermarkLayer) -> WatermarkLayer) -> Unit,
  onApplyAnchorPreset: (AnchorPreset) -> Unit,
) {
  val t = layer.transform
  val imgW = activeAsset?.width ?: 1000
  val imgH = activeAsset?.height ?: 1000

  InspectorCard(title = "9-POINT ALIGNMENT PRESETS") {
    Column(
      modifier = Modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(4.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      val rows = listOf(
        listOf(AnchorPreset.TOP_LEFT, AnchorPreset.TOP_CENTER, AnchorPreset.TOP_RIGHT),
        listOf(AnchorPreset.CENTER_LEFT, AnchorPreset.CENTER, AnchorPreset.CENTER_RIGHT),
        listOf(AnchorPreset.BOTTOM_LEFT, AnchorPreset.BOTTOM_CENTER, AnchorPreset.BOTTOM_RIGHT),
      )
      rows.forEach { row ->
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          row.forEach { preset ->
            val active = t.anchorPreset == preset
            Box(
              modifier = Modifier
                .weight(1f)
                .height(30.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(if (active) StudioAmber else StudioSurfaceElevated)
                .border(1.dp, if (active) StudioAmber else StudioBorder, RoundedCornerShape(5.dp))
                .clickable { onApplyAnchorPreset(preset) }
                .testTag("anchor_${preset.name.lowercase()}"),
              contentAlignment = Alignment.Center,
            ) {
              Box(
                modifier = Modifier
                  .size(7.dp)
                  .clip(CircleShape)
                  .background(if (active) StudioObsidian else StudioCyan),
              )
            }
          }
        }
      }
    }
    Spacer(Modifier.height(6.dp))
    StudioSlider(
      label = "Edge Margin",
      value = t.marginFraction * 100f,
      range = 0f..20f,
      valueText = "${(t.marginFraction * 100).roundToInt()}%",
      onValueChange = { m -> onUpdateLayer { it.copy(transform = it.transform.copy(marginFraction = m / 100f)) } },
      onFinished = onRecordUndo,
    )
  }

  InspectorCard(title = "EXACT COORDINATES & TRANSFORM") {
    val pxX = (t.xFraction * imgW).roundToInt()
    val pxY = (t.yFraction * imgH).roundToInt()
    val estW = (bounds?.width ?: 0f).roundToInt()
    val estH = (bounds?.height ?: 0f).roundToInt()

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(6.dp))
        .background(StudioObsidian)
        .padding(8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text("X: $pxX px", style = MaterialTheme.typography.labelSmall, color = StudioCyan)
      Text("Y: $pxY px", style = MaterialTheme.typography.labelSmall, color = StudioCyan)
      Text("W: $estW • H: $estH", style = MaterialTheme.typography.labelSmall, color = StudioTextSecondary)
    }

    Spacer(Modifier.height(6.dp))
    StudioSlider(
      label = "Position X",
      value = t.xFraction * 100f,
      range = 0f..100f,
      valueText = "%.1f%%".format(t.xFraction * 100f),
      onValueChange = { x -> onUpdateLayer { it.copy(transform = it.transform.copy(xFraction = x / 100f, anchorPreset = null)) } },
      onFinished = onRecordUndo,
    )
    StudioSlider(
      label = "Position Y",
      value = t.yFraction * 100f,
      range = 0f..100f,
      valueText = "%.1f%%".format(t.yFraction * 100f),
      onValueChange = { y -> onUpdateLayer { it.copy(transform = it.transform.copy(yFraction = y / 100f, anchorPreset = null)) } },
      onFinished = onRecordUndo,
    )
    StudioSlider(
      label = "Scale",
      value = t.scale,
      range = 0.2f..3.5f,
      valueText = "${(t.scale * 100).roundToInt()}%",
      onValueChange = { s ->
        onUpdateLayer {
          it.copy(
            transform = it.transform.copy(
              scale = s,
              scaleX = s,
              scaleY = if (it.transform.aspectRatioLocked) s else it.transform.scaleY,
            )
          )
        }
      },
      onFinished = onRecordUndo,
    )
    StudioSlider(
      label = "Rotation",
      value = t.rotationDeg,
      range = -180f..180f,
      valueText = "${t.rotationDeg.roundToInt()}°",
      onValueChange = { r -> onUpdateLayer { it.copy(transform = it.transform.copy(rotationDeg = r)) } },
      onFinished = onRecordUndo,
    )
  }
}

@Composable
private fun TilingSection(
  layer: WatermarkLayer,
  onRecordUndo: () -> Unit,
  onUpdateLayer: ((WatermarkLayer) -> WatermarkLayer) -> Unit,
) {
  val tile = layer.tile
  InspectorCard(title = "TILED PATTERN ENGINE") {
    StudioToggleRow(
      label = "Enable Repeated Pattern",
      checked = tile.enabled,
      onCheckedChange = { en ->
        onRecordUndo()
        onUpdateLayer { it.copy(tile = tile.copy(enabled = en)) }
      },
    )
    if (tile.enabled) {
      StudioToggleRow(
        label = "Stagger Alternate Rows",
        checked = tile.staggerRows,
        onCheckedChange = { st ->
          onRecordUndo()
          onUpdateLayer { it.copy(tile = tile.copy(staggerRows = st)) }
        },
      )
      StudioSlider(
        label = "Pattern Opacity",
        value = tile.opacity * 100f,
        range = 5f..100f,
        valueText = "${(tile.opacity * 100).roundToInt()}%",
        onValueChange = { p -> onUpdateLayer { it.copy(tile = tile.copy(opacity = p / 100f)) } },
        onFinished = onRecordUndo,
      )
      StudioSlider(
        label = "Tile Rotation",
        value = tile.rotationDeg,
        range = -90f..90f,
        valueText = "${tile.rotationDeg.roundToInt()}°",
        onValueChange = { r -> onUpdateLayer { it.copy(tile = tile.copy(rotationDeg = r)) } },
        onFinished = onRecordUndo,
      )
      StudioSlider(
        label = "Tile Scale",
        value = tile.scale,
        range = 0.25f..2.2f,
        valueText = "%.2f×".format(tile.scale),
        onValueChange = { s -> onUpdateLayer { it.copy(tile = tile.copy(scale = s)) } },
        onFinished = onRecordUndo,
      )
      StudioSlider(
        label = "Horizontal Spacing",
        value = tile.spacingX,
        range = 20f..480f,
        valueText = "${tile.spacingX.roundToInt()} px",
        onValueChange = { sx -> onUpdateLayer { it.copy(tile = tile.copy(spacingX = sx)) } },
        onFinished = onRecordUndo,
      )
      StudioSlider(
        label = "Vertical Spacing",
        value = tile.spacingY,
        range = 20f..480f,
        valueText = "${tile.spacingY.roundToInt()} px",
        onValueChange = { sy -> onUpdateLayer { it.copy(tile = tile.copy(spacingY = sy)) } },
        onFinished = onRecordUndo,
      )
    }
  }
}

@Composable
private fun InspectorCard(
  title: String,
  content: @Composable () -> Unit,
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(8.dp))
      .background(StudioSurface)
      .border(1.dp, StudioBorder, RoundedCornerShape(8.dp))
      .padding(10.dp),
  ) {
    Text(
      text = title,
      style = MaterialTheme.typography.labelSmall,
      color = StudioTextSecondary,
      fontWeight = FontWeight.Bold,
    )
    Spacer(Modifier.height(8.dp))
    content()
  }
}

@Composable
private fun StudioSlider(
  label: String,
  value: Float,
  range: ClosedFloatingPointRange<Float>,
  valueText: String,
  onValueChange: (Float) -> Unit,
  onFinished: () -> Unit,
) {
  Column(modifier = Modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(label, style = MaterialTheme.typography.bodySmall, color = StudioTextSecondary)
      Text(valueText, style = MaterialTheme.typography.labelSmall, color = StudioAmber)
    }
    Slider(
      value = value.coerceIn(range.start, range.endInclusive),
      onValueChange = onValueChange,
      onValueChangeFinished = onFinished,
      valueRange = range,
      colors = SliderDefaults.colors(
        thumbColor = StudioAmber,
        activeTrackColor = StudioAmber,
        inactiveTrackColor = StudioBorder,
      ),
      modifier = Modifier.height(30.dp),
    )
  }
}

@Composable
private fun StudioToggleRow(
  label: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(label, style = MaterialTheme.typography.bodySmall, color = StudioTextPrimary)
    Switch(
      checked = checked,
      onCheckedChange = onCheckedChange,
      colors = SwitchDefaults.colors(
        checkedThumbColor = StudioObsidian,
        checkedTrackColor = StudioAmber,
        uncheckedThumbColor = StudioTextMuted,
        uncheckedTrackColor = StudioSurfaceElevated,
      ),
    )
  }
}

@Composable
private fun ColorSwatchRow(
  label: String,
  colorArgb: Long,
  onClick: () -> Unit,
) {
  val color = Color(colorArgb.toInt())
  val hex = "#%06X".format(colorArgb.toInt() and 0xFFFFFF)
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp)
      .clip(RoundedCornerShape(6.dp))
      .background(StudioSurfaceElevated)
      .border(1.dp, StudioBorder, RoundedCornerShape(6.dp))
      .clickable(onClick = onClick)
      .padding(horizontal = 10.dp, vertical = 7.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(label, style = MaterialTheme.typography.bodySmall, color = StudioTextPrimary)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      Text(hex, style = MaterialTheme.typography.labelSmall, color = StudioTextSecondary)
      Box(
        modifier = Modifier
          .size(20.dp)
          .clip(RoundedCornerShape(4.dp))
          .background(color)
          .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(4.dp)),
      )
    }
  }
}
