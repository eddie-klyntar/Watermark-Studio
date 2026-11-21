package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.ImageAsset
import com.example.model.WatermarkLayer
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.StudioPanel
import com.example.ui.theme.StudioTextSecondary
import kotlin.math.roundToInt

@Composable
fun StudioStatusBar(
  activeAsset: ImageAsset?,
  selectedLayer: WatermarkLayer?,
  zoomModeFit: Boolean,
  manualZoom: Float,
  statusMessage: String,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .background(StudioPanel)
      .navigationBarsPadding()
      .height(28.dp)
      .padding(horizontal = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    val dimText = if (activeAsset != null) {
      "${activeAsset.name} • ${activeAsset.width}×${activeAsset.height} px • ${activeAsset.formatLabel}"
    } else {
      "No image loaded"
    }
    Text(
      text = dimText,
      style = MaterialTheme.typography.labelSmall,
      color = StudioTextSecondary,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.weight(1f),
    )

    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      if (selectedLayer != null) {
        Text(
          text = "${selectedLayer.name} (${selectedLayer.blendMode.label})",
          style = MaterialTheme.typography.labelSmall,
          color = StudioCyan,
          maxLines = 1,
        )
      }
      Text(
        text = if (zoomModeFit) "ZOOM FIT" else "ZOOM ${(manualZoom * 100).roundToInt()}%",
        style = MaterialTheme.typography.labelSmall,
        color = StudioAmber,
      )
      Text(
        text = statusMessage,
        style = MaterialTheme.typography.labelSmall,
        color = StudioTextSecondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}
