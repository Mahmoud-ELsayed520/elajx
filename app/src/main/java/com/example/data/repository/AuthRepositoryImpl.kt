package com.example.data.repository

import com.example.core.config.AppEnvironment
import com.example.core.config.EnvironmentConfig
import com.example.core.result.AppError
import com.example.core.result.AppResult
import com.example.core.security.SecureLogger
import com.example.core.util.EgyptianPhoneUtil
import com.example.data.local.AuthSessionStorage
import com.example.data.local.InMemoryAuthSessionStorage
import com.example.data.remote.SupabaseBoundary
import com.example.domain.model.AuthSession
import com.example.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Production implementation of AuthRepository with real Supabase Auth integration.
 *
 * Adheres strictly to Phase 2 remediation requirements:
 * 1. REAL_AUTH_REQUIRED by default across all environments.
 * 2. Simulation mode is strictly guarded and permissible ONLY in AppEnvironment.DEVELOPMENT
 *    when isSimulationModeAllowed == true.
 * 3. NO AUTOMATIC FALLBACK to simulation when real authentication fails.
 * 4. Local storage alone NEVER establishes an authenticated Supabase session.
 * 5. Real authenticated sessions use the verified Supabase user UUID, never synthetic usr_... IDs.
 * 6. Zero logging of sensitive credentials, tokens, or OTP codes.
 */
