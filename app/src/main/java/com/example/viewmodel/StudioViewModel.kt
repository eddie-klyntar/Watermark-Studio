package com.example.viewmodel

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.geometry.Offset
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.PresetRepository
import com.example.data.ProjectManager
import com.example.data.SavedProjectSummary
import com.example.engine.FontRegistry
import com.example.engine.ImageIoManager
import com.example.engine.LayerRenderBounds
import com.example.engine.LogoAsset
import com.example.engine.LogoAssetRepository
import com.example.engine.StudioFont
import com.example.engine.WatermarkRenderer
import com.example.model.AnchorPreset
import com.example.model.BatchError
import com.example.model.BatchJobProgress
import com.example.model.BatchSettings
import com.example.model.ExportFormat
import com.example.model.ExportSettings
import com.example.model.ImageAsset
import com.example.model.InspectorTab
import com.example.model.LayerTransform
import com.example.model.LayerType
import com.example.model.LeftSidebarTab
import com.example.model.LogoLayerStyle
import com.example.model.TextLayerStyle
import com.example.model.WatermarkLayer
import com.example.model.WatermarkPreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.abs

data class HistorySnapshot(
  val layers: List<WatermarkLayer>,
  val selectedLayerId: String?,
)

data class SnapGuideState(
  val snappedCenterX: Boolean = false,
  val snappedCenterY: Boolean = false,
  val snappedLeftMargin: Boolean = false,
  val snappedRightMargin: Boolean = false,
  val snappedTopMargin: Boolean = false,
  val snappedBottomMargin: Boolean = false,
)

data class ExportPreviewState(
  val isGenerating: Boolean = false,
  val previewBitmap: Bitmap? = null,
  val estimatedBytes: Long = 0L,
  val outputWidth: Int = 0,
  val outputHeight: Int = 0,
)

data class StudioUiState(
  val projectName: String = "Atelier Showcase",
  val images: List<ImageAsset> = emptyList(),
  val activeImageId: String? = null,
  val layers: List<WatermarkLayer> = emptyList(),
  val selectedLayerId: String? = null,
  val renderedPreview: Bitmap? = null,
  val layerBounds: List<LayerRenderBounds> = emptyList(),
  val isRendering: Boolean = false,
  val leftSidebarTab: LeftSidebarTab = LeftSidebarTab.LAYERS,
  val inspectorTab: InspectorTab = InspectorTab.CONTENT,
  val zoomModeFit: Boolean = true,
  val manualZoom: Float = 1f,
  val actualPixelZoomRatio: Float = 1f,
  val panOffset: Offset = Offset.Zero,
  val showCheckerboard: Boolean = true,
  val showGridGuides: Boolean = false,
  val snapToGuides: Boolean = true,
  val snapGuideState: SnapGuideState = SnapGuideState(),
  val availableFonts: List<StudioFont> = emptyList(),
  val availableLogos: List<LogoAsset> = emptyList(),
  val builtInPresets: List<WatermarkPreset> = emptyList(),
  val userPresets: List<WatermarkPreset> = emptyList(),
  val savedProjects: List<SavedProjectSummary> = emptyList(),
  val exportSettings: ExportSettings = ExportSettings(),
  val exportPreview: ExportPreviewState = ExportPreviewState(),
  val batchSettings: BatchSettings = BatchSettings(),
  val batchSourceFolderUri: String? = null,
  val batchSourceFolderName: String? = null,
  val batchSourceFolderCount: Int = 0,
  val batchDestFolderUri: String? = null,
  val batchDestFolderName: String? = null,
  val batchProgress: BatchJobProgress = BatchJobProgress(),
  val canUndo: Boolean = false,
  val canRedo: Boolean = false,
  val statusMessage: String = "Ready",
  val showExportDialog: Boolean = false,
  val showBatchDialog: Boolean = false,
  val showProjectDialog: Boolean = false,
  val showSavePresetDialog: Boolean = false,
  val showColorPickerFor: ColorPickerTarget? = null,
)

enum class ColorPickerTarget {
  TEXT_FILL,
  TEXT_GRADIENT_END,
  STROKE,
  SHADOW,
  GLOW,
  BACKGROUND_HIGHLIGHT,
  LOGO_TINT,
}

class StudioViewModel(application: Application) : AndroidViewModel(application) {

  private val context = application.applicationContext
  val fontRegistry = FontRegistry(context)
  val logoRepository = LogoAssetRepository(context)
  val imageIoManager = ImageIoManager(context)
  private val renderer = WatermarkRenderer(fontRegistry, logoRepository)
  private val presetRepository = PresetRepository(context)
  private val projectManager = ProjectManager(context, fontRegistry, logoRepository, imageIoManager)

  private val _uiState = MutableStateFlow(StudioUiState())
  val uiState: StateFlow<StudioUiState> = _uiState.asStateFlow()

