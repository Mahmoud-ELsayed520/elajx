package com.example.presentation.bootstrap

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.config.AppEnvironment
import com.example.domain.model.AuthSession
import com.example.domain.model.BoundaryCheck
import com.example.presentation.components.BadgeStatus
import com.example.presentation.components.ButtonVariant
import com.example.presentation.components.ElajxButton
import com.example.presentation.components.ElajxStatusBadge
import com.example.presentation.navigation.NavDestination
import com.example.presentation.theme.Charcoal100
import com.example.presentation.theme.Charcoal600
import com.example.presentation.theme.ElajxSpacing
import com.example.presentation.theme.Emerald100
import com.example.presentation.theme.Emerald800
import com.example.presentation.theme.Emerald900
import com.example.presentation.theme.PureWhite

@Composable
fun BootstrapScreen(
    viewModel: BootstrapViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(NavDestination.Home.route) }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("bootstrap_screen"),
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("bootstrap_bottom_nav"),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                NavDestination.bottomNavItems.forEach { destination ->
                    val isSelected = selectedTab == destination.route
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = destination.route },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) destination.selectedIcon else destination.unselectedIcon,
                                contentDescription = stringResource(destination.titleRes)
                            )
                        },
                        label = {
                            Text(
                                text = stringResource(destination.titleRes),
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Emerald900,
                            selectedTextColor = Emerald800,
                            indicatorColor = Emerald100
                        ),
                        modifier = Modifier.testTag(destination.testTag)
                    )
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(ElajxSpacing.space4),
            verticalArrangement = Arrangement.spacedBy(ElajxSpacing.space4)
        ) {
            // Header: Branding & Tagline
            item {
                HeaderCard(environment = uiState.environment.environment)
            }

            // Session & Order Authorization Gate Status
            item {
                AuthSessionCard(
                    session = uiState.session,
                    canCreateOrder = uiState.canCreateOrder,
                    onToggleAuth = { viewModel.toggleAuthSession() }
                )
            }

            // System & Boundary Verification Checklist
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.bootstrap_subtitle),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    ElajxStatusBadge(
                        status = BadgeStatus.SUCCESS,
                        label = "${uiState.boundaryChecks.count { it.isVerified }}/${uiState.boundaryChecks.size} Verified"
                    )
                }
            }

            items(uiState.boundaryChecks, key = { it.id }) { check ->
                BoundaryCheckItem(check = check)
            }

            // Verification Actions
            item {
                ElajxButton(
                    text = stringResource(R.string.btn_reverify),
                    onClick = { viewModel.performBoundaryVerification() },
                    isLoading = uiState.isVerifying,
                    variant = ButtonVariant.PRIMARY,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "btn_reverify_foundation"
                )
            }

            // Truthful Feature Status Section
            item {
                FeatureStatusMatrixCard()
            }
        }
    }
}

@Composable
private fun HeaderCard(environment: AppEnvironment) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("header_card"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(ElajxSpacing.space5)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Emerald100, MaterialTheme.shapes.medium),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = Emerald800,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(ElajxSpacing.space3))
                    Column {
                        Text(
                            text = "ElajX · عِلاجِك",
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

                val envLabel = when (environment) {
                    AppEnvironment.DEVELOPMENT -> "DEV"
                    AppEnvironment.STAGING -> "STAGING"
                    AppEnvironment.PRODUCTION -> "PROD"
                }
                ElajxStatusBadge(
                    status = if (environment == AppEnvironment.DEVELOPMENT) BadgeStatus.INFO else BadgeStatus.SUCCESS,
                    label = envLabel,
                    testTag = "env_badge"
                )
            }
        }
    }
}

@Composable
private fun AuthSessionCard(
    session: AuthSession,
    canCreateOrder: Boolean,
    onToggleAuth: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("auth_session_card"),
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
                        imageVector = if (canCreateOrder) Icons.Default.LockOpen else Icons.Default.Lock,
                        contentDescription = null,
                        tint = if (canCreateOrder) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(ElajxSpacing.space2))
                    Text(
                        text = stringResource(R.string.auth_state_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                val (badgeStatus, statusText) = when (session) {
                    is AuthSession.Authenticated -> BadgeStatus.SUCCESS to stringResource(R.string.auth_state_authenticated)
                    is AuthSession.Unauthenticated -> BadgeStatus.WARNING to stringResource(R.string.auth_state_unauthenticated)
                    is AuthSession.Expired -> BadgeStatus.ERROR to stringResource(R.string.auth_state_expired)
                    is AuthSession.Loading -> BadgeStatus.NEUTRAL to stringResource(R.string.auth_state_loading)
                    is AuthSession.Error -> BadgeStatus.ERROR to session.message
                }

                ElajxStatusBadge(status = badgeStatus, label = statusText)
            }

            Spacer(modifier = Modifier.height(ElajxSpacing.space3))

            // Order creation boundary confirmation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Order Creation Allowed:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (canCreateOrder) "YES (Authenticated)" else "NO (Guest Locked)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (canCreateOrder) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(ElajxSpacing.space3))

            ElajxButton(
                text = stringResource(R.string.btn_toggle_auth),
                onClick = onToggleAuth,
                variant = ButtonVariant.SECONDARY,
                modifier = Modifier.fillMaxWidth(),
                testTag = "btn_toggle_auth"
            )
        }
    }
}

@Composable
private fun BoundaryCheckItem(check: BoundaryCheck) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("boundary_check_${check.id}"),
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
                Text(
                    text = check.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                ElajxStatusBadge(
                    status = if (check.isVerified) BadgeStatus.SUCCESS else BadgeStatus.ERROR,
                    label = if (check.isVerified) "Verified" else "Failed"
                )
            }
            Spacer(modifier = Modifier.height(ElajxSpacing.space2))
            Text(
                text = check.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(ElajxSpacing.space2))
            Text(
                text = "Spec: ${check.specificationRef}",
                style = MaterialTheme.typography.bodySmall,
                color = Charcoal600
            )
        }
    }
}

@Composable
private fun FeatureStatusMatrixCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("feature_status_matrix_card"),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(ElajxSpacing.space4)) {
            Text(
                text = "Feature Implementation Matrix",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(ElajxSpacing.space1))
            Text(
                text = "Per Bootstrap specification (17_ANDROID_PROJECT_BOOTSTRAP.md), actual product features remain unbuilt until Phase 1.",
                style = MaterialTheme.typography.bodySmall,
                color = Charcoal600
            )

            Spacer(modifier = Modifier.height(ElajxSpacing.space3))
            HorizontalDivider(color = Charcoal100)
            Spacer(modifier = Modifier.height(ElajxSpacing.space3))

            val unbuiltFeatures = listOf(
                "Medicine Catalog & Search" to "Phase 3 Completed",
                "Cart & Server-Side Totals" to "Phase 4 (Post-Bootstrap)",
                "Checkout & Order Creation" to "Phase 4 (Post-Bootstrap)",
                "Order History & Real-Time Tracking" to "Phase 5 (Post-Bootstrap)",
                "Prescription Capture & AI Pipeline" to "Phase 6 (Post-Bootstrap)",
                "Recurring Medication Reminders" to "Phase 7 (Post-Bootstrap)",
                "Pharmacy Partner Operations" to "TBD / Future Phase",
                "Online Payment Gateway" to "Out of Scope for MVP (COD)"
            )

            unbuiltFeatures.forEach { (feature, target) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = ElajxSpacing.space1),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = feature,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    ElajxStatusBadge(
                        status = BadgeStatus.NEUTRAL,
                        label = target
                    )
                }
            }
        }
    }
}
