package com.example.presentation.screens.search

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import com.example.domain.model.AvailabilityStatus
import com.example.domain.model.Medicine
import com.example.domain.model.MedicineDetail
import com.example.domain.model.MedicineVariant
import com.example.domain.model.PharmacyInventory
import com.example.presentation.components.BadgeStatus
import com.example.presentation.components.ButtonVariant
import com.example.presentation.components.ElajxButton
import com.example.presentation.components.ElajxEmptyState
import com.example.presentation.components.ElajxErrorState
import com.example.presentation.components.ElajxStatusBadge
import com.example.presentation.theme.Amber500
import com.example.presentation.theme.Charcoal100
import com.example.presentation.theme.Charcoal200
import com.example.presentation.theme.Charcoal600
import com.example.presentation.theme.Charcoal900
import com.example.presentation.theme.Red600
import com.example.presentation.theme.ElajxSpacing
import com.example.presentation.theme.Emerald100
import com.example.presentation.theme.Emerald700
import com.example.presentation.theme.Emerald800
import com.example.presentation.theme.Emerald900
import com.example.presentation.theme.PureWhite

/**
 * Phase 3 Authoritative Medicine Search Screen.
 *
 * Mapped to 02_UX_UI_SPEC.md (Section 3 & 5) & 10_FEATURE_REQUIREMENT_MATRIX.md (F-002, F-004, F-005):
 * - Search by Arabic, English, active ingredient, and strength.
 * - Debounced input with race-condition / stale response cancellation.
 * - Loading skeleton/progress, Truthful Empty State, and Error with Retry.
 * - Inspection of Medicine Details, variants, and partner availability in bottom sheet.
 */
