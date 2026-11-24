package com.amverge.android.frontend.screens.preview

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import com.amverge.android.backend.domain.model.Scene
import com.amverge.android.core.common.MediaTime
import com.amverge.android.frontend.theme.AppleCanvasDark
import com.amverge.android.frontend.theme.AppleGold
import com.amverge.android.frontend.theme.AppleGrayDark
import com.amverge.android.frontend.theme.AppleHairlineDark
import com.amverge.android.frontend.theme.AppleIndigo
import com.amverge.android.frontend.theme.AppleRed
import com.amverge.android.frontend.theme.AppleSkyBlue
import com.amverge.android.frontend.theme.AppleTile1
import com.amverge.android.frontend.theme.AppleTile2
import com.amverge.android.frontend.theme.AppleTile3
import com.amverge.android.frontend.theme.AppleWhite
import com.amverge.android.frontend.viewmodel.PreviewViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScenePreviewScreen(
    viewModel: PreviewViewModel,
    onNavigateBack: () -> Unit,
    onSendToAiStudio: ((Scene) -> Unit)? = null
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val playerView = remember {
        PlayerView(context).apply {
            player = viewModel.playerController.player
            useController = false
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            playerView.player = null
        }
    }

    Scaffold(
        containerColor = AppleCanvasDark,
        topBar = {
            TopAppBar(
                title = {
                    val sceneText = state.scene?.let {
                        "Cut #${it.index + 1} (${MediaTime.formatDuration(it.durationMs)})"
                    } ?: "Cut Preview"
                    Column {
                        Text(
                            text = sceneText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AppleWhite,
                            letterSpacing = (-0.28).sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        state.scene?.let { s ->
                            Text(
                                text = "${MediaTime.formatTime(s.startMs)} → ${MediaTime.formatTime(s.endMs)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = AppleSkyBlue,
                                maxLines = 1
                            )
                        }
                    }
                },
                navigationIcon = {
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
                },
                actions = {
                    state.scene?.let { scene ->
                        // Star Button (44x44 dp)
                        IconButton(
                            onClick = { viewModel.toggleFavorite() },
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(AppleTile1)
                                .border(1.dp, AppleHairlineDark, CircleShape)
                        ) {
                            if (scene.isFavorite) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Unfavorite",
                                    tint = AppleGold,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.StarBorder,
                                    contentDescription = "Favorite",
                                    tint = AppleWhite,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
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
        // Structured Non-Overlapping Layout (Video Top, Console Bottom)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(AppleCanvasDark)
        ) {
            // 1. Un-obscured 16:9 Video Player Surface
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = { playerView },
                    modifier = Modifier.fillMaxSize()
                )

                state.errorMessage?.let { error ->
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(16.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(AppleRed.copy(alpha = 0.85f))
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = error,
                            color = AppleWhite,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // 2. Dedicated Bottom Control Console (Clean, No Overlapping!)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = AppleTile1,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                border = BorderStroke(1.dp, AppleHairlineDark)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Row A: Timecode & Cut Details
                    state.scene?.let { s ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Cut #${s.index + 1}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = AppleWhite
                            )
                            Text(
                                text = "Duration: ${MediaTime.formatDuration(s.durationMs)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = AppleSkyBlue,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Row B: Main Transport Controls (Centered, Standardized 44-56dp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Step Backward -1 Frame (44x44 dp)
                        TransportButton(
                            onClick = { viewModel.stepBackward() },
                            icon = Icons.Default.FastRewind,
                            contentDescription = "Step -1 Frame",
                            size = 44.dp
                        )

                        // Previous Cut (46x46 dp)
                        TransportButton(
                            onClick = { viewModel.playPreviousScene() },
                            icon = Icons.Default.SkipPrevious,
                            contentDescription = "Previous Cut",
                            size = 46.dp
                        )

                        // Center Play / Pause Hero CTA (56x56 dp, high contrast Apple Sky Blue)
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(AppleSkyBlue)
                                .clickable { viewModel.togglePlayPause() },
                            contentAlignment = Alignment.Center
                        ) {
                            if (state.isPlaying) {
                                Icon(
                                    Icons.Default.Pause,
                                    contentDescription = "Pause",
                                    tint = Color.Black,
                                    modifier = Modifier.size(28.dp)
                                )
                            } else {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = "Play",
                                    tint = Color.Black,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        // Next Cut (46x46 dp)
                        TransportButton(
                            onClick = { viewModel.playNextScene() },
                            icon = Icons.Default.SkipNext,
                            contentDescription = "Next Cut",
                            size = 46.dp
                        )

                        // Step Forward +1 Frame (44x44 dp)
                        TransportButton(
                            onClick = { viewModel.stepForward() },
                            icon = Icons.Default.FastForward,
                            contentDescription = "Step +1 Frame",
                            size = 44.dp
                        )
                    }

                    // Row C: Secondary Actions & Options (Split Cut, AI Studio, Speed, Loop)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Split Cut Button
                        Button(
                            onClick = { viewModel.splitCurrentScene() },
                            shape = RoundedCornerShape(9999.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppleTile2,
                                contentColor = AppleSkyBlue
                            ),
                            border = BorderStroke(1.dp, AppleHairlineDark),
                            modifier = Modifier.defaultMinSize(minHeight = 42.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.CallSplit,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = AppleSkyBlue
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Split Cut", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                        }

                        // AI Studio Button
                        if (onSendToAiStudio != null && state.scene != null) {
                            Button(
                                onClick = { onSendToAiStudio(state.scene!!) },
                                shape = RoundedCornerShape(9999.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AppleIndigo,
                                    contentColor = AppleWhite
                                ),
                                modifier = Modifier.defaultMinSize(minHeight = 42.dp)
                            ) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = AppleWhite
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("AI Studio", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }

                        FilterChip(
                            selected = state.isLooping,
                            onClick = { viewModel.toggleLooping() },
                            shape = RoundedCornerShape(9999.dp),
                            leadingIcon = {
                                Icon(
                                    imageVector = if (state.isLooping) Icons.Default.RepeatOne else Icons.Default.Repeat,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                            },
                            label = { Text(if (state.isLooping) "Looping" else "Play Once", style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AppleSkyBlue,
                                selectedLabelColor = Color.Black,
                                selectedLeadingIconColor = Color.Black,
                                containerColor = AppleTile2,
                                labelColor = AppleWhite,
                                iconColor = AppleWhite
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = state.isLooping,
                                borderColor = AppleHairlineDark,
                                selectedBorderColor = Color.Transparent,
                                borderWidth = 1.dp
                            ),
                            modifier = Modifier.defaultMinSize(minHeight = 42.dp)
                        )

                        FilterChip(
                            selected = (state.playbackSpeed != 1.0f),
                            onClick = { viewModel.toggleSpeed() },
                            shape = RoundedCornerShape(9999.dp),
                            label = { Text("${state.playbackSpeed}x Speed", style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AppleSkyBlue,
                                selectedLabelColor = Color.Black,
                                containerColor = AppleTile2,
                                labelColor = AppleWhite
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = (state.playbackSpeed != 1.0f),
                                borderColor = AppleHairlineDark,
                                selectedBorderColor = Color.Transparent,
                                borderWidth = 1.dp
                            ),
                            modifier = Modifier.defaultMinSize(minHeight = 42.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TransportButton(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    size: androidx.compose.ui.unit.Dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(AppleTile2)
            .border(1.dp, AppleHairlineDark, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = AppleWhite,
            modifier = Modifier.size(20.dp)
        )
    }
}
