package com.amverge.android.frontend.components

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.amverge.android.backend.domain.model.Scenepack
import com.amverge.android.frontend.theme.AppleGrayDark
import com.amverge.android.frontend.theme.AppleHairlineDark
import com.amverge.android.frontend.theme.AppleSkyBlue
import com.amverge.android.frontend.theme.AppleTile1
import com.amverge.android.frontend.theme.AppleTile2
import com.amverge.android.frontend.theme.AppleTile3
import com.amverge.android.frontend.theme.AppleWhite

@Composable
fun ScenepackDialog(
    selectedSceneCount: Int,
    availableScenepacks: List<Scenepack>,
    onCreateNewScenepack: (name: String, description: String?) -> Unit,
    onAddToExistingScenepack: (scenepackId: String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(if (availableScenepacks.isEmpty()) 0 else 1) }
    var newPackName by remember { mutableStateOf("") }
    var newPackDescription by remember { mutableStateOf("") }
    var selectedExistingPackId by remember {
        mutableStateOf(availableScenepacks.firstOrNull()?.id)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppleTile1,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                "Add $selectedSceneCount Cut(s) to Scenepack",
                color = AppleWhite,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.28).sp
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (availableScenepacks.isNotEmpty()) {
                    SegmentedCapsule(
                        items = listOf("New Pack", "Existing Pack"),
                        selectedIndex = selectedTab,
                        onItemSelected = { selectedTab = it },
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = AppleTile2,
                        height = 44.dp
                    )
                }

                if (selectedTab == 0 || availableScenepacks.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = newPackName,
                            onValueChange = { newPackName = it },
                            label = { Text("Scenepack Name", color = AppleGrayDark) },
                            placeholder = { Text("e.g. Gojo Hollow Purple Cut", color = AppleGrayDark.copy(alpha = 0.5f)) },
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
                            value = newPackDescription,
                            onValueChange = { newPackDescription = it },
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
                } else {
                    Text("Select a Scenepack:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = AppleWhite)
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(availableScenepacks, key = { it.id }) { pack ->
                            val isSelected = selectedExistingPackId == pack.id
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 52.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) AppleTile3 else AppleTile2)
                                    .border(
                                        1.dp,
                                        if (isSelected) AppleSkyBlue else AppleHairlineDark,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { selectedExistingPackId = pack.id }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { selectedExistingPackId = pack.id },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = AppleSkyBlue,
                                            unselectedColor = AppleGrayDark
                                        )
                                    )
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(start = 8.dp)
                                    ) {
                                        Text(
                                            text = pack.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = AppleWhite,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${pack.sceneCount} cut(s) in pack",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = AppleSkyBlue,
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
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedTab == 0 || availableScenepacks.isEmpty()) {
                        if (newPackName.isNotBlank()) {
                            onCreateNewScenepack(newPackName.trim(), newPackDescription.trim().ifEmpty { null })
                        }
                    } else {
                        selectedExistingPackId?.let { id ->
                            onAddToExistingScenepack(id)
                        }
                    }
                },
                shape = RoundedCornerShape(9999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppleSkyBlue,
                    contentColor = Color.Black
                ),
                modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                enabled = (selectedTab == 0 && newPackName.isNotBlank()) ||
                        (selectedTab == 1 && selectedExistingPackId != null)
            ) {
                Text("Save to Pack", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = AppleGrayDark),
                modifier = Modifier.defaultMinSize(minHeight = 48.dp)
            ) {
                Text("Cancel")
            }
        }
    )
}