class AuthRepositoryImpl(
    val config: EnvironmentConfig = EnvironmentConfig.current(),
    val supabaseBoundary: SupabaseBoundary = SupabaseBoundary(config),
    val sessionStorage: AuthSessionStorage = InMemoryAuthSessionStorage()
) : AuthRepository {

    private val tag = "AuthRepositoryImpl"
    private val _sessionState = MutableStateFlow<AuthSession>(AuthSession.Unauthenticated)
    override val sessionState: StateFlow<AuthSession> = _sessionState.asStateFlow()

    private var lastOtpSentTimeMillis: Long = 0L
    private val cooldownSeconds = 60

    /**
     * Security Invariant: Simulation is permitted ONLY for AppEnvironment.DEVELOPMENT
     * when explicitly enabled in configuration.
     */
    fun canUseSimulation(): Boolean {
        return config.environment == AppEnvironment.DEVELOPMENT && config.isSimulationModeAllowed
    }

    override suspend fun checkSession(): AppResult<AuthSession> {
        SecureLogger.d(tag, "Checking session state for ${config.environment}...")

        if (canUseSimulation()) {
            SecureLogger.d(tag, "[DEV SIMULATION] Session check returning in-memory state")
            return AppResult.Success(_sessionState.value)
        }

        // REAL AUTH:
        // Local data alone must NOT establish an authenticated Supabase session.
        val savedToken = sessionStorage.getAccessToken()
        if (savedToken.isNullOrBlank()) {
            SecureLogger.d(tag, "No persisted session token found. Returning Unauthenticated.")
            _sessionState.value = AuthSession.Unauthenticated
            return AppResult.Success(AuthSession.Unauthenticated)
        }

        // Validate persisted token against Supabase Auth GET /auth/v1/user
        SecureLogger.d(tag, "Validating persisted token against Supabase Auth...")
        when (val result = supabaseBoundary.getCurrentUser(savedToken)) {
            is AppResult.Success -> {
                val user = result.data
                val fullName = user.userMetadata?.fullName
                    ?: user.userMetadata?.name
                    ?: sessionStorage.getFullName()
                    ?: "مريض عِلاجِك"

                val authenticatedSession = AuthSession.Authenticated(
                    userId = user.id, // Real Supabase user UUID
                    phone = user.phone ?: sessionStorage.getPhone().orEmpty(),
                    fullName = fullName,
                    locale = user.userMetadata?.locale ?: "ar"
                )
                _sessionState.value = authenticatedSession
                SecureLogger.i(tag, "Persisted session successfully validated with Supabase Auth.")
                return AppResult.Success(authenticatedSession)
            }
            is AppResult.Error -> {
                SecureLogger.w(tag, "Persisted session validation failed: ${result.error.message}. Clearing local session.")
                sessionStorage.clearSession()
                _sessionState.value = AuthSession.Expired
                return AppResult.Error(result.error)
            }
        }
    }

    override fun getRemainingResendCooldown(): Int {
        val elapsed = (System.currentTimeMillis() - lastOtpSentTimeMillis) / 1000
        val remaining = (cooldownSeconds - elapsed).toInt()
        return if (remaining > 0) remaining else 0
    }

    override suspend fun sendOtp(rawPhone: String): AppResult<Unit> {
        val phoneResult = EgyptianPhoneUtil.normalize(rawPhone)
        if (phoneResult is AppResult.Error) {
            return phoneResult
        }
        val normalized = (phoneResult as AppResult.Success).data

        val remainingCooldown = getRemainingResendCooldown()
        if (remainingCooldown > 0) {
            return AppResult.Error(AppError.RateLimitedError(remainingCooldown))
        }

        if (canUseSimulation()) {
            SecureLogger.i(tag, "[DEV SIMULATION] OTP requested for phone: $normalized")
            lastOtpSentTimeMillis = System.currentTimeMillis()
            return AppResult.Success(Unit)
        }

        // REAL AUTH:
        SecureLogger.i(tag, "Initiating real Supabase Auth OTP request for phone: $normalized")
        val result = supabaseBoundary.requestOtp(normalized)
        if (result is AppResult.Success) {
            lastOtpSentTimeMillis = System.currentTimeMillis()
            SecureLogger.i(tag, "Real Supabase Auth OTP request succeeded.")
        } else {
            SecureLogger.w(tag, "Real Supabase Auth OTP request failed: ${(result as AppResult.Error).error.message}")
            // CRITICAL: NEVER automatically fall back to simulation!
        }
        return result
    }

    override suspend fun verifyOtp(rawPhone: String, otpToken: String): AppResult<AuthSession.Authenticated> {
        val phoneResult = EgyptianPhoneUtil.normalize(rawPhone)
        if (phoneResult is AppResult.Error) {
            return AppResult.Error(phoneResult.error)
        }
        val normalized = (phoneResult as AppResult.Success).data

        if (!EgyptianPhoneUtil.isValidOtp(otpToken)) {
            return AppResult.Error(AppError.ValidationError("OTP code must be 6 digits", "otp"))
        }

        if (canUseSimulation()) {
            SecureLogger.i(tag, "[DEV SIMULATION] Verifying OTP in explicit development simulation mode for $normalized")
            // Documented simulation rule tracked as TBD per Section 6.
            // Explicitly identified as simulated to prevent confusion with real identity:
            val simulatedSession = AuthSession.Authenticated(
                userId = "sim_dev_" + normalized.takeLast(8),
                phone = normalized,
                fullName = "مريض تجريبي (محاكاة)",
                locale = "ar"
            )
            _sessionState.value = simulatedSession
            return AppResult.Success(simulatedSession)
        }

        // REAL AUTH:
        SecureLogger.i(tag, "Initiating real Supabase Auth OTP verification for phone: $normalized")
        when (val result = supabaseBoundary.verifyOtp(normalized, otpToken)) {
            is AppResult.Success -> {
                val authResponse = result.data
                val user = authResponse.user
                    ?: return AppResult.Error(AppError.ServerError(500, "Supabase Auth returned null user payload"))

                val fullName = authResponse.user.userMetadata?.fullName
                    ?: authResponse.user.userMetadata?.name
                    ?: "مريض عِلاجِك"

                val authenticatedSession = AuthSession.Authenticated(
                    userId = user.id, // Real Supabase user UUID
                    phone = user.phone ?: normalized,
                    fullName = fullName,
                    locale = authResponse.user.userMetadata?.locale ?: "ar"
                )

                // Persist session tokens for session restoration
                authResponse.accessToken?.let { token ->
                    sessionStorage.saveSession(
                        accessToken = token,
                        refreshToken = authResponse.refreshToken,
                        userId = user.id,
                        phone = user.phone ?: normalized,
                        fullName = fullName
                    )
                }

                _sessionState.value = authenticatedSession
                SecureLogger.i(tag, "Patient session successfully authenticated with real Supabase Auth.")
                return AppResult.Success(authenticatedSession)
            }
            is AppResult.Error -> {
                SecureLogger.w(tag, "Real Supabase Auth verification failed: ${result.error.message}")
                // CRITICAL: NEVER automatically fall back to simulation!
                return AppResult.Error(result.error)
            }
        }
    }

    override suspend fun logout(): AppResult<Unit> {
        SecureLogger.d(tag, "Logging out user...")
        val token = sessionStorage.getAccessToken()
        if (!token.isNullOrBlank() && !canUseSimulation()) {
            try {
                supabaseBoundary.logout(token)
            } catch (e: Exception) {
                SecureLogger.w(tag, "Remote logout network call error: ${e.message}")
            }
        }
        sessionStorage.clearSession()
        _sessionState.value = AuthSession.Unauthenticated
        return AppResult.Success(Unit)
    }

    override fun canCreateOrder(): Boolean {
        return _sessionState.value is AuthSession.Authenticated
    }

    override fun canSavePrescription(): Boolean {
        return _sessionState.value is AuthSession.Authenticated
    }

    /**
     * Explicit simulation helper for development verification.
     * Guarded: strictly rejected if simulation is not explicitly allowed.
     */
    fun toggleMockAuthForVerification(): AppResult<Unit> {
        if (!canUseSimulation()) {
            SecureLogger.w(tag, "Simulation toggle rejected: simulation is forbidden in ${config.environment.name}")
            return AppResult.Error(
                AppError.AuthorizationError("Simulation mode is strictly prohibited in ${config.environment.name} environment.")
            )
        }

        if (_sessionState.value is AuthSession.Authenticated) {
            _sessionState.value = AuthSession.Unauthenticated
            SecureLogger.d(tag, "[DEV SIMULATION] Session toggled to Unauthenticated")
        } else {
            _sessionState.value = AuthSession.Authenticated(
                userId = "sim_dev_toggle_001",
                phone = "+201012345678",
                fullName = "مريض تجريبي (محاكاة)",
                locale = "ar"
            )
            SecureLogger.d(tag, "[DEV SIMULATION] Session toggled to Authenticated: sim_dev_toggle_001")
        }
        return AppResult.Success(Unit)
    }

    companion object {
        /**
         * Documented development simulation OTP rule is TBD (tracked in Phase 2 Remediation Report).
         * Per specification (Section 6), arbitrary fake OTPs such as 123456 are forbidden in production.
         */
        const val SIMULATION_OTP_RULE_TRACKED_TBD = "SIMULATION_OTP_RULE_TRACKED_TBD"
    }
}
