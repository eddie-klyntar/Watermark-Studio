package com.example.ui.components

import android.content.ClipDescription
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Grid4x4
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Texture
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.mimeTypes
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.engine.LayerRenderBounds
import com.example.model.WatermarkLayer
import com.example.ui.theme.CheckerDark
import com.example.ui.theme.CheckerLight
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.StudioObsidian
import com.example.ui.theme.StudioPanel
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import com.example.viewmodel.SnapGuideState
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CentralCanvasWorkspace(
  renderedPreview: Bitmap?,
  layerBounds: List<LayerRenderBounds>,
  selectedLayer: WatermarkLayer?,
  isRendering: Boolean,
  zoomModeFit: Boolean,
  manualZoom: Float,
  panOffset: Offset,
  showCheckerboard: Boolean,
  showGridGuides: Boolean,
  snapToGuides: Boolean,
  snapGuideState: SnapGuideState,
  onSelectAtPoint: (Float, Float) -> Unit,
  onStartLayerEdit: () -> Unit,
  onDragSelectedLayer: (Float, Float) -> Unit,
  onClearSnapGuides: () -> Unit,
  onScaleSelectedLayer: (Float) -> Unit,
  onRotateSelectedLayer: (Float) -> Unit,
  onZoomFit: () -> Unit,
  onZoomActual: () -> Unit,
  onZoomStep: (Float) -> Unit,
  onZoomAndPan: (Float, Offset) -> Unit,
  onToggleCheckerboard: () -> Unit,
  onToggleGridGuides: () -> Unit,
  onToggleSnapToGuides: () -> Unit,
  onDropImageUris: (List<Uri>) -> Unit,
  modifier: Modifier = Modifier,
) {
  var isDropHovering by remember { mutableStateOf(false) }

  val dropTarget = remember {
    object : DragAndDropTarget {
      override fun onStarted(event: DragAndDropEvent) {
        isDropHovering = true
      }

      override fun onEnded(event: DragAndDropEvent) {
        isDropHovering = false
      }

      override fun onDrop(event: DragAndDropEvent): Boolean {
        isDropHovering = false
        val androidEvent = event.toAndroidDragEvent()
        val clipData = androidEvent.clipData ?: return false
        val uris = mutableListOf<Uri>()
        for (i in 0 until clipData.itemCount) {
          clipData.getItemAt(i).uri?.let { uris.add(it) }
        }
        if (uris.isNotEmpty()) {
          onDropImageUris(uris)
          return true
        }
        return false
      }
    }
  }

  BoxWithConstraints(
    modifier = modifier
      .fillMaxSize()
      .background(StudioObsidian)
      .clipToBounds()
      .dragAndDropTarget(
        shouldStartDragAndDrop = { event ->
          event.mimeTypes().any { it.startsWith("image/") || it == ClipDescription.MIMETYPE_TEXT_URILIST }
        },
        target = dropTarget,
      )
      .border(
        width = if (isDropHovering) 2.dp else 0.dp,
        color = if (isDropHovering) StudioCyan else Color.Transparent,
      )
      .testTag("central_canvas_workspace"),
  ) {
    val viewportW = constraints.maxWidth.toFloat().coerceAtLeast(1f)
    val viewportH = constraints.maxHeight.toFloat().coerceAtLeast(1f)

    val bmpW = (renderedPreview?.width ?: 1000).toFloat()
    val bmpH = (renderedPreview?.height ?: 750).toFloat()
    val fitScale = min((viewportW - 48f) / bmpW, (viewportH - 72f) / bmpH).coerceAtLeast(0.05f)
    val effectiveScale = if (zoomModeFit) fitScale else fitScale * manualZoom
    val drawnW = bmpW * effectiveScale
    val drawnH = bmpH * effectiveScale
    val imgLeft = (viewportW - drawnW) / 2f + panOffset.x
    val imgTop = (viewportH - drawnH) / 2f + panOffset.y

    val selectedBounds = remember(layerBounds, selectedLayer?.id) {
      layerBounds.find { it.layerId == selectedLayer?.id }
    }

    // Track gesture mode: dragging selected layer vs corner scale vs rotate vs canvas pan/zoom
    var activeHandleMode by remember { mutableStateOf(HandleMode.NONE) }

    Canvas(
      modifier = Modifier
        .fillMaxSize()
        .pointerInput(imgLeft, imgTop, drawnW, drawnH, selectedBounds, selectedLayer?.locked) {
          detectTapGestures { pos ->
            if (pos.x in imgLeft..(imgLeft + drawnW) && pos.y in imgTop..(imgTop + drawnH)) {
              val nx = (pos.x - imgLeft) / drawnW
              val ny = (pos.y - imgTop) / drawnH
              onSelectAtPoint(nx, ny)
            }
          }
        }
        .pointerInput(imgLeft, imgTop, drawnW, drawnH, selectedBounds, selectedLayer?.id, selectedLayer?.locked) {
          detectDragGestures(
            onDragStart = { start ->
              activeHandleMode = detectHandleHit(
                touch = start,
                bounds = selectedBounds,
                imgLeft = imgLeft,
                imgTop = imgTop,
                scaleX = drawnW / bmpW,
                scaleY = drawnH / bmpH,
                isLocked = selectedLayer?.locked == true,
              )
              if (activeHandleMode != HandleMode.NONE && activeHandleMode != HandleMode.PAN_CANVAS) {
                onStartLayerEdit()
              }
            },
            onDrag = { change, dragAmount ->
              change.consume()
              when (activeHandleMode) {
                HandleMode.MOVE_LAYER -> {
                  onDragSelectedLayer(dragAmount.x / drawnW, dragAmount.y / drawnH)
                }
                HandleMode.SCALE_CORNER -> {
                  val b = selectedBounds ?: return@detectDragGestures
                  val cx = imgLeft + b.centerX * (drawnW / bmpW)
                  val cy = imgTop + b.centerY * (drawnH / bmpH)
                  val prevDist = hypot(change.previousPosition.x - cx, change.previousPosition.y - cy).coerceAtLeast(10f)
                  val curDist = hypot(change.position.x - cx, change.position.y - cy).coerceAtLeast(10f)
                  onScaleSelectedLayer((curDist / prevDist).coerceIn(0.8f, 1.25f))
                }
                HandleMode.ROTATE_HANDLE -> {
                  val b = selectedBounds ?: return@detectDragGestures
                  val cx = imgLeft + b.centerX * (drawnW / bmpW)
                  val cy = imgTop + b.centerY * (drawnH / bmpH)
                  val a1 = atan2(change.previousPosition.y - cy, change.previousPosition.x - cx)
                  val a2 = atan2(change.position.y - cy, change.position.x - cx)
                  val deltaDeg = Math.toDegrees((a2 - a1).toDouble()).toFloat()
                  onRotateSelectedLayer(deltaDeg)
                }
                HandleMode.PAN_CANVAS, HandleMode.NONE -> {
                  onZoomAndPan(
                    if (zoomModeFit) 1f else manualZoom,
                    panOffset + dragAmount,
                  )
                }
              }
            },
            onDragEnd = {
              activeHandleMode = HandleMode.NONE
              onClearSnapGuides()
            },
            onDragCancel = {
              activeHandleMode = HandleMode.NONE
              onClearSnapGuides()
            },
          )
        }
        .pointerInput(zoomModeFit, manualZoom, panOffset) {
          detectTransformGestures { _, pan, zoom, _ ->
            if (kotlin.math.abs(zoom - 1f) > 0.005f) {
              val baseZoom = if (zoomModeFit) 1f else manualZoom
              onZoomAndPan((baseZoom * zoom).coerceIn(0.25f, 6f), panOffset + pan)
            }
          }
        },
    ) {
      // 1. Transparency Checkerboard behind image bounds
      if (showCheckerboard && renderedPreview != null) {
        val tile = 16.dp.toPx()
        val cols = (drawnW / tile).toInt() + 1
        val rows = (drawnH / tile).toInt() + 1
        for (r in 0 until rows) {
          for (c in 0 until cols) {
            val x = imgLeft + c * tile
            val y = imgTop + r * tile
            val w = min(tile, imgLeft + drawnW - x)
            val h = min(tile, imgTop + drawnH - y)
            if (w > 0f && h > 0f) {
              drawRect(
                color = if ((r + c) % 2 == 0) CheckerLight else CheckerDark,
                topLeft = Offset(x, y),
                size = Size(w, h),
              )
            }
          }
        }
      }

      // 2. Rendered Non-Destructive Composition Bitmap
      if (renderedPreview != null) {
        drawImage(
          image = renderedPreview.asImageBitmap(),
          dstOffset = IntOffset(imgLeft.roundToInt(), imgTop.roundToInt()),
          dstSize = IntSize(drawnW.roundToInt().coerceAtLeast(1), drawnH.roundToInt().coerceAtLeast(1)),
        )
        drawRect(
          color = StudioBorder,
          topLeft = Offset(imgLeft, imgTop),
          size = Size(drawnW, drawnH),
          style = Stroke(width = 1.dp.toPx()),
        )
      }

      // 3. Optional Rule-of-Thirds & Safe Margin Guides
      if (showGridGuides && renderedPreview != null) {
        val guideStroke = 1.dp.toPx()
        val dashed = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
        val guideColor = Color.White.copy(alpha = 0.28f)
        for (f in listOf(1f / 3f, 2f / 3f)) {
          drawLine(
            color = guideColor,
            start = Offset(imgLeft + drawnW * f, imgTop),
            end = Offset(imgLeft + drawnW * f, imgTop + drawnH),
            strokeWidth = guideStroke,
            pathEffect = dashed,
          )
          drawLine(
            color = guideColor,
            start = Offset(imgLeft, imgTop + drawnH * f),
            end = Offset(imgLeft + drawnW, imgTop + drawnH * f),
            strokeWidth = guideStroke,
            pathEffect = dashed,
          )
        }
        val margin = (selectedLayer?.transform?.marginFraction ?: 0.04f).coerceIn(0f, 0.25f)
        drawRect(
          color = StudioCyan.copy(alpha = 0.25f),
          topLeft = Offset(imgLeft + drawnW * margin, imgTop + drawnH * margin),
          size = Size(drawnW * (1f - 2f * margin), drawnH * (1f - 2f * margin)),
          style = Stroke(width = 1.dp.toPx(), pathEffect = dashed),
        )
      }

      // 4. Live Snapping Guidelines while dragging
      if (snapGuideState.snappedCenterX) {
        drawLine(
          color = StudioCyan,
          start = Offset(imgLeft + drawnW * 0.5f, imgTop),
          end = Offset(imgLeft + drawnW * 0.5f, imgTop + drawnH),
          strokeWidth = 1.5.dp.toPx(),
        )
      }
      if (snapGuideState.snappedCenterY) {
        drawLine(
          color = StudioCyan,
          start = Offset(imgLeft, imgTop + drawnH * 0.5f),
          end = Offset(imgLeft + drawnW, imgTop + drawnH * 0.5f),
          strokeWidth = 1.5.dp.toPx(),
        )
      }

      // 5. Interactive Selection Box + Corner Scale Handle + Rotation Handle
      if (selectedBounds != null && selectedLayer != null && selectedLayer.visible && !selectedBounds.isTiled) {
        val sx = drawnW / bmpW
        val sy = drawnH / bmpH
        val cx = imgLeft + selectedBounds.centerX * sx
        val cy = imgTop + selectedBounds.centerY * sy
        val bw = selectedBounds.width * sx
        val bh = selectedBounds.height * sy
        val frameColor = if (selectedLayer.locked) Color(0xFF94A3B8) else StudioAmber

        rotate(degrees = selectedBounds.rotationDeg, pivot = Offset(cx, cy)) {
          drawRect(
            color = frameColor,
            topLeft = Offset(cx - bw / 2f, cy - bh / 2f),
            size = Size(bw, bh),
            style = Stroke(width = 1.5.dp.toPx()),
          )
          if (!selectedLayer.locked) {
            val handleRadius = 5.5.dp.toPx()
            val corners = listOf(
              Offset(cx - bw / 2f, cy - bh / 2f),
              Offset(cx + bw / 2f, cy - bh / 2f),
              Offset(cx - bw / 2f, cy + bh / 2f),
              Offset(cx + bw / 2f, cy + bh / 2f),
            )
            corners.forEach { pt ->
              drawCircle(color = StudioObsidian, radius = handleRadius + 1.5f, center = pt)
              drawCircle(color = StudioAmber, radius = handleRadius, center = pt)
            }
            // Rotation stem & handle above top center
            val topCenter = Offset(cx, cy - bh / 2f)
            val rotHandle = Offset(cx, cy - bh / 2f - 24.dp.toPx())
            drawLine(color = StudioCyan, start = topCenter, end = rotHandle, strokeWidth = 1.5.dp.toPx())
            drawCircle(color = StudioObsidian, radius = 6.5.dp.toPx(), center = rotHandle)
            drawCircle(color = StudioCyan, radius = 5.dp.toPx(), center = rotHandle)
          }
        }
      }
    }

    // Rendering indicator pill
    if (isRendering) {
      Row(
        modifier = Modifier
          .align(Alignment.TopEnd)
          .padding(12.dp)
          .clip(RoundedCornerShape(16.dp))
          .background(StudioPanel.copy(alpha = 0.88f))
          .border(1.dp, StudioBorder, RoundedCornerShape(16.dp))
          .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        CircularProgressIndicator(
          modifier = Modifier.size(12.dp),
          strokeWidth = 2.dp,
          color = StudioAmber,
        )
        Text("Rendering", style = MaterialTheme.typography.labelSmall, color = StudioTextSecondary)
      }
    }

    // Floating Viewport Control Bar
    Row(
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .padding(bottom = 10.dp)
        .clip(RoundedCornerShape(20.dp))
        .background(StudioPanel.copy(alpha = 0.92f))
        .border(1.dp, StudioBorder, RoundedCornerShape(20.dp))
        .padding(horizontal = 8.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      CanvasPillBtn(
        label = "Fit",
        active = zoomModeFit,
        tag = "canvas_zoom_fit_btn",
        onClick = onZoomFit,
      )
      CanvasPillBtn(
        label = "100%",
        active = !zoomModeFit && manualZoom >= 1.1f,
        tag = "canvas_zoom_actual_btn",
        onClick = onZoomActual,
      )
      CanvasIconPill(
        icon = Icons.Default.Remove,
        desc = "Zoom Out",
        active = false,
        tag = "canvas_zoom_out_btn",
        onClick = { onZoomStep(0.8f) },
      )
      Text(
        text = "${(effectiveScale * 100).roundToInt()}%",
        style = MaterialTheme.typography.labelSmall,
        color = StudioTextPrimary,
        modifier = Modifier.padding(horizontal = 4.dp),
      )
      CanvasIconPill(
        icon = Icons.Default.Add,
        desc = "Zoom In",
        active = false,
        tag = "canvas_zoom_in_btn",
        onClick = { onZoomStep(1.25f) },
      )
      CanvasIconPill(
        icon = Icons.Default.Texture,
        desc = "Toggle Transparency Checkerboard",
        active = showCheckerboard,
        tag = "canvas_checkerboard_btn",
        onClick = onToggleCheckerboard,
      )
      CanvasIconPill(
        icon = Icons.Default.Grid4x4,
        desc = "Toggle Composition Grid & Safe Margins",
        active = showGridGuides,
        tag = "canvas_grid_btn",
        onClick = onToggleGridGuides,
      )
      CanvasIconPill(
        icon = Icons.Default.CenterFocusStrong,
        desc = "Toggle Snap to Center & Margins",
        active = snapToGuides,
        tag = "canvas_snap_btn",
        onClick = onToggleSnapToGuides,
      )
    }
  }
}

private enum class HandleMode {
  NONE,
  MOVE_LAYER,
  SCALE_CORNER,
  ROTATE_HANDLE,
  PAN_CANVAS,
}

private fun detectHandleHit(
  touch: Offset,
  bounds: LayerRenderBounds?,
  imgLeft: Float,
  imgTop: Float,
  scaleX: Float,
  scaleY: Float,
  isLocked: Boolean,
): HandleMode {
  if (bounds == null || bounds.isTiled || isLocked) return HandleMode.PAN_CANVAS

  val cx = imgLeft + bounds.centerX * scaleX
  val cy = imgTop + bounds.centerY * scaleY
  val bw = bounds.width * scaleX
  val bh = bounds.height * scaleY

  // Transform touch into layer's unrotated local coordinates around (cx, cy)
  val dx = touch.x - cx
  val dy = touch.y - cy
  val rad = -Math.toRadians(bounds.rotationDeg.toDouble())
  val lx = (dx * cos(rad) - dy * sin(rad)).toFloat()
  val ly = (dx * sin(rad) + dy * cos(rad)).toFloat()

  // Check rotation handle at (0, -bh/2 - 50px)
  val rotLocalY = -bh / 2f - 54f
  if (hypot(lx, ly - rotLocalY) < 42f) {
    return HandleMode.ROTATE_HANDLE
  }

  // Check 4 corners
  val corners = listOf(
    Offset(-bw / 2f, -bh / 2f),
    Offset(bw / 2f, -bh / 2f),
    Offset(-bw / 2f, bh / 2f),
    Offset(bw / 2f, bh / 2f),
  )
  if (corners.any { hypot(lx - it.x, ly - it.y) < 38f }) {
    return HandleMode.SCALE_CORNER
  }

  // Check inside bounding box
  if (kotlin.math.abs(lx) <= bw / 2f + 16f && kotlin.math.abs(ly) <= bh / 2f + 16f) {
    return HandleMode.MOVE_LAYER
  }

  return HandleMode.PAN_CANVAS
}

@Composable
private fun CanvasPillBtn(
  label: String,
  active: Boolean,
  tag: String,
  onClick: () -> Unit,
) {
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(14.dp))
      .background(if (active) StudioAmber else StudioSurface)
      .clickable(onClick = onClick)
      .padding(horizontal = 9.dp, vertical = 5.dp)
      .testTag(tag),
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = if (active) StudioObsidian else StudioTextPrimary,
    )
  }
}

@Composable
private fun CanvasIconPill(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  desc: String,
  active: Boolean,
  tag: String,
  onClick: () -> Unit,
) {
  Box(
    modifier = Modifier
      .size(28.dp)
      .clip(RoundedCornerShape(14.dp))
      .background(if (active) StudioCyan.copy(alpha = 0.2f) else Color.Transparent)
      .clickable(onClick = onClick)
      .testTag(tag),
    contentAlignment = Alignment.Center,
  ) {
    Icon(
      imageVector = icon,
      contentDescription = desc,
      tint = if (active) StudioCyan else StudioTextSecondary,
      modifier = Modifier.size(16.dp),
    )
  }
}
