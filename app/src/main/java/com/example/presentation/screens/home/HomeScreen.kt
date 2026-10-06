package com.example.presentation.screens.home

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.config.AppEnvironment
import com.example.domain.model.AuthSession
import com.example.presentation.components.BadgeStatus
import com.example.presentation.components.ElajxEmptyState
import com.example.presentation.components.ElajxStatusBadge
import com.example.presentation.theme.Charcoal200
import com.example.presentation.theme.Charcoal600
import com.example.presentation.theme.Charcoal900
import com.example.presentation.theme.ElajxSpacing
import com.example.presentation.theme.Emerald100
import com.example.presentation.theme.Emerald700
import com.example.presentation.theme.Emerald800
import com.example.presentation.theme.Emerald900
import com.example.presentation.theme.PureWhite
import com.example.presentation.theme.Teal50

/**
 * Home Screen Composable.
 *
 * Sourced directly from 02_UX_UI_SPEC.md (Section 3 & 4):
 * - Top greeting & delivery context
 * - Primary large search entry field
 * - Secondary scan prescription CTA banner
 * - Quick actions
 * - Nearby medicine availability with honest unbuilt/empty state
 */
@Composable
fun HomeScreen(
    session: AuthSession,
    environment: AppEnvironment,
    onNavigateToSearch: () -> Unit,
    onNavigateToPrescription: () -> Unit,
    onNavigateToOrders: () -> Unit,
    onNavigateToBaseline: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("home_screen"),
        contentPadding = PaddingValues(ElajxSpacing.space4),
        verticalArrangement = Arrangement.spacedBy(ElajxSpacing.space4)
    ) {
        // 1. Top Greeting & Delivery Location Context
        item {
            LocationAndGreetingHeader(
                session = session,
                environment = environment
            )
        }

        // 2. Primary Large Search Input Field (Triggers Search Tab)
        item {
            SearchTriggerBar(onClick = onNavigateToSearch)
        }

        // 3. Secondary Hero: Scan Prescription CTA Banner
        item {
            PrescriptionBannerCard(onClick = onNavigateToPrescription)
        }

        // 4. Quick Actions Grid
        item {
            Text(
                text = stringResource(R.string.home_quick_actions),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(ElajxSpacing.space2))
            QuickActionsRow(
                onSearchClick = onNavigateToSearch,
                onScanClick = onNavigateToPrescription,
                onOrdersClick = onNavigateToOrders,
                onBaselineClick = onNavigateToBaseline
            )
        }

        // 5. Nearby Medicine Availability Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.home_nearby_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                ElajxStatusBadge(
                    status = BadgeStatus.SUCCESS,
                    label = "Phase 3 Network",
                    testTag = "nearby_badge"
                )
            }
            Spacer(modifier = Modifier.height(ElajxSpacing.space2))

            // Honest Empty State per spec
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                ElajxEmptyState(
                    title = stringResource(R.string.home_nearby_empty_title),
                    description = stringResource(R.string.home_nearby_empty_desc),
                    actionText = stringResource(R.string.home_action_search),
                    onActionClick = onNavigateToSearch,
                    testTag = "home_nearby_empty"
                )
            }
        }
    }
}

@Composable
private fun LocationAndGreetingHeader(
    session: AuthSession,
    environment: AppEnvironment
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(ElajxSpacing.space4)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(ElajxSpacing.space1))
                    Text(
                        text = stringResource(R.string.home_location_prefix) + " " + stringResource(R.string.home_location_default),
                        style = MaterialTheme.typography.bodySmall,
                        color = Charcoal600
                    )
                }

                val envLabel = when (environment) {
                    AppEnvironment.DEVELOPMENT -> "DEV"
                    AppEnvironment.STAGING -> "STAGING"
                    AppEnvironment.PRODUCTION -> "PROD"
                }
                ElajxStatusBadge(
                    status = if (environment == AppEnvironment.PRODUCTION) BadgeStatus.SUCCESS else BadgeStatus.INFO,
                    label = envLabel
                )
            }

            Spacer(modifier = Modifier.height(ElajxSpacing.space2))

            val userName = when (session) {
                is AuthSession.Authenticated -> session.fullName
                else -> stringResource(R.string.home_greeting)
            }

            Text(
                text = userName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.app_tagline),
                style = MaterialTheme.typography.bodySmall,
                color = Charcoal600
            )
        }
    }
}

@Composable
private fun SearchTriggerBar(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, Charcoal200, MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(horizontal = ElajxSpacing.space4, vertical = ElajxSpacing.space3)
            .testTag("home_search_bar")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(ElajxSpacing.space3))
            Text(
                text = stringResource(R.string.home_search_placeholder),
                style = MaterialTheme.typography.bodyMedium,
                color = Charcoal600
            )
        }
    }
}

@Composable
private fun PrescriptionBannerCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("home_prescription_banner"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = Emerald100),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ElajxSpacing.space4),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.home_prescription_banner_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Emerald900
                )
                Spacer(modifier = Modifier.height(ElajxSpacing.space1))
                Text(
                    text = stringResource(R.string.home_prescription_banner_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = Emerald800
                )
                Spacer(modifier = Modifier.height(ElajxSpacing.space2))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.home_prescription_banner_cta),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Emerald900
                    )
                    Spacer(modifier = Modifier.width(ElajxSpacing.space1))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Emerald900,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(Emerald800, MaterialTheme.shapes.medium),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = PureWhite,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
private fun QuickActionsRow(
    onSearchClick: () -> Unit,
    onScanClick: () -> Unit,
    onOrdersClick: () -> Unit,
    onBaselineClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ElajxSpacing.space2)
    ) {
        QuickActionItem(
            title = stringResource(R.string.home_action_search),
            icon = Icons.Default.Search,
            onClick = onSearchClick,
            modifier = Modifier.weight(1f),
            testTag = "quick_action_search"
        )
        QuickActionItem(
            title = stringResource(R.string.home_action_scan),
            icon = Icons.Default.Description,
            onClick = onScanClick,
            modifier = Modifier.weight(1f),
            testTag = "quick_action_scan"
        )
        QuickActionItem(
            title = stringResource(R.string.home_action_orders),
            icon = Icons.AutoMirrored.Filled.ReceiptLong,
            onClick = onOrdersClick,
            modifier = Modifier.weight(1f),
            testTag = "quick_action_orders"
        )
        QuickActionItem(
            title = stringResource(R.string.home_action_baseline),
            icon = Icons.Default.Shield,
            onClick = onBaselineClick,
            modifier = Modifier.weight(1f),
            testTag = "quick_action_baseline"
        )
    }
}

@Composable
private fun QuickActionItem(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String
) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = ElajxSpacing.space3, horizontal = ElajxSpacing.space2),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Teal50, MaterialTheme.shapes.small),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Emerald700,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(ElajxSpacing.space1))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = Charcoal900,
                maxLines = 1
            )
        }
    }
}
