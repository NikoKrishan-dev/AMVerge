package com.amverge.android.frontend.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amverge.android.backend.domain.model.AspectRatioCrop
import com.amverge.android.backend.domain.model.AudioExportOption
import com.amverge.android.backend.domain.model.ExportMode
import com.amverge.android.backend.domain.model.ExportRequest
import com.amverge.android.frontend.state.ExportUiState
import com.amverge.android.frontend.theme.AppleCanvasDark
import com.amverge.android.frontend.theme.AppleGrayDark
import com.amverge.android.frontend.theme.AppleGreen
import com.amverge.android.frontend.theme.AppleHairlineDark
import com.amverge.android.frontend.theme.AppleRed
import com.amverge.android.frontend.theme.AppleSkyBlue
import com.amverge.android.frontend.theme.AppleTile1
import com.amverge.android.frontend.theme.AppleTile2
import com.amverge.android.frontend.theme.AppleTile3
import com.amverge.android.frontend.theme.AppleWhite

@Composable
fun ExportDialog(
    projectId: String,
    selectedSceneIds: List<String>,
    exportState: ExportUiState,
    onStartExport: (ExportRequest) -> Unit,
    onDismiss: () -> Unit
) {
    val isSingleScene = selectedSceneIds.size == 1
    var selectedMode by remember(selectedSceneIds) {
        mutableStateOf(if (isSingleScene) ExportMode.INDIVIDUAL else ExportMode.MERGED)
    }
    var selectedAspectRatio by remember { mutableStateOf(AspectRatioCrop.ORIGINAL) }
    var selectedAudioOption by remember { mutableStateOf(AudioExportOption.KEEP_AUDIO) }
    var destinationTreeUri by remember { mutableStateOf<String?>(null) }
    var destinationDocUri by remember { mutableStateOf<String?>(null) }

    val treePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            destinationTreeUri = uri.toString()
        }
    }

    val docPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("video/mp4")
    ) { uri: Uri? ->
        if (uri != null) {
            destinationDocUri = uri.toString()
        }
    }

    val edlPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: Uri? ->
        if (uri != null) {
            destinationDocUri = uri.toString()
        }
    }

    AlertDialog(
        onDismissRequest = { if (!exportState.isExporting) onDismiss() },
        containerColor = AppleTile1,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = if (isSingleScene) "Export 1 Scene Cut" else "Export ${selectedSceneIds.size} Scene Cuts",
                color = AppleWhite,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.28).sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (exportState.isExporting) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(AppleTile2)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = exportState.message ?: "Exporting video cuts...",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = AppleWhite
                        )
                        LinearProgressIndicator(
                            progress = { exportState.progress },
                            color = AppleSkyBlue,
                            trackColor = AppleTile3,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(9999.dp))
                        )
                    }
                } else if (exportState.isComplete) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(AppleTile2)
                            .border(1.dp, AppleGreen.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Column {
                            Text(
                                text = "Export Completed Successfully",
                                style = MaterialTheme.typography.titleSmall,
                                color = AppleGreen,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Saved ${exportState.exportedFiles.size} video file(s) to Movies/AMVerge in device storage.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = AppleWhite
                            )
                        }
                    }
                } else {
                    if (exportState.errorMessage != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(AppleTile2)
                                .border(1.dp, AppleRed.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                                .padding(14.dp)
                        ) {
                            Text(
                                text = exportState.errorMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = AppleRed
                            )
                        }
                    }

                    if (isSingleScene) {
                        Text(
                            text = "Exporting the selected scene as an exact high-bitrate MP4 clip.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppleGrayDark
                        )
                    } else {
                        Text("Export Mode:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = AppleWhite)

                        // Full-row touch targets with Title and Subtitle (No truncated text!)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ExportModeRow(
                                title = "Merged Video",
                                subtitle = "Single seamless chronological MP4 cut",
                                isSelected = selectedMode == ExportMode.MERGED,
                                onClick = { selectedMode = ExportMode.MERGED }
                            )

                            ExportModeRow(
                                title = "Individual Clips",
                                subtitle = "One distinct MP4 file per scene cut",
                                isSelected = selectedMode == ExportMode.INDIVIDUAL,
                                onClick = { selectedMode = ExportMode.INDIVIDUAL }
                            )

                            ExportModeRow(
                                title = "EDL Timeline",
                                subtitle = "CMX 3600 manifest for DaVinci & Premiere",
                                isSelected = selectedMode == ExportMode.EDL_TIMELINE,
                                onClick = { selectedMode = ExportMode.EDL_TIMELINE }
                            )
                        }
                    }

                    if (selectedMode != ExportMode.EDL_TIMELINE) {
                        Text("Aspect Ratio Crop:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = AppleWhite)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AspectRatioCrop.entries.forEach { crop ->
                                val label = when (crop) {
                                    AspectRatioCrop.ORIGINAL -> "Original"
                                    AspectRatioCrop.VERTICAL_9_16 -> "9:16 Shorts"
                                    AspectRatioCrop.SQUARE_1_1 -> "1:1 Square"
                                    AspectRatioCrop.STANDARD_4_3 -> "4:3 Retro"
                                }
                                FilterChip(
                                    selected = (selectedAspectRatio == crop),
                                    onClick = { selectedAspectRatio = crop },
                                    label = { Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold) },
                                    shape = RoundedCornerShape(9999.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = AppleSkyBlue,
                                        selectedLabelColor = Color.Black,
                                        containerColor = AppleTile2,
                                        labelColor = AppleWhite
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = (selectedAspectRatio == crop),
                                        borderColor = AppleHairlineDark,
                                        selectedBorderColor = Color.Transparent,
                                        borderWidth = 1.dp
                                    ),
                                    modifier = Modifier.height(38.dp)
                                )
                            }
                        }

                        Text("Audio Track:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = AppleWhite)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = (selectedAudioOption == AudioExportOption.KEEP_AUDIO),
                                onClick = { selectedAudioOption = AudioExportOption.KEEP_AUDIO },
                                label = { Text("Keep Audio", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold) },
                                shape = RoundedCornerShape(9999.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AppleSkyBlue,
                                    selectedLabelColor = Color.Black,
                                    containerColor = AppleTile2,
                                    labelColor = AppleWhite
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = (selectedAudioOption == AudioExportOption.KEEP_AUDIO),
                                    borderColor = AppleHairlineDark,
                                    selectedBorderColor = Color.Transparent,
                                    borderWidth = 1.dp
                                ),
                                modifier = Modifier.height(38.dp)
                            )
                            FilterChip(
                                selected = (selectedAudioOption == AudioExportOption.MUTE_AUDIO),
                                onClick = { selectedAudioOption = AudioExportOption.MUTE_AUDIO },
                                label = { Text("Mute (Clean AMV)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold) },
                                shape = RoundedCornerShape(9999.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AppleSkyBlue,
                                    selectedLabelColor = Color.Black,
                                    containerColor = AppleTile2,
                                    labelColor = AppleWhite
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = (selectedAudioOption == AudioExportOption.MUTE_AUDIO),
                                    borderColor = AppleHairlineDark,
                                    selectedBorderColor = Color.Transparent,
                                    borderWidth = 1.dp
                                ),
                                modifier = Modifier.height(38.dp)
                            )
                        }
                    }

                    Text(
                        text = "Destination: Movies/AMVerge (Default)",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppleGrayDark
                    )

                    if (selectedMode == ExportMode.EDL_TIMELINE) {
                        OutlinedButton(
                            onClick = { edlPickerLauncher.launch("timeline.edl") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 44.dp),
                            shape = RoundedCornerShape(9999.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AppleHairlineDark),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AppleSkyBlue)
                        ) {
                            Text(
                                if (destinationDocUri != null) "Custom EDL Location Set"
                                else "Choose Custom EDL Location (Optional)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else if (!isSingleScene && selectedMode == ExportMode.MERGED) {
                        OutlinedButton(
                            onClick = { docPickerLauncher.launch("merged_scenes.mp4") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 44.dp),
                            shape = RoundedCornerShape(9999.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AppleHairlineDark),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AppleSkyBlue)
                        ) {
                            Text(
                                if (destinationDocUri != null) "Custom Save File Set"
                                else "Choose Custom Save File (Optional)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        OutlinedButton(
                            onClick = { treePickerLauncher.launch(null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 44.dp),
                            shape = RoundedCornerShape(9999.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AppleHairlineDark),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AppleSkyBlue)
                        ) {
                            Text(
                                if (destinationTreeUri != null) "Custom Folder Set"
                                else "Choose Custom Destination Folder (Optional)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (exportState.isComplete) {
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(9999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppleSkyBlue,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Done", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
            } else if (!exportState.isExporting) {
                Button(
                    onClick = {
                        val request = ExportRequest(
                            projectId = projectId,
                            sceneIds = selectedSceneIds,
                            mode = selectedMode,
                            destinationTreeUri = destinationTreeUri,
                            destinationDocumentUri = destinationDocUri,
                            aspectRatio = selectedAspectRatio,
                            audioOption = selectedAudioOption
                        )
                        onStartExport(request)
                    },
                    shape = RoundedCornerShape(9999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppleSkyBlue,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Start Export", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            if (!exportState.isExporting && !exportState.isComplete) {
                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(contentColor = AppleGrayDark),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Cancel", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    )
}

@Composable
private fun ExportModeRow(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) AppleTile3 else AppleTile2)
            .border(
                1.dp,
                if (isSelected) AppleSkyBlue.copy(alpha = 0.5f) else AppleHairlineDark,
                RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = AppleSkyBlue,
                    unselectedColor = AppleGrayDark
                )
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppleWhite
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = AppleGrayDark
                )
            }
        }
    }
}
