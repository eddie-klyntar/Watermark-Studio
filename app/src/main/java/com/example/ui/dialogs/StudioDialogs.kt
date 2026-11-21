package com.example.ui.dialogs

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.SavedProjectSummary
import com.example.model.BatchJobProgress
import com.example.model.BatchSettings
import com.example.model.ExportFormat
import com.example.model.ExportSettings
import com.example.model.IccProfileMode
import com.example.model.ResizeMode
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCoral
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.StudioEmerald
import com.example.ui.theme.StudioObsidian
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import com.example.viewmodel.ExportPreviewState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HexRgbColorPickerDialog(
  initialColorArgb: Long,
  title: String,
  onDismiss: () -> Unit,
  onConfirm: (Long) -> Unit,
) {
  val initInt = initialColorArgb.toInt()
  var red by remember { mutableIntStateOf((initInt shr 16) and 0xFF) }
  var green by remember { mutableIntStateOf((initInt shr 8) and 0xFF) }
  var blue by remember { mutableIntStateOf(initInt and 0xFF) }
  var hexText by remember { mutableStateOf("%02X%02X%02X".format(red, green, blue)) }

  val currentArgb = (0xFFL shl 24) or (red.toLong() shl 16) or (green.toLong() shl 8) or blue.toLong()
  val swatchPalette = listOf(
    0xFFFFFFFFL, 0xFF000000L, 0xFFF59E0BL, 0xFF38BDF8L, 0xFFEF4444L,
    0xFF10B981L, 0xFFE2E8F0L, 0xFF1E293BL, 0xFFA855F7L, 0xFFF472B6L,
  )

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = StudioSurface,
    title = { Text(title, style = MaterialTheme.typography.titleMedium) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Box(
            modifier = Modifier
              .size(48.dp)
              .clip(RoundedCornerShape(8.dp))
              .background(Color(currentArgb.toInt()))
              .border(1.dp, StudioBorder, RoundedCornerShape(8.dp)),
          )
          OutlinedTextField(
            value = hexText,
            onValueChange = { input ->
              val clean = input.uppercase().filter { it in '0'..'9' || it in 'A'..'F' }.take(6)
              hexText = clean
              if (clean.length == 6) {
                val parsed = clean.toInt(16)
                red = (parsed shr 16) and 0xFF
                green = (parsed shr 8) and 0xFF
                blue = parsed and 0xFF
              }
            },
            prefix = { Text("#") },
            label = { Text("HEX") },
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
        }

        RgbChannelSlider("R", red, Color(0xFFEF4444)) {
          red = it
          hexText = "%02X%02X%02X".format(red, green, blue)
        }
        RgbChannelSlider("G", green, Color(0xFF22C55E)) {
          green = it
          hexText = "%02X%02X%02X".format(red, green, blue)
        }
        RgbChannelSlider("B", blue, Color(0xFF3B82F6)) {
          blue = it
          hexText = "%02X%02X%02X".format(red, green, blue)
        }

        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          swatchPalette.forEach { sw ->
            Box(
              modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(Color(sw.toInt()))
                .border(1.dp, StudioBorder, CircleShape)
                .clickable {
                  val c = sw.toInt()
                  red = (c shr 16) and 0xFF
                  green = (c shr 8) and 0xFF
                  blue = c and 0xFF
                  hexText = "%02X%02X%02X".format(red, green, blue)
                },
            )
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = { onConfirm(currentArgb) },
        colors = ButtonDefaults.buttonColors(containerColor = StudioAmber, contentColor = StudioObsidian),
      ) { Text("Apply Color") }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    },
  )
}

@Composable
private fun RgbChannelSlider(
  label: String,
  value: Int,
  color: Color,
  onChange: (Int) -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Text(label, style = MaterialTheme.typography.labelSmall, color = color, modifier = Modifier.width(16.dp))
    Slider(
      value = value.toFloat(),
      onValueChange = { onChange(it.roundToInt().coerceIn(0, 255)) },
      valueRange = 0f..255f,
      colors = SliderDefaults.colors(thumbColor = color, activeTrackColor = color),
      modifier = Modifier.weight(1f).height(28.dp),
    )
    Text("$value", style = MaterialTheme.typography.labelSmall, color = StudioTextSecondary, modifier = Modifier.width(30.dp))
  }
}

