package com.amverge.android.frontend.screens.scenes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlipToBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amverge.android.backend.domain.model.Scene
import com.amverge.android.frontend.components.AiToolsDialog
import com.amverge.android.frontend.components.DetectionSettingsDialog
import com.amverge.android.frontend.components.ExportDialog
import com.amverge.android.frontend.components.SceneCard
import com.amverge.android.frontend.components.ScenepackDialog
import com.amverge.android.frontend.components.SceneSelectionBar
import com.amverge.android.frontend.components.TimelineOverviewBar
import com.amverge.android.frontend.state.SceneFilter
import com.amverge.android.frontend.state.SceneSortOrder
import com.amverge.android.frontend.theme.AppleCanvasDark
import com.amverge.android.frontend.theme.AppleGrayDark
import com.amverge.android.frontend.theme.AppleHairlineDark
import com.amverge.android.frontend.theme.AppleSkyBlue
import com.amverge.android.frontend.theme.AppleTile1
import com.amverge.android.frontend.theme.AppleTile2
import com.amverge.android.frontend.theme.AppleTile3
import com.amverge.android.frontend.theme.AppleWhite
import com.amverge.android.frontend.viewmodel.AiToolsViewModel
import com.amverge.android.frontend.viewmodel.ExportViewModel
import com.amverge.android.frontend.viewmodel.SceneGridViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SceneGridScreen(
    viewModel: SceneGridViewModel,
    exportViewModel: ExportViewModel,
    aiToolsViewModel: AiToolsViewModel,
    onNavigateBack: () -> Unit,
    onPreviewScene: (projectId: String, sceneId: String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val exportState by exportViewModel.uiState.collectAsState()
    val aiState by aiToolsViewModel.uiState.collectAsState()
    var aiTargetScene by remember { mutableStateOf<Scene?>(null) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var moreMenuExpanded by remember { mutableStateOf(false) }
    val gridState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        containerColor = AppleCanvasDark,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = state.project?.displayName ?: "Scenes",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AppleWhite,
                            letterSpacing = (-0.28).sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${state.filteredScenes.size} cuts available",
                            style = MaterialTheme.typography.labelSmall,
                            color = AppleSkyBlue,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
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
                    // Search Trigger (44x44 dp)
                    IconButton(
                        onClick = { viewModel.toggleSearchVisibility() },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Search Cuts",
                            tint = if (state.isSearchVisible) AppleSkyBlue else AppleWhite
                        )
                    }

                    // Sort Menu Trigger (44x44 dp)
                    IconButton(
                        onClick = { sortMenuExpanded = true },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Sort,
                            contentDescription = "Sort Cuts",
                            tint = AppleWhite
                        )
                    }
                    DropdownMenu(
                        expanded = sortMenuExpanded,
                        onDismissRequest = { sortMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Source Order") },
                            onClick = {
                                viewModel.setSortOrder(SceneSortOrder.CHRONOLOGICAL)
                                sortMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Shortest First") },
                            onClick = {
                                viewModel.setSortOrder(SceneSortOrder.SHORTEST_FIRST)
                                sortMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Longest First") },
                            onClick = {
                                viewModel.setSortOrder(SceneSortOrder.LONGEST_FIRST)
                                sortMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Most Dynamic") },
                            onClick = {
                                viewModel.setSortOrder(SceneSortOrder.MOST_DYNAMIC)
                                sortMenuExpanded = false
                            }
                        )
                    }

                    // Consolidated More Menu (Clean, uncluttered top bar)
                    IconButton(
                        onClick = { moreMenuExpanded = true },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "More Options",
                            tint = AppleWhite
                        )
                    }
                    DropdownMenu(
                        expanded = moreMenuExpanded,
                        onDismissRequest = { moreMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Select All Cuts") },
                            leadingIcon = { Icon(Icons.Default.SelectAll, contentDescription = null) },
                            onClick = {
                                viewModel.selectAll()
                                moreMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Invert Selection") },
                            leadingIcon = { Icon(Icons.Default.FlipToBack, contentDescription = null) },
                            onClick = {
                                viewModel.invertSelection()
                                moreMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Detection Settings") },
                            leadingIcon = { Icon(Icons.Default.Tune, contentDescription = null) },
                            onClick = {
                                viewModel.openSettingsDialog()
                                moreMenuExpanded = false
                            }
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
            // Search Bar (48dp height capsule)
            if (state.isSearchVisible) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("Filter cuts (#15, action, static)...", color = AppleGrayDark) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 52.dp),
                        singleLine = true,
                        shape = RoundedCornerShape(9999.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = AppleTile1,
                            unfocusedContainerColor = AppleTile1,
                            focusedBorderColor = AppleSkyBlue,
                            unfocusedBorderColor = AppleHairlineDark,
                            focusedTextColor = AppleWhite,
                            unfocusedTextColor = AppleWhite
                        ),
                        trailingIcon = {
                            if (state.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear search", tint = AppleWhite)
                                }
                            }
                        }
                    )
                }
            }

            // Standardized Capsule Filter Row (38dp height, comfortable spacing)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val chipColors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AppleSkyBlue,
                    selectedLabelColor = Color.Black,
                    containerColor = AppleTile1,
                    labelColor = AppleWhite
                )
                val chipBorder = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = false,
                    borderColor = AppleHairlineDark,
                    selectedBorderColor = Color.Transparent,
                    borderWidth = 1.dp
                )

                FilterChip(
                    selected = (state.filter == SceneFilter.ALL),
                    onClick = { viewModel.setFilter(SceneFilter.ALL) },
                    label = { Text("All (${state.scenes.size})", fontWeight = FontWeight.SemiBold) },
                    shape = RoundedCornerShape(9999.dp),
                    colors = chipColors,
                    border = chipBorder,
                    modifier = Modifier.height(38.dp)
                )
                FilterChip(
                    selected = (state.filter == SceneFilter.DYNAMIC_ONLY),
                    onClick = { viewModel.setFilter(SceneFilter.DYNAMIC_ONLY) },
                    label = { Text("Dynamic (${state.scenes.count { !it.isDeadframe }})", fontWeight = FontWeight.SemiBold) },
                    shape = RoundedCornerShape(9999.dp),
                    colors = chipColors,
                    border = chipBorder,
                    modifier = Modifier.height(38.dp)
                )
                FilterChip(
                    selected = (state.filter == SceneFilter.FAVORITES),
                    onClick = { viewModel.setFilter(SceneFilter.FAVORITES) },
                    label = { Text("Starred (${state.scenes.count { it.isFavorite }})", fontWeight = FontWeight.SemiBold) },
                    shape = RoundedCornerShape(9999.dp),
                    colors = chipColors,
                    border = chipBorder,
                    modifier = Modifier.height(38.dp)
                )
                FilterChip(
                    selected = (state.filter == SceneFilter.ACTION),
                    onClick = { viewModel.setFilter(SceneFilter.ACTION) },
                    label = {
                        Text(
                            "Action (${state.scenes.count { it.tags.any { t -> t.contains("action", true) } || it.durationMs < 1500L }})",
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    shape = RoundedCornerShape(9999.dp),
                    colors = chipColors,
                    border = chipBorder,
                    modifier = Modifier.height(38.dp)
                )
                FilterChip(
                    selected = (state.filter == SceneFilter.SHORT),
                    onClick = { viewModel.setFilter(SceneFilter.SHORT) },
                    label = { Text("< 1.5s (${state.scenes.count { it.durationMs < 1500L }})", fontWeight = FontWeight.SemiBold) },
                    shape = RoundedCornerShape(9999.dp),
                    colors = chipColors,
                    border = chipBorder,
                    modifier = Modifier.height(38.dp)
                )
                FilterChip(
                    selected = (state.filter == SceneFilter.MEDIUM),
                    onClick = { viewModel.setFilter(SceneFilter.MEDIUM) },
                    label = { Text("1.5-4s (${state.scenes.count { it.durationMs in 1500L..4000L }})", fontWeight = FontWeight.SemiBold) },
                    shape = RoundedCornerShape(9999.dp),
                    colors = chipColors,
                    border = chipBorder,
                    modifier = Modifier.height(38.dp)
                )
                FilterChip(
                    selected = (state.filter == SceneFilter.LONG),
                    onClick = { viewModel.setFilter(SceneFilter.LONG) },
                    label = { Text("> 4s (${state.scenes.count { it.durationMs > 4000L }})", fontWeight = FontWeight.SemiBold) },
                    shape = RoundedCornerShape(9999.dp),
                    colors = chipColors,
                    border = chipBorder,
                    modifier = Modifier.height(38.dp)
                )
            }

            // Interactive Proportional Timeline Overview Bar
            val totalDuration = state.project?.durationMs ?: 0L
            if (state.scenes.isNotEmpty() && totalDuration > 0L) {
                TimelineOverviewBar(
                    scenes = state.scenes,
                    totalDurationMs = totalDuration,
                    selectedSceneIds = state.selectedSceneIds,
                    onSceneSelected = { clickedScene ->
                        coroutineScope.launch {
                            val targetIndex = state.filteredScenes.indexOfFirst { it.id == clickedScene.id }
                            if (targetIndex >= 0) {
                                gridState.animateScrollToItem(targetIndex)
                            }
                        }
                    }
                )
            }

            // Scene Cuts Grid with Generous Bottom Clearance for Selection Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 96.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = state.filteredScenes,
                        key = { it.id }
                    ) { scene ->
                        val isSelected = state.selectedSceneIds.contains(scene.id)
                        SceneCard(
                            scene = scene,
                            isSelected = isSelected,
                            onToggleSelect = { viewModel.toggleSceneSelection(scene.id) },
                            onPreview = {
                                state.project?.let { p ->
                                    onPreviewScene(p.id, scene.id)
                                }
                            },
                            onToggleFavorite = { viewModel.toggleFavorite(scene.id) }
                        )
                    }
                }

                if (state.filteredScenes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(AppleTile1)
                                    .border(1.dp, AppleHairlineDark, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = AppleSkyBlue,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Text(
                                text = if (state.scenes.isEmpty()) "No Scene Cuts Detected" else "No Matching Scene Cuts",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = AppleWhite
                            )
                            Text(
                                text = if (state.scenes.isEmpty()) "Try adjusting detection sensitivity in Detection Settings." else "Try changing your active filter or search query.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = AppleGrayDark,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            if (state.scenes.isNotEmpty() && (state.filter != SceneFilter.ALL || state.searchQuery.isNotEmpty())) {
                                androidx.compose.material3.OutlinedButton(
                                    onClick = {
                                        viewModel.setFilter(SceneFilter.ALL)
                                        viewModel.setSearchQuery("")
                                    },
                                    shape = RoundedCornerShape(9999.dp),
                                    border = BorderStroke(1.dp, AppleSkyBlue.copy(alpha = 0.5f)),
                                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = AppleSkyBlue),
                                    modifier = Modifier.defaultMinSize(minHeight = 44.dp)
                                ) {
                                    Text("Reset Filters", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }

                if (state.isSelectionActive) {
                    SceneSelectionBar(
                        selectedCount = state.selectedCount,
                        onClearSelection = { viewModel.clearSelection() },
                        onExportClicked = { viewModel.openExportDialog() },
                        canMerge = state.canMergeSelected,
                        onMergeClicked = { viewModel.mergeSelectedScenes() },
                        onDeleteClicked = { viewModel.deleteSelectedScenes() },
                        onStarToggleClicked = { viewModel.toggleFavoriteSelected() },
                        onAddToScenepackClicked = { viewModel.openScenepackDialog() },
                        onAiToolsClicked = {
                            val singleId = state.selectedSceneIds.firstOrNull()
                            aiTargetScene = state.scenes.firstOrNull { it.id == singleId }
                        },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        }
    }

    aiTargetScene?.let { scene ->
        state.project?.let { project ->
            AiToolsDialog(
                scene = scene,
                sourceUri = project.sourceUri,
                uiState = aiState,
                onSelectTool = { aiToolsViewModel.selectTool(it) },
                onSetInterpolationMultiplier = { aiToolsViewModel.setInterpolationMultiplier(it) },
                onSetInterpolationModel = { aiToolsViewModel.setInterpolationModel(it) },
                onSetUpscaleScale = { aiToolsViewModel.setUpscaleScale(it) },
                onSetUpscaleModel = { aiToolsViewModel.setUpscaleModel(it) },
                onSetUpscaleQuality = { aiToolsViewModel.setUpscaleQuality(it) },
                onSetDepthModel = { aiToolsViewModel.setDepthModel(it) },
                onSetDepthColormap = { aiToolsViewModel.setDepthColormap(it) },
                onStartProcessing = { aiToolsViewModel.runAiTool(project.sourceUri, scene) },
                onSaveToStorage = {
                    aiState.result?.let { aiToolsViewModel.saveResultToStorage(it) }
                },
                onDismiss = {
                    aiTargetScene = null
                    aiToolsViewModel.reset()
                }
            )
        }
    }

    if (state.isExportDialogOpen) {
        state.project?.let { project ->
            ExportDialog(
                projectId = project.id,
                selectedSceneIds = state.selectedSceneIds.toList(),
                exportState = exportState,
                onStartExport = { request ->
                    exportViewModel.startExport(request)
                },
                onDismiss = {
                    viewModel.closeExportDialog()
                    exportViewModel.reset()
                }
            )
        }
    }

    if (state.isScenepackDialogOpen) {
        ScenepackDialog(
            selectedSceneCount = state.selectedCount,
            availableScenepacks = state.availableScenepacks,
            onCreateNewScenepack = { name, desc ->
                viewModel.createScenepack(name, desc)
            },
            onAddToExistingScenepack = { packId ->
                viewModel.addSelectedToScenepack(packId)
            },
            onDismiss = { viewModel.closeScenepackDialog() }
        )
    }

    if (state.isSettingsDialogOpen) {
        DetectionSettingsDialog(
            initialPreferences = state.detectionPreferences,
            onSavePreferences = { viewModel.saveDetectionPreferences(it) },
            onDismiss = { viewModel.closeSettingsDialog() }
        )
    }
}