  private val undoStack = ArrayDeque<HistorySnapshot>()
  private val redoStack = ArrayDeque<HistorySnapshot>()
  private var activeSourcePreview: Bitmap? = null
  private var renderJob: Job? = null
  private var batchJob: Job? = null
  private var exportPreviewJob: Job? = null

  init {
    initializeWorkspace()
  }

  private fun initializeWorkspace() {
    val sampleAlpine = imageIoManager.inspectSampleDrawable(R.drawable.img_sample_alpine, "Dolomites_Twilight.jpg")
    val sampleArch = imageIoManager.inspectSampleDrawable(R.drawable.img_sample_architecture, "Brutalist_Atrium.jpg")
    val initialImages = listOfNotNull(sampleAlpine, sampleArch)
    val builtInPresets = presetRepository.getBuiltInPresets()
    val defaultLayers = builtInPresets.find { it.id == "preset_studio_branding" }?.layers
      ?.map { it.copy(id = UUID.randomUUID().toString()) }
      ?: listOf(createDefaultTextLayer())

    _uiState.update {
      it.copy(
        images = initialImages,
        activeImageId = initialImages.firstOrNull()?.id,
        layers = defaultLayers,
        selectedLayerId = defaultLayers.firstOrNull()?.id,
        availableFonts = fontRegistry.getAllFonts(),
        availableLogos = logoRepository.getAllLogos(),
        builtInPresets = builtInPresets,
        userPresets = presetRepository.getUserPresets(),
        savedProjects = projectManager.listInternalProjects(),
        statusMessage = "Loaded studio workspace",
      )
    }

    initialImages.firstOrNull()?.let { loadActiveAssetBitmap(it) }
  }

  private fun createDefaultTextLayer(): WatermarkLayer = WatermarkLayer(
    id = UUID.randomUUID().toString(),
    name = "Copyright Text",
    type = LayerType.TEXT,
    opacity = 0.9f,
    textStyle = TextLayerStyle(),
  )

  private fun pushUndoSnapshot() {
    val current = _uiState.value
    undoStack.addLast(HistorySnapshot(current.layers, current.selectedLayerId))
    if (undoStack.size > 40) undoStack.removeFirst()
    redoStack.clear()
    _uiState.update {
      it.copy(canUndo = undoStack.isNotEmpty(), canRedo = false)
    }
  }

  fun undo() {
    if (undoStack.isEmpty()) return
    val current = _uiState.value
    redoStack.addLast(HistorySnapshot(current.layers, current.selectedLayerId))
    val prev = undoStack.removeLast()
    _uiState.update {
      it.copy(
        layers = prev.layers,
        selectedLayerId = prev.selectedLayerId ?: prev.layers.firstOrNull()?.id,
        canUndo = undoStack.isNotEmpty(),
        canRedo = redoStack.isNotEmpty(),
        statusMessage = "Undid last edit",
      )
    }
    triggerRender()
  }

  fun redo() {
    if (redoStack.isEmpty()) return
    val current = _uiState.value
    undoStack.addLast(HistorySnapshot(current.layers, current.selectedLayerId))
    val next = redoStack.removeLast()
    _uiState.update {
      it.copy(
        layers = next.layers,
        selectedLayerId = next.selectedLayerId ?: next.layers.firstOrNull()?.id,
        canUndo = undoStack.isNotEmpty(),
        canRedo = redoStack.isNotEmpty(),
        statusMessage = "Redid edit",
      )
    }
    triggerRender()
  }

  fun importImages(uris: List<Uri>) {
    if (uris.isEmpty()) return
    viewModelScope.launch {
      _uiState.update { it.copy(statusMessage = "Importing ${uris.size} image(s)...") }
      val imported = withContext(Dispatchers.IO) {
        uris.mapNotNull { uri ->
          runCatching {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
          }
          imageIoManager.inspectAssetFromUri(uri)
        }
      }
      if (imported.isEmpty()) {
        _uiState.update { it.copy(statusMessage = "No valid images could be decoded") }
        return@launch
      }
      val firstNew = imported.first()
      _uiState.update { state ->
        state.copy(
          images = state.images + imported,
          activeImageId = firstNew.id,
          statusMessage = "Imported ${imported.size} image(s)",
        )
      }
      loadActiveAssetBitmap(firstNew)
    }
  }

  fun selectImage(assetId: String) {
    val asset = _uiState.value.images.find { it.id == assetId } ?: return
    _uiState.update { it.copy(activeImageId = assetId, zoomModeFit = true, panOffset = Offset.Zero) }
    loadActiveAssetBitmap(asset)
  }

  fun removeImage(assetId: String) {
    val current = _uiState.value
    if (current.images.size <= 1) return
    val remaining = current.images.filterNot { it.id == assetId }
    val newActive = if (current.activeImageId == assetId) remaining.firstOrNull() else remaining.find { it.id == current.activeImageId }
    _uiState.update {
      it.copy(images = remaining, activeImageId = newActive?.id, statusMessage = "Removed image from workspace")
    }
    if (current.activeImageId == assetId && newActive != null) {
      loadActiveAssetBitmap(newActive)
    }
  }

