package com.example.data.repository

import com.example.core.config.EnvironmentConfig
import com.example.core.result.AppError
import com.example.core.result.AppResult
import com.example.core.security.SecureLogger
import com.example.data.remote.RealSupabaseOrderClient
import com.example.data.remote.SupabaseBoundary
import com.example.data.remote.SupabaseOrderClient
import com.example.data.remote.model.CreateAddressRequestDto
import com.example.data.remote.model.CreateOrderRpcItem
import com.example.data.remote.model.CreateOrderRpcRequest
import com.example.domain.model.Address
import com.example.domain.model.Order
import com.example.domain.model.OrderDetail
import com.example.domain.repository.OrderRepository

class OrderRepositoryImpl(
    private val config: EnvironmentConfig = EnvironmentConfig.current(),
    private val supabaseBoundary: SupabaseBoundary = SupabaseBoundary(config),
    private val orderClient: SupabaseOrderClient = RealSupabaseOrderClient()
) : OrderRepository {

    private val tag = "OrderRepositoryImpl"

    override suspend fun getAddresses(userToken: String): AppResult<List<Address>> {
        if (userToken.isBlank()) {
            return AppResult.Error(AppError.AuthenticationError("Authentication required to fetch addresses"))
        }
        val headers = supabaseBoundary.getClientSafeHeaders(userToken = userToken)
        return when (val result = orderClient.getUserAddresses(config.supabaseUrl, headers)) {
            is AppResult.Success -> AppResult.Success(result.data.map { it.toDomain() })
            is AppResult.Error -> result
        }
    }

    override suspend fun createAddress(
        userToken: String,
        label: String,
        governorate: String,
        city: String,
        area: String,
        street: String,
        building: String,
        apartment: String?,
        floor: String?,
        landmark: String?,
        isDefault: Boolean
    ): AppResult<Address> {
        if (userToken.isBlank()) {
            return AppResult.Error(AppError.AuthenticationError("Authentication required to create address"))
        }
        val headers = supabaseBoundary.getClientSafeHeaders(userToken = userToken)
        val requestDto = CreateAddressRequestDto(
            label = label.trim().ifBlank { "Home" },
            governorate = governorate.trim().ifBlank { "Cairo" },
            city = city.trim(),
            area = area.trim(),
            street = street.trim(),
            building = building.trim(),
            apartment = apartment?.trim()?.ifBlank { null },
            floor = floor?.trim()?.ifBlank { null },
            landmark = landmark?.trim()?.ifBlank { null },
            isDefault = isDefault
        )

        return when (val result = orderClient.createAddress(config.supabaseUrl, headers, requestDto)) {
            is AppResult.Success -> AppResult.Success(result.data.toDomain())
            is AppResult.Error -> result
        }
    }

    override suspend fun submitOrder(
        userToken: String,
        addressId: String,
        pharmacyId: String,
        items: List<Pair<String, Int>>,
        paymentMethod: String,
        idempotencyKey: String
    ): AppResult<Order> {
        if (userToken.isBlank()) {
            return AppResult.Error(AppError.AuthenticationError("Authentication required to submit order"))
        }
        if (items.isEmpty()) {
            return AppResult.Error(AppError.ValidationError("Cart is empty", "cart"))
        }
        if (addressId.isBlank()) {
            return AppResult.Error(AppError.ValidationError("Delivery address is required", "address"))
        }
        if (pharmacyId.isBlank()) {
            return AppResult.Error(AppError.ValidationError("Fulfillment pharmacy is required", "pharmacy"))
        }
        if (idempotencyKey.isBlank()) {
            return AppResult.Error(AppError.ValidationError("Idempotency key is required", "idempotencyKey"))
        }

        val rpcItems = items.map { (variantId, qty) ->
            CreateOrderRpcItem(variantId = variantId, quantity = qty)
        }

        val rpcRequest = CreateOrderRpcRequest(
            addressId = addressId,
            pharmacyId = pharmacyId,
            items = rpcItems,
            paymentMethod = paymentMethod,
            idempotencyKey = idempotencyKey
        )

        val headers = supabaseBoundary.getClientSafeHeaders(userToken = userToken)
        SecureLogger.i(tag, "Dispatching create_order RPC via SupabaseBoundary")

        return when (val result = orderClient.createOrder(config.supabaseUrl, headers, rpcRequest)) {
            is AppResult.Success -> {
                SecureLogger.i(tag, "Order successfully created: ${result.data.publicOrderNumber}")
                AppResult.Success(result.data.toDomain())
            }
            is AppResult.Error -> {
                SecureLogger.w(tag, "Order submission failed: ${result.error.message}")
                result
            }
        }
    }

    override suspend fun getOrders(userToken: String): AppResult<List<Order>> {
        if (userToken.isBlank()) {
            return AppResult.Error(AppError.AuthenticationError("Authentication required to fetch orders"))
        }
        val headers = supabaseBoundary.getClientSafeHeaders(userToken = userToken)
        SecureLogger.i(tag, "Fetching orders for authenticated patient")
        return when (val result = orderClient.getUserOrders(config.supabaseUrl, headers)) {
            is AppResult.Success -> AppResult.Success(result.data.map { it.toDomain() })
            is AppResult.Error -> result
        }
    }

    override suspend fun getOrderDetails(userToken: String, orderId: String): AppResult<OrderDetail> {
        if (userToken.isBlank()) {
            return AppResult.Error(AppError.AuthenticationError("Authentication required to fetch order details"))
        }
        if (orderId.isBlank()) {
            return AppResult.Error(AppError.ValidationError("Order ID is required", "orderId"))
        }
        val headers = supabaseBoundary.getClientSafeHeaders(userToken = userToken)
        SecureLogger.i(tag, "Fetching order details for order ID: $orderId")

        // First find the order
        val ordersResult = orderClient.getUserOrders(config.supabaseUrl, headers)
        if (ordersResult is AppResult.Error) {
            return ordersResult
        }
        val order = (ordersResult as AppResult.Success).data.find { it.id == orderId }?.toDomain()
            ?: return AppResult.Error(AppError.NotFoundError("Order not found or unauthorized"))

        // Fetch line items
        val itemsResult = orderClient.getOrderItems(config.supabaseUrl, headers, orderId)
        val items = if (itemsResult is AppResult.Success) itemsResult.data.map { it.toDomain() } else emptyList()

        // Fetch status history
        val historyResult = orderClient.getOrderStatusHistory(config.supabaseUrl, headers, orderId)
        val history = if (historyResult is AppResult.Success) historyResult.data.map { it.toDomain() } else emptyList()

        return AppResult.Success(
            OrderDetail(
                order = order,
                items = items,
                statusHistory = history
            )
        )
    }
}
