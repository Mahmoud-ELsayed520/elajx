package com.example.data.repository

import com.example.core.security.SecureLogger
import com.example.domain.model.Cart
import com.example.domain.model.CartItem
import com.example.domain.model.Medicine
import com.example.domain.model.MedicineVariant
import com.example.domain.model.PharmacyInventory
import com.example.domain.repository.CartRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class CartRepositoryImpl : CartRepository {

    private val tag = "CartRepositoryImpl"
    private val _cartState = MutableStateFlow(Cart())
    override val cartState: StateFlow<Cart> = _cartState.asStateFlow()

    override fun addItem(
        medicine: Medicine,
        variant: MedicineVariant,
        inventory: PharmacyInventory,
        quantity: Int
    ): Boolean {
        if (quantity <= 0) return false

        _cartState.update { currentCart ->
            // If cart currently has items from a DIFFERENT pharmacy, reset or override
            val isDifferentPharmacy = currentCart.selectedPharmacyId != null &&
                    currentCart.selectedPharmacyId != inventory.pharmacyId

            val baseItems = if (isDifferentPharmacy) {
                SecureLogger.d(tag, "Switching fulfillment pharmacy to ${inventory.pharmacyDisplayCode}")
                emptyList()
            } else {
                currentCart.items
            }

            val existingIndex = baseItems.indexOfFirst { it.variant.id == variant.id }
            val updatedItems = if (existingIndex >= 0) {
                baseItems.mapIndexed { index, item ->
                    if (index == existingIndex) {
                        item.copy(quantity = item.quantity + quantity)
                    } else item
                }
            } else {
                baseItems + CartItem(
                    medicine = medicine,
                    variant = variant,
                    inventory = inventory,
                    quantity = quantity
                )
            }

            Cart(
                items = updatedItems,
                selectedPharmacyId = inventory.pharmacyId,
                selectedPharmacyDisplayCode = inventory.pharmacyDisplayCode,
                selectedPharmacyArea = inventory.pharmacyArea
            )
        }
        return true
    }

    override fun updateQuantity(variantId: String, newQuantity: Int) {
        _cartState.update { currentCart ->
            if (newQuantity <= 0) {
                val filtered = currentCart.items.filterNot { it.variant.id == variantId }
                if (filtered.isEmpty()) {
                    Cart()
                } else {
                    currentCart.copy(items = filtered)
                }
            } else {
                val updated = currentCart.items.map { item ->
                    if (item.variant.id == variantId) {
                        item.copy(quantity = newQuantity)
                    } else item
                }
                currentCart.copy(items = updated)
            }
        }
    }

    override fun removeItem(variantId: String) {
        _cartState.update { currentCart ->
            val filtered = currentCart.items.filterNot { it.variant.id == variantId }
            if (filtered.isEmpty()) {
                Cart()
            } else {
                currentCart.copy(items = filtered)
            }
        }
    }

    override fun clearCart() {
        SecureLogger.d(tag, "Clearing cart")
        _cartState.value = Cart()
    }
}