@Composable
fun SearchScreen(
    viewModel: SearchViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onAddToCart: (Medicine, MedicineVariant, PharmacyInventory) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val detailState by viewModel.detailState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(ElajxSpacing.space4)
            .testTag("search_screen"),
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
                    text = stringResource(R.string.search_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = stringResource(R.string.search_input_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = Charcoal600
                )
            }
            ElajxStatusBadge(
                status = BadgeStatus.SUCCESS,
                label = "Phase 4 Ready",
                testTag = "search_badge"
            )
        }

        // Search Input Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.onQueryChange(it) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_input_field"),
            placeholder = {
                Text(
                    text = stringResource(R.string.search_input_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Charcoal600
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { viewModel.onQueryChange("") },
                        modifier = Modifier.testTag("search_clear_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear query",
                            tint = Charcoal600
                        )
                    }
                }
            },
            shape = MaterialTheme.shapes.medium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            ),
            singleLine = true
        )

        // Dynamic State Content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when (val state = uiState) {
                is SearchUiState.Initial -> {
                    Card(
                        modifier = Modifier.fillMaxSize(),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        ElajxEmptyState(
                            title = stringResource(R.string.search_empty_prompt),
                            description = stringResource(R.string.search_input_hint),
                            icon = Icons.Default.Search,
                            testTag = "search_initial_state"
                        )
                    }
                }

                is SearchUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("search_loading_state"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(ElajxSpacing.space3)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(36.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(R.string.loading_label),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Charcoal600
                            )
                        }
                    }
                }

                is SearchUiState.Empty -> {
                    Card(
                        modifier = Modifier.fillMaxSize(),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        ElajxEmptyState(
                            title = stringResource(R.string.search_no_results_title),
                            description = stringResource(R.string.search_no_results_desc),
                            icon = Icons.Default.Search,
                            testTag = "search_empty_state"
                        )
                    }
                }

                is SearchUiState.Error -> {
                    Card(
                        modifier = Modifier.fillMaxSize(),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        ElajxErrorState(
                            title = stringResource(R.string.error_state_title),
                            description = state.message,
                            onRetry = if (state.canRetry) { { viewModel.retrySearch() } } else null,
                            testTag = "search_error_state"
                        )
                    }
                }

                is SearchUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("search_results_list"),
                        contentPadding = PaddingValues(vertical = ElajxSpacing.space2),
                        verticalArrangement = Arrangement.spacedBy(ElajxSpacing.space3)
                    ) {
                        items(state.medicines, key = { it.id }) { medicine ->
                            MedicineSearchResultCard(
                                medicine = medicine,
                                onClick = { viewModel.openMedicineDetails(medicine.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Medicine Details Modal Bottom Sheet
    if (detailState !is MedicineDetailUiState.Idle) {
        MedicineDetailBottomSheet(
            state = detailState,
            onDismiss = { viewModel.closeMedicineDetails() },
            onSelectAlternative = { altId -> viewModel.openMedicineDetails(altId) },
            onAddToCart = { med, variant, inv ->
                onAddToCart(med, variant, inv)
                viewModel.closeMedicineDetails()
            }
        )
    }
}

@Composable
fun MedicineSearchResultCard(
    medicine: Medicine,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .testTag("medicine_card_${medicine.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(ElajxSpacing.space4)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = medicine.nameAr,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (medicine.nameEn.isNotBlank() && medicine.nameEn != medicine.nameAr) {
                        Text(
                            text = medicine.nameEn,
                            style = MaterialTheme.typography.bodySmall,
                            color = Charcoal600
                        )
                    }
                }

                if (medicine.prescriptionRequired) {
                    ElajxStatusBadge(
                        status = BadgeStatus.WARNING,
                        label = stringResource(R.string.search_prescription_required),
                        testTag = "badge_rx_${medicine.id}"
                    )
                } else {
                    ElajxStatusBadge(
                        status = BadgeStatus.NEUTRAL,
                        label = stringResource(R.string.search_otc),
                        testTag = "badge_otc_${medicine.id}"
                    )
                }
            }

            Spacer(modifier = Modifier.height(ElajxSpacing.space2))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ElajxSpacing.space2)
            ) {
                if (medicine.activeIngredient.isNotBlank()) {
                    Text(
                        text = "${stringResource(R.string.search_active_ingredient_prefix)} ${medicine.activeIngredient}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Emerald800,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(ElajxSpacing.space1))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ElajxSpacing.space4)
            ) {
                if (medicine.strength.isNotBlank()) {
                    Text(
                        text = "${stringResource(R.string.search_strength_prefix)} ${medicine.strength}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Charcoal600
                    )
                }
                if (medicine.dosageForm.isNotBlank()) {
                    Text(
                        text = "${stringResource(R.string.search_dosage_prefix)} ${medicine.dosageForm}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Charcoal600
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicineDetailBottomSheet(
    state: MedicineDetailUiState,
    onDismiss: () -> Unit,
    onSelectAlternative: (String) -> Unit,
    onAddToCart: (Medicine, com.example.domain.model.MedicineVariant, PharmacyInventory) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("medicine_detail_sheet")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ElajxSpacing.space4)
        ) {
            when (state) {
                is MedicineDetailUiState.Idle -> Unit
                is MedicineDetailUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                is MedicineDetailUiState.Error -> {
                    ElajxErrorState(
                        title = stringResource(R.string.error_state_title),
                        description = state.message,
                        onRetry = null,
                        testTag = "detail_error"
                    )
                }
                is MedicineDetailUiState.Success -> {
                    MedicineDetailContent(
                        detail = state.detail,
                        onSelectAlternative = onSelectAlternative,
                        onAddToCart = onAddToCart
                    )
                }
            }
        }
    }
}

@Composable
fun MedicineDetailContent(
    detail: MedicineDetail,
    onSelectAlternative: (String) -> Unit,
    onAddToCart: (Medicine, com.example.domain.model.MedicineVariant, PharmacyInventory) -> Unit
) {
    val med = detail.medicine
    var selectedInventoryId by remember {
        mutableStateOf(
            detail.availability.firstOrNull {
                it.availabilityStatus == AvailabilityStatus.AVAILABLE ||
                        it.availabilityStatus == AvailabilityStatus.LOW_STOCK
            }?.id
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("medicine_detail_content"),
        verticalArrangement = Arrangement.spacedBy(ElajxSpacing.space3)
    ) {
        // Medicine Title and Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = med.nameAr,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = med.nameEn,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Charcoal600
                    )
                }

                if (med.prescriptionRequired) {
                    ElajxStatusBadge(
                        status = BadgeStatus.WARNING,
                        label = stringResource(R.string.search_prescription_required)
                    )
                } else {
                    ElajxStatusBadge(
                        status = BadgeStatus.NEUTRAL,
                        label = stringResource(R.string.search_otc)
                    )
                }
            }

            Spacer(modifier = Modifier.height(ElajxSpacing.space2))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Charcoal100),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(ElajxSpacing.space3)) {
                    DetailRow(stringResource(R.string.search_active_ingredient_prefix), med.activeIngredient)
                    DetailRow(stringResource(R.string.search_strength_prefix), med.strength)
                    DetailRow(stringResource(R.string.search_dosage_prefix), med.dosageForm)
                    med.manufacturer?.let { DetailRow("Manufacturer:", it) }
                }
            }
        }

        // Section: Partner Pharmacy Availability
        item {
            HorizontalDivider(color = Charcoal100)
            Spacer(modifier = Modifier.height(ElajxSpacing.space2))
            Text(
                text = stringResource(R.string.details_pharmacy_availability),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(ElajxSpacing.space1))

            if (detail.availability.isEmpty()) {
                Text(
                    text = stringResource(R.string.details_no_availability),
                    style = MaterialTheme.typography.bodySmall,
                    color = Charcoal600
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(ElajxSpacing.space2)) {
                    detail.availability.forEach { item ->
                        val isSelected = item.id == selectedInventoryId
                        PharmacyAvailabilityCard(
                            inventory = item,
                            isSelected = isSelected,
                            onSelect = {
                                if (item.availabilityStatus == AvailabilityStatus.AVAILABLE ||
                                    item.availabilityStatus == AvailabilityStatus.LOW_STOCK
                                ) {
                                    selectedInventoryId = item.id
                                }
                            }
                        )
                    }
                }
            }
        }

        // Section: Equivalent Generic Alternatives
        item {
            HorizontalDivider(color = Charcoal100)
            Spacer(modifier = Modifier.height(ElajxSpacing.space2))
            Text(
                text = stringResource(R.string.details_alternatives_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Mandatory Safety Disclaimer per 05_AI_SPEC.md & Phase 3 Contract
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = ElajxSpacing.space1),
                colors = CardDefaults.cardColors(containerColor = Emerald100),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(ElajxSpacing.space3),
                    horizontalArrangement = Arrangement.spacedBy(ElajxSpacing.space2),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Medication,
                        contentDescription = null,
                        tint = Emerald800
                    )
                    Text(
                        text = stringResource(R.string.details_alternatives_disclaimer),
                        style = MaterialTheme.typography.bodySmall,
                        color = Emerald900
                    )
                }
            }

            Spacer(modifier = Modifier.height(ElajxSpacing.space1))

            if (detail.alternatives.isEmpty()) {
                Text(
                    text = stringResource(R.string.details_no_alternatives),
                    style = MaterialTheme.typography.bodySmall,
                    color = Charcoal600
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(ElajxSpacing.space2)) {
                    detail.alternatives.forEach { alt ->
                        AlternativeMedicineCard(
                            medicine = alt,
                            onClick = { onSelectAlternative(alt.id) }
                        )
                    }
                }
            }
        }

        // Phase 4 Active CTA: Add to Cart
        item {
            Spacer(modifier = Modifier.height(ElajxSpacing.space2))
            val selectedInv = detail.availability.firstOrNull { it.id == selectedInventoryId }
            val isAvailable = selectedInv != null && (
                    selectedInv.availabilityStatus == AvailabilityStatus.AVAILABLE ||
                            selectedInv.availabilityStatus == AvailabilityStatus.LOW_STOCK
                    )

            ElajxButton(
                text = stringResource(R.string.details_add_to_cart_action),
                onClick = {
                    if (selectedInv != null) {
                        val variant = detail.variants.firstOrNull { it.id == selectedInv.medicineVariantId }
                            ?: detail.variants.firstOrNull()
                            ?: com.example.domain.model.MedicineVariant(
                                id = selectedInv.medicineVariantId,
                                medicineId = med.id,
                                brandName = med.nameAr,
                                packageSize = "1 Pack",
                                barcode = null,
                                priceReference = selectedInv.price,
                                active = true
                            )
                        onAddToCart(med, variant, selectedInv)
                    }
                },
                variant = ButtonVariant.PRIMARY,
                enabled = isAvailable,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_to_cart_cta")
            )
            if (!isAvailable) {
                Text(
                    text = stringResource(R.string.details_no_availability),
                    style = MaterialTheme.typography.bodySmall,
                    color = Charcoal600,
                    modifier = Modifier.padding(top = ElajxSpacing.space1)
                )
            }
            Spacer(modifier = Modifier.height(ElajxSpacing.space4))
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Charcoal600
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = Charcoal900
        )
    }
}

