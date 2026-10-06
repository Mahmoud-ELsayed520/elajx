package com.example.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.R

/**
 * Foundation navigation destinations for ElajX patient app.
 *
 * Mapped to 02_UX_UI_SPEC.md (Section 2 & 3):
 * Standard bottom navigation: Home, Search, Prescription, Orders, Account.
 */
enum class NavDestination(
    val route: String,
    val titleRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    Home(
        route = "home",
        titleRes = R.string.tab_home,
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
        testTag = "nav_tab_home"
    ),

    Search(
        route = "search",
        titleRes = R.string.tab_search,
        selectedIcon = Icons.Filled.Search,
        unselectedIcon = Icons.Outlined.Search,
        testTag = "nav_tab_search"
    ),

    Prescriptions(
        route = "prescriptions",
        titleRes = R.string.tab_prescriptions,
        selectedIcon = Icons.Filled.Description,
        unselectedIcon = Icons.Outlined.Description,
        testTag = "nav_tab_prescriptions"
    ),

    Orders(
        route = "orders",
        titleRes = R.string.tab_orders,
        selectedIcon = Icons.AutoMirrored.Filled.ReceiptLong,
        unselectedIcon = Icons.AutoMirrored.Outlined.ReceiptLong,
        testTag = "nav_tab_orders"
    ),

    Account(
        route = "account",
        titleRes = R.string.tab_account,
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person,
        testTag = "nav_tab_account"
    );

    companion object {
        val bottomNavItems: List<NavDestination> get() = entries
    }
}
