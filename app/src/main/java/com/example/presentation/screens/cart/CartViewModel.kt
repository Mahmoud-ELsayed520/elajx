package com.example.presentation.screens.cart

import androidx.lifecycle.ViewModel
import com.example.domain.model.Cart
import com.example.domain.repository.CartRepository
import kotlinx.coroutines.flow.StateFlow

class CartViewModel(
    private val cartRepository: CartRepository
) : ViewModel() {

    val cartState: StateFlow<Cart> = cartRepository.cartState

    fun updateQuantity(variantId: String, newQuantity: Int) {
        cartRepository.updateQuantity(variantId, newQuantity)
    }

    fun removeItem(variantId: String) {
        cartRepository.removeItem(variantId)
    }

    fun clearCart() {
        cartRepository.clearCart()
    }
}
