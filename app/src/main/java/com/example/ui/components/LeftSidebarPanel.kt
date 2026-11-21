package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.model.ImageAsset
import com.example.model.LayerType
import com.example.model.LeftSidebarTab
import com.example.model.WatermarkLayer
import com.example.model.WatermarkPreset
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioAmberDark
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCoral
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.StudioPanel
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.StudioTextMuted
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import kotlin.math.roundToInt

@Composable
fun LeftSidebarPanel(
  activeTab: LeftSidebarTab,
  onTabChange: (LeftSidebarTab) -> Unit,
  images: List<ImageAsset>,
  activeImageId: String?,
  onSelectImage: (String) -> Unit,
  onRemoveImage: (String) -> Unit,
  onImportImages: () -> Unit,
  layers: List<WatermarkLayer>,
  selectedLayerId: String?,
  onSelectLayer: (String) -> Unit,
  onAddTextLayer: () -> Unit,
  onAddLogoLayer: () -> Unit,
  onToggleVisibility: (String) -> Unit,
  onToggleLock: (String) -> Unit,
  onDuplicateLayer: (String) -> Unit,
  onDeleteLayer: (String) -> Unit,
  onRenameLayer: (String, String) -> Unit,
  onReorderLayer: (Int, Int) -> Unit,
  builtInPresets: List<WatermarkPreset>,
  userPresets: List<WatermarkPreset>,
  onApplyPreset: (WatermarkPreset) -> Unit,
  onSaveCurrentPreset: () -> Unit,
  onDeleteUserPreset: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .fillMaxHeight()
      .background(StudioPanel),
  ) {
    // Segmented Sidebar Tab Header
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(8.dp)
        .clip(RoundedCornerShape(8.dp))
        .background(StudioSurface)
        .padding(3.dp),
      horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      LeftSidebarTab.entries.forEach { tab ->
        val selected = tab == activeTab
        Box(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) StudioSurfaceElevated else Color.Transparent)
            .border(
              width = if (selected) 1.dp else 0.dp,
              color = if (selected) StudioBorder else Color.Transparent,
              shape = RoundedCornerShape(6.dp),
            )
            .clickable { onTabChange(tab) }
            .padding(vertical = 7.dp)
            .testTag("left_tab_${tab.name.lowercase()}"),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = tab.label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) StudioAmber else StudioTextSecondary,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
          )
        }
      }
    }

    HorizontalDivider(color = StudioBorder)

    when (activeTab) {
      LeftSidebarTab.IMAGES -> ImagesListPane(
        images = images,
        activeImageId = activeImageId,
        onSelectImage = onSelectImage,
        onRemoveImage = onRemoveImage,
        onImportImages = onImportImages,
      )
      LeftSidebarTab.LAYERS -> LayersStackPane(
        layers = layers,
        selectedLayerId = selectedLayerId,
        onSelectLayer = onSelectLayer,
        onAddTextLayer = onAddTextLayer,
        onAddLogoLayer = onAddLogoLayer,
        onToggleVisibility = onToggleVisibility,
        onToggleLock = onToggleLock,
        onDuplicateLayer = onDuplicateLayer,
        onDeleteLayer = onDeleteLayer,
        onRenameLayer = onRenameLayer,
        onReorderLayer = onReorderLayer,
      )
      LeftSidebarTab.PRESETS -> PresetsListPane(
        builtInPresets = builtInPresets,
        userPresets = userPresets,
        onApplyPreset = onApplyPreset,
        onSaveCurrentPreset = onSaveCurrentPreset,
        onDeleteUserPreset = onDeleteUserPreset,
      )
    }
  }
}