@Composable
fun ExportImageDialog(
  settings: ExportSettings,
  previewState: ExportPreviewState,
  onUpdateSettings: ((ExportSettings) -> ExportSettings) -> Unit,
  onConfirmExport: () -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = StudioSurface,
    title = { Text("Export Master Image", style = MaterialTheme.typography.titleMedium) },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(max = 480.dp)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        // Export Preview Box
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(145.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(StudioObsidian)
            .border(1.dp, StudioBorder, RoundedCornerShape(8.dp)),
          contentAlignment = Alignment.Center,
        ) {
          if (previewState.previewBitmap != null) {
            Image(
              bitmap = previewState.previewBitmap.asImageBitmap(),
              contentDescription = "Export Preview",
              contentScale = ContentScale.Fit,
              modifier = Modifier.fillMaxWidth().padding(6.dp),
            )
          }
          if (previewState.isGenerating) {
            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = StudioAmber)
          }
        }

        val sizeText = formatBytes(previewState.estimatedBytes)
        Text(
          text = "Output: ${previewState.outputWidth} × ${previewState.outputHeight} px  •  Est. $sizeText",
          style = MaterialTheme.typography.labelSmall,
          color = StudioCyan,
        )

        // Format Selection
        Text("Format", style = MaterialTheme.typography.labelMedium, color = StudioTextSecondary)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          ExportFormat.entries.forEach { fmt ->
            val active = settings.format == fmt
            Box(
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(6.dp))
                .background(if (active) StudioAmber else StudioSurfaceElevated)
                .border(1.dp, if (active) StudioAmber else StudioBorder, RoundedCornerShape(6.dp))
                .clickable { onUpdateSettings { it.copy(format = fmt) } }
                .padding(vertical = 7.dp)
                .testTag("export_format_${fmt.name.lowercase()}"),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                fmt.label,
                style = MaterialTheme.typography.labelSmall,
                color = if (active) StudioObsidian else StudioTextPrimary,
                fontWeight = FontWeight.Bold,
              )
            }
          }
        }

        if (settings.format == ExportFormat.WEBP) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text("WebP Lossless", style = MaterialTheme.typography.bodySmall, color = StudioTextPrimary)
            Switch(
              checked = settings.webpLossless,
              onCheckedChange = { ll -> onUpdateSettings { it.copy(webpLossless = ll) } },
            )
          }
        }

        if (settings.format == ExportFormat.JPEG || (settings.format == ExportFormat.WEBP && !settings.webpLossless)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text("Quality", style = MaterialTheme.typography.bodySmall, color = StudioTextSecondary)
            Text("${settings.quality}%", style = MaterialTheme.typography.labelSmall, color = StudioAmber)
          }
          Slider(
            value = settings.quality.toFloat(),
            onValueChange = { q -> onUpdateSettings { it.copy(quality = q.roundToInt()) } },
            valueRange = 20f..100f,
            colors = SliderDefaults.colors(thumbColor = StudioAmber, activeTrackColor = StudioAmber),
          )
        }

        // Resize mode
        Text("Dimensions", style = MaterialTheme.typography.labelMedium, color = StudioTextSecondary)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          ResizeMode.entries.forEach { rm ->
            val active = settings.resizeMode == rm
            Box(
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(6.dp))
                .background(if (active) StudioAmber else StudioSurfaceElevated)
                .border(1.dp, if (active) StudioAmber else StudioBorder, RoundedCornerShape(6.dp))
                .clickable { onUpdateSettings { it.copy(resizeMode = rm) } }
                .padding(vertical = 6.dp),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                rm.label,
                style = MaterialTheme.typography.labelSmall,
                color = if (active) StudioObsidian else StudioTextPrimary,
              )
            }
          }
        }

        if (settings.resizeMode == ResizeMode.SCALE_PERCENT) {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Scale Percent", style = MaterialTheme.typography.bodySmall, color = StudioTextSecondary)
            Text("${settings.scalePercent}%", style = MaterialTheme.typography.labelSmall, color = StudioAmber)
          }
          Slider(
            value = settings.scalePercent.toFloat(),
            onValueChange = { p -> onUpdateSettings { it.copy(scalePercent = p.roundToInt()) } },
            valueRange = 10f..150f,
          )
        } else if (settings.resizeMode == ResizeMode.FIT_LONG_EDGE) {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Max Long Edge", style = MaterialTheme.typography.bodySmall, color = StudioTextSecondary)
            Text("${settings.maxLongEdgePx} px", style = MaterialTheme.typography.labelSmall, color = StudioAmber)
          }
          Slider(
            value = settings.maxLongEdgePx.toFloat(),
            onValueChange = { px -> onUpdateSettings { it.copy(maxLongEdgePx = px.roundToInt()) } },
            valueRange = 480f..6000f,
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text("Preserve EXIF Metadata", style = MaterialTheme.typography.bodySmall, color = StudioTextPrimary)
          Switch(
            checked = settings.preserveMetadata,
            onCheckedChange = { p -> onUpdateSettings { it.copy(preserveMetadata = p) } },
          )
        }

        Text("Color Profile (ICC)", style = MaterialTheme.typography.labelMedium, color = StudioTextSecondary)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          IccProfileMode.entries.forEach { icc ->
            val active = settings.iccProfileMode == icc
            Box(
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(6.dp))
                .background(if (active) StudioAmber else StudioSurfaceElevated)
                .border(1.dp, if (active) StudioAmber else StudioBorder, RoundedCornerShape(6.dp))
                .clickable { onUpdateSettings { it.copy(iccProfileMode = icc) } }
                .padding(vertical = 6.dp),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                icc.label,
                style = MaterialTheme.typography.labelSmall,
                color = if (active) StudioObsidian else StudioTextPrimary,
              )
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = onConfirmExport,
        colors = ButtonDefaults.buttonColors(containerColor = StudioAmber, contentColor = StudioObsidian),
        modifier = Modifier.testTag("confirm_export_button"),
      ) { Text("Save ${settings.format.label}") }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    },
  )
}

