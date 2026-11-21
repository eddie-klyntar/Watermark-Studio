package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.net.Uri
import android.provider.OpenableColumns
import com.caverock.androidsvg.SVG
import com.example.model.SerializedLogoEntry
import java.io.ByteArrayInputStream
import java.io.File
import java.util.UUID

data class LogoAsset(
  val id: String,
  val displayName: String,
  val isSvg: Boolean,
  val isBuiltIn: Boolean,
  val fileName: String? = null,
)

class LogoAssetRepository(private val context: Context) {

  private val logoDir = File(context.filesDir, "logo_assets").apply { mkdirs() }
  private val rasterCache = mutableMapOf<String, Bitmap>()
  private val svgCache = mutableMapOf<String, SVG>()

  private val builtInLogos = listOf(
    LogoAsset("builtin_aperture", "Aperture Crest", true, true),
    LogoAsset("builtin_diamond_seal", "Diamond Copyright", true, true),
    LogoAsset("builtin_atelier_frame", "Atelier Monogram", true, true),
    LogoAsset("builtin_verified_stamp", "Studio Stamp", true, true),
  )

  private val customLogos = mutableListOf<LogoAsset>()

  init {
    loadSavedCustomLogos()
  }

  private fun loadSavedCustomLogos() {
    customLogos.clear()
    logoDir.listFiles()?.sortedBy { it.name }?.forEach { file ->
      val id = file.nameWithoutExtension
      val isSvg = file.extension.equals("svg", true)
      val display = id.substringAfter("__", id).replace('_', ' ')
      customLogos.add(
        LogoAsset(
          id = id,
          displayName = display,
          isSvg = isSvg,
          isBuiltIn = false,
          fileName = file.name,
        )
      )
    }
  }

  fun getAllLogos(): List<LogoAsset> = builtInLogos + customLogos

  fun getCustomLogoEntries(): List<SerializedLogoEntry> =
    customLogos.mapNotNull { logo ->
      logo.fileName?.let { SerializedLogoEntry(logo.id, logo.displayName, it, logo.isSvg) }
    }

  fun getCustomLogoFile(assetId: String): File? {
    val entry = customLogos.find { it.id == assetId } ?: return null
    return entry.fileName?.let { File(logoDir, it) }?.takeIf { it.exists() }
  }

  fun importLogoFromUri(uri: Uri): LogoAsset? {
    val rawName = resolveDisplayName(uri) ?: "logo.png"
    val mime = context.contentResolver.getType(uri).orEmpty()
    val isSvg = rawName.endsWith(".svg", true) || mime.contains("svg", true)
    val ext = when {
      isSvg -> "svg"
      rawName.endsWith(".webp", true) -> "webp"
      rawName.endsWith(".jpg", true) || rawName.endsWith(".jpeg", true) -> "jpg"
      rawName.endsWith(".tif", true) || rawName.endsWith(".tiff", true) -> "tif"
      rawName.endsWith(".bmp", true) -> "bmp"
      else -> "png"
    }
    val cleanTitle = rawName.substringBeforeLast('.')
      .replace(Regex("[^A-Za-z0-9 _-]"), "")
      .trim()
      .ifBlank { "Studio Logo" }
    val id = "logo_${UUID.randomUUID().toString().take(8)}__${cleanTitle.replace(' ', '_')}"
    val destFile = File(logoDir, "$id.$ext")

    return runCatching {
      val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
      destFile.writeBytes(bytes)

      if (isSvg) {
        val svg = SVG.getFromInputStream(ByteArrayInputStream(bytes))
        svgCache[id] = svg
      } else {
        val bmp = decodeRasterBytes(bytes) ?: return null
        rasterCache[id] = bmp
      }

      val asset = LogoAsset(
        id = id,
        displayName = cleanTitle,
        isSvg = isSvg,
        isBuiltIn = false,
        fileName = destFile.name,
      )
      customLogos.add(asset)
      asset
    }.onFailure {
      destFile.delete()
    }.getOrNull()
  }

  fun restoreCustomLogo(entry: SerializedLogoEntry, bytes: ByteArray) {
    val dest = File(logoDir, entry.fileName)
    dest.writeBytes(bytes)
    runCatching {
      if (entry.isSvg) {
        svgCache[entry.id] = SVG.getFromInputStream(ByteArrayInputStream(bytes))
      } else {
        decodeRasterBytes(bytes)?.let { rasterCache[entry.id] = it }
      }
      if (customLogos.none { it.id == entry.id }) {
        customLogos.add(
          LogoAsset(
            id = entry.id,
            displayName = entry.displayName,
            isSvg = entry.isSvg,
            isBuiltIn = false,
            fileName = entry.fileName,
          )
        )
      }
    }
  }