@Composable
private fun ImagesListPane(
  images: List<ImageAsset>,
  activeImageId: String?,
  onSelectImage: (String) -> Unit,
  onRemoveImage: (String) -> Unit,
  onImportImages: () -> Unit,
) {
  val context = LocalContext.current
  Column(modifier = Modifier.fillMaxSize()) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 10.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = "${images.size} ASSETS",
        style = MaterialTheme.typography.labelSmall,
        color = StudioTextSecondary,
      )
      Row(
        modifier = Modifier
          .clip(RoundedCornerShape(6.dp))
          .background(StudioAmberDark)
          .border(1.dp, StudioAmber.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
          .clickable(onClick = onImportImages)
          .padding(horizontal = 8.dp, vertical = 4.dp)
          .testTag("sidebar_import_image_btn"),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(Icons.Default.Add, contentDescription = null, tint = StudioAmber, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(3.dp))
        Text("Add Photos", style = MaterialTheme.typography.labelSmall, color = StudioAmber)
      }
    }

    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      items(images, key = { it.id }) { asset ->
        val selected = asset.id == activeImageId
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) StudioSurfaceElevated else StudioSurface)
            .border(1.dp, if (selected) StudioAmber else StudioBorder, RoundedCornerShape(8.dp))
            .clickable { onSelectImage(asset.id) }
            .padding(8.dp)
            .testTag("asset_item_${asset.id}"),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Box(
            modifier = Modifier
              .size(48.dp)
              .clip(RoundedCornerShape(6.dp))
              .background(Color.Black),
          ) {
            if (asset.sampleDrawableRes != null) {
              Image(
                painter = painterResource(asset.sampleDrawableRes),
                contentDescription = asset.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
              )
            } else if (asset.uriString != null) {
              Image(
                painter = rememberAsyncImagePainter(
                  ImageRequest.Builder(context)
                    .data(asset.uriString)
                    .size(160)
                    .build()
                ),
                contentDescription = asset.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
              )
            }
          }
          Spacer(Modifier.width(8.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = asset.name,
              style = MaterialTheme.typography.bodyMedium,
              color = StudioTextPrimary,
              fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = "${asset.width}×${asset.height} • ${asset.formatLabel}",
              style = MaterialTheme.typography.labelSmall,
              color = StudioTextSecondary,
              maxLines = 1,
            )
          }
          if (images.size > 1) {
            IconButton(
              onClick = { onRemoveImage(asset.id) },
              modifier = Modifier.size(28.dp),
            ) {
              Icon(
                Icons.Default.Close,
                contentDescription = "Remove image",
                tint = StudioTextMuted,
                modifier = Modifier.size(15.dp),
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun LayersStackPane(
  layers: List<WatermarkLayer>,
  selectedLayerId: String?,
  onSelectLayer: (String) -> Unit,
  onAddTextLayer: () -> Unit,
  onAddLogoLayer: () -> Unit,
  onToggleVisibility: (String) -> Unit,
  onToggleLock: (String) -> Unit,
  onDuplicateLayer: (String) -> Unit,
  onDeleteLayer: (String) -> Unit,
  onRenameLayer: (String, String) -> Unit,
  onReorderLayer: (Int, Int) -> Unit,
) {
  var renameTarget by remember { mutableStateOf<WatermarkLayer?>(null) }
  var draggingIndex by remember { mutableIntStateOf(-1) }
  var dragOffsetY by remember { mutableFloatStateOf(0f) }

  // Display top-most layer at top of stack
  val displayLayers = remember(layers) { layers.reversed() }

  Column(modifier = Modifier.fillMaxSize()) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 10.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = "${layers.size} LAYERS",
        style = MaterialTheme.typography.labelSmall,
        color = StudioTextSecondary,
      )
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        SmallLayerBtn("+ Text", onAddTextLayer, "layers_add_text_btn")
        SmallLayerBtn("+ Logo", onAddLogoLayer, "layers_add_logo_btn")
      }
    }

    if (displayLayers.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(20.dp),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = "No watermark layers yet.\nTap + Text or + Logo above to begin.",
          style = MaterialTheme.typography.bodySmall,
          color = StudioTextSecondary,
        )
      }
    } else {
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        itemsIndexed(displayLayers, key = { _, l -> l.id }) { displayIdx, layer ->
          val realIndex = layers.size - 1 - displayIdx
          val selected = layer.id == selectedLayerId
          val isDragging = displayIdx == draggingIndex

          Column(
            modifier = Modifier
              .fillMaxWidth()
              .zIndex(if (isDragging) 10f else 0f)
              .offset { IntOffset(0, if (isDragging) dragOffsetY.roundToInt() else 0) }
              .clip(RoundedCornerShape(8.dp))
              .background(if (selected) StudioSurfaceElevated else StudioSurface)
              .border(
                width = 1.dp,
                color = when {
                  isDragging -> StudioCyan
                  selected -> StudioAmber
                  else -> StudioBorder
                },
                shape = RoundedCornerShape(8.dp),
              )
              .pointerInput(displayIdx, layers.size) {
                detectDragGesturesAfterLongPress(
                  onDragStart = {
                    draggingIndex = displayIdx
                    dragOffsetY = 0f
                  },
                  onDrag = { change, dragAmount ->
                    change.consume()
                    dragOffsetY += dragAmount.y
                    val rowPx = 76.dp.toPx()
                    if (dragOffsetY > rowPx && draggingIndex < displayLayers.lastIndex) {
                      val fromReal = layers.size - 1 - draggingIndex
                      val toReal = layers.size - 1 - (draggingIndex + 1)
                      onReorderLayer(fromReal, toReal)
                      draggingIndex += 1
                      dragOffsetY -= rowPx
                    } else if (dragOffsetY < -rowPx && draggingIndex > 0) {
                      val fromReal = layers.size - 1 - draggingIndex
                      val toReal = layers.size - 1 - (draggingIndex - 1)
                      onReorderLayer(fromReal, toReal)
                      draggingIndex -= 1
                      dragOffsetY += rowPx
                    }
                  },
                  onDragEnd = {
                    draggingIndex = -1
                    dragOffsetY = 0f
                  },
                  onDragCancel = {
                    draggingIndex = -1
                    dragOffsetY = 0f
                  },
                )
              }
              .clickable { onSelectLayer(layer.id) }
              .padding(horizontal = 8.dp, vertical = 7.dp)
              .testTag("layer_card_${layer.id}"),
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = "Long press to drag reorder",
                tint = StudioTextMuted,
                modifier = Modifier.size(16.dp),
              )
              Spacer(Modifier.width(6.dp))
              Icon(
                imageVector = when {
                  layer.tile.enabled -> Icons.Default.GridOn
                  layer.type == LayerType.TEXT -> Icons.Default.TextFields
                  else -> Icons.Default.Image
                },
                contentDescription = null,
                tint = if (selected) StudioAmber else StudioCyan,
                modifier = Modifier.size(16.dp),
              )
              Spacer(Modifier.width(6.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = layer.name,
                  style = MaterialTheme.typography.bodyMedium,
                  color = if (layer.visible) StudioTextPrimary else StudioTextMuted,
                  fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  text = "${layer.blendMode.label} • ${(layer.opacity * 100).roundToInt()}%",
                  style = MaterialTheme.typography.labelSmall,
                  color = StudioTextSecondary,
                  maxLines = 1,
                )
              }
              IconButton(
                onClick = { onToggleVisibility(layer.id) },
                modifier = Modifier.size(26.dp),
              ) {
                Icon(
                  imageVector = if (layer.visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                  contentDescription = "Toggle layer visibility",
                  tint = if (layer.visible) StudioTextPrimary else StudioTextMuted,
                  modifier = Modifier.size(15.dp),
                )
              }
              IconButton(
                onClick = { onToggleLock(layer.id) },
                modifier = Modifier.size(26.dp),
              ) {
                Icon(
                  imageVector = if (layer.locked) Icons.Default.Lock else Icons.Default.LockOpen,
                  contentDescription = "Toggle layer lock",
                  tint = if (layer.locked) StudioAmber else StudioTextMuted,
                  modifier = Modifier.size(15.dp),
                )
              }
            }

            if (selected) {
              Spacer(Modifier.height(6.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                  IconButton(
                    onClick = { if (realIndex < layers.lastIndex) onReorderLayer(realIndex, realIndex + 1) },
                    enabled = realIndex < layers.lastIndex,
                    modifier = Modifier.size(26.dp),
                  ) {
                    Icon(Icons.Default.ArrowUpward, "Move layer up", modifier = Modifier.size(14.dp))
                  }
                  IconButton(
                    onClick = { if (realIndex > 0) onReorderLayer(realIndex, realIndex - 1) },
                    enabled = realIndex > 0,
                    modifier = Modifier.size(26.dp),
                  ) {
                    Icon(Icons.Default.ArrowDownward, "Move layer down", modifier = Modifier.size(14.dp))
                  }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                  IconButton(
                    onClick = { renameTarget = layer },
                    modifier = Modifier.size(26.dp),
                  ) {
                    Icon(Icons.Default.Edit, "Rename layer", tint = StudioTextSecondary, modifier = Modifier.size(14.dp))
                  }
                  IconButton(
                    onClick = { onDuplicateLayer(layer.id) },
                    modifier = Modifier.size(26.dp),
                  ) {
                    Icon(Icons.Default.ContentCopy, "Duplicate layer", tint = StudioTextSecondary, modifier = Modifier.size(14.dp))
                  }
                  IconButton(
                    onClick = { onDeleteLayer(layer.id) },
                    modifier = Modifier.size(26.dp),
                  ) {
                    Icon(Icons.Default.Delete, "Delete layer", tint = StudioCoral, modifier = Modifier.size(14.dp))
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  renameTarget?.let { target ->
    var nameInput by remember(target.id) { mutableStateOf(target.name) }
    AlertDialog(
      onDismissRequest = { renameTarget = null },
      containerColor = StudioSurface,
      title = { Text("Rename Layer", style = MaterialTheme.typography.titleMedium) },
      text = {
        OutlinedTextField(
          value = nameInput,
          onValueChange = { nameInput = it },
          singleLine = true,
          label = { Text("Layer Name") },
        )
      },
      confirmButton = {
        TextButton(onClick = {
          onRenameLayer(target.id, nameInput)
          renameTarget = null
        }) { Text("Save", color = StudioAmber) }
      },
      dismissButton = {
        TextButton(onClick = { renameTarget = null }) { Text("Cancel") }
      },
    )
  }
}

@Composable
private fun PresetsListPane(
  builtInPresets: List<WatermarkPreset>,
  userPresets: List<WatermarkPreset>,
  onApplyPreset: (WatermarkPreset) -> Unit,
  onSaveCurrentPreset: () -> Unit,
  onDeleteUserPreset: (String) -> Unit,
) {
  Column(modifier = Modifier.fillMaxSize()) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 10.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = "STUDIO PRESETS",
        style = MaterialTheme.typography.labelSmall,
        color = StudioTextSecondary,
      )
      Row(
        modifier = Modifier
          .clip(RoundedCornerShape(6.dp))
          .background(StudioAmberDark)
          .border(1.dp, StudioAmber.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
          .clickable(onClick = onSaveCurrentPreset)
          .padding(horizontal = 8.dp, vertical = 4.dp)
          .testTag("save_preset_btn"),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(Icons.Default.BookmarkAdd, null, tint = StudioAmber, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text("Save Current", style = MaterialTheme.typography.labelSmall, color = StudioAmber)
      }
    }

    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      if (userPresets.isNotEmpty()) {
        item {
          Text(
            text = "CUSTOM PRESETS",
            style = MaterialTheme.typography.labelSmall,
            color = StudioCyan,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
          )
        }
        items(userPresets, key = { it.id }) { preset ->
          PresetCard(
            preset = preset,
            onApply = { onApplyPreset(preset) },
            onDelete = { onDeleteUserPreset(preset.id) },
          )
        }
      }

      item {
        Text(
          text = "BUILT-IN TEMPLATES",
          style = MaterialTheme.typography.labelSmall,
          color = StudioTextSecondary,
          modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
        )
      }
      items(builtInPresets, key = { it.id }) { preset ->
        PresetCard(
          preset = preset,
          onApply = { onApplyPreset(preset) },
          onDelete = null,
        )
      }
    }
  }
}

@Composable
private fun PresetCard(
  preset: WatermarkPreset,
  onApply: () -> Unit,
  onDelete: (() -> Unit)?,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(8.dp))
      .background(StudioSurface)
      .border(1.dp, StudioBorder, RoundedCornerShape(8.dp))
      .clickable(onClick = onApply)
      .padding(10.dp)
      .testTag("preset_card_${preset.id}"),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = preset.name,
          style = MaterialTheme.typography.bodyMedium,
          color = StudioTextPrimary,
          fontWeight = FontWeight.SemiBold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      Spacer(Modifier.height(2.dp))
      Text(
        text = preset.description,
        style = MaterialTheme.typography.bodySmall,
        color = StudioTextSecondary,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
      Spacer(Modifier.height(4.dp))
      Text(
        text = "${preset.category.uppercase()} • ${preset.layers.size} LAYER(S)",
        style = MaterialTheme.typography.labelSmall,
        color = StudioAmber,
      )
    }
    if (onDelete != null) {
      IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
        Icon(Icons.Default.Delete, "Delete preset", tint = StudioCoral, modifier = Modifier.size(16.dp))
      }
    }
  }
}

@Composable
private fun SmallLayerBtn(label: String, onClick: () -> Unit, tag: String) {
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(6.dp))
      .background(StudioSurface)
      .border(1.dp, StudioBorder, RoundedCornerShape(6.dp))
      .clickable(onClick = onClick)
      .padding(horizontal = 8.dp, vertical = 4.dp)
      .testTag(tag),
  ) {
    Text(label, style = MaterialTheme.typography.labelSmall, color = StudioTextPrimary)
  }
}
