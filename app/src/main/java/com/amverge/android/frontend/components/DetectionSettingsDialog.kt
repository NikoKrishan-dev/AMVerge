package com.amverge.android.frontend.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amverge.android.backend.domain.model.DetectionPreferences
import com.amverge.android.frontend.theme.AppleGrayDark
import com.amverge.android.frontend.theme.AppleGreen
import com.amverge.android.frontend.theme.AppleHairlineDark
import com.amverge.android.frontend.theme.AppleSkyBlue
import com.amverge.android.frontend.theme.AppleTile1
import com.amverge.android.frontend.theme.AppleTile2
import com.amverge.android.frontend.theme.AppleTile3
import com.amverge.android.frontend.theme.AppleWhite
import kotlin.math.roundToLong

@Composable
fun DetectionSettingsDialog(
    initialPreferences: DetectionPreferences,
    onSavePreferences: (DetectionPreferences) -> Unit,
    onDismiss: () -> Unit
) {
    var minDurationMs by remember { mutableLongStateOf(initialPreferences.minSceneDurationMs) }
    var filterDeadframes by remember { mutableStateOf(initialPreferences.filterDeadframes) }
    var deadframeThreshold by remember { mutableFloatStateOf(initialPreferences.deadframeThreshold) }
    var detectTransitions by remember { mutableStateOf(initialPreferences.detectTransitions) }
    var sensitivity by remember { mutableFloatStateOf(initialPreferences.sensitivity) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppleTile1,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                "Detection & Deadframe Settings",
                fontWeight = FontWeight.Bold,
                color = AppleWhite,
                letterSpacing = (-0.28).sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Min scene duration
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Min Scene Duration", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = AppleWhite)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(9999.dp))
                                .background(AppleTile3)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("${minDurationMs}ms", style = MaterialTheme.typography.labelSmall, color = AppleSkyBlue, fontWeight = FontWeight.Bold)
                        }
                    }
                    Slider(
                        value = minDurationMs.toFloat(),
                        onValueChange = { minDurationMs = ((it / 50).roundToLong() * 50).coerceIn(100L, 2000L) },
                        valueRange = 100f..2000f,
                        steps = 37,
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            thumbColor = AppleSkyBlue,
                            activeTrackColor = AppleSkyBlue,
                            inactiveTrackColor = AppleTile3
                        )
                    )
                    Text(
                        text = "Intervals shorter than this will be merged into neighboring cuts.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppleGrayDark
                    )
                }

                // Deadframe filter toggle
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(AppleTile2)
                        .border(1.dp, AppleHairlineDark, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Deadframe Filter", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = AppleWhite)
                            Text(
                                "Automatically flag motionless talking-head scenes",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppleGrayDark
                            )
                        }
                        Switch(
                            checked = filterDeadframes,
                            onCheckedChange = { filterDeadframes = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = AppleGreen,
                                uncheckedThumbColor = AppleGrayDark,
                                uncheckedTrackColor = AppleTile3
                            )
                        )
                    }
                }

                // Motion variance sensitivity threshold
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Motion Variance Threshold", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = AppleWhite)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(9999.dp))
                                .background(AppleTile3)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("${(deadframeThreshold * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = AppleSkyBlue, fontWeight = FontWeight.Bold)
                        }
                    }
                    Slider(
                        value = deadframeThreshold,
                        onValueChange = { deadframeThreshold = ((it * 100).toInt() / 100f).coerceIn(0.05f, 0.40f) },
                        valueRange = 0.05f..0.40f,
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            thumbColor = AppleSkyBlue,
                            activeTrackColor = AppleSkyBlue,
                            inactiveTrackColor = AppleTile3
                        )
                    )
                    Text(
                        text = "Cuts with visual change below this threshold are flagged as static.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppleGrayDark
                    )
                }

                // Transition detection toggle
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(AppleTile2)
                        .border(1.dp, AppleHairlineDark, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Detect Fades & Dissolves", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = AppleWhite)
                            Text(
                                "Detect gradual transitions such as dips to black/white",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppleGrayDark
                            )
                        }
                        Switch(
                            checked = detectTransitions,
                            onCheckedChange = { detectTransitions = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = AppleGreen,
                                uncheckedThumbColor = AppleGrayDark,
                                uncheckedTrackColor = AppleTile3
                            )
                        )
                    }
                }

                // Reset to defaults
                OutlinedButton(
                    onClick = {
                        val defaultPrefs = DetectionPreferences()
                        minDurationMs = defaultPrefs.minSceneDurationMs
                        filterDeadframes = defaultPrefs.filterDeadframes
                        deadframeThreshold = defaultPrefs.deadframeThreshold
                        detectTransitions = defaultPrefs.detectTransitions
                        sensitivity = defaultPrefs.sensitivity
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(9999.dp),
                    border = BorderStroke(1.dp, AppleHairlineDark),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AppleGrayDark)
                ) {
                    Text("Reset to Recommended Defaults", style = MaterialTheme.typography.labelMedium)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val updated = DetectionPreferences(
                        minSceneDurationMs = minDurationMs,
                        filterDeadframes = filterDeadframes,
                        deadframeThreshold = deadframeThreshold,
                        detectTransitions = detectTransitions,
                        sensitivity = sensitivity
                    )
                    onSavePreferences(updated)
                    onDismiss()
                },
                shape = RoundedCornerShape(9999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppleSkyBlue,
                    contentColor = Color.Black
                ),
                modifier = Modifier.height(48.dp)
            ) {
                Text("Save Settings", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = AppleGrayDark),
                modifier = Modifier.height(48.dp)
            ) {
                Text("Cancel", style = MaterialTheme.typography.labelLarge)
            }
        }
    )
}