  /**
   * Renders a logo asset (vector SVG or raster PNG/JPG/WebP/TIFF) into an ARGB_8888 Bitmap
   * scaled cleanly to targetWidthPx while preserving its intrinsic aspect ratio.
   */
  fun renderLogoBitmap(assetId: String, targetWidthPx: Int): Bitmap? {
    val safeTargetW = targetWidthPx.coerceIn(16, 4096)

    val svg = getSvg(assetId)
    if (svg != null) {
      val docW = svg.documentWidth.takeIf { it > 0f } ?: 200f
      val docH = svg.documentHeight.takeIf { it > 0f } ?: 200f
      val aspect = docH / docW
      val outW = safeTargetW
      val outH = (safeTargetW * aspect).toInt().coerceIn(16, 4096)
      svg.setDocumentWidth(outW.toFloat())
      svg.setDocumentHeight(outH.toFloat())
      val bmp = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
      val canvas = Canvas(bmp)
      svg.renderToCanvas(canvas)
      return bmp
    }

    val raster = getRasterBitmap(assetId) ?: return null
    val aspect = raster.height.toFloat() / raster.width.coerceAtLeast(1).toFloat()
    val outW = safeTargetW
    val outH = (safeTargetW * aspect).toInt().coerceIn(16, 4096)
    return if (raster.width == outW && raster.height == outH) {
      raster
    } else {
      Bitmap.createScaledBitmap(raster, outW, outH, true)
    }
  }

  private fun getSvg(assetId: String): SVG? {
    svgCache[assetId]?.let { return it }
    val builtInMarkup = builtInSvgMarkup(assetId)
    if (builtInMarkup != null) {
      return runCatching {
        SVG.getFromString(builtInMarkup).also { svgCache[assetId] = it }
      }.getOrNull()
    }
    val file = getCustomLogoFile(assetId)?.takeIf { it.extension.equals("svg", true) } ?: return null
    return runCatching {
      file.inputStream().use { SVG.getFromInputStream(it) }.also { svgCache[assetId] = it }
    }.getOrNull()
  }

  private fun getRasterBitmap(assetId: String): Bitmap? {
    rasterCache[assetId]?.let { return it }
    val file = getCustomLogoFile(assetId) ?: return null
    return runCatching {
      decodeRasterBytes(file.readBytes())?.also { rasterCache[assetId] = it }
    }.getOrNull()
  }

  private fun decodeRasterBytes(bytes: ByteArray): Bitmap? {
    if (TiffCodec.isTiff(bytes)) return TiffCodec.decodeTiff(bytes)
    val opts = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
  }

  private fun resolveDisplayName(uri: Uri): String? {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
      ?.use { cursor ->
        if (cursor.moveToFirst()) {
          val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
          if (idx >= 0) return cursor.getString(idx)
        }
      }
    return uri.lastPathSegment
  }

  private fun builtInSvgMarkup(id: String): String? = when (id) {
    "builtin_aperture" -> """
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 200 200">
        <circle cx="100" cy="100" r="88" fill="none" stroke="#FFFFFF" stroke-width="8"/>
        <circle cx="100" cy="100" r="72" fill="none" stroke="#FFFFFF" stroke-width="2.5" stroke-dasharray="8 6"/>
        <polygon points="100,32 132,76 112,76 144,122 100,104 68,144 82,102 48,76 84,76" fill="#F59E0B" opacity="0.95"/>
        <circle cx="100" cy="100" r="18" fill="#0D0F14" stroke="#FFFFFF" stroke-width="5"/>
      </svg>
    """.trimIndent()
    "builtin_diamond_seal" -> """
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 200 200">
        <polygon points="100,14 186,100 100,186 14,100" fill="none" stroke="#FFFFFF" stroke-width="8"/>
        <polygon points="100,32 168,100 100,168 32,100" fill="none" stroke="#38BDF8" stroke-width="3"/>
        <circle cx="100" cy="100" r="36" fill="none" stroke="#FFFFFF" stroke-width="7"/>
        <path d="M116 82 A24 24 0 1 0 116 118" fill="none" stroke="#FFFFFF" stroke-width="8" stroke-linecap="round"/>
      </svg>
    """.trimIndent()
    "builtin_atelier_frame" -> """
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 240 160">
        <rect x="10" y="10" width="220" height="140" rx="8" fill="none" stroke="#FFFFFF" stroke-width="6"/>
        <rect x="22" y="22" width="196" height="116" rx="4" fill="none" stroke="#FFFFFF" stroke-width="2"/>
        <path d="M70 112 L104 44 L138 112 M84 88 L124 88" fill="none" stroke="#FFFFFF" stroke-width="7" stroke-linecap="round" stroke-linejoin="round"/>
        <circle cx="164" cy="78" r="24" fill="none" stroke="#F59E0B" stroke-width="6"/>
      </svg>
    """.trimIndent()
    "builtin_verified_stamp" -> """
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 200 200">
        <circle cx="100" cy="100" r="82" fill="none" stroke="#FFFFFF" stroke-width="10"/>
        <circle cx="100" cy="100" r="64" fill="none" stroke="#FFFFFF" stroke-width="3"/>
        <path d="M62 104 L88 130 L140 72" fill="none" stroke="#F59E0B" stroke-width="12" stroke-linecap="round" stroke-linejoin="round"/>
      </svg>
    """.trimIndent()
    else -> null
  }
}