  private fun loadActiveAssetBitmap(asset: ImageAsset) {
    viewModelScope.launch {
      _uiState.update { it.copy(isRendering = true, statusMessage = "Loading ${asset.name}...") }
      val decoded = withContext(Dispatchers.IO) {
        imageIoManager.decodePreviewBitmap(asset, maxLongEdge = 1800)
      }
      if (decoded != null) {
        activeSourcePreview?.recycle()
        activeSourcePreview = decoded
        val pixelZoomRatio = asset.width.toFloat() / decoded.width.coerceAtLeast(1).toFloat()
        _uiState.update {
          it.copy(
            actualPixelZoomRatio = pixelZoomRatio.coerceAtLeast(1f),
            statusMessage = "Loaded ${asset.name} (${asset.width} × ${asset.height})",
          )
        }
        triggerRender()
      } else {
        _uiState.update { it.copy(isRendering = false, statusMessage = "Failed to decode ${asset.name}") }
      }
    }
  }

  fun triggerRender() {
    val src = activeSourcePreview ?: return
    val state = _uiState.value
    val activeAsset = state.images.find { it.id == state.activeImageId }
    val layersSnapshot = state.layers

    renderJob?.cancel()
    renderJob = viewModelScope.launch {
      _uiState.update { it.copy(isRendering = true) }
      val result = withContext(Dispatchers.Default) {
        renderer.renderComposition(
          sourceBitmap = src,
          sourceAsset = activeAsset,
          layers = layersSnapshot,
        )
      }
      if (isActive) {
        _uiState.update {
          it.copy(
            renderedPreview = result.bitmap,
            layerBounds = result.layerBounds,
            isRendering = false,
          )
        }
      } else {
        result.bitmap.recycle()
      }
    }
  }

  // Layer CRUD & Ordering
  fun selectLayer(layerId: String) {
    _uiState.update { it.copy(selectedLayerId = layerId) }
  }

  fun selectLayerAtCanvasPoint(normX: Float, normY: Float) {
    val src = activeSourcePreview ?: return
    val px = normX * src.width
    val py = normY * src.height
    val bounds = _uiState.value.layerBounds
    // Search top-most layer first
    for (b in bounds.asReversed()) {
      if (b.isTiled) continue
      val halfW = b.width * 0.55f
      val halfH = b.height * 0.55f
      val dx = px - b.centerX
      val dy = py - b.centerY
      val rad = -Math.toRadians(b.rotationDeg.toDouble())
      val rx = (dx * kotlin.math.cos(rad) - dy * kotlin.math.sin(rad)).toFloat()
      val ry = (dx * kotlin.math.sin(rad) + dy * kotlin.math.cos(rad)).toFloat()
      if (abs(rx) <= halfW && abs(ry) <= halfH) {
        _uiState.update { it.copy(selectedLayerId = b.layerId) }
        return
      }
    }
  }

  fun addTextLayer() {
    pushUndoSnapshot()
    val count = _uiState.value.layers.count { it.type == LayerType.TEXT } + 1
    val layer = WatermarkLayer(
      id = UUID.randomUUID().toString(),
      name = "Text Layer $count",
      type = LayerType.TEXT,
      opacity = 0.92f,
      transform = LayerTransform(xFraction = 0.5f, yFraction = 0.5f),
      textStyle = TextLayerStyle(text = "© {YEAR} Watermark Studio"),
    )
    _uiState.update {
      it.copy(
        layers = it.layers + layer,
        selectedLayerId = layer.id,
        inspectorTab = InspectorTab.CONTENT,
        statusMessage = "Added ${layer.name}",
      )
    }
    triggerRender()
  }

  fun addLogoLayer(asset: LogoAsset? = null) {
    pushUndoSnapshot()
    val chosen = asset ?: _uiState.value.availableLogos.firstOrNull()
    val count = _uiState.value.layers.count { it.type == LayerType.LOGO } + 1
    val layer = WatermarkLayer(
      id = UUID.randomUUID().toString(),
      name = chosen?.displayName ?: "Logo $count",
      type = LayerType.LOGO,
      opacity = 0.9f,
      transform = LayerTransform(xFraction = 0.5f, yFraction = 0.45f),
      logoStyle = LogoLayerStyle(
        assetId = chosen?.id ?: "builtin_aperture",
        assetName = chosen?.displayName ?: "Aperture Crest",
        isSvg = chosen?.isSvg ?: true,
      ),
    )
    _uiState.update {
      it.copy(
        layers = it.layers + layer,
        selectedLayerId = layer.id,
        inspectorTab = InspectorTab.CONTENT,
        statusMessage = "Added logo layer ${layer.name}",
      )
    }
    triggerRender()
  }

