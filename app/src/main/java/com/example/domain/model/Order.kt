package com.example.domain.model

/**
 * Domain representation of an authoritative patient order.
 * Strictly populated from backend/RPC state (04_DATA_MODEL.md §12).
 */
data class Order(
    val id: String,
    val publicOrderNumber: String,
    val status: String,
    val subtotal: Double,
    val deliveryFee: Double,
    val discount: Double = 0.0,
    val total: Double,
    val paymentMethod: String,
    val paymentStatus: String,
    val createdAt: String
)

/**
 * Snapshot of an ordered item associated with an order.
 */
data class OrderItem(
    val id: String,
    val orderId: String,
    val medicineVariantId: String,
    val medicineNameSnapshot: String,
    val quantity: Int,
    val unitPrice: Double,
    val lineTotal: Double,
    val sourcePharmacyId: String
)

/**
 * Historical status transition entry associated with an order (04_DATA_MODEL.md §14).
 */
data class OrderStatusHistory(
    val id: String,
    val orderId: String,
    val fromStatus: String?,
    val toStatus: String,
    val actorType: String,
    val actorId: String? = null,
    val note: String? = null,
    val createdAt: String
)

/**
 * Full details of an order including its line items and status transition timeline.
 */
data class OrderDetail(
    val order: Order,
    val items: List<OrderItem> = emptyList(),
    val statusHistory: List<OrderStatusHistory> = emptyList()
)
