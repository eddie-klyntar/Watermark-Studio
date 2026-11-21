package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.LayerType
import com.example.ui.components.CentralCanvasWorkspace
import com.example.ui.components.LeftSidebarPanel
import com.example.ui.components.PropertiesInspectorPanel
import com.example.ui.components.StudioStatusBar
import com.example.ui.components.StudioTopToolbar
import com.example.ui.dialogs.BatchProcessDialog
import com.example.ui.dialogs.ExportImageDialog
import com.example.ui.dialogs.HexRgbColorPickerDialog
import com.example.ui.dialogs.ProjectsDialog
import com.example.ui.dialogs.SavePresetDialog
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioObsidian
import com.example.ui.theme.StudioPanel
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import com.example.viewmodel.ColorPickerTarget
import com.example.viewmodel.StudioViewModel

private enum class CompactDockMode(val label: String) {
  WORKSPACE("Assets & Layers"),
  INSPECTOR("Properties Inspector"),
}

@Composable
fun WatermarkStudioScreen(
  viewModel: StudioViewModel = viewModel(),
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  val activeAsset = remember(state.images, state.activeImageId) {
    state.images.find { it.id == state.activeImageId }
  }
  val selectedLayer = remember(state.layers, state.selectedLayerId) {
    state.layers.find { it.id == state.selectedLayerId }
  }
  val selectedBounds = remember(state.layerBounds, state.selectedLayerId) {
    state.layerBounds.find { it.layerId == state.selectedLayerId }
  }

  var pendingExportProjectName by remember { mutableStateOf("WatermarkProject") }
  var compactDockMode by remember { mutableStateOf(CompactDockMode.INSPECTOR) }

  // Zero-permission Photo Picker for photos
  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickMultipleVisualMedia(),
  ) { uris: List<Uri> ->
    if (uris.isNotEmpty()) viewModel.importImages(uris)
  }

  // Document picker for TIFF, BMP, PNG, JPG, WebP
  val docImagesLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenMultipleDocuments(),
  ) { uris: List<Uri> ->
    if (uris.isNotEmpty()) viewModel.importImages(uris)
  }

  // Custom Font (.ttf / .otf) picker
  val fontPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument(),
  ) { uri: Uri? ->
    uri?.let { viewModel.importCustomFont(it) }
  }

  // Custom Logo (PNG / SVG / JPG / WebP) picker
  val logoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument(),
  ) { uri: Uri? ->
    uri?.let { viewModel.importCustomLogo(it) }
  }

  // Batch source folder picker
  val batchSourceFolderLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocumentTree(),
  ) { uri: Uri? ->
    uri?.let { viewModel.setBatchSourceFolder(it) }
  }

  // Batch destination folder picker
  val batchDestFolderLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocumentTree(),
  ) { uri: Uri? ->
    uri?.let { viewModel.setBatchDestinationFolder(it) }
  }

  // Single image export CreateDocument launcher
  val exportImageLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument(state.exportSettings.format.mimeType),
  ) { uri: Uri? ->
    uri?.let { viewModel.exportActiveImageToUri(it) }
  }

  // Export .wmstudio project file launcher
  val exportProjectLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("application/octet-stream"),
  ) { uri: Uri? ->
    uri?.let { viewModel.exportProjectFile(it, pendingExportProjectName) }
  }

  // Open .wmstudio project file launcher
  val openProjectLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument(),
  ) { uri: Uri? ->
    uri?.let { viewModel.loadProjectFromUri(it) }
  }

  val launchPhotoImport = {
    photoPickerLauncher.launch(
      PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
    )
  }

  val launchDocImageImport = {
    docImagesLauncher.launch(arrayOf("image/*", "image/tiff", "image/x-ms-bmp"))
  }

  val launchFontImport = {
    fontPickerLauncher.launch(arrayOf("font/*", "application/x-font-ttf", "application/x-font-opentype", "*/*"))
  }

  val launchLogoImport = {
    logoPickerLauncher.launch(arrayOf("image/*", "image/svg+xml"))
  }

  BoxWithConstraints(
    modifier = Modifier
      .fillMaxSize()
      .background(StudioObsidian)
      .statusBarsPadding(),
  ) {
    val isExpanded = maxWidth >= 840.dp

    Column(modifier = Modifier.fillMaxSize()) {
      StudioTopToolbar(
        projectName = state.projectName,
        isExpanded = isExpanded,
        canUndo = state.canUndo,
        canRedo = state.canRedo,
        onImportImages = launchPhotoImport,
        onImportDocumentImages = launchDocImageImport,
        onOpenProjects = { viewModel.setShowProjectDialog(true) },
        onUndo = { viewModel.undo() },
        onRedo = { viewModel.redo() },
        onAddText = { viewModel.addTextLayer() },
        onAddLogo = { viewModel.addLogoLayer() },
        onOpenBatch = { viewModel.openBatchDialog() },
        onOpenExport = { viewModel.openExportDialog() },
      )

      HorizontalDivider(color = StudioBorder)

      if (isExpanded) {
        // Desktop / Tablet Three-Region Creative Studio Layout
        Row(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth(),
        ) {
          LeftSidebarPanel(
            activeTab = state.leftSidebarTab,
            onTabChange = viewModel::setLeftSidebarTab,
            images = state.images,
            activeImageId = state.activeImageId,
            onSelectImage = viewModel::selectImage,
            onRemoveImage = viewModel::removeImage,
            onImportImages = launchPhotoImport,
            layers = state.layers,
            selectedLayerId = state.selectedLayerId,
            onSelectLayer = viewModel::selectLayer,
            onAddTextLayer = viewModel::addTextLayer,
            onAddLogoLayer = { viewModel.addLogoLayer() },
            onToggleVisibility = viewModel::toggleLayerVisibility,
            onToggleLock = viewModel::toggleLayerLock,
            onDuplicateLayer = viewModel::duplicateLayer,
            onDeleteLayer = viewModel::deleteLayer,
            onRenameLayer = viewModel::renameLayer,
            onReorderLayer = viewModel::reorderLayer,
            builtInPresets = state.builtInPresets,
            userPresets = state.userPresets,
            onApplyPreset = viewModel::applyPreset,
            onSaveCurrentPreset = { viewModel.setShowSavePresetDialog(true) },
            onDeleteUserPreset = viewModel::deleteUserPreset,
            modifier = Modifier.width(256.dp),
          )

          VerticalDivider(color = StudioBorder)

          CentralCanvasWorkspace(
            renderedPreview = state.renderedPreview,
            layerBounds = state.layerBounds,
            selectedLayer = selectedLayer,
            isRendering = state.isRendering,
            zoomModeFit = state.zoomModeFit,
            manualZoom = state.manualZoom,
            panOffset = state.panOffset,
            showCheckerboard = state.showCheckerboard,
            showGridGuides = state.showGridGuides,
            snapToGuides = state.snapToGuides,
            snapGuideState = state.snapGuideState,
            onSelectAtPoint = viewModel::selectLayerAtCanvasPoint,
            onStartLayerEdit = viewModel::recordUndoPoint,
            onDragSelectedLayer = viewModel::dragSelectedLayerBy,
            onClearSnapGuides = viewModel::clearSnapGuides,
            onScaleSelectedLayer = { factor ->
              viewModel.updateSelectedLayer(recordUndo = false) { layer ->
                val t = layer.transform
                val nextScale = (t.scale * factor).coerceIn(0.2f, 4f)
                layer.copy(
                  transform = t.copy(
                    scale = nextScale,
                    scaleX = (t.scaleX * factor).coerceIn(0.2f, 4f),
                    scaleY = if (t.aspectRatioLocked) (t.scaleX * factor).coerceIn(0.2f, 4f)
                    else (t.scaleY * factor).coerceIn(0.2f, 4f),
                  )
                )
              }
            },
            onRotateSelectedLayer = { deltaDeg ->
              viewModel.updateSelectedLayer(recordUndo = false) { layer ->
                var deg = layer.transform.rotationDeg + deltaDeg
                while (deg > 180f) deg -= 360f
                while (deg < -180f) deg += 360f
                layer.copy(transform = layer.transform.copy(rotationDeg = deg))
              }
            },
            onZoomFit = viewModel::setZoomFit,
            onZoomActual = viewModel::setZoomActualSize,
            onZoomStep = viewModel::adjustZoom,
            onZoomAndPan = viewModel::setZoomAndPan,
            onToggleCheckerboard = viewModel::toggleCheckerboard,
            onToggleGridGuides = viewModel::toggleGridGuides,
            onToggleSnapToGuides = viewModel::toggleSnapToGuides,
            onDropImageUris = viewModel::importImages,
            modifier = Modifier
              .weight(1f)
              .fillMaxHeight(),
          )

          VerticalDivider(color = StudioBorder)

          PropertiesInspectorPanel(
            selectedLayer = selectedLayer,
            selectedBounds = selectedBounds,
            activeAsset = activeAsset,
            activeTab = state.inspectorTab,
            onTabChange = viewModel::setInspectorTab,
            availableFonts = state.availableFonts,
            availableLogos = state.availableLogos,
            onImportFont = launchFontImport,
            onImportLogo = launchLogoImport,
            onRecordUndo = viewModel::recordUndoPoint,
            onUpdateLayer = { transform -> viewModel.updateSelectedLayer(recordUndo = false, transform = transform) },
            onApplyAnchorPreset = viewModel::applyAnchorPreset,
            onOpenColorPicker = viewModel::openColorPicker,
            modifier = Modifier.width(308.dp),
          )
        }
      } else {
        // Handheld / Narrow Responsive Studio Layout
        Column(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth(),
        ) {
          CentralCanvasWorkspace(
            renderedPreview = state.renderedPreview,
            layerBounds = state.layerBounds,
            selectedLayer = selectedLayer,
            isRendering = state.isRendering,
            zoomModeFit = state.zoomModeFit,
            manualZoom = state.manualZoom,
            panOffset = state.panOffset,
            showCheckerboard = state.showCheckerboard,
            showGridGuides = state.showGridGuides,
            snapToGuides = state.snapToGuides,
            snapGuideState = state.snapGuideState,
            onSelectAtPoint = viewModel::selectLayerAtCanvasPoint,
            onStartLayerEdit = viewModel::recordUndoPoint,
            onDragSelectedLayer = viewModel::dragSelectedLayerBy,
            onClearSnapGuides = viewModel::clearSnapGuides,
            onScaleSelectedLayer = { factor ->
              viewModel.updateSelectedLayer(recordUndo = false) { layer ->
                val t = layer.transform
                val nextScale = (t.scale * factor).coerceIn(0.2f, 4f)
                layer.copy(
                  transform = t.copy(
                    scale = nextScale,
                    scaleX = (t.scaleX * factor).coerceIn(0.2f, 4f),
                    scaleY = if (t.aspectRatioLocked) (t.scaleX * factor).coerceIn(0.2f, 4f)
                    else (t.scaleY * factor).coerceIn(0.2f, 4f),
                  )
                )
              }
            },
            onRotateSelectedLayer = { deltaDeg ->
              viewModel.updateSelectedLayer(recordUndo = false) { layer ->
                var deg = layer.transform.rotationDeg + deltaDeg
                while (deg > 180f) deg -= 360f
                while (deg < -180f) deg += 360f
                layer.copy(transform = layer.transform.copy(rotationDeg = deg))
              }
            },
            onZoomFit = viewModel::setZoomFit,
            onZoomActual = viewModel::setZoomActualSize,
            onZoomStep = viewModel::adjustZoom,
            onZoomAndPan = viewModel::setZoomAndPan,
            onToggleCheckerboard = viewModel::toggleCheckerboard,
            onToggleGridGuides = viewModel::toggleGridGuides,
            onToggleSnapToGuides = viewModel::toggleSnapToGuides,
            onDropImageUris = viewModel::importImages,
            modifier = Modifier
              .weight(1f)
              .fillMaxWidth(),
          )

          HorizontalDivider(color = StudioBorder)

          // Dock Mode Switcher
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .background(StudioPanel)
              .padding(horizontal = 8.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            CompactDockMode.entries.forEach { mode ->
              val active = compactDockMode == mode
              Box(
                modifier = Modifier
                  .weight(1f)
                  .clip(RoundedCornerShape(6.dp))
                  .background(if (active) StudioSurfaceElevated else StudioSurface)
                  .border(1.dp, if (active) StudioAmber else StudioBorder, RoundedCornerShape(6.dp))
                  .clickable { compactDockMode = mode }
                  .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center,
              ) {
                Text(
                  text = mode.label,
                  style = MaterialTheme.typography.labelSmall,
                  color = if (active) StudioAmber else StudioTextSecondary,
                  fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                )
              }
            }
          }

          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(270.dp),
          ) {
            if (compactDockMode == CompactDockMode.WORKSPACE) {
              LeftSidebarPanel(
                activeTab = state.leftSidebarTab,
                onTabChange = viewModel::setLeftSidebarTab,
                images = state.images,
                activeImageId = state.activeImageId,
                onSelectImage = viewModel::selectImage,
                onRemoveImage = viewModel::removeImage,
                onImportImages = launchPhotoImport,
                layers = state.layers,
                selectedLayerId = state.selectedLayerId,
                onSelectLayer = viewModel::selectLayer,
                onAddTextLayer = viewModel::addTextLayer,
                onAddLogoLayer = { viewModel.addLogoLayer() },
                onToggleVisibility = viewModel::toggleLayerVisibility,
                onToggleLock = viewModel::toggleLayerLock,
                onDuplicateLayer = viewModel::duplicateLayer,
                onDeleteLayer = viewModel::deleteLayer,
                onRenameLayer = viewModel::renameLayer,
                onReorderLayer = viewModel::reorderLayer,
                builtInPresets = state.builtInPresets,
                userPresets = state.userPresets,
                onApplyPreset = viewModel::applyPreset,
                onSaveCurrentPreset = { viewModel.setShowSavePresetDialog(true) },
                onDeleteUserPreset = viewModel::deleteUserPreset,
                modifier = Modifier.fillMaxSize(),
              )
            } else {
              PropertiesInspectorPanel(
                selectedLayer = selectedLayer,
                selectedBounds = selectedBounds,
                activeAsset = activeAsset,
                activeTab = state.inspectorTab,
                onTabChange = viewModel::setInspectorTab,
                availableFonts = state.availableFonts,
                availableLogos = state.availableLogos,
                onImportFont = launchFontImport,
                onImportLogo = launchLogoImport,
                onRecordUndo = viewModel::recordUndoPoint,
                onUpdateLayer = { transform -> viewModel.updateSelectedLayer(recordUndo = false, transform = transform) },
                onApplyAnchorPreset = viewModel::applyAnchorPreset,
                onOpenColorPicker = viewModel::openColorPicker,
                modifier = Modifier.fillMaxSize(),
              )
            }
          }
        }
      }

      HorizontalDivider(color = StudioBorder)

      StudioStatusBar(
        activeAsset = activeAsset,
        selectedLayer = selectedLayer,
        zoomModeFit = state.zoomModeFit,
        manualZoom = state.manualZoom,
        statusMessage = state.statusMessage,
      )
    }
  }

  // Color Picker Dialog
  state.showColorPickerFor?.let { target ->
    val layer = selectedLayer
    if (layer != null) {
      val initialColor = when (target) {
        ColorPickerTarget.TEXT_FILL -> layer.textStyle?.fillColorArgb ?: 0xFFFFFFFFL
        ColorPickerTarget.TEXT_GRADIENT_END -> layer.textStyle?.gradientEndColorArgb ?: 0xFFF59E0BL
        ColorPickerTarget.STROKE -> layer.textStyle?.stroke?.colorArgb ?: layer.logoStyle?.stroke?.colorArgb ?: 0xFF000000L
        ColorPickerTarget.SHADOW -> layer.textStyle?.shadow?.colorArgb ?: layer.logoStyle?.shadow?.colorArgb ?: 0xFF000000L
        ColorPickerTarget.GLOW -> layer.textStyle?.glow?.colorArgb ?: layer.logoStyle?.glow?.colorArgb ?: 0xFF38BDF8L
        ColorPickerTarget.BACKGROUND_HIGHLIGHT -> layer.textStyle?.background?.colorArgb ?: 0xFF0D0F14L
        ColorPickerTarget.LOGO_TINT -> layer.logoStyle?.tintColorArgb ?: 0xFFFFFFFFL
      }
      val title = when (target) {
        ColorPickerTarget.TEXT_FILL -> "Text Fill Color"
        ColorPickerTarget.TEXT_GRADIENT_END -> "Gradient End Color"
        ColorPickerTarget.STROKE -> "Outline Color"
        ColorPickerTarget.SHADOW -> "Drop Shadow Color"
        ColorPickerTarget.GLOW -> "Outer Glow Color"
        ColorPickerTarget.BACKGROUND_HIGHLIGHT -> "Background Highlight Color"
        ColorPickerTarget.LOGO_TINT -> "Logo Tint Color"
      }
      HexRgbColorPickerDialog(
        initialColorArgb = initialColor,
        title = title,
        onDismiss = { viewModel.openColorPicker(null) },
        onConfirm = { argb ->
          viewModel.updateSelectedLayer(recordUndo = true) { l ->
            when (target) {
              ColorPickerTarget.TEXT_FILL ->
                l.copy(textStyle = l.textStyle?.copy(fillColorArgb = argb))
              ColorPickerTarget.TEXT_GRADIENT_END ->
                l.copy(textStyle = l.textStyle?.copy(gradientEndColorArgb = argb))
              ColorPickerTarget.STROKE ->
                if (l.type == LayerType.TEXT) l.copy(textStyle = l.textStyle?.copy(stroke = l.textStyle.stroke.copy(colorArgb = argb)))
                else l.copy(logoStyle = l.logoStyle?.copy(stroke = l.logoStyle.stroke.copy(colorArgb = argb)))
              ColorPickerTarget.SHADOW ->
                if (l.type == LayerType.TEXT) l.copy(textStyle = l.textStyle?.copy(shadow = l.textStyle.shadow.copy(colorArgb = argb)))
                else l.copy(logoStyle = l.logoStyle?.copy(shadow = l.logoStyle.shadow.copy(colorArgb = argb)))
              ColorPickerTarget.GLOW ->
                if (l.type == LayerType.TEXT) l.copy(textStyle = l.textStyle?.copy(glow = l.textStyle.glow.copy(colorArgb = argb)))
                else l.copy(logoStyle = l.logoStyle?.copy(glow = l.logoStyle.glow.copy(colorArgb = argb)))
              ColorPickerTarget.BACKGROUND_HIGHLIGHT ->
                l.copy(textStyle = l.textStyle?.copy(background = l.textStyle.background.copy(colorArgb = argb)))
              ColorPickerTarget.LOGO_TINT ->
                l.copy(logoStyle = l.logoStyle?.copy(tintColorArgb = argb))
            }
          }
          viewModel.openColorPicker(null)
        },
      )
    }
  }

  // Export Dialog
  if (state.showExportDialog) {
    ExportImageDialog(
      settings = state.exportSettings,
      previewState = state.exportPreview,
      onUpdateSettings = viewModel::updateExportSettings,
      onConfirmExport = {
        val base = activeAsset?.name?.substringBeforeLast('.') ?: "watermarked"
        val ext = state.exportSettings.format.extension
        exportImageLauncher.launch("${base}_watermarked.$ext")
      },
      onDismiss = viewModel::closeExportDialog,
    )
  }

  // Batch Dialog
  if (state.showBatchDialog) {
    BatchProcessDialog(
      workspaceCount = state.images.size,
      sourceFolderName = state.batchSourceFolderName,
      sourceFolderCount = state.batchSourceFolderCount,
      destFolderName = state.batchDestFolderName,
      batchSettings = state.batchSettings,
      progress = state.batchProgress,
      onPickSourceFolder = { batchSourceFolderLauncher.launch(null) },
      onClearSourceFolder = viewModel::clearBatchSourceFolder,
      onPickDestFolder = { batchDestFolderLauncher.launch(null) },
      onUpdateBatchSettings = viewModel::updateBatchSettings,
      onStartBatch = viewModel::startBatchProcessing,
      onCancelBatch = viewModel::cancelBatchProcessing,
      onDismiss = {
        viewModel.resetBatchProgress()
        viewModel.closeBatchDialog()
      },
    )
  }

  // Projects Dialog
  if (state.showProjectDialog) {
    ProjectsDialog(
      currentProjectName = state.projectName,
      savedProjects = state.savedProjects,
      onSaveInternal = viewModel::saveProjectInternal,
      onExportProjectFile = { name ->
        pendingExportProjectName = name
        val clean = name.replace(Regex("[^A-Za-z0-9_-]"), "_").ifBlank { "StudioProject" }
        exportProjectLauncher.launch("$clean.wmstudio")
      },
      onImportProjectFile = { openProjectLauncher.launch(arrayOf("*/*")) },
      onLoadInternal = viewModel::loadProjectInternal,
      onDeleteInternal = viewModel::deleteSavedProject,
      onDismiss = { viewModel.setShowProjectDialog(false) },
    )
  }

  // Save Preset Dialog
  if (state.showSavePresetDialog) {
    SavePresetDialog(
      onSave = viewModel::saveCurrentAsPreset,
      onDismiss = { viewModel.setShowSavePresetDialog(false) },
    )
  }
}
