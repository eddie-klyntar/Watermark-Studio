package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ColorSpace
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import androidx.exifinterface.media.ExifInterface
import com.example.model.ExportFormat
import com.example.model.ExportSettings
import com.example.model.IccProfileMode
import com.example.model.ImageAsset
import com.example.model.ResizeMode
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream
import kotlin.math.max
import kotlin.math.roundToInt

class ImageIoManager(private val context: Context) {

  fun inspectAssetFromUri(uri: Uri): ImageAsset? {
    val name = resolveDisplayName(uri) ?: "Image"
    val size = resolveFileSize(uri)
    val mime = context.contentResolver.getType(uri).orEmpty()

    return runCatching {
      val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
      if (TiffCodec.isTiff(bytes)) {
        val bmp = TiffCodec.decodeTiff(bytes) ?: return null
        val w = bmp.width
        val h = bmp.height
        val hasAlpha = bmp.hasAlpha()
        bmp.recycle()
        return ImageAsset(
          name = name,
          uriString = uri.toString(),
          width = w,
          height = h,
          formatLabel = "TIFF",
          fileSizeBytes = bytes.size.toLong(),
          hasAlpha = hasAlpha,
          hasExif = false,
          iccProfileName = "sRGB IEC61966-2.1",
        )
      }

      val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
      BitmapFactory.decodeByteArray(bytes, 0, bytes.size, boundsOpts)
      var w = boundsOpts.outWidth
      var h = boundsOpts.outHeight
      if (w <= 0 || h <= 0) return null

      var hasExif = false
      val exifOrientation = runCatching {
        ExifInterface(ByteArrayInputStream(bytes)).let { exif ->
          val orient = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
          hasExif = exif.getAttribute(ExifInterface.TAG_MAKE) != null ||
            exif.getAttribute(ExifInterface.TAG_DATETIME) != null ||
            orient != ExifInterface.ORIENTATION_UNDEFINED
          orient
        }
      }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

      if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90 ||
        exifOrientation == ExifInterface.ORIENTATION_ROTATE_270 ||
        exifOrientation == ExifInterface.ORIENTATION_TRANSPOSE ||
        exifOrientation == ExifInterface.ORIENTATION_TRANSVERSE
      ) {
        val tmp = w
        w = h
        h = tmp
      }

      val fmt = when {
        mime.contains("png", true) || name.endsWith(".png", true) -> "PNG"
        mime.contains("webp", true) || name.endsWith(".webp", true) -> "WEBP"
        mime.contains("bmp", true) || name.endsWith(".bmp", true) -> "BMP"
        else -> "JPEG"
      }

      val iccName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        boundsOpts.outColorSpace?.name ?: "sRGB IEC61966-2.1"
      } else {
        "sRGB IEC61966-2.1"
      }

