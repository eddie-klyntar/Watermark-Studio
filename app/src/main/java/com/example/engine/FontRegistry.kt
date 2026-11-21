package com.example.engine

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.model.SerializedFontEntry
import java.io.File
import java.util.UUID

data class StudioFont(
  val id: String,
  val displayName: String,
  val category: String,
  val isBuiltIn: Boolean,
  val fileName: String? = null,
)

class FontRegistry(private val context: Context) {

  private val customFontsDir = File(context.filesDir, "custom_fonts").apply { mkdirs() }
  private val typefaceCache = mutableMapOf<String, Typeface>()

  private val builtInFonts = listOf(
    StudioFont("montserrat", "Montserrat", "Geometric Sans", true),
    StudioFont("playfair_display", "Playfair Display", "Editorial Serif", true),
    StudioFont("cormorant_garamond", "Cormorant Garamond", "Fine Art Serif", true),
    StudioFont("space_grotesk", "Space Grotesk", "Modern Studio", true),
    StudioFont("dancing_script", "Dancing Script", "Signature Script", true),
    StudioFont("oswald", "Oswald", "Condensed Stamp", true),
    StudioFont("inter", "Inter", "Clean UI Sans", true),
    StudioFont("jetbrains_mono", "JetBrains Mono", "Technical Mono", true),
  )

  private val customFonts = mutableListOf<StudioFont>()

  init {
    loadSavedCustomFonts()
  }

  private fun loadSavedCustomFonts() {
    customFonts.clear()
    customFontsDir.listFiles()
      ?.filter { it.isFile && (it.name.endsWith(".ttf", true) || it.name.endsWith(".otf", true)) }
      ?.sortedBy { it.name }
      ?.forEach { file ->
        val id = file.nameWithoutExtension
        val display = id.substringAfter("__", id).replace('_', ' ')
        customFonts.add(
          StudioFont(
            id = id,
            displayName = display,
            category = "Custom Font",
            isBuiltIn = false,
            fileName = file.name,
          )
        )
      }
  }

  fun getAllFonts(): List<StudioFont> = builtInFonts + customFonts

  fun getCustomFontEntries(): List<SerializedFontEntry> =
    customFonts.mapNotNull { font ->
      font.fileName?.let { SerializedFontEntry(font.id, font.displayName, it) }
    }

  fun getCustomFontFile(fontId: String): File? {
    val entry = customFonts.find { it.id == fontId } ?: return null
    return entry.fileName?.let { File(customFontsDir, it) }?.takeIf { it.exists() }
  }

  fun importFontFromUri(uri: Uri): StudioFont? {
    val rawName = resolveDisplayName(uri) ?: "custom_font.ttf"
    val ext = when {
      rawName.endsWith(".otf", true) -> "otf"
      rawName.endsWith(".ttf", true) -> "ttf"
      else -> "ttf"
    }
    val cleanTitle = rawName.substringBeforeLast('.')
      .replace(Regex("[^A-Za-z0-9 _-]"), "")
      .trim()
      .ifBlank { "Custom Font" }
    val id = "custom_${UUID.randomUUID().toString().take(8)}__${cleanTitle.replace(' ', '_')}"
    val destFile = File(customFontsDir, "$id.$ext")

    return runCatching {
      context.contentResolver.openInputStream(uri)?.use { input ->
        destFile.outputStream().use { output -> input.copyTo(output) }
      } ?: return null

      // Validate that Android can actually parse the TTF/OTF font
      val tf = Typeface.createFromFile(destFile)
      typefaceCache[id] = tf
      val font = StudioFont(
        id = id,
        displayName = cleanTitle,
        category = "Custom (${ext.uppercase()})",
        isBuiltIn = false,
        fileName = destFile.name,
      )
      customFonts.add(font)
      font
    }.onFailure {
      destFile.delete()
    }.getOrNull()
  }

  fun restoreCustomFont(entry: SerializedFontEntry, bytes: ByteArray) {
    val dest = File(customFontsDir, entry.fileName)
    dest.writeBytes(bytes)
    runCatching {
      val tf = Typeface.createFromFile(dest)
      typefaceCache[entry.id] = tf
      if (customFonts.none { it.id == entry.id }) {
        customFonts.add(
          StudioFont(
            id = entry.id,
            displayName = entry.displayName,
            category = "Custom Font",
            isBuiltIn = false,
            fileName = entry.fileName,
          )
        )
      }
    }
  }

  fun resolveTypeface(fontId: String, weight: Int, italic: Boolean): Typeface {
    val cacheKey = "${fontId}_${weight}_$italic"
    typefaceCache[cacheKey]?.let { return it }

    val base = resolveBaseTypeface(fontId)
    val resolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      Typeface.create(base, weight.coerceIn(100, 900), italic)
    } else {
      val style = when {
        weight >= 600 && italic -> Typeface.BOLD_ITALIC
        weight >= 600 -> Typeface.BOLD
        italic -> Typeface.ITALIC
        else -> Typeface.NORMAL
      }
      Typeface.create(base, style)
    }
    typefaceCache[cacheKey] = resolved
    return resolved
  }

  private fun resolveBaseTypeface(fontId: String): Typeface {
    typefaceCache[fontId]?.let { return it }
    val builtInRes = when (fontId) {
      "montserrat" -> R.font.montserrat
      "playfair_display" -> R.font.playfair_display
      "cormorant_garamond" -> R.font.cormorant_garamond
      "space_grotesk" -> R.font.space_grotesk
      "dancing_script" -> R.font.dancing_script
      "oswald" -> R.font.oswald
      "inter" -> R.font.inter
      "jetbrains_mono" -> R.font.jetbrains_mono
      else -> null
    }
    val tf = if (builtInRes != null) {
      ResourcesCompat.getFont(context, builtInRes) ?: Typeface.DEFAULT
    } else {
      val customFile = getCustomFontFile(fontId)
      if (customFile != null) Typeface.createFromFile(customFile) else Typeface.DEFAULT
    }
    typefaceCache[fontId] = tf
    return tf
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
}