@Composable
fun PharmacyAvailabilityCard(
    inventory: PharmacyInventory,
    isSelected: Boolean = false,
    onSelect: () -> Unit = {}
) {
    val canSelect = inventory.availabilityStatus == AvailabilityStatus.AVAILABLE ||
            inventory.availabilityStatus == AvailabilityStatus.LOW_STOCK

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = canSelect, onClick = onSelect)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) Emerald800 else Charcoal200,
                shape = RoundedCornerShape(8.dp)
            )
            .testTag("pharmacy_availability_${inventory.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Emerald100.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(8.dp),
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(ElajxSpacing.space1)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (isSelected) Emerald800 else MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = inventory.pharmacyDisplayCode,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (isSelected) {
                        ElajxStatusBadge(status = BadgeStatus.SUCCESS, label = "Selected")
                    }
                }
                Text(
                    text = inventory.pharmacyArea,
                    style = MaterialTheme.typography.bodySmall,
                    color = Charcoal600
                )
                if (inventory.lastSyncedAt.isNotBlank()) {
                    Text(
                        text = stringResource(R.string.details_last_synced, inventory.lastSyncedAt.take(10)),
                        style = MaterialTheme.typography.bodySmall,
                        color = Charcoal600
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                if (inventory.price > 0.0) {
                    Text(
                        text = "${inventory.price} ${stringResource(R.string.currency_egp)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Emerald800
                    )
                }

                val (badgeStatus, labelRes) = when (inventory.availabilityStatus) {
                    AvailabilityStatus.AVAILABLE -> BadgeStatus.SUCCESS to R.string.availability_status_available
                    AvailabilityStatus.LOW_STOCK -> BadgeStatus.WARNING to R.string.availability_status_low_stock
                    AvailabilityStatus.OUT_OF_STOCK -> BadgeStatus.ERROR to R.string.availability_status_out_of_stock
                    AvailabilityStatus.RESERVED -> BadgeStatus.NEUTRAL to R.string.availability_status_reserved
                    AvailabilityStatus.UNAVAILABLE -> BadgeStatus.NEUTRAL to R.string.availability_status_unavailable
                    AvailabilityStatus.EXPIRED -> BadgeStatus.ERROR to R.string.availability_status_expired
                }

                ElajxStatusBadge(
                    status = badgeStatus,
                    label = stringResource(labelRes),
                    testTag = "badge_status_${inventory.id}"
                )
            }
        }
    }
}

@Composable
fun AlternativeMedicineCard(
    medicine: Medicine,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .border(1.dp, Charcoal200, RoundedCornerShape(8.dp))
            .testTag("alt_card_${medicine.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                    text = medicine.nameAr,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${medicine.strength} • ${medicine.dosageForm}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Charcoal600
                )
            }

            ElajxStatusBadge(
                status = BadgeStatus.INFO,
                label = "Alternative"
            )
        }
    }
}
