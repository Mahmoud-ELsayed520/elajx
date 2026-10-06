package com.example.presentation.shell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.config.EnvironmentConfig
import com.example.core.security.SecureLogger
import com.example.data.repository.AuthRepositoryImpl
import com.example.data.repository.CartRepositoryImpl
import com.example.data.repository.OrderRepositoryImpl
import com.example.domain.model.AuthSession
import com.example.domain.model.Medicine
import com.example.domain.model.MedicineVariant
import com.example.domain.model.Order
import com.example.domain.model.PharmacyInventory
import com.example.domain.repository.CartRepository
import com.example.domain.repository.OrderRepository
import com.example.presentation.auth.AuthViewModel
import com.example.presentation.navigation.NavDestination
import com.example.presentation.screens.cart.CartViewModel
import com.example.presentation.screens.checkout.CheckoutViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AppShellUiState(
    val currentTab: NavDestination = NavDestination.Home,
    val isViewingBaseline: Boolean = false,
    val isViewingCart: Boolean = false,
    val isViewingCheckout: Boolean = false,
    val confirmedOrder: Order? = null,
    val isShowingAuthDialog: Boolean = false,
    val session: AuthSession = AuthSession.Unauthenticated,
    val environmentConfig: EnvironmentConfig = EnvironmentConfig.current(),
    val cartItemCount: Int = 0
)

class AppShellViewModel(
    val authRepository: AuthRepositoryImpl = AuthRepositoryImpl(),
    val cartRepository: CartRepository = CartRepositoryImpl(),
    val orderRepository: OrderRepository = OrderRepositoryImpl(
        config = authRepository.config,
        supabaseBoundary = authRepository.supabaseBoundary
    )
) : ViewModel() {

    private val tag = "AppShellViewModel"
    val authViewModel = AuthViewModel(authRepository)
    val cartViewModel = CartViewModel(cartRepository)
    val checkoutViewModel = CheckoutViewModel(
        orderRepository = orderRepository,
        cartRepository = cartRepository,
        sessionStorage = authRepository.sessionStorage
    )

    private val _uiState = MutableStateFlow(
        AppShellUiState(
            currentTab = NavDestination.Home,
            isViewingBaseline = false,
            isViewingCart = false,
            isViewingCheckout = false,
            confirmedOrder = null,
            isShowingAuthDialog = false,
            session = authRepository.sessionState.value,
            environmentConfig = EnvironmentConfig.current(),
            cartItemCount = 0
        )
    )
    val uiState: StateFlow<AppShellUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.sessionState.collect { session ->
                _uiState.update { it.copy(session = session) }
                if (session is AuthSession.Authenticated) {
                    checkoutViewModel.onSessionRestored()
                }
            }
        }
        viewModelScope.launch {
            cartRepository.cartState.collect { cart ->
                _uiState.update { it.copy(cartItemCount = cart.totalItemsCount) }
            }
        }
        viewModelScope.launch {
            authRepository.checkSession()
        }
    }

    fun selectTab(tab: NavDestination) {
        SecureLogger.d(tag, "Navigating to tab: ${tab.route}")
        _uiState.update {
            it.copy(
                currentTab = tab,
                isViewingBaseline = false,
                isViewingCart = false,
                isViewingCheckout = false,
                confirmedOrder = null
            )
        }
    }

    fun openBaseline() {
        SecureLogger.d(tag, "Opening system baseline verification")
        _uiState.update { it.copy(isViewingBaseline = true) }
    }

    fun closeBaseline() {
        _uiState.update { it.copy(isViewingBaseline = false) }
    }

    fun openCart() {
        SecureLogger.d(tag, "Opening shopping cart")
        _uiState.update {
            it.copy(
                isViewingCart = true,
                isViewingCheckout = false,
                isViewingBaseline = false,
                confirmedOrder = null
            )
        }
    }

    fun closeCart() {
        _uiState.update { it.copy(isViewingCart = false) }
    }

    fun openCheckout() {
        SecureLogger.d(tag, "Navigating to checkout")
        checkoutViewModel.loadAddresses()
        _uiState.update {
            it.copy(
                isViewingCheckout = true,
                isViewingCart = false,
                isViewingBaseline = false,
                confirmedOrder = null
            )
        }
    }

    fun closeCheckout() {
        _uiState.update { it.copy(isViewingCheckout = false, isViewingCart = true) }
    }

    fun onOrderConfirmed(order: Order) {
        SecureLogger.i(tag, "Order confirmed: ${order.publicOrderNumber}")
        _uiState.update {
            it.copy(
                confirmedOrder = order,
                isViewingCheckout = false,
                isViewingCart = false
            )
        }
    }

    fun closeOrderConfirmation(goToOrders: Boolean = false) {
        checkoutViewModel.resetIdempotencyKey()
        _uiState.update {
            it.copy(
                confirmedOrder = null,
                currentTab = if (goToOrders) NavDestination.Orders else NavDestination.Home
            )
        }
    }

    fun addToCart(medicine: Medicine, variant: MedicineVariant, inventory: PharmacyInventory) {
        cartRepository.addItem(medicine, variant, inventory, 1)
    }

    fun openAuthDialog() {
        authViewModel.resetToPhoneInput()
        _uiState.update { it.copy(isShowingAuthDialog = true) }
    }

    fun closeAuthDialog() {
        _uiState.update { it.copy(isShowingAuthDialog = false) }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _uiState.update { it.copy(session = AuthSession.Unauthenticated) }
        }
    }

    fun handleBackPress(): Boolean {
        val current = _uiState.value
        return when {
            current.isShowingAuthDialog -> {
                closeAuthDialog()
                true
            }
            current.confirmedOrder != null -> {
                closeOrderConfirmation(goToOrders = true)
                true
            }
            current.isViewingCheckout -> {
                closeCheckout()
                true
            }
            current.isViewingCart -> {
                closeCart()
                true
            }
            current.isViewingBaseline -> {
                _uiState.update { it.copy(isViewingBaseline = false) }
                true
            }
            current.currentTab != NavDestination.Home -> {
                _uiState.update { it.copy(currentTab = NavDestination.Home) }
                true
            }
            else -> false
        }
    }

    fun toggleAuthSession() {
        authRepository.toggleMockAuthForVerification()
        _uiState.update { it.copy(session = authRepository.sessionState.value) }
    }
}
