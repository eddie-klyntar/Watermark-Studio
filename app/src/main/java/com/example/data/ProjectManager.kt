package com.example.data

import android.content.Context
import android.net.Uri
import com.example.engine.FontRegistry
import com.example.engine.ImageIoManager
import com.example.engine.LogoAssetRepository
import com.example.model.ExportSettings
import com.example.model.ImageAsset
import com.example.model.WatermarkLayer
import com.example.model.WatermarkProjectFile
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class SavedProjectSummary(
  val fileName: String,
  val projectName: String,
  val savedAtMillis: Long,
  val layerCount: Int,
)

data class LoadedProjectResult(
  val project: WatermarkProjectFile,
  val embeddedImageFile: File?,
)

class ProjectManager(
  private val context: Context,
  private val fontRegistry: FontRegistry,
  private val logoRepository: LogoAssetRepository,
  private val imageIoManager: ImageIoManager,
) {

  private val projectsDir = File(context.filesDir, "projects").apply { mkdirs() }
  private val restoredImagesDir = File(context.filesDir, "restored_project_images").apply { mkdirs() }
  private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
  private val projectAdapter = moshi.adapter(WatermarkProjectFile::class.java)

  fun saveProjectToInternal(
    projectName: String,
    activeAsset: ImageAsset?,
    layers: List<WatermarkLayer>,
    exportSettings: ExportSettings,
  ): File {
    val sanitized = projectName.replace(Regex("[^A-Za-z0-9 _-]"), "").trim().ifBlank { "StudioProject" }
    val outFile = File(projectsDir, "${sanitized.replace(' ', '_')}.wmstudio")
    outFile.outputStream().use { out ->
      writeProjectArchive(projectName, activeAsset, layers, exportSettings, out)
    }
    return outFile
  }

  fun exportProjectToUri(
    uri: Uri,
    projectName: String,
    activeAsset: ImageAsset?,
    layers: List<WatermarkLayer>,
    exportSettings: ExportSettings,
  ): Boolean {
    return runCatching {
      context.contentResolver.openOutputStream(uri)?.use { out ->
        writeProjectArchive(projectName, activeAsset, layers, exportSettings, out)
      } ?: false
      true
    }.getOrDefault(false)
  }

  fun listInternalProjects(): List<SavedProjectSummary> {
    return projectsDir.listFiles()
      ?.filter { it.isFile && it.extension == "wmstudio" }
      ?.sortedByDescending { it.lastModified() }
      ?.mapNotNull { file ->
        runCatching {
          var summary: SavedProjectSummary? = null
          ZipInputStream(file.inputStream()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
              if (entry.name == "project.json") {
                val json = zis.readBytes().toString(Charsets.UTF_8)
                projectAdapter.fromJson(json)?.let { proj ->
                  summary = SavedProjectSummary(
                    fileName = file.name,
                    projectName = proj.projectName,
                    savedAtMillis = proj.savedAtMillis,
                    layerCount = proj.layers.size,
                  )
                }
                break
              }
              entry = zis.nextEntry
            }
          }
          summary
        }.getOrNull()
      } ?: emptyList()
  }

  fun loadProjectFromInternal(fileName: String): LoadedProjectResult? {
    val file = File(projectsDir, fileName)
    if (!file.exists()) return null
    return file.inputStream().use { readProjectArchive(it) }
  }

  fun loadProjectFromUri(uri: Uri): LoadedProjectResult? {
    return context.contentResolver.openInputStream(uri)?.use { readProjectArchive(it) }
  }

  fun deleteInternalProject(fileName: String) {
    File(projectsDir, fileName).delete()
  }

  private fun writeProjectArchive(
    projectName: String,
    activeAsset: ImageAsset?,
    layers: List<WatermarkLayer>,
    exportSettings: ExportSettings,
    outputStream: OutputStream,
  ) {
    val customFonts = fontRegistry.getCustomFontEntries()
    val customLogos = logoRepository.getCustomLogoEntries()

    val embeddedImageBytes = activeAsset?.let { imageIoManager.readSourceBytes(it) }
    val embeddedImgName = if (embeddedImageBytes != null && activeAsset != null) {
      "source_${activeAsset.name.replace(Regex("[^A-Za-z0-9._-]"), "_")}"
    } else null

    val sampleResName = activeAsset?.sampleDrawableRes?.let { resId ->
      runCatching { context.resources.getResourceEntryName(resId) }.getOrNull()
    }

    val projectFile = WatermarkProjectFile(
      version = 1,
      projectName = projectName,
      savedAtMillis = System.currentTimeMillis(),
      activeImageName = activeAsset?.name,
      activeSampleResName = sampleResName,
      activeImageUri = activeAsset?.uriString,
      embeddedImageFileName = embeddedImgName,
      layers = layers,
      customFonts = customFonts,
      customLogos = customLogos,
      exportSettings = exportSettings,
    )

    ZipOutputStream(outputStream).use { zos ->
      // 1. project.json
      zos.putNextEntry(ZipEntry("project.json"))
      zos.write(projectAdapter.toJson(projectFile).toByteArray(Charsets.UTF_8))
      zos.closeEntry()

      // 2. custom fonts
      for (font in customFonts) {
        val f = fontRegistry.getCustomFontFile(font.id) ?: continue
        zos.putNextEntry(ZipEntry("fonts/${font.fileName}"))
        zos.write(f.readBytes())
        zos.closeEntry()
      }

      // 3. custom logos
      for (logo in customLogos) {
        val f = logoRepository.getCustomLogoFile(logo.id) ?: continue
        zos.putNextEntry(ZipEntry("assets/${logo.fileName}"))
        zos.write(f.readBytes())
        zos.closeEntry()
      }

      // 4. embedded active image so project is self-contained
      if (embeddedImageBytes != null && embeddedImgName != null) {
        zos.putNextEntry(ZipEntry("images/$embeddedImgName"))
        zos.write(embeddedImageBytes)
        zos.closeEntry()
      }
    }
  }

  private fun readProjectArchive(inputStream: InputStream): LoadedProjectResult? {
    var projectJson: String? = null
    val fontBytes = mutableMapOf<String, ByteArray>()
    val logoBytes = mutableMapOf<String, ByteArray>()
    val imageBytes = mutableMapOf<String, ByteArray>()

    ZipInputStream(inputStream).use { zis ->
      var entry = zis.nextEntry
      while (entry != null) {
        if (!entry.isDirectory) {
          val buffer = ByteArrayOutputStream()
          zis.copyTo(buffer)
          val bytes = buffer.toByteArray()
          when {
            entry.name == "project.json" -> projectJson = bytes.toString(Charsets.UTF_8)
            entry.name.startsWith("fonts/") -> fontBytes[entry.name.removePrefix("fonts/")] = bytes
            entry.name.startsWith("assets/") -> logoBytes[entry.name.removePrefix("assets/")] = bytes
            entry.name.startsWith("images/") -> imageBytes[entry.name.removePrefix("images/")] = bytes
          }
        }
        entry = zis.nextEntry
      }
    }

    val proj = projectJson?.let { runCatching { projectAdapter.fromJson(it) }.getOrNull() } ?: return null

    // Restore embedded custom fonts
    for (fontEntry in proj.customFonts) {
      fontBytes[fontEntry.fileName]?.let { fontRegistry.restoreCustomFont(fontEntry, it) }
    }

    // Restore embedded custom logos
    for (logoEntry in proj.customLogos) {
      logoBytes[logoEntry.fileName]?.let { logoRepository.restoreCustomLogo(logoEntry, it) }
    }

    // Restore embedded canvas image if present
    val restoredFile = proj.embeddedImageFileName?.let { fileName ->
      imageBytes[fileName]?.let { bytes ->
        File(restoredImagesDir, fileName).apply { writeBytes(bytes) }
      }
    }

    return LoadedProjectResult(project = proj, embeddedImageFile = restoredFile)
  }
}
