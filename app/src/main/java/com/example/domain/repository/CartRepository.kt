package com.example.domain.repository

import com.example.domain.model.Cart
import com.example.domain.model.CartItem
import com.example.domain.model.Medicine
import com.example.domain.model.MedicineVariant
import com.example.domain.model.PharmacyInventory
import kotlinx.coroutines.flow.StateFlow

/**
 * Contract for managing the active patient cart.
 * Purely in-memory checkout state as mandated by Phase 4 Contract (zero Room database).
 */
interface CartRepository {
    val cartState: StateFlow<Cart>

    fun addItem(
        medicine: Medicine,
        variant: MedicineVariant,
        inventory: PharmacyInventory,
        quantity: Int = 1
    ): Boolean

    fun updateQuantity(variantId: String, newQuantity: Int)
    fun removeItem(variantId: String)
    fun clearCart()
}
