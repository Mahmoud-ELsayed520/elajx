package com.example.presentation.screens.orders

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.presentation.components.BadgeStatus
import com.example.presentation.components.ElajxEmptyState
import com.example.presentation.components.ElajxStatusBadge
import com.example.presentation.theme.Charcoal600
import com.example.presentation.theme.ElajxSpacing

/**
 * Order History Screen.
 *
 * Mapped to 02_UX_UI_SPEC.md (Section 3 & 9):
 * - Empty state: "لا توجد طلبات حتى الآن" / "No orders yet".
 * - Shows truthful unbuilt state; order checkout and state machine connect in Phase 4 & 5.
 */
@Composable
fun OrdersScreen(
    onNavigateToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(ElajxSpacing.space4)
            .testTag("orders_screen"),
        verticalArrangement = Arrangement.spacedBy(ElajxSpacing.space4)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.orders_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Server-Authoritative Orders (Phase 5)",
                    style = MaterialTheme.typography.bodySmall,
                    color = Charcoal600
                )
            }
            ElajxStatusBadge(
                status = BadgeStatus.INFO,
                label = "Phase 5 Lifecycle",
                testTag = "orders_badge"
            )
        }

        // Empty state per spec
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            ElajxEmptyState(
                title = stringResource(R.string.orders_empty_title),
                description = stringResource(R.string.orders_empty_desc),
                icon = Icons.AutoMirrored.Filled.ReceiptLong,
                actionText = stringResource(R.string.back_to_home),
                onActionClick = onNavigateToHome,
                testTag = "orders_empty_state"
            )
        }
    }
}
