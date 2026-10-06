package com.example.domain.repository

import com.example.core.result.AppResult
import com.example.domain.model.Address
import com.example.domain.model.Order

/**
 * Contract for executing authoritative order operations and address management.
 */
interface OrderRepository {
    suspend fun getAddresses(userToken: String): AppResult<List<Address>>

    suspend fun createAddress(
        userToken: String,
        label: String,
        governorate: String,
        city: String,
        area: String,
        street: String,
        building: String,
        apartment: String? = null,
        floor: String? = null,
        landmark: String? = null,
        isDefault: Boolean = false
    ): AppResult<Address>

    suspend fun submitOrder(
        userToken: String,
        addressId: String,
        pharmacyId: String,
        items: List<Pair<String, Int>>, // variantId to quantity
        paymentMethod: String = "CASH_ON_DELIVERY",
        idempotencyKey: String
    ): AppResult<Order>
}
