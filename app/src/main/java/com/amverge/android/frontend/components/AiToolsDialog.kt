package com.amverge.android.frontend.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amverge.android.backend.domain.model.Scene
import com.amverge.android.backend.domain.model.ai.AiToolType
import com.amverge.android.backend.domain.model.ai.DepthColormap
import com.amverge.android.frontend.components.SegmentedCapsule
import com.amverge.android.backend.domain.model.ai.DepthModel
import com.amverge.android.backend.domain.model.ai.InterpolationModel
import com.amverge.android.backend.domain.model.ai.UpscaleModel
import com.amverge.android.backend.domain.model.ai.UpscaleQuality
import com.amverge.android.frontend.state.AiToolsUiState
import com.amverge.android.frontend.theme.AppleGrayDark
import com.amverge.android.frontend.theme.AppleGreen
import com.amverge.android.frontend.theme.AppleHairlineDark
import com.amverge.android.frontend.theme.AppleIndigo
import com.amverge.android.frontend.theme.AppleRed
import com.amverge.android.frontend.theme.AppleSkyBlue
import com.amverge.android.frontend.theme.AppleTile1
import com.amverge.android.frontend.theme.AppleTile2
import com.amverge.android.frontend.theme.AppleTile3
import com.amverge.android.frontend.theme.AppleWhite

