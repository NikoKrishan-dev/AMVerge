package com.amverge.android.frontend.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMerge
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amverge.android.frontend.theme.AppleGold
import com.amverge.android.frontend.theme.AppleHairlineDark
import com.amverge.android.frontend.theme.AppleIndigo
import com.amverge.android.frontend.theme.AppleRed
import com.amverge.android.frontend.theme.AppleSkyBlue
import com.amverge.android.frontend.theme.AppleTile2
import com.amverge.android.frontend.theme.AppleTile3
import com.amverge.android.frontend.theme.AppleWhite

@Composable
fun SceneSelectionBar(
    selectedCount: Int,
    onClearSelection: () -> Unit,
    onExportClicked: () -> Unit,
    canMerge: Boolean = false,
    onMergeClicked: (() -> Unit)? = null,
    onDeleteClicked: (() -> Unit)? = null,
    onStarToggleClicked: (() -> Unit)? = null,
    onAddToScenepackClicked: (() -> Unit)? = null,
    onAiToolsClicked: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(9999.dp))
            .background(Color(0xF51C1C1E))
            .border(1.dp, AppleHairlineDark, RoundedCornerShape(9999.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Dismiss and Counter (Protected fixed area, never overlapped!)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(AppleTile3)
                        .clickable { onClearSelection() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear Selection",
                        tint = AppleWhite,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Text(
                    text = "$selectedCount cuts",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = AppleWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Right: Actions Row (weight constrained so it NEVER collides with left text)
            Row(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (canMerge && onMergeClicked != null) {
                    DockIconButton(
                        icon = Icons.AutoMirrored.Filled.CallMerge,
                        contentDescription = "Merge Cuts",
                        tint = AppleWhite,
                        onClick = onMergeClicked
                    )
                }

                if (onAddToScenepackClicked != null) {
                    DockIconButton(
                        icon = Icons.Default.CollectionsBookmark,
                        contentDescription = "Add to Scenepack",
                        tint = AppleWhite,
                        onClick = onAddToScenepackClicked
                    )
                }

                if (onStarToggleClicked != null) {
                    DockIconButton(
                        icon = Icons.Default.Star,
                        contentDescription = "Toggle Star",
                        tint = AppleGold,
                        onClick = onStarToggleClicked
                    )
                }

                if (onDeleteClicked != null) {
                    DockIconButton(
                        icon = Icons.Default.Delete,
                        contentDescription = "Delete Cuts",
                        tint = AppleRed,
                        backgroundColor = AppleRed.copy(alpha = 0.2f),
                        borderColor = AppleRed.copy(alpha = 0.4f),
                        onClick = onDeleteClicked
                    )
                }

                if (selectedCount == 1 && onAiToolsClicked != null) {
                    Button(
                        onClick = onAiToolsClicked,
                        shape = RoundedCornerShape(9999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppleIndigo,
                            contentColor = AppleWhite
                        ),
                        modifier = Modifier
                            .defaultMinSize(minHeight = 44.dp)
                            .height(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = AppleWhite
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "AI Studio",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Primary Export Pill CTA
                Button(
                    onClick = onExportClicked,
                    shape = RoundedCornerShape(9999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppleSkyBlue,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier
                        .defaultMinSize(minHeight = 44.dp)
                        .height(44.dp)
                ) {
                    Text(
                        "Export",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }
        }
    }
}

@Composable
private fun DockIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    tint: Color,
    backgroundColor: Color = AppleTile2,
    borderColor: Color = AppleHairlineDark,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .border(1.dp, borderColor, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
    }
}
