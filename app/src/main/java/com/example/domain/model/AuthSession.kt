package com.example.domain.model

/**
 * Domain representation of user authentication session.
 *
 * Mapped to 01_PRODUCT_SPEC.md (Section 6) and 14_STATE_AND_EDGE_CASES.md (Section 5):
 * - Unauthenticated users can browse and search catalog.
 * - Authenticated users are strictly required for saving prescriptions, addresses, and creating orders.
 * - Guest checkout / order creation is prohibited.
 */
sealed class AuthSession {
    object Loading : AuthSession()
    object Unauthenticated : AuthSession()
    data class Authenticated(
        val userId: String,
        val phone: String,
        val fullName: String,
        val locale: String = "ar"
    ) : AuthSession()
    object Expired : AuthSession()
    data class Error(val message: String) : AuthSession()

    val isAuthenticated: Boolean get() = this is Authenticated
}

/**
 * Basic patient profile representation.
 */
data class UserProfile(
    val id: String,
    val phone: String,
    val fullName: String,
    val locale: String = "ar"
)

/**
 * Architectural verification item representation for the bootstrap screen.
 */
data class BoundaryCheck(
    val id: String,
    val title: String,
    val description: String,
    val isVerified: Boolean,
    val specificationRef: String
)