      ImageAsset(
        name = name,
        uriString = uri.toString(),
        width = w,
        height = h,
        formatLabel = fmt,
        fileSizeBytes = if (size > 0) size else bytes.size.toLong(),
        hasAlpha = fmt == "PNG" || fmt == "WEBP",
        hasExif = hasExif,
        iccProfileName = iccName,
      )
    }.getOrNull()
  }

  fun inspectSampleDrawable(resId: Int, name: String): ImageAsset? {
    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeResource(context.resources, resId, opts)
    if (opts.outWidth <= 0 || opts.outHeight <= 0) return null
    return ImageAsset(
      name = name,
      sampleDrawableRes = resId,
      width = opts.outWidth,
      height = opts.outHeight,
      formatLabel = "JPEG",
      fileSizeBytes = 0L,
      hasAlpha = false,
      hasExif = false,
      iccProfileName = "sRGB IEC61966-2.1",
    )
  }

  fun decodePreviewBitmap(asset: ImageAsset, maxLongEdge: Int = 1800): Bitmap? {
    if (asset.sampleDrawableRes != null) {
      val opts = BitmapFactory.Options().apply {
        inPreferredConfig = Bitmap.Config.ARGB_8888
        inSampleSize = computeSampleSize(asset.width, asset.height, maxLongEdge)
      }
      val bmp = BitmapFactory.decodeResource(context.resources, asset.sampleDrawableRes, opts) ?: return null
      return downscaleIfNeeded(bmp, maxLongEdge)
    }

    val uriStr = asset.uriString ?: return null
    val uri = Uri.parse(uriStr)
    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null

    if (TiffCodec.isTiff(bytes)) {
      val full = TiffCodec.decodeTiff(bytes) ?: return null
      return downscaleIfNeeded(full, maxLongEdge)
    }

    val opts = BitmapFactory.Options().apply {
      inPreferredConfig = Bitmap.Config.ARGB_8888
      inSampleSize = computeSampleSize(asset.width, asset.height, maxLongEdge)
    }
    val raw = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts) ?: return null
    val oriented = applyExifOrientation(raw, bytes)
    return downscaleIfNeeded(oriented, maxLongEdge)
  }

  fun decodeFullResolutionBitmap(asset: ImageAsset): Pair<Bitmap, ExifInterface?>? {
    if (asset.sampleDrawableRes != null) {
      val opts = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
      val bmp = BitmapFactory.decodeResource(context.resources, asset.sampleDrawableRes, opts) ?: return null
      return bmp to null
    }
    val uri = asset.uriString?.let { Uri.parse(it) } ?: return null
    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
    if (TiffCodec.isTiff(bytes)) {
      val bmp = TiffCodec.decodeTiff(bytes) ?: return null
      return bmp to null
    }
    val opts = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
    val raw = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts) ?: return null
    val exif = runCatching { ExifInterface(ByteArrayInputStream(bytes)) }.getOrNull()
    val oriented = applyExifOrientation(raw, bytes)
    return oriented to exif
  }

  fun readSourceBytes(asset: ImageAsset): ByteArray? {
    if (asset.sampleDrawableRes != null) return null
    val uri = asset.uriString?.let { Uri.parse(it) } ?: return null
    return runCatching {
      context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
    }.getOrNull()
  }

  fun computeTargetDimensions(srcW: Int, srcH: Int, settings: ExportSettings): Pair<Int, Int> {
    return when (settings.resizeMode) {
      ResizeMode.ORIGINAL -> srcW to srcH
      ResizeMode.SCALE_PERCENT -> {
        val factor = settings.scalePercent.coerceIn(10, 200) / 100f
        max(1, (srcW * factor).roundToInt()) to max(1, (srcH * factor).roundToInt())
      }
      ResizeMode.FIT_LONG_EDGE -> {
        val maxEdge = settings.maxLongEdgePx.coerceIn(128, 12000)
        val curLong = max(srcW, srcH)
        if (curLong <= maxEdge) {
          srcW to srcH
        } else {
          val ratio = maxEdge.toFloat() / curLong.toFloat()
          max(1, (srcW * ratio).roundToInt()) to max(1, (srcH * ratio).roundToInt())
        }
      }
    }
  }

  fun writeExportedImage(
    renderedBitmap: Bitmap,
    sourceExif: ExifInterface?,
    settings: ExportSettings,
    outputStream: OutputStream,
  ) {
    val preparedBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
      settings.iccProfileMode == IccProfileMode.EMBED_SRGB &&
      renderedBitmap.colorSpace != ColorSpace.get(ColorSpace.Named.SRGB)
    ) {
      renderedBitmap.copy(Bitmap.Config.ARGB_8888, false).also {
        it.setColorSpace(ColorSpace.get(ColorSpace.Named.SRGB))
      }
    } else {
      renderedBitmap
    }

    when (settings.format) {
      ExportFormat.TIFF -> {
        TiffCodec.encodeTiff(preparedBitmap, outputStream)
      }
      ExportFormat.PNG -> {
        preparedBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        outputStream.flush()
      }
      ExportFormat.WEBP -> {
        val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
          if (settings.webpLossless) Bitmap.CompressFormat.WEBP_LOSSLESS else Bitmap.CompressFormat.WEBP_LOSSY
        } else {
          @Suppress("DEPRECATION")
          Bitmap.CompressFormat.WEBP
        }
        val q = if (settings.webpLossless) 100 else settings.quality.coerceIn(1, 100)
        preparedBitmap.compress(format, q, outputStream)
        outputStream.flush()
      }
      ExportFormat.JPEG -> {
        if (!settings.preserveMetadata || sourceExif == null) {
          preparedBitmap.compress(Bitmap.CompressFormat.JPEG, settings.quality.coerceIn(1, 100), outputStream)
          outputStream.flush()
        } else {
          val tempFile = File.createTempFile("wm_exp_", ".jpg", context.cacheDir)
          try {
            tempFile.outputStream().use {
              preparedBitmap.compress(Bitmap.CompressFormat.JPEG, settings.quality.coerceIn(1, 100), it)
            }
            copyExifMetadata(sourceExif, tempFile, preparedBitmap.width, preparedBitmap.height)
            tempFile.inputStream().use { it.copyTo(outputStream) }
            outputStream.flush()
          } finally {
            tempFile.delete()
          }
        }
      }
    }

    if (preparedBitmap !== renderedBitmap) {
      preparedBitmap.recycle()
    }
  }

  private fun copyExifMetadata(src: ExifInterface, destFile: File, width: Int, height: Int) {
    val dst = ExifInterface(destFile.absolutePath)
    val tags = listOf(
      ExifInterface.TAG_MAKE,
      ExifInterface.TAG_MODEL,
      ExifInterface.TAG_DATETIME,
      ExifInterface.TAG_DATETIME_ORIGINAL,
      ExifInterface.TAG_EXPOSURE_TIME,
      ExifInterface.TAG_F_NUMBER,
      ExifInterface.TAG_ISO_SPEED_RATINGS,
      ExifInterface.TAG_FOCAL_LENGTH,
      ExifInterface.TAG_LENS_MAKE,
      ExifInterface.TAG_LENS_MODEL,
      ExifInterface.TAG_COPYRIGHT,
      ExifInterface.TAG_ARTIST,
      ExifInterface.TAG_GPS_LATITUDE,
      ExifInterface.TAG_GPS_LATITUDE_REF,
      ExifInterface.TAG_GPS_LONGITUDE,
      ExifInterface.TAG_GPS_LONGITUDE_REF,
    )
    for (tag in tags) {
      src.getAttribute(tag)?.let { dst.setAttribute(tag, it) }
    }
    dst.setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
    dst.setAttribute(ExifInterface.TAG_IMAGE_WIDTH, width.toString())
    dst.setAttribute(ExifInterface.TAG_IMAGE_LENGTH, height.toString())
    dst.setAttribute(ExifInterface.TAG_SOFTWARE, "Watermark Studio")
    dst.saveAttributes()
  }

  private fun applyExifOrientation(bitmap: Bitmap, bytes: ByteArray): Bitmap {
    val orient = runCatching {
      ExifInterface(ByteArrayInputStream(bytes))
        .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

    val matrix = Matrix()
    when (orient) {
      ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
      ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
      ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
      ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
      ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
      ExifInterface.ORIENTATION_TRANSPOSE -> {
        matrix.postRotate(90f)
        matrix.postScale(-1f, 1f)
      }
      ExifInterface.ORIENTATION_TRANSVERSE -> {
        matrix.postRotate(270f)
        matrix.postScale(-1f, 1f)
      }
      else -> return bitmap
    }
    val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    if (rotated !== bitmap) bitmap.recycle()
    return rotated
  }

  private fun downscaleIfNeeded(bitmap: Bitmap, maxLongEdge: Int): Bitmap {
    val curLong = max(bitmap.width, bitmap.height)
    if (curLong <= maxLongEdge) {
      return if (bitmap.config == Bitmap.Config.ARGB_8888) bitmap else bitmap.copy(Bitmap.Config.ARGB_8888, false)
    }
    val ratio = maxLongEdge.toFloat() / curLong.toFloat()
    val w = max(1, (bitmap.width * ratio).roundToInt())
    val h = max(1, (bitmap.height * ratio).roundToInt())
    val scaled = Bitmap.createScaledBitmap(bitmap, w, h, true)
    if (scaled !== bitmap) bitmap.recycle()
    return if (scaled.config == Bitmap.Config.ARGB_8888) scaled else scaled.copy(Bitmap.Config.ARGB_8888, false)
  }

  private fun computeSampleSize(w: Int, h: Int, maxLongEdge: Int): Int {
    val longEdge = max(w, h)
    var sample = 1
    while (longEdge / sample > maxLongEdge * 2) {
      sample *= 2
    }
    return sample
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

  private fun resolveFileSize(uri: Uri): Long {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)
      ?.use { cursor ->
        if (cursor.moveToFirst()) {
          val idx = cursor.getColumnIndex(OpenableColumns.SIZE)
          if (idx >= 0) return cursor.getLong(idx)
        }
      }
    return 0L
  }
}
