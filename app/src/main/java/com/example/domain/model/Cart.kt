package com.example.domain.model

/**
 * Domain representation of an individual item in the patient's cart.
 */
data class CartItem(
    val medicine: Medicine,
    val variant: MedicineVariant,
    val inventory: PharmacyInventory,
    val quantity: Int
) {
    val lineTotal: Double get() = inventory.price * quantity
}

/**
 * Domain representation of the active in-memory cart state.
 * Scoped to a selected pharmacy to preserve single-pharmacy fulfillment integrity.
 */
data class Cart(
    val items: List<CartItem> = emptyList(),
    val selectedPharmacyId: String? = null,
    val selectedPharmacyDisplayCode: String? = null,
    val selectedPharmacyArea: String? = null
) {
    val totalItemsCount: Int get() = items.sumOf { it.quantity }
    val subtotal: Double get() = items.sumOf { it.lineTotal }
    val isEmpty: Boolean get() = items.isEmpty()
}
