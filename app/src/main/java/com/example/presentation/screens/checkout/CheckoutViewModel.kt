package com.example.presentation.screens.checkout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.result.AppError
import com.example.core.result.AppResult
import com.example.core.security.SecureLogger
import com.example.data.local.AuthSessionStorage
import com.example.domain.model.Address
import com.example.domain.model.Cart
import com.example.domain.model.Order
import com.example.domain.repository.CartRepository
import com.example.domain.repository.OrderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class CheckoutUiState(
    val cart: Cart = Cart(),
    val addresses: List<Address> = emptyList(),
    val selectedAddressId: String? = null,
    val isLoadingAddresses: Boolean = false,
    val isAddingNewAddress: Boolean = false,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val sessionExpired: Boolean = false,
    val createdOrder: Order? = null,
    val paymentMethod: String = "CASH_ON_DELIVERY"
)

class CheckoutViewModel(
    private val orderRepository: OrderRepository,
    private val cartRepository: CartRepository,
    private val sessionStorage: AuthSessionStorage
) : ViewModel() {

    private val tag = "CheckoutViewModel"

    // Idempotency key is generated once per checkout intent and preserved for retries
    private var idempotencyKey: String = UUID.randomUUID().toString()

    private val _uiState = MutableStateFlow(
        CheckoutUiState(
            cart = cartRepository.cartState.value
        )
    )
    val uiState: StateFlow<CheckoutUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            cartRepository.cartState.collect { updatedCart ->
                _uiState.update { it.copy(cart = updatedCart) }
            }
        }
        loadAddresses()
    }

    fun getIdempotencyKey(): String = idempotencyKey

    fun resetIdempotencyKey() {
        idempotencyKey = UUID.randomUUID().toString()
    }

    fun loadAddresses() {
        val token = sessionStorage.getAccessToken()
        if (token.isNullOrBlank()) {
            SecureLogger.d(tag, "No token found when loading addresses. Marking session expired / auth required.")
            _uiState.update { it.copy(sessionExpired = true) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingAddresses = true, errorMessage = null) }
            when (val result = orderRepository.getAddresses(token)) {
                is AppResult.Success -> {
                    val addresses = result.data
                    val defaultId = addresses.firstOrNull { it.isDefault }?.id
                        ?: addresses.firstOrNull()?.id
                    _uiState.update {
                        it.copy(
                            addresses = addresses,
                            selectedAddressId = it.selectedAddressId ?: defaultId,
                            isLoadingAddresses = false,
                            sessionExpired = false
                        )
                    }
                }
                is AppResult.Error -> {
                    if (result.error is AppError.AuthenticationError) {
                        SecureLogger.w(tag, "Authentication failure loading addresses")
                        _uiState.update { it.copy(sessionExpired = true, isLoadingAddresses = false) }
                    } else {
                        _uiState.update {
                            it.copy(
                                errorMessage = result.error.message,
                                isLoadingAddresses = false
                            )
                        }
                    }
                }
            }
        }
    }

    fun selectAddress(addressId: String) {
        _uiState.update { it.copy(selectedAddressId = addressId) }
    }

    fun toggleNewAddressForm(show: Boolean) {
        _uiState.update { it.copy(isAddingNewAddress = show) }
    }

    fun createNewAddress(
        label: String,
        governorate: String,
        city: String,
        area: String,
        street: String,
        building: String,
        apartment: String?,
        floor: String?,
        landmark: String?
    ) {
        val token = sessionStorage.getAccessToken()
        if (token.isNullOrBlank()) {
            _uiState.update { it.copy(sessionExpired = true) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            when (val result = orderRepository.createAddress(
                userToken = token,
                label = label,
                governorate = governorate,
                city = city,
                area = area,
                street = street,
                building = building,
                apartment = apartment,
                floor = floor,
                landmark = landmark,
                isDefault = true
            )) {
                is AppResult.Success -> {
                    val newAddress = result.data
                    _uiState.update { current ->
                        val updatedList = listOf(newAddress) + current.addresses
                        current.copy(
                            addresses = updatedList,
                            selectedAddressId = newAddress.id,
                            isAddingNewAddress = false,
                            isSubmitting = false
                        )
                    }
                }
                is AppResult.Error -> {
                    _uiState.update {
                        it.copy(
                            errorMessage = result.error.message,
                            isSubmitting = false
                        )
                    }
                }
            }
        }
    }

    fun submitOrder() {
        val state = _uiState.value
        val token = sessionStorage.getAccessToken()

        if (token.isNullOrBlank()) {
            SecureLogger.w(tag, "Cannot submit order: user is unauthenticated")
            _uiState.update { it.copy(sessionExpired = true) }
            return
        }

        if (state.cart.isEmpty) {
            _uiState.update { it.copy(errorMessage = "Cannot place order: cart is empty") }
            return
        }

        val addressId = state.selectedAddressId
        if (addressId.isNullOrBlank()) {
            _uiState.update { it.copy(errorMessage = "Please select or add a delivery address") }
            return
        }

        val pharmacyId = state.cart.selectedPharmacyId
        if (pharmacyId.isNullOrBlank()) {
            _uiState.update { it.copy(errorMessage = "No fulfillment pharmacy selected") }
            return
        }

        val items = state.cart.items.map { it.variant.id to it.quantity }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            SecureLogger.i(tag, "Dispatching create_order RPC with idempotency key: $idempotencyKey")

            when (val result = orderRepository.submitOrder(
                userToken = token,
                addressId = addressId,
                pharmacyId = pharmacyId,
                items = items,
                paymentMethod = "CASH_ON_DELIVERY",
                idempotencyKey = idempotencyKey
            )) {
                is AppResult.Success -> {
                    SecureLogger.i(tag, "Order successfully confirmed: ${result.data.publicOrderNumber}")
                    // Cart state is cleared only AFTER successful server creation
                    cartRepository.clearCart()
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            createdOrder = result.data,
                            errorMessage = null
                        )
                    }
                }
                is AppResult.Error -> {
                    if (result.error is AppError.AuthenticationError) {
                        SecureLogger.w(tag, "Session expired during order submission. Preserving cart and checkout state.")
                        _uiState.update {
                            it.copy(
                                isSubmitting = false,
                                sessionExpired = true
                            )
                        }
                    } else {
                        SecureLogger.w(tag, "Order placement rejected: ${result.error.message}")
                        _uiState.update {
                            it.copy(
                                isSubmitting = false,
                                errorMessage = result.error.message
                            )
                        }
                    }
                }
            }
        }
    }

    fun onSessionRestored() {
        SecureLogger.i(tag, "Session restored after OTP verification. Resuming checkout with preserved state.")
        _uiState.update { it.copy(sessionExpired = false) }
        loadAddresses()
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