  fun duplicateLayer(layerId: String) {
    val src = _uiState.value.layers.find { it.id == layerId } ?: return
    pushUndoSnapshot()
    val copy = src.copy(
      id = UUID.randomUUID().toString(),
      name = "${src.name} Copy",
      transform = src.transform.copy(
        xFraction = (src.transform.xFraction + 0.04f).coerceAtMost(0.95f),
        yFraction = (src.transform.yFraction + 0.04f).coerceAtMost(0.95f),
      ),
    )
    _uiState.update {
      it.copy(
        layers = it.layers + copy,
        selectedLayerId = copy.id,
        statusMessage = "Duplicated ${src.name}",
      )
    }
    triggerRender()
  }

  fun deleteLayer(layerId: String) {
    val current = _uiState.value
    if (current.layers.none { it.id == layerId }) return
    pushUndoSnapshot()
    val updated = current.layers.filterNot { it.id == layerId }
    val nextSel = if (current.selectedLayerId == layerId) updated.lastOrNull()?.id else current.selectedLayerId
    _uiState.update {
      it.copy(layers = updated, selectedLayerId = nextSel, statusMessage = "Deleted layer")
    }
    triggerRender()
  }

  fun renameLayer(layerId: String, newName: String) {
    val trimmed = newName.trim()
    if (trimmed.isEmpty()) return
    pushUndoSnapshot()
    updateLayer(layerId, recordUndo = false) { it.copy(name = trimmed) }
  }

  fun toggleLayerVisibility(layerId: String) {
    pushUndoSnapshot()
    updateLayer(layerId, recordUndo = false) { it.copy(visible = !it.visible) }
  }

  fun toggleLayerLock(layerId: String) {
    pushUndoSnapshot()
    updateLayer(layerId, recordUndo = false) { it.copy(locked = !it.locked) }
  }

  fun reorderLayer(fromIndex: Int, toIndex: Int) {
    val list = _uiState.value.layers.toMutableList()
    if (fromIndex !in list.indices || toIndex !in list.indices || fromIndex == toIndex) return
    pushUndoSnapshot()
    val item = list.removeAt(fromIndex)
    list.add(toIndex, item)
    _uiState.update { it.copy(layers = list, statusMessage = "Reordered layer") }
    triggerRender()
  }

  fun recordUndoPoint() {
    pushUndoSnapshot()
  }

  fun updateSelectedLayer(recordUndo: Boolean = false, transform: (WatermarkLayer) -> WatermarkLayer) {
    val selId = _uiState.value.selectedLayerId ?: return
    updateLayer(selId, recordUndo, transform)
  }

  fun updateLayer(layerId: String, recordUndo: Boolean = false, transform: (WatermarkLayer) -> WatermarkLayer) {
    if (recordUndo) pushUndoSnapshot()
    _uiState.update { state ->
      val updated = state.layers.map { if (it.id == layerId) transform(it) else it }
      state.copy(layers = updated)
    }
    triggerRender()
  }

  fun dragSelectedLayerBy(deltaXFraction: Float, deltaYFraction: Float) {
    val state = _uiState.value
    val layer = state.layers.find { it.id == state.selectedLayerId } ?: return
    if (layer.locked || layer.tile.enabled) return

    var nx = (layer.transform.xFraction + deltaXFraction).coerceIn(0f, 1f)
    var ny = (layer.transform.yFraction + deltaYFraction).coerceIn(0f, 1f)
    val margin = layer.transform.marginFraction.coerceIn(0f, 0.25f)
    val snapThreshold = 0.018f

    var snapCx = false
    var snapCy = false
    var snapL = false
    var snapR = false
    var snapT = false
    var snapB = false

    if (state.snapToGuides) {
      when {
        abs(nx - 0.5f) < snapThreshold -> { nx = 0.5f; snapCx = true }
        abs(nx - margin) < snapThreshold -> { nx = margin; snapL = true }
        abs(nx - (1f - margin)) < snapThreshold -> { nx = 1f - margin; snapR = true }
      }
      when {
        abs(ny - 0.5f) < snapThreshold -> { ny = 0.5f; snapCy = true }
        abs(ny - margin) < snapThreshold -> { ny = margin; snapT = true }
        abs(ny - (1f - margin)) < snapThreshold -> { ny = 1f - margin; snapB = true }
      }
    }

    _uiState.update {
      it.copy(
        layers = it.layers.map { l ->
          if (l.id == layer.id) {
            l.copy(transform = l.transform.copy(xFraction = nx, yFraction = ny, anchorPreset = null))
          } else l
        },
        snapGuideState = SnapGuideState(snapCx, snapCy, snapL, snapR, snapT, snapB),
      )
    }
    triggerRender()
  }

  fun clearSnapGuides() {
    _uiState.update { it.copy(snapGuideState = SnapGuideState()) }
  }

