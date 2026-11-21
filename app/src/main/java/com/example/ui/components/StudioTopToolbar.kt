package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DynamicFeed
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioObsidian
import com.example.ui.theme.StudioPanel
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary

@Composable
fun StudioTopToolbar(
  projectName: String,
  isExpanded: Boolean,
  canUndo: Boolean,
  canRedo: Boolean,
  onImportImages: () -> Unit,
  onImportDocumentImages: () -> Unit,
  onOpenProjects: () -> Unit,
  onUndo: () -> Unit,
  onRedo: () -> Unit,
  onAddText: () -> Unit,
  onAddLogo: () -> Unit,
  onOpenBatch: () -> Unit,
  onOpenExport: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var showOverflow by remember { mutableStateOf(false) }

  Row(
    modifier = modifier
      .fillMaxWidth()
      .height(52.dp)
      .background(StudioPanel)
      .padding(horizontal = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    // Brand & Project badge
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.weight(1f, fill = false),
    ) {
      Image(
        painter = painterResource(id = R.drawable.ic_studio_emblem),
        contentDescription = "Watermark Studio Emblem",
        contentScale = ContentScale.Crop,
        modifier = Modifier
          .size(30.dp)
          .clip(RoundedCornerShape(7.dp))
          .border(1.dp, StudioBorder, RoundedCornerShape(7.dp)),
      )
      Spacer(Modifier.width(8.dp))
      Text(
        text = "WATERMARK STUDIO",
        style = MaterialTheme.typography.titleSmall,
        color = StudioTextPrimary,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      if (isExpanded) {
        Spacer(Modifier.width(10.dp))
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(StudioSurface)
            .border(1.dp, StudioBorder, RoundedCornerShape(6.dp))
            .clickable(onClick = onOpenProjects)
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag("project_pill_button"),
        ) {
          Text(
            text = projectName,
            style = MaterialTheme.typography.labelSmall,
            color = StudioTextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }

    // Center & Right Controls
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      if (isExpanded) {
        ToolbarActionChip(
          icon = Icons.Default.AddPhotoAlternate,
          label = "Import",
          tooltip = "Import photos (JPG, PNG, WebP)",
          tag = "toolbar_import_button",
          onClick = onImportImages,
        )
        ToolbarActionChip(
          icon = Icons.Default.FolderOpen,
          label = "TIFF/Files",
          tooltip = "Import TIFF, BMP, PNG, JPG via file browser",
          tag = "toolbar_import_files_button",
          onClick = onImportDocumentImages,
        )
        ToolbarActionChip(
          icon = Icons.Default.Save,
          label = "Project",
          tooltip = "Save or open .wmstudio project",
          tag = "toolbar_project_button",
          onClick = onOpenProjects,
        )
        VerticalToolbarDivider()
      }

      ToolbarIconBtn(
        icon = Icons.AutoMirrored.Filled.Undo,
        contentDescription = "Undo",
        enabled = canUndo,
        tag = "toolbar_undo_button",
        onClick = onUndo,
      )
      ToolbarIconBtn(
        icon = Icons.AutoMirrored.Filled.Redo,
        contentDescription = "Redo",
        enabled = canRedo,
        tag = "toolbar_redo_button",
        onClick = onRedo,
      )

      VerticalToolbarDivider()

      ToolbarActionChip(
        icon = Icons.Default.TextFields,
        label = if (isExpanded) "+ Text" else "Text",
        tooltip = "Add Text Watermark Layer",
        tag = "toolbar_add_text_button",
        onClick = onAddText,
      )
      ToolbarActionChip(
        icon = Icons.Default.CollectionsBookmark,
        label = if (isExpanded) "+ Logo" else "Logo",
        tooltip = "Add Logo / Graphic Layer",
        tag = "toolbar_add_logo_button",
        onClick = onAddLogo,
      )

      if (isExpanded) {
        ToolbarActionChip(
          icon = Icons.Default.DynamicFeed,
          label = "Batch",
          tooltip = "Batch process multiple images or folders",
          tag = "toolbar_batch_button",
          onClick = onOpenBatch,
        )
      }

      Spacer(Modifier.width(2.dp))

      Button(
        onClick = onOpenExport,
        colors = ButtonDefaults.buttonColors(
          containerColor = StudioAmber,
          contentColor = StudioObsidian,
        ),
        shape = RoundedCornerShape(7.dp),
        contentPadding = PaddingValues(horizontal = if (isExpanded) 12.dp else 10.dp, vertical = 0.dp),
        modifier = Modifier
          .height(34.dp)
          .testTag("toolbar_export_button"),
      ) {
        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(
          text = "Export",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
        )
      }

      if (!isExpanded) {
        Box {
          IconButton(
            onClick = { showOverflow = true },
            modifier = Modifier.size(36.dp).testTag("toolbar_overflow_button"),
          ) {
            Icon(Icons.Default.MoreVert, contentDescription = "More tools", tint = StudioTextPrimary)
          }
          DropdownMenu(
            expanded = showOverflow,
            onDismissRequest = { showOverflow = false },
            containerColor = StudioSurface,
          ) {
            DropdownMenuItem(
              text = { Text("Import Photos (Gallery)") },
              leadingIcon = { Icon(Icons.Default.AddPhotoAlternate, null) },
              onClick = { showOverflow = false; onImportImages() },
            )
            DropdownMenuItem(
              text = { Text("Import TIFF / BMP / Files") },
              leadingIcon = { Icon(Icons.Default.FolderOpen, null) },
              onClick = { showOverflow = false; onImportDocumentImages() },
            )
            DropdownMenuItem(
              text = { Text("Batch Process Folder / Queue") },
              leadingIcon = { Icon(Icons.Default.DynamicFeed, null) },
              onClick = { showOverflow = false; onOpenBatch() },
            )
            HorizontalDivider(color = StudioBorder)
            DropdownMenuItem(
              text = { Text("Projects (.wmstudio)") },
              leadingIcon = { Icon(Icons.Default.Save, null) },
              onClick = { showOverflow = false; onOpenProjects() },
            )
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ToolbarActionChip(
  icon: ImageVector,
  label: String,
  tooltip: String,
  tag: String,
  onClick: () -> Unit,
) {
  TooltipBox(
    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
    tooltip = { PlainTooltip { Text(tooltip) } },
    state = rememberTooltipState(),
  ) {
    Row(
      modifier = Modifier
        .height(34.dp)
        .clip(RoundedCornerShape(7.dp))
        .background(StudioSurface)
        .border(1.dp, StudioBorder, RoundedCornerShape(7.dp))
        .clickable(onClick = onClick)
        .padding(horizontal = 8.dp)
        .testTag(tag),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Icon(icon, contentDescription = tooltip, tint = StudioTextPrimary, modifier = Modifier.size(16.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = StudioTextPrimary,
        maxLines = 1,
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ToolbarIconBtn(
  icon: ImageVector,
  contentDescription: String,
  enabled: Boolean,
  tag: String,
  onClick: () -> Unit,
) {
  TooltipBox(
    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
    tooltip = { PlainTooltip { Text(contentDescription) } },
    state = rememberTooltipState(),
  ) {
    IconButton(
      onClick = onClick,
      enabled = enabled,
      modifier = Modifier
        .size(34.dp)
        .testTag(tag),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        tint = if (enabled) StudioTextPrimary else StudioTextSecondary.copy(alpha = 0.35f),
        modifier = Modifier.size(18.dp),
      )
    }
  }
}

@Composable
private fun VerticalToolbarDivider() {
  Box(
    modifier = Modifier
      .padding(horizontal = 2.dp)
      .width(1.dp)
      .height(22.dp)
      .background(StudioBorder),
  )
}
