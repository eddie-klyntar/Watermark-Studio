package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.example.data.PresetRepository
import com.example.data.ProjectManager
import com.example.engine.BlendCompositor
import com.example.engine.FontRegistry
import com.example.engine.ImageIoManager
import com.example.engine.LogoAssetRepository
import com.example.engine.TiffCodec
import com.example.engine.WatermarkRenderer
import com.example.model.ExportSettings
import com.example.model.FillMode
import com.example.model.LayerType
import com.example.model.LogoLayerStyle
import com.example.model.TextLayerStyle
import com.example.model.TileSettings
import com.example.model.WatermarkBlendMode
import com.example.model.WatermarkLayer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WatermarkEngineRobolectricTest {

  @Test
  fun `tiff codec roundtrip preserves rgba pixels`() {
    val src = Bitmap.createBitmap(16, 12, Bitmap.Config.ARGB_8888)
    src.eraseColor(Color.argb(200, 42, 135, 220))
    src.setPixel(3, 4, Color.argb(255, 245, 158, 11))

    val baos = ByteArrayOutputStream()
    TiffCodec.encodeTiff(src, baos)
    val bytes = baos.toByteArray()

    assertTrue(TiffCodec.isTiff(bytes))
    val decoded = TiffCodec.decodeTiff(bytes)
    assertNotNull(decoded)
    assertEquals(16, decoded!!.width)
    assertEquals(12, decoded.height)
    assertEquals(Color.argb(255, 245, 158, 11), decoded.getPixel(3, 4))
    assertEquals(Color.argb(200, 42, 135, 220), decoded.getPixel(0, 0))
  }

  @Test
  fun `blend compositor computes distinct real blend math`() {
    fun runBlend(mode: WatermarkBlendMode): Int {
      val dst = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
      dst.eraseColor(Color.rgb(180, 120, 80))
      val src = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
      src.eraseColor(Color.rgb(100, 200, 150))
      BlendCompositor.compositeLayer(dst, src, mode, 1.0f)
      return dst.getPixel(1, 1)
    }

    val normal = runBlend(WatermarkBlendMode.NORMAL)
    val multiply = runBlend(WatermarkBlendMode.MULTIPLY)
    val screen = runBlend(WatermarkBlendMode.SCREEN)
    val difference = runBlend(WatermarkBlendMode.DIFFERENCE)
    val subtract = runBlend(WatermarkBlendMode.SUBTRACT)

    assertNotEquals(normal, multiply)
    assertNotEquals(multiply, screen)
    assertNotEquals(screen, difference)
    assertNotEquals(difference, subtract)

    // Subtract: (180-100, 120-200->0, 80-150->0) = (80, 0, 0)
    assertEquals(80, Color.red(subtract))
    assertEquals(0, Color.green(subtract))
    assertEquals(0, Color.blue(subtract))
  }

  @Test
  fun `renderer preserves original bitmap and renders tiled and svg layers`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val fontRegistry = FontRegistry(context)
    val logoRepo = LogoAssetRepository(context)
    val renderer = WatermarkRenderer(fontRegistry, logoRepo)

    val source = Bitmap.createBitmap(320, 240, Bitmap.Config.ARGB_8888)
    source.eraseColor(Color.rgb(30, 40, 60))

    val layers = listOf(
      WatermarkLayer(
        id = "t1",
        name = "Tiled Text",
        type = LayerType.TEXT,
        opacity = 0.8f,
        blendMode = WatermarkBlendMode.SCREEN,
        tile = TileSettings(enabled = true, spacingX = 60f, spacingY = 50f),
        textStyle = TextLayerStyle(
          text = "© STUDIO",
          fillMode = FillMode.LINEAR_GRADIENT,
        ),
      ),
      WatermarkLayer(
        id = "l1",
        name = "Crest",
        type = LayerType.LOGO,
        opacity = 0.9f,
        blendMode = WatermarkBlendMode.OVERLAY,
        logoStyle = LogoLayerStyle(assetId = "builtin_aperture", grayscale = true),
      ),
    )

    val res = renderer.renderComposition(source, null, layers)
    assertEquals(320, res.bitmap.width)
    assertEquals(240, res.bitmap.height)
    assertEquals(2, res.layerBounds.size)
    // Original source bitmap remains completely untouched
    assertEquals(Color.rgb(30, 40, 60), source.getPixel(10, 10))
  }

  @Test
  fun `presets and wmstudio project archive save and restore cleanly`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val presetRepo = PresetRepository(context)
    val builtIn = presetRepo.getBuiltInPresets()
    assertTrue(builtIn.size >= 6)

    val fontRegistry = FontRegistry(context)
    val logoRepo = LogoAssetRepository(context)
    val imageIo = ImageIoManager(context)
    val projectMgr = ProjectManager(context, fontRegistry, logoRepo, imageIo)

    val layers = builtIn.first().layers
    val saved = projectMgr.saveProjectToInternal("Test Studio", null, layers, ExportSettings())
    assertTrue(saved.exists())

    val loaded = projectMgr.loadProjectFromInternal(saved.name)
    assertNotNull(loaded)
    assertEquals("Test Studio", loaded!!.project.projectName)
    assertEquals(layers.size, loaded.project.layers.size)
  }
}