  fun applyAnchorPreset(preset: AnchorPreset) {
    val sel = _uiState.value.layers.find { it.id == _uiState.value.selectedLayerId } ?: return
    if (sel.locked) return
    pushUndoSnapshot()
    val m = sel.transform.marginFraction.coerceIn(0f, 0.25f)
    val x = when (preset.xFrac) {
      0f -> m + 0.06f
      1f -> 1f - m - 0.06f
      else -> 0.5f
    }
    val y = when (preset.yFrac) {
      0f -> m + 0.06f
      1f -> 1f - m - 0.06f
      else -> 0.5f
    }
    updateLayer(sel.id, recordUndo = false) {
      it.copy(transform = it.transform.copy(xFraction = x, yFraction = y, anchorPreset = preset))
    }
  }

  // Viewport controls
  fun setZoomFit() {
    _uiState.update { it.copy(zoomModeFit = true, manualZoom = 1f, panOffset = Offset.Zero) }
  }

  fun setZoomActualSize() {
    _uiState.update {
      it.copy(
        zoomModeFit = false,
        manualZoom = it.actualPixelZoomRatio.coerceIn(1f, 4f),
        panOffset = Offset.Zero,
      )
    }
  }

  fun adjustZoom(factor: Float) {
    _uiState.update {
      val next = (it.manualZoom * factor).coerceIn(0.25f, 6f)
      it.copy(zoomModeFit = false, manualZoom = next)
    }
  }

  fun setZoomAndPan(zoom: Float, pan: Offset) {
    _uiState.update {
      it.copy(
        zoomModeFit = false,
        manualZoom = zoom.coerceIn(0.25f, 6f),
        panOffset = pan,
      )
    }
  }

  fun panBy(delta: Offset) {
    _uiState.update { it.copy(panOffset = it.panOffset + delta) }
  }

  fun toggleCheckerboard() = _uiState.update { it.copy(showCheckerboard = !it.showCheckerboard) }
  fun toggleGridGuides() = _uiState.update { it.copy(showGridGuides = !it.showGridGuides) }
  fun toggleSnapToGuides() = _uiState.update { it.copy(snapToGuides = !it.snapToGuides) }
  fun setLeftSidebarTab(tab: LeftSidebarTab) = _uiState.update { it.copy(leftSidebarTab = tab) }
  fun setInspectorTab(tab: InspectorTab) = _uiState.update { it.copy(inspectorTab = tab) }

  // Fonts & Logos import
  fun importCustomFont(uri: Uri) {
    viewModelScope.launch {
      val imported = withContext(Dispatchers.IO) { fontRegistry.importFontFromUri(uri) }
      if (imported != null) {
        _uiState.update {
          it.copy(
            availableFonts = fontRegistry.getAllFonts(),
            statusMessage = "Imported font: ${imported.displayName}",
          )
        }
        val sel = _uiState.value.layers.find { it.id == _uiState.value.selectedLayerId }
        if (sel?.type == LayerType.TEXT && sel.textStyle != null) {
          updateSelectedLayer(recordUndo = true) {
            it.copy(textStyle = it.textStyle?.copy(fontId = imported.id))
          }
        }
      } else {
        _uiState.update { it.copy(statusMessage = "Invalid or unsupported font file") }
      }
    }
  }

  fun importCustomLogo(uri: Uri) {
    viewModelScope.launch {
      val imported = withContext(Dispatchers.IO) { logoRepository.importLogoFromUri(uri) }
      if (imported != null) {
        _uiState.update {
          it.copy(
            availableLogos = logoRepository.getAllLogos(),
            statusMessage = "Imported logo: ${imported.displayName}",
          )
        }
        val sel = _uiState.value.layers.find { it.id == _uiState.value.selectedLayerId }
        if (sel?.type == LayerType.LOGO && sel.logoStyle != null) {
          updateSelectedLayer(recordUndo = true) {
            it.copy(
              logoStyle = it.logoStyle?.copy(
                assetId = imported.id,
                assetName = imported.displayName,
                isSvg = imported.isSvg,
              )
            )
          }
        } else {
          addLogoLayer(imported)
        }
      } else {
        _uiState.update { it.copy(statusMessage = "Failed to import logo graphic") }
      }
    }
  }

  // Presets
  fun applyPreset(preset: WatermarkPreset) {
    pushUndoSnapshot()
    val clonedLayers = preset.layers.map { it.copy(id = UUID.randomUUID().toString()) }
    _uiState.update {
      it.copy(
        layers = clonedLayers,
        selectedLayerId = clonedLayers.firstOrNull()?.id,
        statusMessage = "Applied preset: ${preset.name}",
      )
    }
    triggerRender()
  }

  fun saveCurrentAsPreset(name: String, description: String) {
    val layers = _uiState.value.layers
    if (layers.isEmpty()) return
    presetRepository.saveUserPreset(name, description, layers)
    _uiState.update {
      it.copy(
        userPresets = presetRepository.getUserPresets(),
        showSavePresetDialog = false,
        statusMessage = "Saved custom preset: $name",
      )
    }
  }