@Composable
fun BatchProcessDialog(
  workspaceCount: Int,
  sourceFolderName: String?,
  sourceFolderCount: Int,
  destFolderName: String?,
  batchSettings: BatchSettings,
  progress: BatchJobProgress,
  onPickSourceFolder: () -> Unit,
  onClearSourceFolder: () -> Unit,
  onPickDestFolder: () -> Unit,
  onUpdateBatchSettings: ((BatchSettings) -> BatchSettings) -> Unit,
  onStartBatch: (Boolean) -> Unit,
  onCancelBatch: () -> Unit,
  onDismiss: () -> Unit,
) {
  var useFolderSource by remember(sourceFolderName) { mutableStateOf(sourceFolderName != null) }

  AlertDialog(
    onDismissRequest = { if (!progress.isRunning) onDismiss() },
    containerColor = StudioSurface,
    title = { Text("Batch Watermark Processing", style = MaterialTheme.typography.titleMedium) },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(max = 480.dp)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        // Source Selection
        Text("Source Images", style = MaterialTheme.typography.labelMedium, color = StudioTextSecondary)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          Box(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(6.dp))
              .background(if (!useFolderSource) StudioAmber else StudioSurfaceElevated)
              .border(1.dp, StudioBorder, RoundedCornerShape(6.dp))
              .clickable { useFolderSource = false; onClearSourceFolder() }
              .padding(8.dp),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              "Workspace ($workspaceCount)",
              style = MaterialTheme.typography.labelSmall,
              color = if (!useFolderSource) StudioObsidian else StudioTextPrimary,
            )
          }
          Box(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(6.dp))
              .background(if (useFolderSource) StudioAmber else StudioSurfaceElevated)
              .border(1.dp, StudioBorder, RoundedCornerShape(6.dp))
              .clickable { useFolderSource = true; onPickSourceFolder() }
              .padding(8.dp),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              if (sourceFolderName != null) "$sourceFolderName ($sourceFolderCount)" else "Pick Folder...",
              style = MaterialTheme.typography.labelSmall,
              color = if (useFolderSource) StudioObsidian else StudioTextPrimary,
            )
          }
        }

        // Destination Folder
        Text("Destination Folder", style = MaterialTheme.typography.labelMedium, color = StudioTextSecondary)
        OutlinedButton(
          onClick = onPickDestFolder,
          modifier = Modifier.fillMaxWidth().testTag("batch_pick_dest_btn"),
        ) {
          Icon(Icons.Default.FolderOpen, null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(6.dp))
          Text(destFolderName ?: "Select Output Folder...")
        }

        // Naming template
        OutlinedTextField(
          value = batchSettings.namingTemplate,
          onValueChange = { tpl -> onUpdateBatchSettings { it.copy(namingTemplate = tpl) } },
          label = { Text("Naming Template ({name}, {index}, {date})") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )

        // Format selector
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          ExportFormat.entries.forEach { fmt ->
            val active = batchSettings.exportSettings.format == fmt
            Box(
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(6.dp))
                .background(if (active) StudioAmber else StudioSurfaceElevated)
                .border(1.dp, StudioBorder, RoundedCornerShape(6.dp))
                .clickable {
                  onUpdateBatchSettings { it.copy(exportSettings = it.exportSettings.copy(format = fmt)) }
                }
                .padding(vertical = 6.dp),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                fmt.label,
                style = MaterialTheme.typography.labelSmall,
                color = if (active) StudioObsidian else StudioTextPrimary,
              )
            }
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text("Auto-Scale to Image Size", style = MaterialTheme.typography.bodySmall, color = StudioTextPrimary)
            Text(
              "Proportionally scales watermarks across mixed resolutions",
              style = MaterialTheme.typography.labelSmall,
              color = StudioTextSecondary,
            )
          }
          Switch(
            checked = batchSettings.autoScaleToImage,
            onCheckedChange = { a -> onUpdateBatchSettings { it.copy(autoScaleToImage = a) } },
          )
        }

        // Progress & Error Reporting
        if (progress.isRunning || progress.isFinished) {
          HorizontalDivider(color = StudioBorder)
          val frac = if (progress.total > 0) progress.completed.toFloat() / progress.total.toFloat() else 0f
          LinearProgressIndicator(
            progress = { frac },
            color = StudioAmber,
            trackColor = StudioSurfaceElevated,
            modifier = Modifier.fillMaxWidth(),
          )
          Text(
            text = if (progress.isRunning) {
              "Processing ${progress.completed + 1}/${progress.total}: ${progress.currentFileName}"
            } else {
              "Completed: ${progress.succeeded} succeeded, ${progress.failed} failed"
            },
            style = MaterialTheme.typography.labelSmall,
            color = if (progress.failed > 0) StudioAmber else StudioEmerald,
          )

          if (progress.errors.isNotEmpty()) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(StudioObsidian)
                .padding(8.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              progress.errors.forEach { err ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(Icons.Default.Error, null, tint = StudioCoral, modifier = Modifier.size(14.dp))
                  Spacer(Modifier.width(4.dp))
                  Text(
                    "${err.fileName}: ${err.reason}",
                    style = MaterialTheme.typography.labelSmall,
                    color = StudioCoral,
                  )
                }
              }
            }
          } else if (progress.isFinished && !progress.cancelled) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.CheckCircle, null, tint = StudioEmerald, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(6.dp))
              Text("All files exported cleanly.", style = MaterialTheme.typography.bodySmall, color = StudioEmerald)
            }
          }
        }
      }
    },
    confirmButton = {
      if (progress.isRunning) {
        Button(
          onClick = onCancelBatch,
          colors = ButtonDefaults.buttonColors(containerColor = StudioCoral, contentColor = StudioObsidian),
        ) { Text("Cancel Job") }
      } else {
        Button(
          onClick = { onStartBatch(useFolderSource) },
          enabled = destFolderName != null,
          colors = ButtonDefaults.buttonColors(containerColor = StudioAmber, contentColor = StudioObsidian),
          modifier = Modifier.testTag("batch_start_button"),
        ) { Text("Start Batch") }
      }
    },
    dismissButton = {
      if (!progress.isRunning) {
        TextButton(onClick = onDismiss) { Text("Close") }
      }
    },
  )
}

