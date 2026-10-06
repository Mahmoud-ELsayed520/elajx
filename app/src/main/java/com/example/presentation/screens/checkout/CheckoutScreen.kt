package com.example.presentation.screens.checkout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.Address
import com.example.domain.model.Order
import com.example.presentation.components.BadgeStatus
import com.example.presentation.components.ButtonVariant
import com.example.presentation.components.ElajxButton
import com.example.presentation.components.ElajxErrorState
import com.example.presentation.components.ElajxStatusBadge
import com.example.presentation.theme.Charcoal100
import com.example.presentation.theme.Charcoal200
import com.example.presentation.theme.Charcoal600
import com.example.presentation.theme.Charcoal900
import com.example.presentation.theme.ElajxSpacing
import com.example.presentation.theme.Emerald100
import com.example.presentation.theme.Emerald700
import com.example.presentation.theme.Emerald800
import com.example.presentation.theme.Emerald900
import com.example.presentation.theme.PureWhite
import com.example.presentation.theme.Red600

@Composable
fun CheckoutScreen(
    viewModel: CheckoutViewModel,
    onNavigateBack: () -> Unit,
    onOrderConfirmed: (Order) -> Unit,
    onRequireAuth: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    // Trigger navigation when order is successfully created
    LaunchedEffect(uiState.createdOrder) {
        uiState.createdOrder?.let { order ->
            onOrderConfirmed(order)
        }
    }

    // Trigger auth dialog if session expired
    LaunchedEffect(uiState.sessionExpired) {
        if (uiState.sessionExpired) {
            onRequireAuth()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(ElajxSpacing.space4)
            .testTag("checkout_screen"),
        verticalArrangement = Arrangement.spacedBy(ElajxSpacing.space3)
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.testTag("btn_checkout_back")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to cart"
                )
            }
            Text(
                text = stringResource(R.string.checkout_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Error Banner
        uiState.errorMessage?.let { errorMsg ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("checkout_error_banner"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(ElajxSpacing.space3),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = errorMsg,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                    ElajxButton(
                        text = stringResource(R.string.retry_action),
                        onClick = { viewModel.submitOrder() },
                        variant = ButtonVariant.SECONDARY,
                        modifier = Modifier.testTag("btn_retry_order")
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .testTag("checkout_scroll_content"),
            verticalArrangement = Arrangement.spacedBy(ElajxSpacing.space3)
        ) {
            // 1. Delivery Address Section
            item {
                Text(
                    text = stringResource(R.string.checkout_address_section),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(ElajxSpacing.space1))

                if (uiState.isLoadingAddresses) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                } else if (uiState.addresses.isEmpty() && !uiState.isAddingNewAddress) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(ElajxSpacing.space3)) {
                            Text(
                                text = stringResource(R.string.checkout_no_addresses),
                                style = MaterialTheme.typography.bodySmall,
                                color = Charcoal600
                            )
                            Spacer(modifier = Modifier.height(ElajxSpacing.space2))
                            ElajxButton(
                                text = stringResource(R.string.checkout_add_address_button),
                                onClick = { viewModel.toggleNewAddressForm(true) },
                                variant = ButtonVariant.SECONDARY,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_prompt_add_address")
                            )
                        }
                    }
                } else if (!uiState.isAddingNewAddress) {
                    Column(verticalArrangement = Arrangement.spacedBy(ElajxSpacing.space2)) {
                        uiState.addresses.forEach { address ->
                            AddressCard(
                                address = address,
                                isSelected = address.id == uiState.selectedAddressId,
                                onSelect = { viewModel.selectAddress(address.id) }
                            )
                        }
                        ElajxButton(
                            text = stringResource(R.string.checkout_add_address_button),
                            onClick = { viewModel.toggleNewAddressForm(true) },
                            variant = ButtonVariant.GHOST,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_show_add_address_form")
                        )
                    }
                }

                if (uiState.isAddingNewAddress) {
                    NewAddressForm(
                        isSubmitting = uiState.isSubmitting,
                        onCancel = { viewModel.toggleNewAddressForm(false) },
                        onSave = { label, gov, city, area, street, bld, apt, floor, landmark ->
                            viewModel.createNewAddress(label, gov, city, area, street, bld, apt, floor, landmark)
                        }
                    )
                }
            }

            // 2. Fulfilling Partner Pharmacy Context
            item {
                HorizontalDivider(color = Charcoal100)
                Spacer(modifier = Modifier.height(ElajxSpacing.space1))
                Text(
                    text = stringResource(R.string.checkout_pharmacy_section),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(ElajxSpacing.space1))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
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
                        Column {
                            Text(
                                text = uiState.cart.selectedPharmacyDisplayCode ?: "Partner Pharmacy",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = uiState.cart.selectedPharmacyArea ?: "Cairo",
                                style = MaterialTheme.typography.bodySmall,
                                color = Charcoal600
                            )
                        }
                        ElajxStatusBadge(
                            status = BadgeStatus.SUCCESS,
                            label = "Verified Stock"
                        )
                    }
                }
            }

            // 3. Payment Method Section (Strictly COD per Phase 4 Contract)
            item {
                HorizontalDivider(color = Charcoal100)
                Spacer(modifier = Modifier.height(ElajxSpacing.space1))
                Text(
                    text = stringResource(R.string.checkout_payment_section),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(ElajxSpacing.space1))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("payment_method_cod_card"),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Emerald100)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(ElajxSpacing.space3),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(ElajxSpacing.space2)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Payments,
                            contentDescription = null,
                            tint = Emerald800,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.checkout_payment_cod),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Emerald900
                            )
                            Text(
                                text = stringResource(R.string.checkout_payment_cod_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = Emerald800
                            )
                        }
                        ElajxStatusBadge(
                            status = BadgeStatus.SUCCESS,
                            label = "Active"
                        )
                    }
                }
            }

            // 4. Order Items Summary
            item {
                HorizontalDivider(color = Charcoal100)
                Spacer(modifier = Modifier.height(ElajxSpacing.space1))
                Text(
                    text = stringResource(R.string.checkout_items_summary),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(ElajxSpacing.space1))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(ElajxSpacing.space3),
                        verticalArrangement = Arrangement.spacedBy(ElajxSpacing.space2)
                    ) {
                        uiState.cart.items.forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${item.medicine.nameAr} (${item.variant.brandName})",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = Charcoal900
                                    )
                                    Text(
                                        text = "${item.quantity} × ${item.inventory.price} ${stringResource(R.string.currency_egp)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Charcoal600
                                    )
                                }
                                Text(
                                    text = String.format("%.2f %s", item.lineTotal, stringResource(R.string.currency_egp)),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Emerald800
                                )
                            }
                        }

                        HorizontalDivider(color = Charcoal100, modifier = Modifier.padding(vertical = ElajxSpacing.space1))

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
                                text = String.format("%.2f %s", uiState.cart.subtotal, stringResource(R.string.currency_egp)),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Emerald800
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.checkout_delivery_fee_label),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Charcoal600
                            )
                            Text(
                                text = stringResource(R.string.checkout_delivery_fee_pending),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Charcoal600
                            )
                        }

                        Text(
                            text = stringResource(R.string.checkout_price_notice),
                            style = MaterialTheme.typography.bodySmall,
                            color = Charcoal600
                        )
                    }
                }
            }
        }

        // Sticky Bottom Submit Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = ElajxSpacing.space2)
        ) {
            ElajxButton(
                text = if (uiState.isSubmitting) {
                    stringResource(R.string.checkout_submitting)
                } else {
                    stringResource(R.string.checkout_confirm_button)
                },
                onClick = { viewModel.submitOrder() },
                variant = ButtonVariant.PRIMARY,
                enabled = !uiState.isSubmitting && uiState.selectedAddressId != null && !uiState.cart.isEmpty,
                isLoading = uiState.isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_confirm_order_submission")
            )
        }
    }
}