  fun deleteUserPreset(id: String) {
    presetRepository.deleteUserPreset(id)
    _uiState.update {
      it.copy(
        userPresets = presetRepository.getUserPresets(),
        statusMessage = "Deleted custom preset",
      )
    }
  }

  // Projects (.wmstudio)
  fun saveProjectInternal(name: String) {
    val state = _uiState.value
    val activeAsset = state.images.find { it.id == state.activeImageId }
    viewModelScope.launch {
      withContext(Dispatchers.IO) {
        projectManager.saveProjectToInternal(name, activeAsset, state.layers, state.exportSettings)
      }
      _uiState.update {
        it.copy(
          projectName = name,
          savedProjects = projectManager.listInternalProjects(),
          statusMessage = "Saved project \"$name\"",
        )
      }
    }
  }

  fun exportProjectFile(uri: Uri, projectName: String) {
    val state = _uiState.value
    val activeAsset = state.images.find { it.id == state.activeImageId }
    viewModelScope.launch {
      val ok = withContext(Dispatchers.IO) {
        projectManager.exportProjectToUri(uri, projectName, activeAsset, state.layers, state.exportSettings)
      }
      _uiState.update {
        it.copy(
          projectName = projectName,
          statusMessage = if (ok) "Exported .wmstudio project" else "Failed to export project",
        )
      }
    }
  }

  fun loadProjectInternal(fileName: String) {
    viewModelScope.launch {
      val loaded = withContext(Dispatchers.IO) { projectManager.loadProjectFromInternal(fileName) }
      if (loaded != null) applyLoadedProject(loaded)
    }
  }

  fun loadProjectFromUri(uri: Uri) {
    viewModelScope.launch {
      val loaded = withContext(Dispatchers.IO) { projectManager.loadProjectFromUri(uri) }
      if (loaded != null) {
        applyLoadedProject(loaded)
      } else {
        _uiState.update { it.copy(statusMessage = "Invalid or corrupted .wmstudio file") }
      }
    }
  }

  fun deleteSavedProject(fileName: String) {
    projectManager.deleteInternalProject(fileName)
    _uiState.update { it.copy(savedProjects = projectManager.listInternalProjects()) }
  }

  private fun applyLoadedProject(loaded: com.example.data.LoadedProjectResult) {
    val proj = loaded.project
    pushUndoSnapshot()

    var restoredAsset: ImageAsset? = null
    if (loaded.embeddedImageFile != null) {
      restoredAsset = imageIoManager.inspectAssetFromUri(Uri.fromFile(loaded.embeddedImageFile))
    } else if (proj.activeSampleResName != null) {
      val resId = context.resources.getIdentifier(proj.activeSampleResName, "drawable", context.packageName)
      if (resId != 0) {
        restoredAsset = imageIoManager.inspectSampleDrawable(resId, proj.activeImageName ?: "Sample.jpg")
      }
    } else if (proj.activeImageUri != null) {
      restoredAsset = imageIoManager.inspectAssetFromUri(Uri.parse(proj.activeImageUri))
    }

    _uiState.update { state ->
      val nextImages = if (restoredAsset != null) {
        (listOf(restoredAsset) + state.images).distinctBy { it.uriString ?: it.sampleDrawableRes ?: it.id }
      } else state.images

      state.copy(
        projectName = proj.projectName,
        images = nextImages,
        activeImageId = restoredAsset?.id ?: state.activeImageId,
        layers = proj.layers,
        selectedLayerId = proj.layers.firstOrNull()?.id,
        exportSettings = proj.exportSettings,
        availableFonts = fontRegistry.getAllFonts(),
        availableLogos = logoRepository.getAllLogos(),
        showProjectDialog = false,
        statusMessage = "Opened project \"${proj.projectName}\"",
      )
    }

    val active = _uiState.value.images.find { it.id == _uiState.value.activeImageId }
    if (active != null) {
      loadActiveAssetBitmap(active)
    } else {
      triggerRender()
    }
  }

  // Export Dialog & Single High-Res Export
  fun openExportDialog() {
    _uiState.update { it.copy(showExportDialog = true) }
    refreshExportPreview()
  }

  fun closeExportDialog() {
    _uiState.update { it.copy(showExportDialog = false) }
  }

  fun updateExportSettings(transform: (ExportSettings) -> ExportSettings) {
    _uiState.update { it.copy(exportSettings = transform(it.exportSettings)) }
    refreshExportPreview()
  }

