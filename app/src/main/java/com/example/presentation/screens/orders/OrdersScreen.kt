package com.example.presentation.screens.orders

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.Order
import com.example.domain.model.OrderDetail
import com.example.presentation.components.BadgeStatus
import com.example.presentation.components.ElajxButton
import com.example.presentation.components.ElajxEmptyState
import com.example.presentation.components.ElajxErrorState
import com.example.presentation.components.ElajxLoadingState
import com.example.presentation.components.ElajxStatusBadge
import com.example.presentation.theme.Charcoal100
import com.example.presentation.theme.Charcoal600
import com.example.presentation.theme.ElajxSpacing
import com.example.presentation.theme.Emerald800

/**
 * Order History and Active Order Tracking Screen.
 *
 * Implements Phase 5 specification (01_PRODUCT_SPEC.md §7, 02_UX_UI_SPEC.md §3, 14_STATE_AND_EDGE_CASES.md §10):
 * - Displays active order with authoritative 7-stage status progression timeline.
 * - Displays patient past order history.
 * - Truthful empty state when 0 orders exist (never fabricates data).
 * - Authentication gate when unauthenticated.
 * - Order detail dialog showing itemized snapshots and status history.
 */
@Composable
fun OrdersScreen(
    viewModel: OrdersViewModel,
    onNavigateToHome: () -> Unit,
    onRequireAuth: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val detailState by viewModel.detailState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(ElajxSpacing.space4)
            .testTag("orders_screen"),
        verticalArrangement = Arrangement.spacedBy(ElajxSpacing.space3)
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
                    text = "Track your medicine requests",
                    style = MaterialTheme.typography.bodySmall,
                    color = Charcoal600
                )
            }
            IconButton(
                onClick = { viewModel.loadOrders() },
                modifier = Modifier.testTag("btn_refresh_orders")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh orders",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Body State
        when (val state = uiState) {
            is OrdersUiState.Unauthenticated -> {
                UnauthenticatedOrdersCard(onRequireAuth = onRequireAuth)
            }
            is OrdersUiState.Loading -> {
                ElajxLoadingState(
                    message = stringResource(R.string.loading_label),
                    modifier = Modifier.weight(1f)
                )
            }
            is OrdersUiState.Empty -> {
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
            is OrdersUiState.Error -> {
                ElajxErrorState(
                    title = stringResource(R.string.error_state_title),
                    description = state.message,
                    onRetry = { viewModel.loadOrders() },
                    modifier = Modifier.weight(1f),
                    testTag = "orders_error_state"
                )
            }
            is OrdersUiState.Success -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("orders_list"),
                    verticalArrangement = Arrangement.spacedBy(ElajxSpacing.space3)
                ) {
                    // Active Orders Section
                    if (state.activeOrders.isNotEmpty()) {
                        item {
                            Text(
                                text = "Active Orders (${state.activeOrders.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        items(state.activeOrders, key = { it.id }) { order ->
                            ActiveOrderCard(
                                order = order,
                                onViewDetails = { viewModel.openOrderDetail(order.id) }
                            )
                        }
                    }

                    // Past Orders Section
                    if (state.pastOrders.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(ElajxSpacing.space2))
                            Text(
                                text = "Past Orders (${state.pastOrders.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        items(state.pastOrders, key = { it.id }) { order ->
                            PastOrderCard(
                                order = order,
                                onViewDetails = { viewModel.openOrderDetail(order.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Order Detail Dialog
    when (val detail = detailState) {
        is OrderDetailUiState.Idle -> Unit
        is OrderDetailUiState.Loading -> {
            AlertDialog(
                onDismissRequest = { viewModel.closeOrderDetail() },
                confirmButton = {},
                text = {
                    ElajxLoadingState(message = "Loading order details...")
                }
            )
        }
        is OrderDetailUiState.Error -> {
            AlertDialog(
                onDismissRequest = { viewModel.closeOrderDetail() },
                title = { Text("Error") },
                text = { Text(detail.message) },
                confirmButton = {
                    TextButton(onClick = { viewModel.closeOrderDetail() }) {
                        Text("Close")
                    }
                }
            )
        }
        is OrderDetailUiState.Success -> {
            OrderDetailDialog(
                detail = detail.detail,
                onDismiss = { viewModel.closeOrderDetail() }
            )
        }
    }
}

@Composable
private fun UnauthenticatedOrdersCard(onRequireAuth: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("orders_unauthenticated_card"),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(ElajxSpacing.space5),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ElajxSpacing.space3)
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = stringResource(R.string.checkout_auth_required_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.checkout_auth_required_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = Charcoal600
            )
            ElajxButton(
                text = stringResource(R.string.auth_sign_in),
                onClick = onRequireAuth,
                modifier = Modifier.fillMaxWidth(),
                testTag = "btn_orders_login"
            )
        }
    }
}

@Composable
private fun ActiveOrderCard(
    order: Order,
    onViewDetails: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("active_order_card_${order.publicOrderNumber}"),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(ElajxSpacing.space4),
            verticalArrangement = Arrangement.spacedBy(ElajxSpacing.space3)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = order.publicOrderNumber,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = order.createdAt.take(10),
                        style = MaterialTheme.typography.bodySmall,
                        color = Charcoal600
                    )
                }
                ElajxStatusBadge(
                    status = mapOrderStatusToBadge(order.status),
                    label = formatOrderStatusLabel(order.status),
                    testTag = "badge_active_order_${order.status}"
                )
            }

            HorizontalDivider()

            // Canonical 7-stage Status Timeline
            OrderStatusTimeline(currentStatus = order.status)

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Total (COD):",
                        style = MaterialTheme.typography.bodySmall,
                        color = Charcoal600
                    )
                    Text(
                        text = "${String.format("%.2f", order.total)} ${stringResource(R.string.currency_egp)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Emerald800
                    )
                }
                ElajxButton(
                    text = "View Details",
                    onClick = onViewDetails,
                    testTag = "btn_view_details_${order.publicOrderNumber}"
                )
            }
        }
    }
}

