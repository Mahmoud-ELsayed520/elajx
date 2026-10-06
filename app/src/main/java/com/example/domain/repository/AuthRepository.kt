package com.example.domain.repository

import com.example.core.result.AppResult
import com.example.domain.model.AuthSession
import kotlinx.coroutines.flow.StateFlow

/**
 * Domain boundary for Authentication & Session management.
 * Presentation layer interacts only with this interface, never with raw networking or tokens.
 */
interface AuthRepository {
    val sessionState: StateFlow<AuthSession>

    suspend fun checkSession(): AppResult<AuthSession>
    suspend fun sendOtp(rawPhone: String): AppResult<Unit>
    suspend fun verifyOtp(rawPhone: String, otpToken: String): AppResult<AuthSession.Authenticated>
    suspend fun logout(): AppResult<Unit>

    fun getRemainingResendCooldown(): Int

    /**
     * Business validation rule:
     * Orders and prescriptions can ONLY be created by authenticated users.
     */
    fun canCreateOrder(): Boolean
    fun canSavePrescription(): Boolean
}
