package com.example.presentation.screens.cart

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.CartItem
import com.example.presentation.components.BadgeStatus
import com.example.presentation.components.ButtonVariant
import com.example.presentation.components.ElajxButton
import com.example.presentation.components.ElajxEmptyState
import com.example.presentation.components.ElajxStatusBadge
import com.example.presentation.theme.Charcoal100
import com.example.presentation.theme.Charcoal200
import com.example.presentation.theme.Charcoal600
import com.example.presentation.theme.Charcoal900
import com.example.presentation.theme.ElajxSpacing
import com.example.presentation.theme.Emerald100
import com.example.presentation.theme.Emerald800
import com.example.presentation.theme.Emerald900
import com.example.presentation.theme.Red600

@Composable
fun CartScreen(
    viewModel: CartViewModel,
    isAuthenticated: Boolean,
    onNavigateToCheckout: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onRequireAuth: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cart by viewModel.cartState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(ElajxSpacing.space4)
            .testTag("cart_screen"),
        verticalArrangement = Arrangement.spacedBy(ElajxSpacing.space3)
    ) {
        // Title Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.cart_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (!cart.isEmpty) {
                    Text(
                        text = "${cart.totalItemsCount} items",
                        style = MaterialTheme.typography.bodySmall,
                        color = Charcoal600
                    )
                }
            }

            if (!cart.isEmpty && !cart.selectedPharmacyDisplayCode.isNullOrBlank()) {
                ElajxStatusBadge(
                    status = BadgeStatus.SUCCESS,
                    label = cart.selectedPharmacyDisplayCode ?: "",
                    testTag = "cart_pharmacy_badge"
                )
            }
        }

        if (cart.isEmpty) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                ElajxEmptyState(
                    title = stringResource(R.string.cart_empty_title),
                    description = stringResource(R.string.cart_empty_desc),
                    icon = Icons.Default.ShoppingCart,
                    actionText = stringResource(R.string.home_action_search),
                    onActionClick = onNavigateToSearch,
                    testTag = "cart_empty_view"
                )
            }
        } else {
            // Fulfilling Pharmacy context
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Emerald100)
            ) {
                Row(
                    modifier = Modifier.padding(ElajxSpacing.space3),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(ElajxSpacing.space2)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Emerald800,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "${stringResource(R.string.cart_pharmacy_fulfillment)} ${cart.selectedPharmacyDisplayCode}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Emerald900
                        )
                        Text(
                            text = cart.selectedPharmacyArea.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = Emerald800
                        )
                    }
                }
            }

            // Items List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("cart_items_list"),
                verticalArrangement = Arrangement.spacedBy(ElajxSpacing.space2)
            ) {
                items(cart.items, key = { it.variant.id }) { item ->
                    CartItemCard(
                        item = item,
                        onIncrease = { viewModel.updateQuantity(item.variant.id, item.quantity + 1) },
                        onDecrease = { viewModel.updateQuantity(item.variant.id, item.quantity - 1) },
                        onRemove = { viewModel.removeItem(item.variant.id) }
                    )
                }
            }

            // Subtotal and Server-Authoritative Fee Notice Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(ElajxSpacing.space3)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.cart_subtotal_label),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Charcoal600
                        )
                        Text(
                            text = String.format("%.2f %s", cart.subtotal, stringResource(R.string.currency_egp)),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Emerald800
                        )
                    }

                    Spacer(modifier = Modifier.height(ElajxSpacing.space1))

                    Text(
                        text = stringResource(R.string.cart_delivery_fee_notice),
                        style = MaterialTheme.typography.bodySmall,
                        color = Charcoal600
                    )

                    Spacer(modifier = Modifier.height(ElajxSpacing.space3))

                    if (isAuthenticated) {
                        ElajxButton(
                            text = stringResource(R.string.cart_checkout_button),
                            onClick = onNavigateToCheckout,
                            variant = ButtonVariant.PRIMARY,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_proceed_to_checkout")
                        )
                    } else {
                        ElajxButton(
                            text = stringResource(R.string.auth_sign_in),
                            onClick = onRequireAuth,
                            variant = ButtonVariant.PRIMARY,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_cart_sign_in_required")
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CartItemCard(
    item: CartItem,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("cart_item_${item.variant.id}"),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(ElajxSpacing.space3)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.medicine.nameAr,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${item.variant.brandName} • ${item.medicine.strength}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Charcoal600
                    )
                }

                IconButton(
                    onClick = onRemove,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("btn_remove_${item.variant.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = stringResource(R.string.cart_remove_item),
                        tint = Red600
                    )
                }
            }

            Spacer(modifier = Modifier.height(ElajxSpacing.space2))
            HorizontalDivider(color = Charcoal100)
            Spacer(modifier = Modifier.height(ElajxSpacing.space2))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Unit Price & Line Total
                Column {
                    Text(
                        text = "${item.inventory.price} ${stringResource(R.string.currency_egp)} / unit",
                        style = MaterialTheme.typography.bodySmall,
                        color = Charcoal600
                    )
                    Text(
                        text = String.format("%.2f %s", item.lineTotal, stringResource(R.string.currency_egp)),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Emerald800
                    )
                }

                // Quantity Controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(ElajxSpacing.space1)
                ) {
                    OutlinedIconButton(
                        onClick = onDecrease,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("btn_decrease_${item.variant.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Decrease quantity",
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Text(
                        text = "${item.quantity}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Charcoal900,
                        modifier = Modifier.padding(horizontal = ElajxSpacing.space2)
                    )

                    OutlinedIconButton(
                        onClick = onIncrease,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("btn_increase_${item.variant.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Increase quantity",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