  private fun refreshExportPreview() {
    val src = activeSourcePreview ?: return
    val state = _uiState.value
    val activeAsset = state.images.find { it.id == state.activeImageId } ?: return
    val settings = state.exportSettings
    val layers = state.layers

    exportPreviewJob?.cancel()
    exportPreviewJob = viewModelScope.launch {
      _uiState.update { it.copy(exportPreview = it.exportPreview.copy(isGenerating = true)) }
      val (targetW, targetH) = imageIoManager.computeTargetDimensions(activeAsset.width, activeAsset.height, settings)
      val previewResult = withContext(Dispatchers.Default) {
        val rendered = renderer.renderComposition(src, activeAsset, layers)
        val baos = ByteArrayOutputStream()
        imageIoManager.writeExportedImage(rendered.bitmap, null, settings, baos)
        val previewBytes = baos.size().toLong()
        // Estimate full-resolution size proportionally
        val pixelScale = (targetW.toDouble() * targetH.toDouble()) /
          (rendered.bitmap.width.toDouble() * rendered.bitmap.height.toDouble()).coerceAtLeast(1.0)
        val estimatedTotal = (previewBytes * pixelScale).toLong()
        Triple(rendered.bitmap, estimatedTotal, targetW to targetH)
      }
      _uiState.update {
        it.copy(
          exportPreview = ExportPreviewState(
            isGenerating = false,
            previewBitmap = previewResult.first,
            estimatedBytes = previewResult.second,
            outputWidth = previewResult.third.first,
            outputHeight = previewResult.third.second,
          )
        )
      }
    }
  }

  fun exportActiveImageToUri(uri: Uri) {
    val state = _uiState.value
    val activeAsset = state.images.find { it.id == state.activeImageId } ?: return
    val settings = state.exportSettings
    val layers = state.layers

    viewModelScope.launch {
      _uiState.update { it.copy(statusMessage = "Exporting full-resolution ${settings.format.label}...") }
      val result = withContext(Dispatchers.IO) {
        runCatching {
          val (fullSrc, exif) = imageIoManager.decodeFullResolutionBitmap(activeAsset)
            ?: error("Could not decode original source image")
          val (outW, outH) = imageIoManager.computeTargetDimensions(fullSrc.width, fullSrc.height, settings)
          val rendered = renderer.renderComposition(
            sourceBitmap = fullSrc,
            sourceAsset = activeAsset,
            layers = layers,
            targetWidth = outW,
            targetHeight = outH,
          )
          fullSrc.recycle()
          context.contentResolver.openOutputStream(uri)?.use { out ->
            imageIoManager.writeExportedImage(rendered.bitmap, exif, settings, out)
          } ?: error("Cannot open output destination")
          rendered.bitmap.recycle()
        }
      }
      _uiState.update {
        it.copy(
          showExportDialog = false,
          statusMessage = if (result.isSuccess) {
            "Exported ${activeAsset.name} as ${settings.format.label}"
          } else {
            "Export failed: ${result.exceptionOrNull()?.message}"
          },
        )
      }
    }
  }

  // Batch Processing
  fun openBatchDialog() {
    _uiState.update { it.copy(showBatchDialog = true) }
  }

  fun closeBatchDialog() {
    _uiState.update { it.copy(showBatchDialog = false) }
  }

  fun updateBatchSettings(transform: (BatchSettings) -> BatchSettings) {
    _uiState.update { it.copy(batchSettings = transform(it.batchSettings)) }
  }