@Composable
fun ProjectsDialog(
  currentProjectName: String,
  savedProjects: List<SavedProjectSummary>,
  onSaveInternal: (String) -> Unit,
  onExportProjectFile: (String) -> Unit,
  onImportProjectFile: () -> Unit,
  onLoadInternal: (String) -> Unit,
  onDeleteInternal: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  var nameInput by remember(currentProjectName) { mutableStateOf(currentProjectName) }
  val fmt = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = StudioSurface,
    title = { Text("Watermark Studio Projects (.wmstudio)", style = MaterialTheme.typography.titleMedium) },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedTextField(
          value = nameInput,
          onValueChange = { nameInput = it },
          label = { Text("Project Name") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Button(
            onClick = { onSaveInternal(nameInput) },
            colors = ButtonDefaults.buttonColors(containerColor = StudioAmber, contentColor = StudioObsidian),
            modifier = Modifier.weight(1f),
          ) {
            Icon(Icons.Default.Save, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Quick Save")
          }
          OutlinedButton(
            onClick = { onExportProjectFile(nameInput) },
            modifier = Modifier.weight(1f),
          ) {
            Text("Export .wmstudio")
          }
        }

        OutlinedButton(
          onClick = onImportProjectFile,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Icon(Icons.Default.FolderOpen, null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(6.dp))
          Text("Open .wmstudio File...")
        }

        if (savedProjects.isNotEmpty()) {
          HorizontalDivider(color = StudioBorder)
          Text("Saved Studio Projects", style = MaterialTheme.typography.labelMedium, color = StudioTextSecondary)
          LazyColumn(
            modifier = Modifier.heightIn(max = 180.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            items(savedProjects, key = { it.fileName }) { proj ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(6.dp))
                  .background(StudioSurfaceElevated)
                  .border(1.dp, StudioBorder, RoundedCornerShape(6.dp))
                  .clickable { onLoadInternal(proj.fileName) }
                  .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(proj.projectName, style = MaterialTheme.typography.bodyMedium, color = StudioTextPrimary)
                  Text(
                    "${proj.layerCount} layer(s) • ${fmt.format(Date(proj.savedAtMillis))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = StudioTextSecondary,
                  )
                }
                IconButton(onClick = { onDeleteInternal(proj.fileName) }, modifier = Modifier.size(28.dp)) {
                  Icon(Icons.Default.Delete, "Delete project", tint = StudioCoral, modifier = Modifier.size(16.dp))
                }
              }
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) { Text("Done") }
    },
  )
}

@Composable
fun SavePresetDialog(
  onSave: (String, String) -> Unit,
  onDismiss: () -> Unit,
) {
  var name by remember { mutableStateOf("") }
  var description by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = StudioSurface,
    title = { Text("Save Watermark Preset", style = MaterialTheme.typography.titleMedium) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Preset Name") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = description,
          onValueChange = { description = it },
          label = { Text("Description (optional)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onSave(name.ifBlank { "Custom Watermark" }, description) },
        colors = ButtonDefaults.buttonColors(containerColor = StudioAmber, contentColor = StudioObsidian),
      ) { Text("Save Preset") }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    },
  )
}

private fun formatBytes(bytes: Long): String {
  if (bytes <= 0L) return "--"
  return when {
    bytes < 1024L -> "$bytes B"
    bytes < 1024L * 1024L -> "${(bytes / 1024.0).roundToInt()} KB"
    else -> "%.2f MB".format(bytes / (1024.0 * 1024.0))
  }
}
