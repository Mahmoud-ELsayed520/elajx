package com.example.data.remote.model

import com.example.domain.model.Address
import com.example.domain.model.Order
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CreateOrderRpcItem(
    @Json(name = "variant_id") val variantId: String,
    @Json(name = "quantity") val quantity: Int
)

@JsonClass(generateAdapter = true)
data class CreateOrderRpcRequest(
    @Json(name = "p_address_id") val addressId: String,
    @Json(name = "p_pharmacy_id") val pharmacyId: String,
    @Json(name = "p_items") val items: List<CreateOrderRpcItem>,
    @Json(name = "p_payment_method") val paymentMethod: String = "CASH_ON_DELIVERY",
    @Json(name = "p_idempotency_key") val idempotencyKey: String
)

@JsonClass(generateAdapter = true)
data class OrderResponseDto(
    @Json(name = "id") val id: String,
    @Json(name = "public_order_number") val publicOrderNumber: String,
    @Json(name = "status") val status: String,
    @Json(name = "subtotal") val subtotal: Double,
    @Json(name = "delivery_fee") val deliveryFee: Double,
    @Json(name = "discount") val discount: Double? = 0.0,
    @Json(name = "total") val total: Double,
    @Json(name = "payment_method") val paymentMethod: String,
    @Json(name = "payment_status") val paymentStatus: String,
    @Json(name = "created_at") val createdAt: String
) {
    fun toDomain(): Order {
        return Order(
            id = id,
            publicOrderNumber = publicOrderNumber,
            status = status,
            subtotal = subtotal,
            deliveryFee = deliveryFee,
            discount = discount ?: 0.0,
            total = total,
            paymentMethod = paymentMethod,
            paymentStatus = paymentStatus,
            createdAt = createdAt
        )
    }
}

@JsonClass(generateAdapter = true)
data class AddressDto(
    @Json(name = "id") val id: String,
    @Json(name = "user_id") val userId: String? = null,
    @Json(name = "label") val label: String? = "Home",
    @Json(name = "governorate") val governorate: String? = "Cairo",
    @Json(name = "city") val city: String? = null,
    @Json(name = "area") val area: String? = null,
    @Json(name = "street") val street: String? = null,
    @Json(name = "building") val building: String? = null,
    @Json(name = "apartment") val apartment: String? = null,
    @Json(name = "floor") val floor: String? = null,
    @Json(name = "landmark") val landmark: String? = null,
    @Json(name = "is_default") val isDefault: Boolean? = false
) {
    fun toDomain(): Address {
        return Address(
            id = id,
            userId = userId ?: "",
            label = label ?: "Home",
            governorate = governorate ?: "Cairo",
            city = city ?: "",
            area = area ?: "",
            street = street ?: "",
            building = building ?: "",
            apartment = apartment,
            floor = floor,
            landmark = landmark,
            isDefault = isDefault ?: false
        )
    }
}

@JsonClass(generateAdapter = true)
data class CreateAddressRequestDto(
    @Json(name = "label") val label: String,
    @Json(name = "governorate") val governorate: String,
    @Json(name = "city") val city: String,
    @Json(name = "area") val area: String,
    @Json(name = "street") val street: String,
    @Json(name = "building") val building: String,
    @Json(name = "apartment") val apartment: String? = null,
    @Json(name = "floor") val floor: String? = null,
    @Json(name = "landmark") val landmark: String? = null,
    @Json(name = "is_default") val isDefault: Boolean = false
)