@Composable
fun AiToolsDialog(
    scene: Scene,
    sourceUri: String,
    uiState: AiToolsUiState,
    onSelectTool: (AiToolType) -> Unit,
    onSetInterpolationMultiplier: (Int) -> Unit,
    onSetInterpolationModel: (InterpolationModel) -> Unit,
    onSetUpscaleScale: (Int) -> Unit,
    onSetUpscaleModel: (UpscaleModel) -> Unit,
    onSetUpscaleQuality: (UpscaleQuality) -> Unit,
    onSetDepthModel: (DepthModel) -> Unit,
    onSetDepthColormap: (DepthColormap) -> Unit,
    onStartProcessing: () -> Unit,
    onSaveToStorage: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!uiState.isProcessing) onDismiss() },
        containerColor = AppleTile1,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = AppleIndigo,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    "AI Suite — Scene #${scene.index + 1}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppleWhite,
                    letterSpacing = (-0.28).sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Segmented Capsule Tool Tabs (44dp height)
                SegmentedCapsule(
                    items = listOf("16x Flow", "4x Upscale", "Depth Map"),
                    selectedIndex = uiState.selectedTool.ordinal,
                    onItemSelected = { if (!uiState.isProcessing) onSelectTool(AiToolType.entries[it]) },
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = AppleTile2,
                    height = 44.dp
                )

                // Tool-specific Configuration (All horizontally scrollable to prevent cramping)
                when (uiState.selectedTool) {
                    AiToolType.INTERPOLATION -> {
                        Text("Multiplier (up to 16x):", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = AppleWhite)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(2, 4, 8, 16).forEach { mult ->
                                AppleDialogFilterChip(
                                    selected = uiState.interpolationConfig.multiplier == mult,
                                    onClick = { onSetInterpolationMultiplier(mult) },
                                    label = "${mult}x"
                                )
                            }
                        }

                        Text("Algorithm Model:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = AppleWhite)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            InterpolationModel.entries.forEach { model ->
                                AppleDialogFilterChip(
                                    selected = uiState.interpolationConfig.model == model,
                                    onClick = { onSetInterpolationModel(model) },
                                    label = model.displayName
                                )
                            }
                        }
                    }

                    AiToolType.UPSCALE -> {
                        Text("Upscale Factor (up to 4x):", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = AppleWhite)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(2, 4).forEach { scale ->
                                AppleDialogFilterChip(
                                    selected = uiState.upscaleConfig.scale == scale,
                                    onClick = { onSetUpscaleScale(scale) },
                                    label = "${scale}x"
                                )
                            }
                        }

                        Text("Model:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = AppleWhite)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            UpscaleModel.entries.forEach { model ->
                                AppleDialogFilterChip(
                                    selected = uiState.upscaleConfig.model == model,
                                    onClick = { onSetUpscaleModel(model) },
                                    label = model.displayName
                                )
                            }
                        }

                        Text("Sharpness Quality:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = AppleWhite)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            UpscaleQuality.entries.forEach { quality ->
                                AppleDialogFilterChip(
                                    selected = uiState.upscaleConfig.quality == quality,
                                    onClick = { onSetUpscaleQuality(quality) },
                                    label = quality.name.lowercase().replaceFirstChar { it.uppercase() }
                                )
                            }
                        }
                    }

                    AiToolType.DEPTH_MAP -> {
                        Text("Depth Architecture:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = AppleWhite)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DepthModel.entries.forEach { model ->
                                AppleDialogFilterChip(
                                    selected = uiState.depthConfig.model == model,
                                    onClick = { onSetDepthModel(model) },
                                    label = model.displayName
                                )
                            }
                        }

                        Text("Colormap:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = AppleWhite)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                DepthColormap.INFERNO,
                                DepthColormap.VIRIDIS,
                                DepthColormap.TURBO,
                                DepthColormap.GRAYSCALE
                            ).forEach { colormap ->
                                AppleDialogFilterChip(
                                    selected = uiState.depthConfig.colormap == colormap,
                                    onClick = { onSetDepthColormap(colormap) },
                                    label = colormap.displayName
                                )
                            }
                        }
                    }
                }

                // Processing Progress
                if (uiState.isProcessing) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(AppleTile2)
                            .border(1.dp, AppleHairlineDark, RoundedCornerShape(16.dp))
                            .padding(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = uiState.message.ifEmpty { "Executing AI neural pass..." },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = AppleWhite
                            )
                            LinearProgressIndicator(
                                progress = { uiState.progress },
                                color = AppleIndigo,
                                trackColor = AppleTile3,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(9999.dp))
                            )
                        }
                    }
                }

                // Error State
                if (uiState.error != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(AppleTile2)
                            .border(1.dp, AppleRed.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                            .padding(14.dp)
                    ) {
                        Text(
                            text = uiState.error,
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppleRed
                        )
                    }
                }

                // Result Preview & Save Option
                if (uiState.result != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(AppleTile2)
                            .border(1.dp, AppleGreen.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "AI Pass Complete",
                                style = MaterialTheme.typography.titleSmall,
                                color = AppleGreen,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Output: ${uiState.result.width}x${uiState.result.height} @ ${uiState.result.fps.toInt()}fps (${uiState.result.frameCount} frames)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = AppleWhite
                            )

                            if (uiState.isSavedToStorage) {
                                Text(
                                    text = "Saved to Movies/AMVerge in device storage.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = AppleGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Button(
                                    onClick = onSaveToStorage,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .defaultMinSize(minHeight = 46.dp),
                                    shape = RoundedCornerShape(9999.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AppleSkyBlue,
                                        contentColor = Color.Black
                                    )
                                ) {
                                    Text("Save to Movies/AMVerge", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (uiState.result != null) {
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(9999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppleSkyBlue,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                ) {
                    Text("Done", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
            } else if (!uiState.isProcessing) {
                Button(
                    onClick = onStartProcessing,
                    shape = RoundedCornerShape(9999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppleIndigo,
                        contentColor = AppleWhite
                    ),
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                ) {
                    Text("Run AI Pass", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            if (!uiState.isProcessing) {
                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(contentColor = AppleGrayDark),
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                ) {
                    Text("Cancel")
                }
            }
        }
    )
}

@Composable
private fun AppleDialogFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        },
        shape = RoundedCornerShape(9999.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = AppleSkyBlue,
            selectedLabelColor = Color.Black,
            containerColor = AppleTile2,
            labelColor = AppleWhite
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = AppleHairlineDark,
            selectedBorderColor = Color.Transparent,
            borderWidth = 1.dp
        ),
        modifier = Modifier.height(38.dp)
    )
}