@Composable
fun AddressCard(
    address: Address,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onSelect)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Charcoal200,
                shape = RoundedCornerShape(8.dp)
            )
            .testTag("address_card_${address.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Emerald100.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ElajxSpacing.space3),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ElajxSpacing.space2)
        ) {
            Icon(
                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isSelected) Emerald800 else Charcoal600
            )

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(ElajxSpacing.space1)
                ) {
                    Text(
                        text = address.label,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (address.isDefault) {
                        ElajxStatusBadge(
                            status = BadgeStatus.INFO,
                            label = "Default"
                        )
                    }
                }
                Text(
                    text = address.formattedAddress,
                    style = MaterialTheme.typography.bodySmall,
                    color = Charcoal600
                )
            }
        }
    }
}

@Composable
fun NewAddressForm(
    isSubmitting: Boolean,
    onCancel: () -> Unit,
    onSave: (label: String, gov: String, city: String, area: String, street: String, bld: String, apt: String?, floor: String?, landmark: String?) -> Unit
) {
    var label by remember { mutableStateOf("Home") }
    var city by remember { mutableStateOf("Cairo") }
    var area by remember { mutableStateOf("") }
    var street by remember { mutableStateOf("") }
    var building by remember { mutableStateOf("") }
    var apartment by remember { mutableStateOf("") }
    var floor by remember { mutableStateOf("") }
    var landmark by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("new_address_form"),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(ElajxSpacing.space3),
            verticalArrangement = Arrangement.spacedBy(ElajxSpacing.space2)
        ) {
            Text(
                text = stringResource(R.string.checkout_add_address_button),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text(stringResource(R.string.address_label)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_address_label"),
                singleLine = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ElajxSpacing.space2)
            ) {
                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text(stringResource(R.string.address_city)) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_address_city"),
                    singleLine = true
                )
                OutlinedTextField(
                    value = area,
                    onValueChange = { area = it },
                    label = { Text(stringResource(R.string.address_area)) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_address_area"),
                    singleLine = true
                )
            }

            OutlinedTextField(
                value = street,
                onValueChange = { street = it },
                label = { Text(stringResource(R.string.address_street)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_address_street"),
                singleLine = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ElajxSpacing.space2)
            ) {
                OutlinedTextField(
                    value = building,
                    onValueChange = { building = it },
                    label = { Text(stringResource(R.string.address_building)) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_address_building"),
                    singleLine = true
                )
                OutlinedTextField(
                    value = apartment,
                    onValueChange = { apartment = it },
                    label = { Text(stringResource(R.string.address_apartment)) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_address_apartment"),
                    singleLine = true
                )
            }

            OutlinedTextField(
                value = landmark,
                onValueChange = { landmark = it },
                label = { Text(stringResource(R.string.address_landmark)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_address_landmark"),
                singleLine = true
            )

            validationError?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = Red600
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ElajxSpacing.space2)
            ) {
                ElajxButton(
                    text = "Cancel",
                    onClick = onCancel,
                    variant = ButtonVariant.GHOST,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_cancel_add_address")
                )

                ElajxButton(
                    text = stringResource(R.string.checkout_save_address),
                    onClick = {
                        if (city.isBlank() || area.isBlank() || street.isBlank() || building.isBlank()) {
                            validationError = "Please fill in City, Area, Street, and Building"
                        } else {
                            validationError = null
                            onSave(label, "Cairo", city, area, street, building, apartment, floor, landmark)
                        }
                    },
                    variant = ButtonVariant.PRIMARY,
                    isLoading = isSubmitting,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_save_address")
                )
            }
        }
    }
}
