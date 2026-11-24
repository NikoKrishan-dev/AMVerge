package com.amverge.android.frontend.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amverge.android.frontend.theme.AppleGrayDark
import com.amverge.android.frontend.theme.AppleHairlineDark
import com.amverge.android.frontend.theme.AppleTile1
import com.amverge.android.frontend.theme.AppleTile3
import com.amverge.android.frontend.theme.AppleWhite

@Composable
fun SegmentedCapsule(
    items: List<String>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 48.dp,
    containerColor: Color = AppleTile1,
    selectedColor: Color = AppleTile3,
    activeTextColor: Color = AppleWhite,
    inactiveTextColor: Color = AppleGrayDark
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(9999.dp))
            .background(containerColor)
            .border(1.dp, AppleHairlineDark, RoundedCornerShape(9999.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEachIndexed { index, title ->
            val isSelected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(9999.dp))
                    .background(if (isSelected) selectedColor else Color.Transparent)
                    .then(
                        if (isSelected) Modifier.border(1.dp, AppleHairlineDark, RoundedCornerShape(9999.dp))
                        else Modifier
                    )
                    .clickable { onItemSelected(index) }
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium.copy(
                        letterSpacing = (-0.14).sp
                    ),
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) activeTextColor else inactiveTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
