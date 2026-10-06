package com.example.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.presentation.theme.Amber100
import com.example.presentation.theme.Amber600
import com.example.presentation.theme.Blue100
import com.example.presentation.theme.Blue600
import com.example.presentation.theme.Charcoal100
import com.example.presentation.theme.Charcoal700
import com.example.presentation.theme.ElajxSpacing
import com.example.presentation.theme.Emerald100
import com.example.presentation.theme.Emerald800
import com.example.presentation.theme.Red100
import com.example.presentation.theme.Red600

enum class BadgeStatus {
    SUCCESS,
    WARNING,
    ERROR,
    INFO,
    NEUTRAL
}

/**
 * Status badge that complies with accessibility standards:
 * Never communicates status using color alone (always pairs visual icon + semantic text).
 */
@Composable
fun ElajxStatusBadge(
    status: BadgeStatus,
    label: String,
    modifier: Modifier = Modifier,
    testTag: String = "status_badge"
) {
    val (bgColor, textColor, icon) = when (status) {
        BadgeStatus.SUCCESS -> Triple(Emerald100, Emerald800, Icons.Default.CheckCircle)
        BadgeStatus.WARNING -> Triple(Amber100, Amber600, Icons.Default.Warning)
        BadgeStatus.ERROR -> Triple(Red100, Red600, Icons.Default.Error)
        BadgeStatus.INFO -> Triple(Blue100, Blue600, Icons.Default.Info)
        BadgeStatus.NEUTRAL -> Triple(Charcoal100, Charcoal700, Icons.Default.Info)
    }

    Row(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(bgColor)
            .padding(horizontal = ElajxSpacing.space3, vertical = ElajxSpacing.space1)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null, // Decorative within the badge because label conveys the text
            tint = textColor,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(ElajxSpacing.space1))
        Text(
            text = label,
            color = textColor,
            style = MaterialTheme.typography.labelSmall
        )
    }
}
