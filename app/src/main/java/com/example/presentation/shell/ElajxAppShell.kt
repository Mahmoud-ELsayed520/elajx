package com.example.presentation.shell

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.config.AppEnvironment
import com.example.domain.model.AuthSession
import com.example.domain.model.MedicineVariant
import com.example.domain.model.PharmacyInventory
import com.example.presentation.auth.AuthFlowDialog
import com.example.presentation.bootstrap.BootstrapScreen
import com.example.presentation.bootstrap.BootstrapViewModel
import com.example.presentation.components.BadgeStatus
import com.example.presentation.components.ElajxStatusBadge
import com.example.presentation.navigation.NavDestination
import com.example.presentation.screens.account.AccountScreen
import com.example.presentation.screens.cart.CartScreen
import com.example.presentation.screens.checkout.CheckoutScreen
import com.example.presentation.screens.checkout.OrderConfirmationScreen
import com.example.presentation.screens.home.HomeScreen
import com.example.presentation.screens.orders.OrdersScreen
import com.example.presentation.screens.prescription.PrescriptionScreen
import com.example.presentation.screens.search.SearchScreen
import com.example.presentation.theme.Emerald100
import com.example.presentation.theme.Emerald800
import com.example.presentation.theme.Emerald900

/**
 * Main Application Shell.
 *
 * Implements Phase 1 foundation & Phase 4 Cart / Checkout integration:
 * - App shell
 * - TopAppBar with branding, cart badge & environment indicator
 * - Bottom navigation bar (Home, Search, Prescription, Orders, Account)
 * - Complete Cart -> Checkout -> Atomic Order Confirmation navigation flow
 * - BackHandler managing sub-screens and returning to Home tab
 * - Accessible touch targets, RTL support, and centralized tokens
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ElajxAppShell(
    viewModel: AppShellViewModel,
    bootstrapViewModel: BootstrapViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    // Handle Back Press to pop sub-screens or return to Home
    BackHandler(
        enabled = uiState.isViewingBaseline ||
                uiState.isViewingCart ||
                uiState.isViewingCheckout ||
                uiState.confirmedOrder != null ||
                uiState.currentTab != NavDestination.Home
    ) {
        viewModel.handleBackPress()
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("app_shell_scaffold"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when {
                            uiState.isViewingBaseline -> stringResource(R.string.bootstrap_title)
                            uiState.confirmedOrder != null -> stringResource(R.string.confirmation_title)
                            uiState.isViewingCheckout -> stringResource(R.string.checkout_title)
                            uiState.isViewingCart -> stringResource(R.string.cart_title)
                            else -> "ElajX · عِلاجِك"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    when {
                        uiState.isViewingBaseline -> {
                            IconButton(
                                onClick = { viewModel.closeBaseline() },
                                modifier = Modifier.testTag("btn_close_baseline")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.back_to_home)
                                )
                            }
                        }
                        uiState.confirmedOrder != null -> {
                            IconButton(
                                onClick = { viewModel.closeOrderConfirmation(goToOrders = true) },
                                modifier = Modifier.testTag("btn_back_confirmed_order")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Close confirmation"
                                )
                            }
                        }
                        uiState.isViewingCheckout -> {
                            IconButton(
                                onClick = { viewModel.closeCheckout() },
                                modifier = Modifier.testTag("btn_back_from_checkout")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back to cart"
                                )
                            }
                        }
                        uiState.isViewingCart -> {
                            IconButton(
                                onClick = { viewModel.closeCart() },
                                modifier = Modifier.testTag("btn_close_cart")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Close cart"
                                )
                            }
                        }
                    }
                },
                actions = {
                    // Cart Icon with Badge
                    if (!uiState.isViewingCart && !uiState.isViewingCheckout && uiState.confirmedOrder == null) {
                        BadgedBox(
                            badge = {
                                if (uiState.cartItemCount > 0) {
                                    Badge(
                                        containerColor = Emerald800,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ) {
                                        Text(
                                            text = "${uiState.cartItemCount}",
                                            modifier = Modifier.testTag("cart_badge_count")
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            IconButton(
                                onClick = { viewModel.openCart() },
                                modifier = Modifier.testTag("btn_top_cart")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingCart,
                                    contentDescription = stringResource(R.string.cart_title)
                                )
                            }
                        }
                    }

                    val envLabel = when (uiState.environmentConfig.environment) {
                        AppEnvironment.DEVELOPMENT -> "DEV"
                        AppEnvironment.STAGING -> "STAGING"
                        AppEnvironment.PRODUCTION -> "PROD"
                    }
                    ElajxStatusBadge(
                        status = if (uiState.environmentConfig.environment == AppEnvironment.PRODUCTION) BadgeStatus.SUCCESS else BadgeStatus.INFO,
                        label = envLabel,
                        modifier = Modifier.padding(end = 12.dp),
                        testTag = "app_shell_env_badge"
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            if (!uiState.isViewingBaseline && !uiState.isViewingCheckout && uiState.confirmedOrder == null) {
                NavigationBar(
                    modifier = Modifier.testTag("app_shell_bottom_nav"),
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp
                ) {
                    NavDestination.bottomNavItems.forEach { destination ->
                        val isSelected = uiState.currentTab == destination && !uiState.isViewingCart
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                if (uiState.isViewingCart) viewModel.closeCart()
                                viewModel.selectTab(destination)
                            },
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
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when {
                uiState.isViewingBaseline -> {
                    BootstrapScreen(viewModel = bootstrapViewModel)
                }
                uiState.confirmedOrder != null -> {
                    OrderConfirmationScreen(
                        order = uiState.confirmedOrder!!,
                        onNavigateToOrders = {
                            viewModel.closeOrderConfirmation(goToOrders = true)
                        },
                        onNavigateToHome = {
                            viewModel.closeOrderConfirmation(goToOrders = false)
                        }
                    )
                }
                uiState.isViewingCheckout -> {
                    CheckoutScreen(
                        viewModel = viewModel.checkoutViewModel,
                        onNavigateBack = { viewModel.closeCheckout() },
                        onOrderConfirmed = { order ->
                            viewModel.onOrderConfirmed(order)
                        },
                        onRequireAuth = { viewModel.openAuthDialog() }
                    )
                }
                uiState.isViewingCart -> {
                    CartScreen(
                        viewModel = viewModel.cartViewModel,
                        isAuthenticated = uiState.session is AuthSession.Authenticated,
                        onNavigateToCheckout = { viewModel.openCheckout() },
                        onNavigateToSearch = {
                            viewModel.closeCart()
                            viewModel.selectTab(NavDestination.Search)
                        },
                        onRequireAuth = { viewModel.openAuthDialog() }
                    )
                }
                else -> {
                    when (uiState.currentTab) {
                        NavDestination.Home -> HomeScreen(
                            session = uiState.session,
                            environment = uiState.environmentConfig.environment,
                            onNavigateToSearch = { viewModel.selectTab(NavDestination.Search) },
                            onNavigateToPrescription = { viewModel.selectTab(NavDestination.Prescriptions) },
                            onNavigateToOrders = { viewModel.selectTab(NavDestination.Orders) },
                            onNavigateToBaseline = { viewModel.openBaseline() }
                        )
                        NavDestination.Search -> SearchScreen(
                            onAddToCart = { med, variant, inv ->
                                viewModel.addToCart(med, variant, inv)
                            }
                        )
                        NavDestination.Prescriptions -> PrescriptionScreen()
                        NavDestination.Orders -> OrdersScreen(
                            onNavigateToHome = { viewModel.selectTab(NavDestination.Home) }
                        )
                        NavDestination.Account -> AccountScreen(
                            session = uiState.session,
                            environmentConfig = uiState.environmentConfig,
                            onSignIn = { viewModel.openAuthDialog() },
                            onSignOut = { viewModel.logout() },
                            onViewBaseline = { viewModel.openBaseline() }
                        )
                    }
                }
            }

            if (uiState.isShowingAuthDialog) {
                AuthFlowDialog(
                    viewModel = viewModel.authViewModel,
                    onDismiss = { viewModel.closeAuthDialog() },
                    onSuccess = { viewModel.closeAuthDialog() }
                )
            }
        }
    }
}
