package com.example.data.remote

import com.example.core.config.EnvironmentConfig
import com.example.core.result.AppResult
import com.example.core.security.SecureLogger
import com.example.data.remote.model.SupabaseAuthResponse
import com.example.data.remote.model.SupabaseUserResponse

/**
 * Supabase Client Boundary.
 *
 * Implements the architecture constraints from 03_TECH_ARCHITECTURE.md & 13_API_CONTRACT.md:
 * 1. The Android client is an UNTRUSTED CLIENT.
 * 2. Only public/client-safe endpoints and the anonymous key are exposed.
 * 3. SUPABASE_SERVICE_ROLE_KEY is strictly FORBIDDEN and must NEVER be present.
 * 4. Authorization decisions and transactional mutations are authoritative on the backend (RLS + Postgres).
 * 5. Supabase Auth REST endpoints:
 *    - POST /auth/v1/otp
 *    - POST /auth/v1/verify
 *    - GET /auth/v1/user
 *    - POST /auth/v1/logout
 */
class SupabaseBoundary(
    private val config: EnvironmentConfig = EnvironmentConfig.current(),
    private val authClient: SupabaseAuthClient = RealSupabaseAuthClient()
) {
    private val tag = "SupabaseBoundary"

    init {
        SecureLogger.d(tag, "SupabaseBoundary initialized for ${config.environment}")
    }

    val endpointUrl: String get() = config.supabaseUrl

    /**
     * Confirms that no privileged service role key is present in client configuration.
     */
    fun isServiceRoleForbiddenAndSafe(): Boolean {
        return !config.supabaseAnonKey.contains("service_role", ignoreCase = true)
    }

    /**
     * Returns standard client-safe headers for Supabase public & authenticated requests.
     */
    fun getClientSafeHeaders(userToken: String? = null): Map<String, String> {
        val headers = mutableMapOf(
            "apikey" to config.supabaseAnonKey,
            "Content-Type" to "application/json"
        )
        if (!userToken.isNullOrBlank()) {
            headers["Authorization"] = "Bearer $userToken"
        } else {
            headers["Authorization"] = "Bearer ${config.supabaseAnonKey}"
        }
        return headers
    }

    /**
     * Requests an SMS OTP from Supabase Auth: POST /auth/v1/otp
     */
    suspend fun requestOtp(normalizedPhone: String): AppResult<Unit> {
        val headers = getClientSafeHeaders()
        return authClient.requestOtp(
            baseUrl = config.supabaseUrl,
            headers = headers,
            phone = normalizedPhone
        )
    }

    /**
     * Verifies an SMS OTP with Supabase Auth: POST /auth/v1/verify
     */
    suspend fun verifyOtp(normalizedPhone: String, otpToken: String): AppResult<SupabaseAuthResponse> {
        val headers = getClientSafeHeaders()
        return authClient.verifyOtp(
            baseUrl = config.supabaseUrl,
            headers = headers,
            phone = normalizedPhone,
            token = otpToken
        )
    }

    /**
     * Validates and retrieves current authenticated user session from Supabase Auth: GET /auth/v1/user
     */
    suspend fun getCurrentUser(accessToken: String): AppResult<SupabaseUserResponse> {
        val headers = getClientSafeHeaders(userToken = accessToken)
        return authClient.getCurrentUser(
            baseUrl = config.supabaseUrl,
            headers = headers
        )
    }

    /**
     * Logs out the user session from Supabase Auth: POST /auth/v1/logout
     */
    suspend fun logout(accessToken: String): AppResult<Unit> {
        val headers = getClientSafeHeaders(userToken = accessToken)
        return authClient.logout(
            baseUrl = config.supabaseUrl,
            headers = headers
        )
    }
}
