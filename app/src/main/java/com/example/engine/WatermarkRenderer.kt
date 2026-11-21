package com.example.engine

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.example.model.ImageAsset
import com.example.model.LayerType
import com.example.model.LogoLayerStyle
import com.example.model.TextAlignOption
import com.example.model.TextLayerStyle
import com.example.model.WatermarkLayer
import java.util.Calendar
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

data class LayerRenderBounds(
  val layerId: String,
  val centerX: Float,
  val centerY: Float,
  val width: Float,
  val height: Float,
  val rotationDeg: Float,
  val isTiled: Boolean,
)

data class RenderResult(
  val bitmap: Bitmap,
  val layerBounds: List<LayerRenderBounds>,
)

class WatermarkRenderer(
  private val fontRegistry: FontRegistry,
  private val logoRepository: LogoAssetRepository,
) {

  fun renderComposition(
    sourceBitmap: Bitmap,
    sourceAsset: ImageAsset?,
    layers: List<WatermarkLayer>,
    targetWidth: Int = sourceBitmap.width,
    targetHeight: Int = sourceBitmap.height,
    referenceLongEdgeOverride: Float? = null,
  ): RenderResult {
    val outW = targetWidth.coerceAtLeast(1)
    val outH = targetHeight.coerceAtLeast(1)

    val output = if (sourceBitmap.width == outW && sourceBitmap.height == outH) {
      sourceBitmap.copy(Bitmap.Config.ARGB_8888, true)
    } else {
      Bitmap.createScaledBitmap(sourceBitmap, outW, outH, true).let {
        if (it.config != Bitmap.Config.ARGB_8888) it.copy(Bitmap.Config.ARGB_8888, true) else it
      }
    }

    val unitScale = (referenceLongEdgeOverride ?: max(outW, outH).toFloat()) / 1000f
    val boundsList = mutableListOf<LayerRenderBounds>()

    for (layer in layers) {
      if (!layer.visible) continue
      val layerBitmap = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
      val layerCanvas = Canvas(layerBitmap)

      val bounds = renderSingleLayerToCanvas(
        canvas = layerCanvas,
        canvasW = outW,
        canvasH = outH,
        unitScale = unitScale,
        layer = layer,
        asset = sourceAsset,
      )
      if (bounds != null) {
        boundsList.add(bounds)
      }

      BlendCompositor.compositeLayer(
        dstBitmap = output,
        srcLayerBitmap = layerBitmap,
        blendMode = layer.blendMode,
        layerOpacity = layer.opacity,
      )
      layerBitmap.recycle()
    }

    return RenderResult(bitmap = output, layerBounds = boundsList)
  }

  private fun renderSingleLayerToCanvas(
    canvas: Canvas,
    canvasW: Int,
    canvasH: Int,
    unitScale: Float,
    layer: WatermarkLayer,
    asset: ImageAsset?,
  ): LayerRenderBounds? {
    val elementBitmap: Bitmap? = when (layer.type) {
      LayerType.TEXT -> layer.textStyle?.let { renderTextElement(it, unitScale, asset, canvasW, canvasH) }
      LayerType.LOGO -> layer.logoStyle?.let { renderLogoElement(it, canvasW, unitScale, layer.transform.scale) }
    }
    if (elementBitmap == null) return null

    if (layer.tile.enabled) {
      renderTiledElement(canvas, canvasW, canvasH, unitScale, elementBitmap, layer)
      val bounds = LayerRenderBounds(
        layerId = layer.id,
        centerX = canvasW / 2f,
        centerY = canvasH / 2f,
        width = canvasW.toFloat(),
        height = canvasH.toFloat(),
        rotationDeg = layer.tile.rotationDeg,
        isTiled = true,
      )
      elementBitmap.recycle()
      return bounds
    }

    val effScaleX = if (layer.type == LayerType.TEXT) layer.transform.scale else layer.transform.scaleX
    val effScaleY = if (layer.type == LayerType.TEXT) layer.transform.scale else {
      if (layer.transform.aspectRatioLocked) layer.transform.scaleX else layer.transform.scaleY
    }

    // For logos, renderLogoElement already baked transform.scale into width, so apply ratio overrides
    val drawScaleX = if (layer.type == LayerType.LOGO) (effScaleX / layer.transform.scale.coerceAtLeast(0.05f)) * layer.transform.scale else effScaleX
    val drawScaleY = if (layer.type == LayerType.LOGO) (effScaleY / layer.transform.scale.coerceAtLeast(0.05f)) * layer.transform.scale else effScaleY

    val finalScaleX = if (layer.type == LayerType.LOGO) effScaleX else layer.transform.scale * effScaleX.let { 1f }
    val finalScaleY = if (layer.type == LayerType.LOGO) effScaleY else layer.transform.scale * effScaleY.let { 1f }

    val scaledW = if (layer.type == LayerType.LOGO) {
      elementBitmap.width * (effScaleX / layer.transform.scale.coerceAtLeast(0.01f))
    } else {
      elementBitmap.width * layer.transform.scale
    }
    val scaledH = if (layer.type == LayerType.LOGO) {
      elementBitmap.height * (effScaleY / layer.transform.scale.coerceAtLeast(0.01f))
    } else {
      elementBitmap.height * layer.transform.scale
    }

    val cx = (layer.transform.xFraction * canvasW).coerceIn(-canvasW.toFloat(), canvasW * 2f)
    val cy = (layer.transform.yFraction * canvasH).coerceIn(-canvasH.toFloat(), canvasH * 2f)

    val matrix = Matrix().apply {
      postTranslate(-elementBitmap.width / 2f, -elementBitmap.height / 2f)
      if (layer.type == LayerType.LOGO) {
        val sx = effScaleX / layer.transform.scale.coerceAtLeast(0.01f)
        val sy = effScaleY / layer.transform.scale.coerceAtLeast(0.01f)
        postScale(sx, sy)
      } else {
        postScale(finalScaleX, finalScaleY)
      }
      postRotate(layer.transform.rotationDeg)
      postTranslate(cx, cy)
    }

    val drawPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    canvas.drawBitmap(elementBitmap, matrix, drawPaint)

    val bounds = LayerRenderBounds(
      layerId = layer.id,
      centerX = cx,
      centerY = cy,
      width = max(24f, scaledW),
      height = max(24f, scaledH),
      rotationDeg = layer.transform.rotationDeg,
      isTiled = false,
    )
    elementBitmap.recycle()
    return bounds
  }

  private fun renderTiledElement(
    canvas: Canvas,
    canvasW: Int,
    canvasH: Int,
    unitScale: Float,
    elementBitmap: Bitmap,
    layer: WatermarkLayer,
  ) {
    val tile = layer.tile
    val tileScale = tile.scale.coerceIn(0.15f, 3f)
    val stepX = max(40f, (elementBitmap.width * tileScale) + tile.spacingX * unitScale)
    val stepY = max(40f, (elementBitmap.height * tileScale) + tile.spacingY * unitScale)
    val diagonal = hypot(canvasW.toFloat(), canvasH.toFloat())
    val halfDiag = diagonal * 0.75f

    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
      alpha = (tile.opacity.coerceIn(0f, 1f) * 255f).toInt().coerceIn(0, 255)
    }

    canvas.save()
    canvas.translate(canvasW / 2f, canvasH / 2f)
    canvas.rotate(tile.rotationDeg)

    var rowIndex = 0
    var y = -halfDiag
    while (y <= halfDiag) {
      val rowOffset = if (tile.staggerRows && (rowIndex % 2 == 1)) stepX * 0.5f else 0f
      var x = -halfDiag - stepX + rowOffset
      while (x <= halfDiag + stepX) {
        val m = Matrix().apply {
          postTranslate(-elementBitmap.width / 2f, -elementBitmap.height / 2f)
          postScale(tileScale, tileScale)
          postTranslate(x, y)
        }
        canvas.drawBitmap(elementBitmap, m, paint)
        x += stepX
      }
      y += stepY
      rowIndex++
    }
    canvas.restore()
  }

  private fun expandTokens(raw: String, asset: ImageAsset?, canvasW: Int, canvasH: Int): String {
    val year = Calendar.getInstance().get(Calendar.YEAR).toString()
    val cal = Calendar.getInstance()
    val dateStr = "%04d-%02d-%02d".format(
      cal.get(Calendar.YEAR),
      cal.get(Calendar.MONTH) + 1,
      cal.get(Calendar.DAY_OF_MONTH),
    )
    val baseName = asset?.name?.substringBeforeLast('.') ?: "Untitled"
    val realW = asset?.width ?: canvasW
    val realH = asset?.height ?: canvasH
    return raw
      .replace("{YEAR}", year)
      .replace("{DATE}", dateStr)
      .replace("{FILE}", baseName)
      .replace("{FILENAME}", baseName)
      .replace("{WIDTH}", realW.toString())
      .replace("{HEIGHT}", realH.toString())
      .replace("{RES}", "${realW}x${realH}")
  }

  private fun renderTextElement(
    style: TextLayerStyle,
    unitScale: Float,
    asset: ImageAsset?,
    canvasW: Int,
    canvasH: Int,
  ): Bitmap {
    val resolvedText = expandTokens(style.text, asset, canvasW, canvasH).ifEmpty { " " }
    val fontSizePx = max(8f, style.fontSize * unitScale)
    val typeface = fontRegistry.resolveTypeface(style.fontId, style.fontWeight, style.isItalic)

    val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
      this.typeface = typeface
      this.textSize = fontSizePx
      this.letterSpacing = style.letterSpacingEm
      if (style.fontWeight >= 700) {
        isFakeBoldText = true
      }
    }

    val lines = resolvedText.split("\n")
    val lineWidths = lines.map { measureLineWidth(it, textPaint, style.wordSpacingPx * unitScale) }
    val maxTextW = max(20f, lineWidths.maxOrNull() ?: 20f)
    val fm = textPaint.fontMetrics
    val singleLineH = (fm.descent - fm.ascent) * style.lineHeightMult.coerceIn(0.6f, 3f)
    val totalTextH = max(20f, singleLineH * lines.size)

    val strokePad = if (style.stroke.enabled) style.stroke.widthPx * unitScale else 0f
    val shadowPad = if (style.shadow.enabled) (style.shadow.blurRadius + style.shadow.distance) * unitScale else 0f
    val glowPad = if (style.glow.enabled) style.glow.radius * unitScale * 1.5f else 0f
    val embossPad = if (style.emboss.enabled) style.emboss.depth * unitScale * 2f else 0f
    val bgPadH = if (style.background.enabled) style.background.paddingH * unitScale else 0f
    val bgPadV = if (style.background.enabled) style.background.paddingV * unitScale else 0f

    val extraPad = max(max(strokePad, shadowPad), max(glowPad, embossPad)) + 16f
    val padX = extraPad + bgPadH
    val padY = extraPad + bgPadV

    val bmpW = (maxTextW + padX * 2f).toInt().coerceIn(24, 4096)
    val bmpH = (totalTextH + padY * 2f).toInt().coerceIn(24, 4096)

    val bmp = Bitmap.createBitmap(bmpW, bmpH, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)

    // 1. Background highlight box
    if (style.background.enabled && style.background.opacity > 0.005f) {
      val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = applyOpacity(style.background.colorArgb.toInt(), style.background.opacity)
        this.style = Paint.Style.FILL
      }
      val bgRect = RectF(
        extraPad,
        extraPad,
        bmpW - extraPad,
        bmpH - extraPad,
      )
      val cr = style.background.cornerRadius * unitScale
      canvas.drawRoundRect(bgRect, cr, cr, bgPaint)
    }

    val contentLeft = padX
    val contentTop = padY

    fun drawTextPass(paint: TextPaint, offsetX: Float = 0f, offsetY: Float = 0f) {
      if (kotlin.math.abs(style.wordSpacingPx) < 0.2f) {
        val alignment = when (style.textAlign) {
          TextAlignOption.LEFT -> Layout.Alignment.ALIGN_NORMAL
          TextAlignOption.CENTER -> Layout.Alignment.ALIGN_CENTER
          TextAlignOption.RIGHT -> Layout.Alignment.ALIGN_OPPOSITE
        }
        val layout = StaticLayout.Builder
          .obtain(resolvedText, 0, resolvedText.length, paint, maxTextW.toInt().coerceAtLeast(1))
          .setAlignment(alignment)
          .setLineSpacing(0f, style.lineHeightMult.coerceIn(0.6f, 3f))
          .setIncludePad(false)
          .build()
        canvas.save()
        canvas.translate(contentLeft + offsetX, contentTop + offsetY)
        layout.draw(canvas)
        canvas.restore()
      } else {
        val wordExtra = style.wordSpacingPx * unitScale
        var y = contentTop + offsetY - fm.ascent
        for (i in lines.indices) {
          val line = lines[i]
          val lineW = lineWidths[i]
          val startX = when (style.textAlign) {
            TextAlignOption.LEFT -> contentLeft + offsetX
            TextAlignOption.CENTER -> contentLeft + offsetX + (maxTextW - lineW) / 2f
            TextAlignOption.RIGHT -> contentLeft + offsetX + (maxTextW - lineW)
          }
          drawLineWithWordSpacing(canvas, line, startX, y, paint, wordExtra)
          y += singleLineH
        }
      }
    }

    // 2. Outer Glow pass
    if (style.glow.enabled && style.glow.opacity > 0.005f) {
      val glowPaint = TextPaint(textPaint).apply {
        color = applyOpacity(style.glow.colorArgb.toInt(), style.glow.opacity)
        val r = (style.glow.radius * unitScale).coerceAtLeast(1f)
        maskFilter = BlurMaskFilter(r, BlurMaskFilter.Blur.NORMAL)
      }
      drawTextPass(glowPaint)
    }

    // 3. Drop Shadow pass
    if (style.shadow.enabled && style.shadow.opacity > 0.005f) {
      val rad = style.shadow.angleDeg * PI.toFloat() / 180f
      val dx = cos(rad) * style.shadow.distance * unitScale
      val dy = sin(rad) * style.shadow.distance * unitScale
      val shadowPaint = TextPaint(textPaint).apply {
        color = applyOpacity(style.shadow.colorArgb.toInt(), style.shadow.opacity)
        val blur = (style.shadow.blurRadius * unitScale).coerceAtLeast(0.5f)
        maskFilter = BlurMaskFilter(blur, BlurMaskFilter.Blur.NORMAL)
      }
      drawTextPass(shadowPaint, dx, dy)
    }

    // 4. Emboss bevel pass
    if (style.emboss.enabled && style.emboss.opacity > 0.005f) {
      val rad = style.emboss.azimuthDeg * PI.toFloat() / 180f
      val d = style.emboss.depth * unitScale
      val ox = cos(rad) * d
      val oy = sin(rad) * d
      val hiPaint = TextPaint(textPaint).apply {
        color = applyOpacity(Color.WHITE, style.emboss.opacity * 0.75f)
      }
      val loPaint = TextPaint(textPaint).apply {
        color = applyOpacity(Color.BLACK, style.emboss.opacity * 0.75f)
      }
      drawTextPass(loPaint, ox, oy)
      drawTextPass(hiPaint, -ox, -oy)
    }

    // 5. Stroke / Outline pass
    if (style.stroke.enabled && style.stroke.opacity > 0.005f && style.stroke.widthPx > 0.1f) {
      val strokePaint = TextPaint(textPaint).apply {
        this.style = Paint.Style.STROKE
        strokeWidth = style.stroke.widthPx * unitScale * 2f
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        color = applyOpacity(style.stroke.colorArgb.toInt(), style.stroke.opacity)
      }
      drawTextPass(strokePaint)
    }

    // 6. Main Text Fill (Solid or Gradient)
    if (style.fillOpacity > 0.005f) {
      val fillPaint = TextPaint(textPaint).apply {
        this.style = Paint.Style.FILL
        val c1 = applyOpacity(style.fillColorArgb.toInt(), style.fillOpacity)
        val c2 = applyOpacity(style.gradientEndColorArgb.toInt(), style.fillOpacity)
        when (style.fillMode) {
          com.example.model.FillMode.SOLID -> {
            color = c1
          }
          com.example.model.FillMode.LINEAR_GRADIENT -> {
            val rad = style.gradientAngleDeg * PI.toFloat() / 180f
            val cx = contentLeft + maxTextW / 2f
            val cy = contentTop + totalTextH / 2f
            val halfSpan = max(maxTextW, totalTextH) / 2f
            val dx = cos(rad) * halfSpan
            val dy = sin(rad) * halfSpan
            shader = LinearGradient(
              cx - dx, cy - dy, cx + dx, cy + dy,
              c1, c2, Shader.TileMode.CLAMP,
            )
          }
          com.example.model.FillMode.RADIAL_GRADIENT -> {
            val cx = contentLeft + maxTextW / 2f
            val cy = contentTop + totalTextH / 2f
            val r = max(20f, max(maxTextW, totalTextH) * 0.6f)
            shader = RadialGradient(
              cx, cy, r,
              c1, c2, Shader.TileMode.CLAMP,
            )
          }
        }
      }
      drawTextPass(fillPaint)
    }

    return bmp
  }

  private fun measureLineWidth(line: String, paint: TextPaint, extraWordSpacing: Float): Float {
    if (kotlin.math.abs(extraWordSpacing) < 0.2f || !line.contains(' ')) {
      return paint.measureText(line)
    }
    val words = line.split(' ')
    var total = 0f
    val spaceW = paint.measureText(" ")
    for (i in words.indices) {
      total += paint.measureText(words[i])
      if (i < words.lastIndex) {
        total += spaceW + extraWordSpacing
      }
    }
    return total
  }

  private fun drawLineWithWordSpacing(
    canvas: Canvas,
    line: String,
    startX: Float,
    baselineY: Float,
    paint: TextPaint,
    extraWordSpacing: Float,
  ) {
    val words = line.split(' ')
    var x = startX
    val spaceW = paint.measureText(" ")
    for (i in words.indices) {
      val word = words[i]
      canvas.drawText(word, x, baselineY, paint)
      x += paint.measureText(word) + spaceW + extraWordSpacing
    }
  }

  private fun renderLogoElement(
    style: LogoLayerStyle,
    canvasW: Int,
    unitScale: Float,
    layerScale: Float,
  ): Bitmap? {
    val targetW = (canvasW * style.baseWidthFraction * layerScale).toInt().coerceIn(24, 3072)
    val rawLogo = logoRepository.renderLogoBitmap(style.assetId, targetW) ?: return null

    // Apply brightness, contrast, saturation, grayscale, monochrome, tint, and opacity
    val adjusted = Bitmap.createBitmap(rawLogo.width, rawLogo.height, Bitmap.Config.ARGB_8888)
    val adjCanvas = Canvas(adjusted)

    val cm = ColorMatrix()
    if (style.grayscale || style.monochrome) {
      val sat = ColorMatrix().apply { setSaturation(0f) }
      cm.postConcat(sat)
    } else if (kotlin.math.abs(style.saturation - 1f) > 0.01f) {
      val sat = ColorMatrix().apply { setSaturation(style.saturation.coerceIn(0f, 3f)) }
      cm.postConcat(sat)
    }

    val c = if (style.monochrome) style.contrast.coerceAtLeast(2.2f) else style.contrast
    val b = style.brightness * 255f
    val translate = (-0.5f * c + 0.5f) * 255f + b
    val bcMatrix = ColorMatrix(
      floatArrayOf(
        c, 0f, 0f, 0f, translate,
        0f, c, 0f, 0f, translate,
        0f, 0f, c, 0f, translate,
        0f, 0f, 0f, 1f, 0f,
      )
    )
    cm.postConcat(bcMatrix)

    val logoPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
      colorFilter = ColorMatrixColorFilter(cm)
      alpha = (style.logoOpacity.coerceIn(0f, 1f) * 255f).toInt().coerceIn(0, 255)
    }
    adjCanvas.drawBitmap(rawLogo, 0f, 0f, logoPaint)

    if (style.tintEnabled && style.tintIntensity > 0.01f) {
      val tintAlpha = (style.tintIntensity.coerceIn(0f, 1f) * 255f).toInt().coerceIn(0, 255)
      val tintColor = (style.tintColorArgb.toInt() and 0x00FFFFFF) or (tintAlpha shl 24)
      val tinted = Bitmap.createBitmap(adjusted.width, adjusted.height, Bitmap.Config.ARGB_8888)
      val tCanvas = Canvas(tinted)
      tCanvas.drawBitmap(adjusted, 0f, 0f, null)
      val tPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        colorFilter = PorterDuffColorFilter(tintColor, PorterDuff.Mode.SRC_ATOP)
        alpha = tintAlpha
      }
      tCanvas.drawBitmap(adjusted, 0f, 0f, tPaint)
      adjusted.recycle()
      // Replace adjusted with tinted
      return decorateLogoWithEffects(tinted, style, unitScale)
    }

    return decorateLogoWithEffects(adjusted, style, unitScale)
  }

  private fun decorateLogoWithEffects(
    logoBmp: Bitmap,
    style: LogoLayerStyle,
    unitScale: Float,
  ): Bitmap {
    val hasEffects = style.shadow.enabled || style.glow.enabled || style.stroke.enabled || style.emboss.enabled
    if (!hasEffects) return logoBmp

    val strokePad = if (style.stroke.enabled) style.stroke.widthPx * unitScale else 0f
    val shadowPad = if (style.shadow.enabled) (style.shadow.blurRadius + style.shadow.distance) * unitScale else 0f
    val glowPad = if (style.glow.enabled) style.glow.radius * unitScale * 1.5f else 0f
    val embossPad = if (style.emboss.enabled) style.emboss.depth * unitScale * 2f else 0f
    val pad = max(max(strokePad, shadowPad), max(glowPad, embossPad)) + 8f

    val outW = (logoBmp.width + pad * 2).toInt().coerceIn(24, 4096)
    val outH = (logoBmp.height + pad * 2).toInt().coerceIn(24, 4096)
    val out = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(out)
    val alphaMask = logoBmp.extractAlpha()

    if (style.glow.enabled && style.glow.opacity > 0.005f) {
      val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = applyOpacity(style.glow.colorArgb.toInt(), style.glow.opacity)
        maskFilter = BlurMaskFilter((style.glow.radius * unitScale).coerceAtLeast(1f), BlurMaskFilter.Blur.NORMAL)
      }
      canvas.drawBitmap(alphaMask, pad, pad, p)
    }

    if (style.shadow.enabled && style.shadow.opacity > 0.005f) {
      val rad = style.shadow.angleDeg * PI.toFloat() / 180f
      val dx = cos(rad) * style.shadow.distance * unitScale
      val dy = sin(rad) * style.shadow.distance * unitScale
      val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = applyOpacity(style.shadow.colorArgb.toInt(), style.shadow.opacity)
        maskFilter = BlurMaskFilter((style.shadow.blurRadius * unitScale).coerceAtLeast(0.5f), BlurMaskFilter.Blur.NORMAL)
      }
      canvas.drawBitmap(alphaMask, pad + dx, pad + dy, p)
    }

    if (style.stroke.enabled && style.stroke.opacity > 0.005f) {
      val r = (style.stroke.widthPx * unitScale).coerceAtLeast(1f)
      val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = applyOpacity(style.stroke.colorArgb.toInt(), style.stroke.opacity)
      }
      val steps = 8
      for (i in 0 until steps) {
        val a = (2f * PI.toFloat() * i) / steps
        canvas.drawBitmap(alphaMask, pad + cos(a) * r, pad + sin(a) * r, p)
      }
    }

    if (style.emboss.enabled && style.emboss.opacity > 0.005f) {
      val rad = style.emboss.azimuthDeg * PI.toFloat() / 180f
      val d = style.emboss.depth * unitScale
      val ox = cos(rad) * d
      val oy = sin(rad) * d
      val lo = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = applyOpacity(Color.BLACK, style.emboss.opacity * 0.7f) }
      val hi = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = applyOpacity(Color.WHITE, style.emboss.opacity * 0.7f) }
      canvas.drawBitmap(alphaMask, pad + ox, pad + oy, lo)
      canvas.drawBitmap(alphaMask, pad - ox, pad - oy, hi)
    }

    canvas.drawBitmap(logoBmp, pad, pad, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
    alphaMask.recycle()
    logoBmp.recycle()
    return out
  }

  private fun applyOpacity(argbColor: Int, opacity: Float): Int {
    val baseAlpha = (argbColor ushr 24) and 0xFF
    val effectiveAlpha = ((baseAlpha / 255f) * opacity.coerceIn(0f, 1f) * 255f).toInt().coerceIn(0, 255)
    return (argbColor and 0x00FFFFFF) or (effectiveAlpha shl 24)
  }
}
