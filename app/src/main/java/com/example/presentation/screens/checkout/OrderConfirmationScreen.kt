package com.example.presentation.screens.checkout

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.Order
import com.example.presentation.components.BadgeStatus
import com.example.presentation.components.ButtonVariant
import com.example.presentation.components.ElajxButton
import com.example.presentation.components.ElajxStatusBadge
import com.example.presentation.theme.Charcoal100
import com.example.presentation.theme.Charcoal600
import com.example.presentation.theme.Charcoal900
import com.example.presentation.theme.ElajxSpacing
import com.example.presentation.theme.Emerald100
import com.example.presentation.theme.Emerald800
import com.example.presentation.theme.Emerald900
import com.example.presentation.theme.PureWhite

/**
 * Phase 4 Order Confirmation Screen.
 *
 * Implements 02_UX_UI_SPEC.md (§42) and 11_DESIGN_SYSTEM.md (§42).
 * Displays authoritative values returned directly by the server create_order RPC.
 */
@Composable
fun OrderConfirmationScreen(
    order: Order,
    onNavigateToOrders: () -> Unit,
    onNavigateToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(ElajxSpacing.space4)
            .testTag("order_confirmation_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ElajxSpacing.space3),
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.height(ElajxSpacing.space3))

            // Success Icon Indicator
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Emerald800),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = PureWhite,
                    modifier = Modifier.size(40.dp)
                )
            }

            Text(
                text = stringResource(R.string.confirmation_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = stringResource(R.string.confirmation_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = Charcoal600
            )

            // Authoritative Order Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("order_confirmation_card"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(ElajxSpacing.space4),
                    verticalArrangement = Arrangement.spacedBy(ElajxSpacing.space2)
                ) {
                    // Order Reference Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.confirmation_order_number),
                                style = MaterialTheme.typography.bodySmall,
                                color = Charcoal600
                            )
                            Text(
                                text = order.publicOrderNumber,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Emerald900,
                                modifier = Modifier.testTag("confirmed_order_number")
                            )
                        }

                        ElajxStatusBadge(
                            status = BadgeStatus.SUCCESS,
                            label = stringResource(R.string.confirmation_status_placed),
                            testTag = "confirmed_order_status"
                        )
                    }

                    HorizontalDivider(color = Charcoal100, modifier = Modifier.padding(vertical = ElajxSpacing.space1))

                    // Authoritative Breakdown
                    OrderSummaryRow(
                        label = stringResource(R.string.confirmation_subtotal),
                        value = String.format("%.2f %s", order.subtotal, stringResource(R.string.currency_egp))
                    )

                    OrderSummaryRow(
                        label = stringResource(R.string.confirmation_delivery_fee),
                        value = String.format("%.2f %s", order.deliveryFee, stringResource(R.string.currency_egp))
                    )

                    if (order.discount > 0.0) {
                        OrderSummaryRow(
                            label = "Discount",
                            value = String.format("-%.2f %s", order.discount, stringResource(R.string.currency_egp))
                        )
                    }

                    HorizontalDivider(color = Charcoal100, modifier = Modifier.padding(vertical = ElajxSpacing.space1))

                    // Grand Total Due
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.confirmation_total),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Charcoal900
                        )
                        Text(
                            text = String.format("%.2f %s", order.total, stringResource(R.string.currency_egp)),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Emerald800,
                            modifier = Modifier.testTag("confirmed_order_total")
                        )
                    }

                    Spacer(modifier = Modifier.height(ElajxSpacing.space1))

                    // Payment Method Badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.confirmation_payment_method),
                            style = MaterialTheme.typography.bodySmall,
                            color = Charcoal600
                        )
                        Text(
                            text = "الدفع نقدًا عند الاستلام (COD)",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = Charcoal900
                        )
                    }
                }
            }
        }

        // Actions Bottom
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(ElajxSpacing.space2)
        ) {
            ElajxButton(
                text = stringResource(R.string.confirmation_cta_orders),
                onClick = onNavigateToOrders,
                variant = ButtonVariant.PRIMARY,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_goto_orders")
            )

            ElajxButton(
                text = stringResource(R.string.confirmation_cta_home),
                onClick = onNavigateToHome,
                variant = ButtonVariant.GHOST,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_goto_home")
            )
        }
    }
}

@Composable
private fun OrderSummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Charcoal600
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = Charcoal900
        )
    }
}
