package com.amverge.android.frontend.screens.ai

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.amverge.android.backend.domain.model.ai.AiToolType
import com.amverge.android.backend.domain.model.ai.DepthColormap
import com.amverge.android.backend.domain.model.ai.DepthModel
import com.amverge.android.backend.domain.model.ai.InterpolationModel
import com.amverge.android.backend.domain.model.ai.UpscaleModel
import com.amverge.android.backend.domain.model.ai.UpscaleQuality
import com.amverge.android.core.common.MediaTime
import com.amverge.android.frontend.components.SegmentedCapsule
import com.amverge.android.frontend.state.AiToolsUiState
import com.amverge.android.frontend.theme.AppleCanvasDark
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
import com.amverge.android.frontend.viewmodel.AiToolsViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiStudioScreen(
    viewModel: AiToolsViewModel,
    onNavigateBack: (() -> Unit)? = null,
    showTopBar: Boolean = true
) {
    val state by viewModel.uiState.collectAsState()

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.onClipSelected(uri)
        }
    }

    if (showTopBar) {
        Scaffold(
            containerColor = AppleCanvasDark,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "AI Studio",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = AppleWhite,
                            letterSpacing = (-0.38).sp
                        )
                    },
                    navigationIcon = {
                        if (onNavigateBack != null) {
                            IconButton(
                                onClick = onNavigateBack,
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(AppleTile1)
                                    .border(1.dp, AppleHairlineDark, CircleShape)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = AppleWhite,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = AppleCanvasDark,
                        titleContentColor = AppleWhite
                    )
                )
            }
        ) { paddingValues ->
            AiStudioContent(
                state = state,
                viewModel = viewModel,
                onPickFile = { filePicker.launch(arrayOf("video/*")) },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        }
    } else {
        AiStudioContent(
            state = state,
            viewModel = viewModel,
            onPickFile = { filePicker.launch(arrayOf("video/*")) },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun AiStudioContent(
    state: AiToolsUiState,
    viewModel: AiToolsViewModel,
    onPickFile: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(AppleCanvasDark)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // 1. Clip Selection Card (Spacious, Clear, Intentional)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(AppleTile1)
                .border(1.dp, AppleHairlineDark, RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "Input Video Clip",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppleWhite,
                    letterSpacing = (-0.28).sp
                )

                if (state.selectedClipUri == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(175.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(AppleTile2)
                            .border(1.dp, AppleSkyBlue.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                            .clickable { onPickFile() }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(AppleTile3),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VideoLibrary,
                                    contentDescription = null,
                                    tint = AppleSkyBlue,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Text(
                                text = "Select Any Video from Device",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = AppleWhite,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Choose any clip from storage to test Flow, Upscale, or Depth",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppleGrayDark,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    Text(
                        text = state.selectedClipName ?: "Selected Video",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = AppleWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        state.selectedClipWidth?.let { w ->
                            val h = state.selectedClipHeight ?: 0
                            AppleMetaBadge(label = "${w}x${h}")
                        }
                        state.selectedClipFps?.let { fps ->
                            AppleMetaBadge(label = "${fps.toInt()} fps")
                        }
                        state.selectedClipDurationMs?.let { dur ->
                            AppleMetaBadge(label = MediaTime.formatDuration(dur))
                        }
                    }

                    // Video Player Preview
                    AiVideoPlayer(
                        uriOrPath = state.selectedClipUri.toString(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.Black)
                            .border(1.dp, AppleHairlineDark, RoundedCornerShape(14.dp))
                    )

                    OutlinedButton(
                        onClick = onPickFile,
                        enabled = !state.isProcessing,
                        shape = RoundedCornerShape(9999.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AppleSkyBlue.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AppleSkyBlue),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 46.dp)
                    ) {
                        Text("Select Different Video Clip", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // 2. Tool Mode Tabs & Config
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(AppleTile1)
                .border(1.dp, AppleHairlineDark, RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "AI Processing Engine",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppleWhite,
                    letterSpacing = (-0.28).sp
                )

                // Segmented Capsule Tab Row (48dp height)
                SegmentedCapsule(
                    items = listOf("16x Flow", "4x Upscale", "Depth Map"),
                    selectedIndex = state.selectedTool.ordinal,
                    onItemSelected = { if (!state.isProcessing) viewModel.selectTool(AiToolType.entries[it]) },
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = AppleTile2,
                    height = 48.dp
                )

                // Tool Options with Horizontal Scrollable Rows (No cramped wrapping!)
                when (state.selectedTool) {
                    AiToolType.INTERPOLATION -> {
                        Text(
                            "Framerate Multiplier:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = AppleWhite
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(2, 4, 8, 16).forEach { mult ->
                                AppleFilterChip(
                                    selected = state.interpolationConfig.multiplier == mult,
                                    onClick = { viewModel.setInterpolationMultiplier(mult) },
                                    label = "${mult}x"
                                )
                            }
                        }

                        val baseFps = state.selectedClipFps ?: 24.0f
                        val rawFps = (baseFps * state.interpolationConfig.multiplier).toInt()
                        val clampedFps = minOf(120, rawFps)
                        Text(
                            text = "Target Framerate: ~${clampedFps} fps (from ${baseFps.toInt()}fps * ${state.interpolationConfig.multiplier}x)",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppleSkyBlue,
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            "Model Architecture:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = AppleWhite
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            InterpolationModel.entries.forEach { model ->
                                AppleFilterChip(
                                    selected = state.interpolationConfig.model == model,
                                    onClick = { viewModel.setInterpolationModel(model) },
                                    label = model.displayName
                                )
                            }
                        }
                    }

                    AiToolType.UPSCALE -> {
                        Text(
                            "Upscale Factor:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = AppleWhite
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(2, 4).forEach { scale ->
                                AppleFilterChip(
                                    selected = state.upscaleConfig.scale == scale,
                                    onClick = { viewModel.setUpscaleScale(scale) },
                                    label = "${scale}x Resolution"
                                )
                            }
                        }

                        val baseW = state.selectedClipWidth ?: 1280
                        val baseH = state.selectedClipHeight ?: 720
                        val scale = state.upscaleConfig.scale
                        Text(
                            text = "Target Resolution: ${baseW * scale}x${baseH * scale}",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppleSkyBlue,
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            "Model Engine:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = AppleWhite
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            UpscaleModel.entries.forEach { model ->
                                AppleFilterChip(
                                    selected = state.upscaleConfig.model == model,
                                    onClick = { viewModel.setUpscaleModel(model) },
                                    label = model.displayName
                                )
                            }
                        }

                        Text(
                            "Sharpness Preset:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = AppleWhite
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            UpscaleQuality.entries.forEach { quality ->
                                AppleFilterChip(
                                    selected = state.upscaleConfig.quality == quality,
                                    onClick = { viewModel.setUpscaleQuality(quality) },
                                    label = quality.name.lowercase().replaceFirstChar { it.uppercase() }
                                )
                            }
                        }
                    }

                    AiToolType.DEPTH_MAP -> {
                        Text(
                            "Depth Architecture:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = AppleWhite
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DepthModel.entries.forEach { model ->
                                AppleFilterChip(
                                    selected = state.depthConfig.model == model,
                                    onClick = { viewModel.setDepthModel(model) },
                                    label = model.displayName
                                )
                            }
                        }

                        Text(
                            "Colormap Preset:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = AppleWhite
                        )
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
                                DepthColormap.PLASMA,
                                DepthColormap.GRAYSCALE
                            ).forEach { colormap ->
                                AppleFilterChip(
                                    selected = state.depthConfig.colormap == colormap,
                                    onClick = { viewModel.setDepthColormap(colormap) },
                                    label = colormap.displayName
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Execution Action Button (52dp height, bold, unmistakable priority)
        Button(
            onClick = { viewModel.runAiToolOnSelectedClip() },
            enabled = state.selectedClipUri != null && !state.isProcessing,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 52.dp),
            shape = RoundedCornerShape(9999.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AppleIndigo,
                contentColor = AppleWhite,
                disabledContainerColor = AppleTile2,
                disabledContentColor = AppleGrayDark
            )
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (state.selectedClipUri != null && !state.isProcessing) AppleWhite else AppleGrayDark
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Run AI Processing Pass",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        // 4. Processing Progress Card
        if (state.isProcessing) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(AppleTile1)
                    .border(1.dp, AppleHairlineDark, RoundedCornerShape(18.dp))
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Processing in Progress...",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = AppleWhite
                        )
                        Text(
                            text = "${(state.progress * 100).toInt()}%",
                            style = MaterialTheme.typography.titleMedium,
                            color = AppleSkyBlue,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    LinearProgressIndicator(
                        progress = { state.progress },
                        color = AppleSkyBlue,
                        trackColor = AppleTile3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(9999.dp))
                    )

                    Text(
                        text = state.message.ifEmpty { "Executing AI neural pass..." },
                        style = MaterialTheme.typography.bodySmall,
                        color = AppleGrayDark
                    )
                }
            }
        }

        // 5. Error Card
        if (state.error != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(AppleTile1)
                    .border(1.dp, AppleRed.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = state.error ?: "An unexpected error occurred during AI processing",
                    color = AppleRed,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // 6. AI Result Output Card & Player
        if (state.result != null) {
            val res = state.result!!
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(AppleTile1)
                    .border(1.dp, AppleGreen.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "AI Result Ready",
                        style = MaterialTheme.typography.titleMedium,
                        color = AppleGreen,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.28).sp
                    )

                    Text(
                        text = "Output: ${res.width}x${res.height} @ ${res.fps.toInt()}fps (${res.frameCount} frames)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = AppleWhite
                    )

                    // Embedded Player for AI output
                    AiVideoPlayer(
                        uriOrPath = res.processedVideo.absolutePath,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.Black)
                            .border(1.dp, AppleHairlineDark, RoundedCornerShape(14.dp))
                    )

                    if (state.isSavedToStorage) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = AppleGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Saved to Movies/AMVerge in device storage",
                                style = MaterialTheme.typography.bodyMedium,
                                color = AppleGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Button(
                            onClick = { viewModel.saveResultToStorage(res) },
                            shape = RoundedCornerShape(9999.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppleSkyBlue,
                                contentColor = Color.Black
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                        ) {
                            Text(
                                "Save to Device (Movies/AMVerge)",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
    }
}

@Composable
private fun AppleFilterChip(
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

@Composable
private fun AppleMetaBadge(label: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(9999.dp))
            .background(AppleTile2)
            .border(1.dp, AppleHairlineDark, RoundedCornerShape(9999.dp))
            .padding(horizontal = 12.dp, vertical = 5.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = AppleSkyBlue,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun AiVideoPlayer(
    uriOrPath: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val player = remember(uriOrPath) {
        ExoPlayer.Builder(context).build().apply {
            val uri = if (uriOrPath.startsWith("content://") || uriOrPath.startsWith("file://")) {
                Uri.parse(uriOrPath)
            } else {
                Uri.fromFile(File(uriOrPath))
            }
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
            playWhenReady = false
        }
    }

    DisposableEffect(player) {
        onDispose {
            player.release()
        }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                this.player = player
                useController = true
            }
        },
        modifier = modifier
    )
}
