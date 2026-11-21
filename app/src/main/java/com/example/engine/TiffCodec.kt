package com.example.engine

import android.graphics.Bitmap
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.min

/**
 * Pure Kotlin Baseline TIFF Reader & Writer supporting:
 * - Reading: Little-endian (II) & Big-endian (MM), Uncompressed (1) & PackBits (32773),
 *   Grayscale (1 spp), RGB (3 spp), RGBA (4 spp), 8-bit per channel, single or multi-strip.
 * - Writing: Standard Little-endian Uncompressed 32-bit RGBA Baseline TIFF with DPI & ICC tags.
 */
object TiffCodec {

  fun isTiff(bytes: ByteArray): Boolean {
    if (bytes.size < 4) return false
    val le = bytes[0] == 'I'.code.toByte() && bytes[1] == 'I'.code.toByte() &&
      bytes[2] == 42.toByte() && bytes[3] == 0.toByte()
    val be = bytes[0] == 'M'.code.toByte() && bytes[1] == 'M'.code.toByte() &&
      bytes[2] == 0.toByte() && bytes[3] == 42.toByte()
    return le || be
  }

  fun decodeTiff(bytes: ByteArray): Bitmap? {
    if (!isTiff(bytes)) return null
    val order = if (bytes[0] == 'I'.code.toByte()) ByteOrder.LITTLE_ENDIAN else ByteOrder.BIG_ENDIAN
    val buf = ByteBuffer.wrap(bytes).order(order)
    val ifdOffset = buf.getInt(4)
    if (ifdOffset < 8 || ifdOffset + 2 > bytes.size) return null

    val entryCount = buf.getShort(ifdOffset).toInt() and 0xFFFF
    var width = 0
    var height = 0
    var samplesPerPixel = 3
    var compression = 1
    var photometric = 2
    var rowsPerStrip = Int.MAX_VALUE
    var stripOffsets = IntArray(0)
    var stripByteCounts = IntArray(0)

    for (i in 0 until entryCount) {
      val base = ifdOffset + 2 + i * 12
      if (base + 12 > bytes.size) break
      val tag = buf.getShort(base).toInt() and 0xFFFF
      val type = buf.getShort(base + 2).toInt() and 0xFFFF
      val count = buf.getInt(base + 4)
      val valueOrOffset = base + 8

      fun readIntArray(): IntArray {
        val elemSize = when (type) {
          3 -> 2 // SHORT
          4 -> 4 // LONG
          else -> 4
        }
        val totalBytes = count * elemSize
        val start = if (totalBytes <= 4) valueOrOffset else buf.getInt(valueOrOffset)
        if (start < 0 || start + totalBytes > bytes.size) return IntArray(0)
        return IntArray(count) { idx ->
          if (type == 3) buf.getShort(start + idx * 2).toInt() and 0xFFFF
          else buf.getInt(start + idx * 4)
        }
      }

      fun readSingleInt(): Int {
        return when (type) {
          3 -> buf.getShort(valueOrOffset).toInt() and 0xFFFF
          4 -> buf.getInt(valueOrOffset)
          else -> buf.getInt(valueOrOffset)
        }
      }

      when (tag) {
        256 -> width = readSingleInt()
        257 -> height = readSingleInt()
        259 -> compression = readSingleInt()
        262 -> photometric = readSingleInt()
        273 -> stripOffsets = readIntArray()
        277 -> samplesPerPixel = readSingleInt()
        278 -> rowsPerStrip = readSingleInt()
        279 -> stripByteCounts = readIntArray()
      }
    }

    if (width <= 0 || height <= 0 || stripOffsets.isEmpty()) return null
    if (compression != 1 && compression != 32773) return null

    val pixels = IntArray(width * height)
    var currentRow = 0
    for (sIdx in stripOffsets.indices) {
      val offset = stripOffsets[sIdx]
      if (offset < 0 || offset >= bytes.size) break
      val stripCount = if (sIdx < stripByteCounts.size) stripByteCounts[sIdx] else (bytes.size - offset)
      val rowsInThisStrip = min(rowsPerStrip, height - currentRow)
      val expectedBytes = rowsInThisStrip * width * samplesPerPixel
      val stripData: ByteArray = if (compression == 1) {
        val end = min(bytes.size, offset + min(stripCount, expectedBytes))
        bytes.copyOfRange(offset, end)
      } else {
        decodePackBits(bytes, offset, min(bytes.size - offset, stripCount), expectedBytes)
      }

      var pos = 0
      for (r in 0 until rowsInThisStrip) {
        val y = currentRow + r
        if (y >= height) break
        for (x in 0 until width) {
          val argb = when (samplesPerPixel) {
            1 -> {
              if (pos >= stripData.size) 0xFF000000.toInt()
              else {
                var v = stripData[pos++].toInt() and 0xFF
                if (photometric == 0) v = 255 - v
                (0xFF shl 24) or (v shl 16) or (v shl 8) or v
              }
            }
            3 -> {
              if (pos + 2 >= stripData.size) 0xFF000000.toInt()
              else {
                val red = stripData[pos++].toInt() and 0xFF
                val green = stripData[pos++].toInt() and 0xFF
                val blue = stripData[pos++].toInt() and 0xFF
                (0xFF shl 24) or (red shl 16) or (green shl 8) or blue
              }
            }
            else -> {
              if (pos + 3 >= stripData.size) 0
              else {
                val red = stripData[pos++].toInt() and 0xFF
                val green = stripData[pos++].toInt() and 0xFF
                val blue = stripData[pos++].toInt() and 0xFF
                val alpha = stripData[pos++].toInt() and 0xFF
                pos += (samplesPerPixel - 4).coerceAtLeast(0)
                (alpha shl 24) or (red shl 16) or (green shl 8) or blue
              }
            }
          }
          pixels[y * width + x] = argb
        }
      }
      currentRow += rowsInThisStrip
    }

    return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
  }

