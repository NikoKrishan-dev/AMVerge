package com.amverge.android.frontend.screens.import

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amverge.android.core.common.MediaTime
import com.amverge.android.frontend.components.DetectionSettingsDialog
import com.amverge.android.frontend.components.SegmentedCapsule
import com.amverge.android.frontend.screens.ai.AiStudioScreen
import com.amverge.android.frontend.screens.library.SeriesLibraryScreen
import com.amverge.android.frontend.theme.AppleCanvasDark
import com.amverge.android.frontend.theme.AppleGrayDark
import com.amverge.android.frontend.theme.AppleHairlineDark
import com.amverge.android.frontend.theme.AppleRed
import com.amverge.android.frontend.theme.AppleSkyBlue
import com.amverge.android.frontend.theme.AppleTile1
import com.amverge.android.frontend.theme.AppleTile2
import com.amverge.android.frontend.theme.AppleTile3
import com.amverge.android.frontend.theme.AppleWhite
import com.amverge.android.frontend.viewmodel.AiToolsViewModel
import com.amverge.android.frontend.viewmodel.ImportViewModel
import com.amverge.android.frontend.viewmodel.SeriesLibraryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(
    viewModel: ImportViewModel,
    aiToolsViewModel: AiToolsViewModel,
    seriesLibraryViewModel: SeriesLibraryViewModel? = null,
    onNavigateToProject: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var selectedMainTab by remember { mutableIntStateOf(1) } // Default to Cuts Studio

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.onVideoSelected(uri) { project ->
                onNavigateToProject(project.id)
            }
        }
    }

    Scaffold(
        containerColor = AppleCanvasDark,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "AMVerge",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = AppleWhite,
                            letterSpacing = (-0.38).sp
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(9999.dp))
                                .background(AppleSkyBlue.copy(alpha = 0.15f))
                                .border(1.dp, AppleSkyBlue.copy(alpha = 0.35f), RoundedCornerShape(9999.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "STUDIO",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = AppleSkyBlue
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.openSettingsDialog() },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(AppleTile1)
                            .border(1.dp, AppleHairlineDark, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Detection Settings",
                            tint = AppleWhite,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppleCanvasDark,
                    titleContentColor = AppleWhite
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Ergonomic Segmented Capsule Bar (48dp height, reliable Apple styling)
            SegmentedCapsule(
                items = listOf("AI Suite", "Scene Cuts", "Library"),
                selectedIndex = selectedMainTab,
                onItemSelected = { selectedMainTab = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                height = 48.dp
            )

            when (selectedMainTab) {
                0 -> {
                    AiStudioScreen(
                        viewModel = aiToolsViewModel,
                        showTopBar = false
                    )
                }
                1 -> {
                    // Scene Detection Studio with Spacious Layout
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Spacer(modifier = Modifier.height(4.dp))

                        // Hero Presentation Card (Spacious, Clear, Intentional)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(22.dp))
                                .background(AppleTile1)
                                .border(1.dp, AppleHairlineDark, RoundedCornerShape(22.dp))
                                .padding(24.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .background(AppleTile2)
                                        .border(1.dp, AppleSkyBlue.copy(alpha = 0.35f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VideoFile,
                                        contentDescription = null,
                                        modifier = Modifier.size(36.dp),
                                        tint = AppleSkyBlue
                                    )
                                }

                                Text(
                                    text = "Anime Scene Detection",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = AppleWhite,
                                    textAlign = TextAlign.Center,
                                    letterSpacing = (-0.28).sp
                                )

                                Text(
                                    text = "Instant hardware-accelerated cut extraction with non-destructive EDL timeline export for DaVinci, Premiere, and mobile editors.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = AppleGrayDark,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 22.sp
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                if (state.isLoading) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(AppleTile2)
                                            .padding(16.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(36.dp),
                                            color = AppleSkyBlue,
                                            strokeWidth = 3.dp
                                        )
                                        Text(
                                            text = state.statusMessage ?: "Extracting keyframe cuts...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = AppleWhite
                                        )
                                        LinearProgressIndicator(
                                            progress = { state.progress },
                                            color = AppleSkyBlue,
                                            trackColor = AppleTile3,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(9999.dp))
                                        )
                                    }
                                } else {
                                    // High-contrast, effortless primary button
                                    Button(
                                        onClick = { filePicker.launch(arrayOf("video/*")) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(52.dp),
                                        shape = RoundedCornerShape(9999.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = AppleSkyBlue,
                                            contentColor = Color.Black
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Movie,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = Color.Black
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Select Episode Video",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black
                                        )
                                    }

                                    state.errorMessage?.let { error ->
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(AppleTile2)
                                                .border(1.dp, AppleRed.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                                                .padding(14.dp)
                                        ) {
                                            Text(
                                                text = error,
                                                color = AppleRed,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Active / Recent Project Section
                        state.latestProject?.let { project ->
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Latest Project",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = AppleGrayDark,
                                    letterSpacing = (-0.14).sp
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(AppleTile1)
                                        .border(1.dp, AppleHairlineDark, RoundedCornerShape(18.dp))
                                        .clickable { onNavigateToProject(project.id) }
                                        .padding(18.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(end = 12.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = project.displayName,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = AppleWhite,
                                                letterSpacing = (-0.28).sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )

                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.horizontalScroll(rememberScrollState())
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(9999.dp))
                                                        .background(AppleTile2)
                                                        .border(1.dp, AppleHairlineDark, RoundedCornerShape(9999.dp))
                                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                                ) {
                                                    Text(
                                                        text = MediaTime.formatDuration(project.durationMs),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = AppleSkyBlue,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }

                                                project.width?.let { w ->
                                                    val h = project.height ?: 0
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(9999.dp))
                                                            .background(AppleTile2)
                                                            .border(1.dp, AppleHairlineDark, RoundedCornerShape(9999.dp))
                                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                                    ) {
                                                        Text(
                                                            text = "${w}x${h}",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = AppleGrayDark,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        // 48x48 dp touch target circle
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(CircleShape)
                                                .background(AppleTile2)
                                                .border(1.dp, AppleHairlineDark, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                                contentDescription = "Open project",
                                                tint = AppleSkyBlue,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
                2 -> {
                    seriesLibraryViewModel?.let { libVm ->
                        SeriesLibraryScreen(
                            viewModel = libVm,
                            onNavigateToProject = onNavigateToProject
                        )
                    }
                }
            }
        }
    }

    if (state.isSettingsDialogOpen) {
        DetectionSettingsDialog(
            initialPreferences = state.detectionPreferences,
            onSavePreferences = { viewModel.saveDetectionPreferences(it) },
            onDismiss = { viewModel.closeSettingsDialog() }
        )
    }
}