  fun setBatchSourceFolder(uri: Uri) {
    runCatching {
      context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    viewModelScope.launch {
      val (name, count) = withContext(Dispatchers.IO) {
        val doc = DocumentFile.fromTreeUri(context, uri)
        val files = doc?.listFiles()?.filter { isSupportedImageFile(it) } ?: emptyList()
        (doc?.name ?: "Selected Folder") to files.size
      }
      _uiState.update {
        it.copy(
          batchSourceFolderUri = uri.toString(),
          batchSourceFolderName = name,
          batchSourceFolderCount = count,
        )
      }
    }
  }

  fun clearBatchSourceFolder() {
    _uiState.update {
      it.copy(batchSourceFolderUri = null, batchSourceFolderName = null, batchSourceFolderCount = 0)
    }
  }

  fun setBatchDestinationFolder(uri: Uri) {
    runCatching {
      context.contentResolver.takePersistableUriPermission(
        uri,
        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
      )
    }
    val doc = DocumentFile.fromTreeUri(context, uri)
    _uiState.update {
      it.copy(
        batchDestFolderUri = uri.toString(),
        batchDestFolderName = doc?.name ?: "Output Folder",
      )
    }
  }

  fun startBatchProcessing(useSourceFolder: Boolean) {
    val state = _uiState.value
    val destUriStr = state.batchDestFolderUri ?: return
    val destTree = DocumentFile.fromTreeUri(context, Uri.parse(destUriStr)) ?: return
    val settings = state.batchSettings
    val layers = state.layers
    val refLongEdge = if (settings.autoScaleToImage) null else {
      val active = state.images.find { it.id == state.activeImageId }
      maxOf(active?.width ?: 1000, active?.height ?: 1000).toFloat()
    }

    batchJob?.cancel()
    batchJob = viewModelScope.launch {
      val sourceAssets: List<ImageAsset> = withContext(Dispatchers.IO) {
        if (useSourceFolder && state.batchSourceFolderUri != null) {
          val folder = DocumentFile.fromTreeUri(context, Uri.parse(state.batchSourceFolderUri))
          folder?.listFiles()
            ?.filter { isSupportedImageFile(it) }
            ?.mapNotNull { doc -> imageIoManager.inspectAssetFromUri(doc.uri) }
            ?: emptyList()
        } else {
          state.images
        }
      }

      if (sourceAssets.isEmpty()) {
        _uiState.update {
          it.copy(
            batchProgress = BatchJobProgress(
              isFinished = true,
              errors = listOf(BatchError("Batch Queue", "No supported image files found.")),
            )
          )
        }
        return@launch
      }

      _uiState.update {
        it.copy(
          batchProgress = BatchJobProgress(
            isRunning = true,
            total = sourceAssets.size,
            currentFileName = sourceAssets.first().name,
          )
        )
      }

      var succeeded = 0
      var failed = 0
      val errors = mutableListOf<BatchError>()
      val dateStamp = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())

      for ((idx, asset) in sourceAssets.withIndex()) {
        if (!isActive) break
        _uiState.update {
          it.copy(
            batchProgress = it.batchProgress.copy(
              completed = idx,
              succeeded = succeeded,
              failed = failed,
              currentFileName = asset.name,
              errors = errors.toList(),
            )
          )
        }

        val outcome = withContext(Dispatchers.IO) {
          runCatching {
            val (fullSrc, exif) = imageIoManager.decodeFullResolutionBitmap(asset)
              ?: error("Unsupported or unreadable image format")
            val (outW, outH) = imageIoManager.computeTargetDimensions(
              fullSrc.width,
              fullSrc.height,
              settings.exportSettings,
            )
            val rendered = renderer.renderComposition(
              sourceBitmap = fullSrc,
              sourceAsset = asset,
              layers = layers,
              targetWidth = outW,
              targetHeight = outH,
              referenceLongEdgeOverride = refLongEdge,
            )
            fullSrc.recycle()

            val baseName = asset.name.substringBeforeLast('.')
            val outBase = settings.namingTemplate
              .replace("{name}", baseName)
              .replace("{index}", (idx + 1).toString().padStart(3, '0'))
              .replace("{date}", dateStamp)
              .ifBlank { "${baseName}_watermarked" }
            val outFileName = "$outBase.${settings.exportSettings.format.extension}"
            destTree.findFile(outFileName)?.delete()
            val outDoc = destTree.createFile(settings.exportSettings.format.mimeType, outFileName)
              ?: error("Unable to create $outFileName in destination folder")

            context.contentResolver.openOutputStream(outDoc.uri)?.use { out ->
              imageIoManager.writeExportedImage(rendered.bitmap, exif, settings.exportSettings, out)
            } ?: error("Failed writing to $outFileName")
            rendered.bitmap.recycle()
          }
        }

        if (outcome.isSuccess) {
          succeeded++
        } else {
          failed++
          errors.add(BatchError(asset.name, outcome.exceptionOrNull()?.message ?: "Unknown error"))
        }
      }

      _uiState.update {
        it.copy(
          batchProgress = BatchJobProgress(
            isRunning = false,
            isFinished = true,
            total = sourceAssets.size,
            completed = succeeded + failed,
            succeeded = succeeded,
            failed = failed,
            currentFileName = "",
            errors = errors,
            cancelled = !isActive,
          ),
          statusMessage = "Batch complete: $succeeded succeeded, $failed failed",
        )
      }
    }
  }

  fun cancelBatchProcessing() {
    batchJob?.cancel()
    _uiState.update {
      it.copy(
        batchProgress = it.batchProgress.copy(isRunning = false, isFinished = true, cancelled = true),
        statusMessage = "Batch processing cancelled",
      )
    }
  }

  fun resetBatchProgress() {
    _uiState.update { it.copy(batchProgress = BatchJobProgress()) }
  }

  private fun isSupportedImageFile(doc: DocumentFile): Boolean {
    if (!doc.isFile) return false
    val name = doc.name?.lowercase() ?: return false
    return name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") ||
      name.endsWith(".webp") || name.endsWith(".tif") || name.endsWith(".tiff") ||
      name.endsWith(".bmp")
  }

  // Dialog toggles
  fun setShowProjectDialog(show: Boolean) {
    if (show) {
      _uiState.update { it.copy(savedProjects = projectManager.listInternalProjects(), showProjectDialog = true) }
    } else {
      _uiState.update { it.copy(showProjectDialog = false) }
    }
  }

  fun setShowSavePresetDialog(show: Boolean) {
    _uiState.update { it.copy(showSavePresetDialog = show) }
  }

  fun openColorPicker(target: ColorPickerTarget?) {
    _uiState.update { it.copy(showColorPickerFor = target) }
  }

  override fun onCleared() {
    super.onCleared()
    activeSourcePreview?.recycle()
  }
}
