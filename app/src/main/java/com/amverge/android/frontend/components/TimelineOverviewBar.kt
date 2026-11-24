package com.amverge.android.frontend.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.amverge.android.backend.domain.model.Scene
import com.amverge.android.core.common.MediaTime
import com.amverge.android.frontend.theme.AppleGold
import com.amverge.android.frontend.theme.AppleHairlineDark
import com.amverge.android.frontend.theme.AppleRed
import com.amverge.android.frontend.theme.AppleSkyBlue
import com.amverge.android.frontend.theme.AppleTile1

@Composable
fun TimelineOverviewBar(
    scenes: List<Scene>,
    totalDurationMs: Long,
    selectedSceneIds: Set<String>,
    onSceneSelected: (Scene) -> Unit,
    modifier: Modifier = Modifier
) {
    if (scenes.isEmpty() || totalDurationMs <= 0L) return

    val primaryColor = AppleSkyBlue
    var hoveredScene by remember { mutableStateOf<Scene?>(null) }

    fun findSceneAtX(x: Float, width: Float) {
        if (width <= 0f) return
        val fraction = (x / width).coerceIn(0f, 1f)
        val targetMs = (fraction * totalDurationMs).toLong()
        val found = scenes.find { targetMs in it.startMs..it.endMs } ?: scenes.lastOrNull()
        if (found != null) {
            hoveredScene = found
            onSceneSelected(found)
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(14.dp),
        color = AppleTile1,
        border = BorderStroke(1.dp, AppleHairlineDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Timeline Overview (${scenes.size} cuts)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Text(
                    text = hoveredScene?.let { "Cut #${it.index + 1} (${MediaTime.formatTime(it.startMs)})" }
                        ?: "Total: ${MediaTime.formatDuration(totalDurationMs)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppleSkyBlue,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF141416))
            ) {
                Canvas(
                    modifier = Modifier
                        .matchParentSize()
                        .pointerInput(scenes, totalDurationMs) {
                            detectTapGestures(
                                onPress = { offset ->
                                    findSceneAtX(offset.x, size.width.toFloat())
                                },
                                onTap = { offset ->
                                    findSceneAtX(offset.x, size.width.toFloat())
                                }
                            )
                        }
                        .pointerInput(scenes, totalDurationMs) {
                            detectHorizontalDragGestures(
                                onDragStart = { offset ->
                                    findSceneAtX(offset.x, size.width.toFloat())
                                },
                                onHorizontalDrag = { change, _ ->
                                    findSceneAtX(change.position.x, size.width.toFloat())
                                }
                            )
                        }
                ) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height

                    for (scene in scenes) {
                        val startX = (scene.startMs.toFloat() / totalDurationMs) * canvasWidth
                        val endX = (scene.endMs.toFloat() / totalDurationMs) * canvasWidth
                        val sliceWidth = (endX - startX).coerceAtLeast(1f)

                        val sliceColor = when {
                            selectedSceneIds.contains(scene.id) -> primaryColor
                            scene.isFavorite -> AppleGold
                            scene.isDeadframe -> AppleRed.copy(alpha = 0.5f)
                            scene.index % 2 == 0 -> Color(0xFF242426)
                            else -> Color(0xFF2C2C2E)
                        }

                        drawRect(
                            color = sliceColor,
                            topLeft = Offset(startX, 0f),
                            size = Size(sliceWidth, canvasHeight)
                        )

                        // Draw divider line
                        drawLine(
                            color = Color.Black.copy(alpha = 0.6f),
                            start = Offset(startX, 0f),
                            end = Offset(startX, canvasHeight),
                            strokeWidth = 1f
                        )
                    }
                }
            }
        }
    }
}