  private fun decodePackBits(
    src: ByteArray,
    offset: Int,
    len: Int,
    expectedOut: Int,
  ): ByteArray {
    val out = ByteArrayOutputStream(expectedOut)
    var i = offset
    val end = min(src.size, offset + len)
    while (i < end && out.size() < expectedOut) {
      val n = src[i++].toInt() // signed byte -128..127
      if (n in 0..127) {
        val count = min(n + 1, end - i)
        out.write(src, i, count)
        i += count
      } else if (n in -127..-1) {
        if (i >= end) break
        val repeatByte = src[i++].toInt()
        val count = -n + 1
        repeat(count) {
          if (out.size() < expectedOut) out.write(repeatByte)
        }
      }
      // n == -128 is no-op
    }
    return out.toByteArray()
  }

  /**
   * Encodes an ARGB_8888 Bitmap as an uncompressed Baseline RGBA TIFF (Little-Endian).
   */
  fun encodeTiff(
    bitmap: Bitmap,
    outputStream: OutputStream,
    iccProfileBytes: ByteArray? = null,
  ) {
    val w = bitmap.width
    val h = bitmap.height
    val pixels = IntArray(w * h)
    bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

    val pixelBytes = ByteArray(w * h * 4)
    var idx = 0
    for (p in pixels) {
      pixelBytes[idx++] = ((p shr 16) and 0xFF).toByte() // R
      pixelBytes[idx++] = ((p shr 8) and 0xFF).toByte()  // G
      pixelBytes[idx++] = (p and 0xFF).toByte()          // B
      pixelBytes[idx++] = ((p shr 24) and 0xFF).toByte() // A
    }

    val softwareBytes = "Watermark Studio\u0000".toByteArray(Charsets.US_ASCII)
    val hasIcc = iccProfileBytes != null && iccProfileBytes.isNotEmpty()
    val entryCount = if (hasIcc) 14 else 13
    val ifdOffset = 8
    val ifdSize = 2 + entryCount * 12 + 4

    val bitsPerSampleOffset = ifdOffset + ifdSize // 8 bytes (4 shorts)
    val xResOffset = bitsPerSampleOffset + 8      // 8 bytes (rational)
    val yResOffset = xResOffset + 8               // 8 bytes (rational)
    val softwareOffset = yResOffset + 8           // softwareBytes.size
    val iccOffset = softwareOffset + softwareBytes.size
    val pixelDataOffset = iccOffset + (if (hasIcc) iccProfileBytes!!.size else 0)

    val headerSize = pixelDataOffset
    val headerBuf = ByteBuffer.allocate(headerSize).order(ByteOrder.LITTLE_ENDIAN)

    // TIFF Header: "II", 42, IFD offset
    headerBuf.put('I'.code.toByte())
    headerBuf.put('I'.code.toByte())
    headerBuf.putShort(42.toShort())
    headerBuf.putInt(ifdOffset)

    // IFD Entry count
    headerBuf.putShort(entryCount.toShort())

    fun writeEntry(tag: Int, type: Int, count: Int, valueOrOffset: Int) {
      headerBuf.putShort(tag.toShort())
      headerBuf.putShort(type.toShort())
      headerBuf.putInt(count)
      headerBuf.putInt(valueOrOffset)
    }

    fun writeShortInline(tag: Int, v: Int) = writeEntry(tag, 3, 1, v)
    fun writeLongInline(tag: Int, v: Int) = writeEntry(tag, 4, 1, v)

    // Tags MUST be sorted ascending by tag number per TIFF 6.0 spec
    writeLongInline(256, w)                            // ImageWidth
    writeLongInline(257, h)                            // ImageLength
    writeEntry(258, 3, 4, bitsPerSampleOffset)         // BitsPerSample (8,8,8,8)
    writeShortInline(259, 1)                           // Compression = Uncompressed
    writeShortInline(262, 2)                           // PhotometricInterpretation = RGB
    writeLongInline(273, pixelDataOffset)              // StripOffsets
    writeShortInline(277, 4)                           // SamplesPerPixel = 4 (RGBA)
    writeLongInline(278, h)                            // RowsPerStrip = all rows
    writeLongInline(279, pixelBytes.size)              // StripByteCounts
    writeEntry(282, 5, 1, xResOffset)                  // XResolution
    writeEntry(283, 5, 1, yResOffset)                  // YResolution
    writeShortInline(296, 2)                           // ResolutionUnit = Inch
    writeEntry(305, 2, softwareBytes.size, softwareOffset) // Software
    if (hasIcc) {
      writeEntry(34675, 7, iccProfileBytes!!.size, iccOffset) // ICCProfile (UNDEFINED)
    }
    headerBuf.putInt(0) // Next IFD = 0

    // Extra tag data
    // BitsPerSample: 8, 8, 8, 8
    repeat(4) { headerBuf.putShort(8.toShort()) }
    // XResolution: 300 / 1
    headerBuf.putInt(300)
    headerBuf.putInt(1)
    // YResolution: 300 / 1
    headerBuf.putInt(300)
    headerBuf.putInt(1)
    // Software string
    headerBuf.put(softwareBytes)
    // Optional ICC profile
    if (hasIcc) {
      headerBuf.put(iccProfileBytes)
    }

    outputStream.write(headerBuf.array())
    outputStream.write(pixelBytes)
    outputStream.flush()
  }
}