@Composable
private fun PastOrderCard(
    order: Order,
    onViewDetails: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewDetails() }
            .testTag("past_order_card_${order.publicOrderNumber}"),
        shape = MaterialTheme.shapes.small,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ElajxSpacing.space3),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = order.publicOrderNumber,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${order.createdAt.take(10)} · ${String.format("%.2f", order.total)} ${stringResource(R.string.currency_egp)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Charcoal600
                )
            }
            ElajxStatusBadge(
                status = mapOrderStatusToBadge(order.status),
                label = formatOrderStatusLabel(order.status),
                testTag = "badge_past_order_${order.status}"
            )
        }
    }
}

@Composable
private fun OrderStatusTimeline(currentStatus: String) {
    val steps = listOf(
        "PLACED" to "Placed",
        "CONFIRMED" to "Confirmed",
        "PREPARING" to "Preparing",
        "READY_FOR_PICKUP" to "Ready",
        "PICKED_UP" to "Picked Up",
        "OUT_FOR_DELIVERY" to "Delivering",
        "DELIVERED" to "Delivered"
    )

    val currentNormalized = currentStatus.uppercase()
    val currentIndex = steps.indexOfFirst { it.first == currentNormalized }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(ElajxSpacing.space2)
    ) {
        Text(
            text = "Status Timeline",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            steps.forEachIndexed { index, (stepKey, label) ->
                val isCompleted = currentIndex >= 0 && index < currentIndex
                val isCurrent = currentIndex >= 0 && index == currentIndex

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isCurrent -> MaterialTheme.colorScheme.primary
                                    isCompleted -> Emerald800
                                    else -> Charcoal100
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCompleted) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        } else if (isCurrent) {
                            Icon(
                                imageVector = Icons.Default.LocalShipping,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        } else {
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Charcoal600
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                        color = if (isCurrent) MaterialTheme.colorScheme.primary else Charcoal600,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun OrderDetailDialog(
    detail: OrderDetail,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = detail.order.publicOrderNumber,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(ElajxSpacing.space3)
            ) {
                // Status Badge & Date
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Date: ${detail.order.createdAt.take(16).replace("T", " ")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Charcoal600
                        )
                        ElajxStatusBadge(
                            status = mapOrderStatusToBadge(detail.order.status),
                            label = formatOrderStatusLabel(detail.order.status)
                        )
                    }
                }

                // Ordered Line Items
                item {
                    HorizontalDivider()
                    Text(
                        text = "Ordered Items (${detail.items.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(detail.items) { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${item.medicineNameSnapshot} × ${item.quantity}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${String.format("%.2f", item.lineTotal)} EGP",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Price Breakdown
                item {
                    HorizontalDivider()
                    Column(verticalArrangement = Arrangement.spacedBy(ElajxSpacing.space1)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Subtotal", style = MaterialTheme.typography.bodySmall, color = Charcoal600)
                            Text("${String.format("%.2f", detail.order.subtotal)} EGP", style = MaterialTheme.typography.bodySmall)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Delivery Fee (Authoritative)", style = MaterialTheme.typography.bodySmall, color = Charcoal600)
                            Text("${String.format("%.2f", detail.order.deliveryFee)} EGP", style = MaterialTheme.typography.bodySmall)
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = ElajxSpacing.space1))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Grand Total (Due on Delivery)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(
                                "${String.format("%.2f", detail.order.total)} EGP",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Emerald800
                            )
                        }
                    }
                }

                // Status Transition History
                if (detail.statusHistory.isNotEmpty()) {
                    item {
                        HorizontalDivider()
                        Text(
                            text = "Status History",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    items(detail.statusHistory) { history ->
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = formatOrderStatusLabel(history.toStatus),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = history.createdAt.take(16).replace("T", " "),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Charcoal600
                                )
                            }
                            if (!history.note.isNullOrBlank()) {
                                Text(
                                    text = history.note,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Charcoal600
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

private fun mapOrderStatusToBadge(status: String): BadgeStatus {
    return when (status.uppercase()) {
        "DELIVERED" -> BadgeStatus.SUCCESS
        "PLACED", "CONFIRMED", "PREPARING", "READY_FOR_PICKUP", "PICKED_UP" -> BadgeStatus.INFO
        "OUT_FOR_DELIVERY" -> BadgeStatus.WARNING
        "CANCELLED", "FAILED" -> BadgeStatus.ERROR
        else -> BadgeStatus.NEUTRAL
    }
}

private fun formatOrderStatusLabel(status: String): String {
    return when (status.uppercase()) {
        "PLACED" -> "Placed"
        "CONFIRMED" -> "Confirmed"
        "PREPARING" -> "Preparing"
        "READY_FOR_PICKUP" -> "Ready for Pickup"
        "PICKED_UP" -> "Picked Up"
        "OUT_FOR_DELIVERY" -> "Out for Delivery"
        "DELIVERED" -> "Delivered"
        "CANCELLED" -> "Cancelled"
        "FAILED" -> "Failed"
        else -> status
    }
}
