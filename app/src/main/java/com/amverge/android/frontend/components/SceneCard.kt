package com.amverge.android.frontend.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.amverge.android.backend.domain.model.Scene
import com.amverge.android.backend.domain.model.ThumbnailStatus
import com.amverge.android.core.common.MediaTime
import com.amverge.android.frontend.theme.AppleGold
import com.amverge.android.frontend.theme.AppleGrayDark
import com.amverge.android.frontend.theme.AppleGreen
import com.amverge.android.frontend.theme.AppleHairlineDark
import com.amverge.android.frontend.theme.AppleHudDark
import com.amverge.android.frontend.theme.AppleRed
import com.amverge.android.frontend.theme.AppleSkyBlue
import com.amverge.android.frontend.theme.AppleTile1
import com.amverge.android.frontend.theme.AppleTile2
import com.amverge.android.frontend.theme.AppleWhite
import java.io.File

@Composable
fun SceneCard(
    scene: Scene,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onPreview: () -> Unit,
    onToggleFavorite: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val borderStroke = if (isSelected) {
        BorderStroke(2.dp, AppleSkyBlue)
    } else {
        BorderStroke(1.dp, AppleHairlineDark)
    }

    var isScrubbing by remember { mutableStateOf(false) }
    var scrubProgress by remember { mutableFloatStateOf(0f) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onToggleSelect() },
        shape = RoundedCornerShape(14.dp),
        border = borderStroke,
        colors = CardDefaults.cardColors(
            containerColor = AppleTile1
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 1. Clean 16:9 Video Frame (Only non-overlapping corner pills)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                    .pointerInput(scene.id) {
                        detectHorizontalDragGestures(
                            onDragStart = { offset ->
                                isScrubbing = true
                                scrubProgress = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                            },
                            onDragEnd = { isScrubbing = false },
                            onDragCancel = { isScrubbing = false },
                            onHorizontalDrag = { change, _ ->
                                scrubProgress = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                            }
                        )
                    }
            ) {
                val thumbPath = scene.thumbnailPath
                val hasValidFile = thumbPath != null && File(thumbPath).exists()

                if (hasValidFile) {
                    AsyncImage(
                        model = File(thumbPath!!),
                        contentDescription = "Scene ${scene.index + 1}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (scene.thumbnailStatus == ThumbnailStatus.RUNNING) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(AppleTile2),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = AppleSkyBlue
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(AppleTile2),
                        contentAlignment = Alignment.Center
                    ) {
                        // Subtle video reel icon when thumbnail is missing or generating, leaving center clear for play button
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = AppleHairlineDark,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // Selection tint overlay
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(AppleSkyBlue.copy(alpha = 0.2f))
                    )
                }

                // Top-left: Cut Index Pill (#01)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(9999.dp))
                        .background(AppleHudDark)
                        .border(1.dp, AppleHairlineDark, RoundedCornerShape(9999.dp))
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = String.format("#%02d", scene.index + 1),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = AppleWhite
                    )
                }

                // Top-right: Selected Checkmark badge (only when selected, never colliding)
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(AppleSkyBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = Color.Black,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                // Bottom-right: Duration pill
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(9999.dp))
                        .background(AppleHudDark)
                        .border(1.dp, AppleHairlineDark, RoundedCornerShape(9999.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = MediaTime.formatDuration(scene.durationMs),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = AppleWhite
                    )
                }

                // Center: Subtle tap-to-preview trigger (40dp, never covers corner badges)
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(AppleHudDark)
                        .border(1.dp, AppleHairlineDark, CircleShape)
                        .clickable { onPreview() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Preview Cut",
                        tint = AppleWhite,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Scrubbing HUD overlay during horizontal drag
                if (isScrubbing) {
                    val scrubMs = (scene.startMs + (scene.durationMs * scrubProgress).toLong())
                    val deltaSec = (scene.durationMs * scrubProgress) / 1000.0

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.85f))
                    ) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(horizontal = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = String.format("+%.2fs", deltaSec),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = AppleSkyBlue
                            )
                            Text(
                                text = MediaTime.formatDuration(scrubMs),
                                style = MaterialTheme.typography.labelSmall,
                                color = AppleWhite
                            )
                        }

                        LinearProgressIndicator(
                            progress = { scrubProgress },
                            color = AppleSkyBlue,
                            trackColor = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .height(3.dp)
                        )
                    }
                }
            }

            // 2. Dedicated Info & Action Area Below Thumbnail (ZERO collision, ZERO text cutting)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Top Row: Cut Name + Favorite Star
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Cut #${scene.index + 1}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = AppleWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(36.dp)
                    ) {
                        if (scene.isFavorite) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Unstar",
                                tint = AppleGold,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.StarBorder,
                                contentDescription = "Star",
                                tint = AppleGrayDark,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Bottom Row: Timecode + Dynamic/Static Badge (Clear separation, never truncates)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = MediaTime.formatTime(scene.startMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = AppleGrayDark,
                        maxLines = 1
                    )

                    if (scene.isDeadframe) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(9999.dp))
                                .background(AppleRed.copy(alpha = 0.2f))
                                .border(1.dp, AppleRed.copy(alpha = 0.4f), RoundedCornerShape(9999.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "STATIC",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                color = AppleRed
                            )
                        }
                    } else if (scene.tags.any { it.contains("action", ignoreCase = true) } || scene.motionScore > 0.45f) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(9999.dp))
                                .background(AppleGreen.copy(alpha = 0.2f))
                                .border(1.dp, AppleGreen.copy(alpha = 0.4f), RoundedCornerShape(9999.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "DYNAMIC",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                color = AppleGreen
                            )
                        }
                    }
                }
            }
        }
    }
}
