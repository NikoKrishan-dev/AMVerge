package com.amverge.android.frontend.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.amverge.android.backend.domain.model.Series
import com.amverge.android.core.common.MediaTime
import com.amverge.android.frontend.components.SegmentedCapsule
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
import com.amverge.android.frontend.viewmodel.SeriesLibraryViewModel

@Composable
fun SeriesLibraryScreen(
    viewModel: SeriesLibraryViewModel,
    onNavigateToProject: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    var isCreateSeriesDialogOpen by remember { mutableStateOf(false) }
    var newSeriesTitle by remember { mutableStateOf("") }
    var newSeriesDesc by remember { mutableStateOf("") }
    var attachingSeriesId by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppleCanvasDark)
            .padding(16.dp)
    ) {
        // Ergonomic Segmented Capsule Tab Bar (48dp height)
        SegmentedCapsule(
            items = listOf("Anime Series (${state.seriesList.size})", "Scenepacks (${state.scenepacks.size})"),
            selectedIndex = selectedTab,
            onItemSelected = { selectedTab = it },
            modifier = Modifier.fillMaxWidth(),
            height = 48.dp
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (state.errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(AppleTile1)
                    .border(1.dp, AppleRed.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Text(
                    text = state.errorMessage ?: "",
                    color = AppleRed,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        if (state.exportedZipLocation != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(AppleTile1)
                    .border(1.dp, AppleGreen.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Scenepack Exported",
                            style = MaterialTheme.typography.titleSmall,
                            color = AppleGreen,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "ZIP archive and CMX 3600 EDL timeline delivered to Movies/AMVerge.",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppleGrayDark
                        )
                    }
                    Button(
                        onClick = { viewModel.clearStatus() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppleTile3,
                            contentColor = AppleWhite
                        ),
                        shape = RoundedCornerShape(9999.dp),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Text("Dismiss", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        if (selectedTab == 0) {
            // Anime Series Section Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Anime Series Library",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppleWhite,
                    letterSpacing = (-0.28).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(end = 8.dp)
                )
                Button(
                    onClick = { isCreateSeriesDialogOpen = true },
                    shape = RoundedCornerShape(9999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppleSkyBlue,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier.defaultMinSize(minHeight = 44.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color.Black
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "New Series",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (state.seriesList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
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
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = AppleSkyBlue,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Text(
                            text = "No Anime Series Created Yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AppleWhite
                        )
                        Text(
                            text = "Group episodes by anime title to organize your AMV workflow.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppleGrayDark
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(state.seriesList, key = { it.id }) { series ->
                        val seriesEpisodes = state.allProjects.filter { it.seriesId == series.id }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(AppleTile1)
                                .border(1.dp, AppleHairlineDark, RoundedCornerShape(18.dp))
                                .padding(18.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(end = 8.dp)
                                    ) {
                                        Text(
                                            text = series.title,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = AppleWhite,
                                            letterSpacing = (-0.28).sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (!series.description.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = series.description,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = AppleGrayDark,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "${seriesEpisodes.size} episode(s) attached",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = AppleSkyBlue,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.deleteSeries(series.id) },
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(AppleTile2)
                                            .border(1.dp, AppleHairlineDark, CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Series",
                                            tint = AppleRed,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                if (seriesEpisodes.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    // Clean list layout without nested card clutter
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        seriesEpisodes.forEach { ep ->
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .defaultMinSize(minHeight = 54.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(AppleTile2)
                                                    .border(1.dp, AppleHairlineDark, RoundedCornerShape(12.dp))
                                                    .clickable { onNavigateToProject(ep.id) }
                                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                                contentAlignment = Alignment.CenterStart
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Column(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .padding(end = 8.dp)
                                                    ) {
                                                        Text(
                                                            text = ep.displayName,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            color = AppleWhite,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Text(
                                                            text = "Duration: ${MediaTime.formatDuration(ep.durationMs)}",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = AppleGrayDark,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                    Box(
                                                        modifier = Modifier
                                                            .size(36.dp)
                                                            .clip(CircleShape)
                                                            .background(AppleTile3),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.PlayArrow,
                                                            contentDescription = "Open Scenes",
                                                            tint = AppleSkyBlue,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))
                                OutlinedButton(
                                    onClick = { attachingSeriesId = series.id },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .defaultMinSize(minHeight = 44.dp),
                                    shape = RoundedCornerShape(9999.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, AppleSkyBlue.copy(alpha = 0.5f)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AppleSkyBlue)
                                ) {
                                    Text("Attach Unassigned Episodes", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Scenepacks Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Scenepack Collections",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppleWhite,
                    letterSpacing = (-0.28).sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (state.scenepacks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
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
                                imageVector = Icons.Default.CollectionsBookmark,
                                contentDescription = null,
                                tint = AppleSkyBlue,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Text(
                            text = "No Scenepacks Created Yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AppleWhite
                        )
                        Text(
                            text = "Select cuts from the Scene Grid and tap 'Pack' to create a collection.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppleGrayDark
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(state.scenepacks, key = { it.id }) { pack ->
                        val isExportingThis = state.isExportingPack && state.exportingPackId == pack.id
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(AppleTile1)
                                .border(1.dp, AppleHairlineDark, RoundedCornerShape(18.dp))
                                .padding(18.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(end = 8.dp)
                                    ) {
                                        Text(
                                            text = pack.name,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = AppleWhite,
                                            letterSpacing = (-0.28).sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (!pack.description.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = pack.description,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = AppleGrayDark,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "${pack.sceneCount} scene cut(s)",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = AppleSkyBlue,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.deleteScenepack(pack.id) },
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(AppleTile2)
                                            .border(1.dp, AppleHairlineDark, CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Scenepack",
                                            tint = AppleRed,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                if (isExportingThis) {
                                    LinearProgressIndicator(
                                        progress = { state.exportProgress },
                                        color = AppleSkyBlue,
                                        trackColor = AppleTile3,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(9999.dp))
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = state.exportMessage ?: "Exporting scenepack...",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = AppleSkyBlue
                                    )
                                } else {
                                    Button(
                                        onClick = { viewModel.exportScenepackToZip(pack.id) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .defaultMinSize(minHeight = 48.dp),
                                        shape = RoundedCornerShape(9999.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = AppleSkyBlue,
                                            contentColor = Color.Black
                                        ),
                                        enabled = !state.isExportingPack
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Archive,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = Color.Black
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "Export ZIP & EDL",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (isCreateSeriesDialogOpen) {
        AlertDialog(
            onDismissRequest = { isCreateSeriesDialogOpen = false },
            containerColor = AppleTile1,
            title = {
                Text(
                    "Create Anime Series",
                    color = AppleWhite,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.28).sp
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(
                        value = newSeriesTitle,
                        onValueChange = { newSeriesTitle = it },
                        label = { Text("Series Title", color = AppleGrayDark) },
                        placeholder = { Text("e.g. Jujutsu Kaisen S2", color = AppleGrayDark.copy(alpha = 0.5f)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = AppleWhite,
                            unfocusedTextColor = AppleWhite,
                            focusedBorderColor = AppleSkyBlue,
                            unfocusedBorderColor = AppleHairlineDark
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newSeriesDesc,
                        onValueChange = { newSeriesDesc = it },
                        label = { Text("Description (Optional)", color = AppleGrayDark) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = AppleWhite,
                            unfocusedTextColor = AppleWhite,
                            focusedBorderColor = AppleSkyBlue,
                            unfocusedBorderColor = AppleHairlineDark
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newSeriesTitle.isNotBlank()) {
                            viewModel.createSeries(newSeriesTitle.trim(), newSeriesDesc.trim().ifEmpty { null })
                            newSeriesTitle = ""
                            newSeriesDesc = ""
                            isCreateSeriesDialogOpen = false
                        }
                    },
                    shape = RoundedCornerShape(9999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppleSkyBlue,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier.height(44.dp),
                    enabled = newSeriesTitle.isNotBlank()
                ) {
                    Text("Create Series", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { isCreateSeriesDialogOpen = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = AppleGrayDark),
                    modifier = Modifier.height(44.dp)
                ) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(22.dp)
        )
    }

    attachingSeriesId?.let { sId ->
        val unassignedEpisodes = state.allProjects.filter { it.seriesId == null }
        AlertDialog(
            onDismissRequest = { attachingSeriesId = null },
            containerColor = AppleTile1,
            title = {
                Text(
                    "Attach Episode to Series",
                    color = AppleWhite,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.28).sp
                )
            },
            text = {
                if (unassignedEpisodes.isEmpty()) {
                    Text(
                        "No unassigned imported episodes found. Import a new episode from the Scene Cuts tab.",
                        color = AppleGrayDark,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(unassignedEpisodes, key = { it.id }) { ep ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(AppleTile2)
                                    .border(1.dp, AppleHairlineDark, RoundedCornerShape(12.dp))
                                    .clickable {
                                        viewModel.assignEpisodeToSeries(ep.id, sId)
                                        attachingSeriesId = null
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Movie,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = AppleSkyBlue
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = ep.displayName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = AppleWhite,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = { attachingSeriesId = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = AppleGrayDark),
                    modifier = Modifier.height(44.dp)
                ) {
                    Text("Close")
                }
            },
            shape = RoundedCornerShape(22.dp)
        )
    }
}
