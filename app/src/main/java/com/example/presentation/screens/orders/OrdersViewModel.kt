package com.example.presentation.screens.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.result.AppError
import com.example.core.result.AppResult
import com.example.core.security.SecureLogger
import com.example.data.local.AuthSessionStorage
import com.example.data.local.InMemoryAuthSessionStorage
import com.example.data.repository.OrderRepositoryImpl
import com.example.domain.model.Order
import com.example.domain.model.OrderDetail
import com.example.domain.repository.OrderRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class OrdersUiState {
    data object Unauthenticated : OrdersUiState()
    data object Loading : OrdersUiState()
    data object Empty : OrdersUiState()
    data class Success(
        val activeOrders: List<Order>,
        val pastOrders: List<Order>
    ) : OrdersUiState()
    data class Error(val message: String) : OrdersUiState()
}

sealed class OrderDetailUiState {
    data object Idle : OrderDetailUiState()
    data object Loading : OrderDetailUiState()
    data class Success(val detail: OrderDetail) : OrderDetailUiState()
    data class Error(val message: String) : OrderDetailUiState()
}

class OrdersViewModel(
    private val orderRepository: OrderRepository = OrderRepositoryImpl(),
    private val sessionStorage: AuthSessionStorage = InMemoryAuthSessionStorage(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main
) : ViewModel() {

    private val tag = "OrdersViewModel"

    private val _uiState = MutableStateFlow<OrdersUiState>(OrdersUiState.Loading)
    val uiState: StateFlow<OrdersUiState> = _uiState.asStateFlow()

    private val _detailState = MutableStateFlow<OrderDetailUiState>(OrderDetailUiState.Idle)
    val detailState: StateFlow<OrderDetailUiState> = _detailState.asStateFlow()

    init {
        loadOrders()
    }

    fun loadOrders() {
        val token = sessionStorage.getAccessToken()
        if (token.isNullOrBlank()) {
            SecureLogger.d(tag, "No active session token - setting state to Unauthenticated")
            _uiState.value = OrdersUiState.Unauthenticated
            return
        }

        _uiState.value = OrdersUiState.Loading
        viewModelScope.launch(dispatcher) {
            when (val result = orderRepository.getOrders(token)) {
                is AppResult.Success -> {
                    val orders = result.data
                    if (orders.isEmpty()) {
                        SecureLogger.i(tag, "Authoritative orders response: 0 orders found (truthful empty state)")
                        _uiState.value = OrdersUiState.Empty
                    } else {
                        val active = orders.filter { isActiveStatus(it.status) }
                        val past = orders.filter { !isActiveStatus(it.status) }
                        SecureLogger.i(tag, "Authoritative orders loaded: ${active.size} active, ${past.size} past")
                        _uiState.value = OrdersUiState.Success(
                            activeOrders = active,
                            pastOrders = past
                        )
                    }
                }
                is AppResult.Error -> {
                    when (result.error) {
                        is AppError.AuthenticationError -> {
                            _uiState.value = OrdersUiState.Unauthenticated
                        }
                        else -> {
                            SecureLogger.w(tag, "Failed to load orders: ${result.error.message}")
                            _uiState.value = OrdersUiState.Error(
                                result.error.message ?: "Unable to complete request. Please try again."
                            )
                        }
                    }
                }
            }
        }
    }

    fun openOrderDetail(orderId: String) {
        val token = sessionStorage.getAccessToken()
        if (token.isNullOrBlank()) {
            _detailState.value = OrderDetailUiState.Error("Authentication required")
            return
        }

        _detailState.value = OrderDetailUiState.Loading
        viewModelScope.launch(dispatcher) {
            when (val result = orderRepository.getOrderDetails(token, orderId)) {
                is AppResult.Success -> {
                    _detailState.value = OrderDetailUiState.Success(result.data)
                }
                is AppResult.Error -> {
                    _detailState.value = OrderDetailUiState.Error(
                        result.error.message ?: "Unable to load order details"
                    )
                }
            }
        }
    }

    fun closeOrderDetail() {
        _detailState.value = OrderDetailUiState.Idle
    }

    companion object {
        /**
         * Canonical active vs terminal status partition per 01_PRODUCT_SPEC.md (§7) & 14_STATE_AND_EDGE_CASES.md (§10):
         * Active: PLACED, CONFIRMED, PREPARING, READY_FOR_PICKUP, PICKED_UP, OUT_FOR_DELIVERY
         * Terminal / Past: DELIVERED, CANCELLED, FAILED
         */
        fun isActiveStatus(status: String): Boolean {
            return when (status.uppercase()) {
                "PLACED", "CONFIRMED", "PREPARING", "READY_FOR_PICKUP", "PICKED_UP", "OUT_FOR_DELIVERY" -> true
                else -> false
            }
        }
    }
}
