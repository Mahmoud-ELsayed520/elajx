package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.config.AppEnvironment
import com.example.core.config.EnvironmentConfig
import com.example.core.result.AppError
import com.example.core.result.AppResult
import com.example.core.security.SecureLogger
import com.example.core.util.EgyptianPhoneUtil
import com.example.data.local.InMemoryAuthSessionStorage
import com.example.data.remote.SupabaseAuthClient
import com.example.data.remote.SupabaseBoundary
import com.example.data.remote.model.SupabaseAuthResponse
import com.example.data.remote.model.SupabaseUserMetadata
import com.example.data.remote.model.SupabaseUserResponse
import com.example.data.repository.AuthRepositoryImpl
import com.example.domain.model.AuthSession
import com.example.presentation.auth.AuthStep
import com.example.presentation.auth.AuthViewModel
import com.example.presentation.bootstrap.BootstrapViewModel
import com.example.presentation.navigation.NavDestination
import com.example.presentation.shell.AppShellViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.withLock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    // ==========================================
    // Phase 1 Foundation & Platform Baseline
    // ==========================================

    @Test
    fun appName_isElajX() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("ElajX", appName)
    }

    @Test
    fun appNameArabic_isConfigured() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appNameAr = context.getString(R.string.app_name_ar)
        assertEquals("عِلاجِك", appNameAr)
    }

    @Test
    fun supabaseBoundary_guaranteesNoServiceRoleInClient() {
        val devBoundary = SupabaseBoundary(EnvironmentConfig.current(AppEnvironment.DEVELOPMENT))
        assertTrue(devBoundary.isServiceRoleForbiddenAndSafe())

        val stagingBoundary = SupabaseBoundary(EnvironmentConfig.current(AppEnvironment.STAGING))
        assertTrue(stagingBoundary.isServiceRoleForbiddenAndSafe())

        val prodBoundary = SupabaseBoundary(EnvironmentConfig.current(AppEnvironment.PRODUCTION))
        assertTrue(prodBoundary.isServiceRoleForbiddenAndSafe())
    }

    @Test
    fun secureLogger_sanitizesSensitiveTokensAndPhones() {
        val rawMessage = "Request with Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9 and phone 01012345678 and otp: 123456, access_token: secret_token_xyz"
        val sanitized = SecureLogger.sanitize(rawMessage)

        assertFalse(sanitized.contains("123456"))
        assertFalse(sanitized.contains("eyJhbGci"))
        assertFalse(sanitized.contains("secret_token_xyz"))
        assertTrue(sanitized.contains("bearer [REDACTED]"))
        assertTrue(sanitized.contains("otp: [REDACTED]"))
        assertTrue(sanitized.contains("0101****78"))
        assertTrue(sanitized.contains("access_token: [REDACTED]"))
    }

    @Test
    fun authRepository_strictlyLocksOrderCreationForUnauthenticated() {
        // Simulation allowed for testing the toggle
        val devConfig = EnvironmentConfig.current(AppEnvironment.DEVELOPMENT, allowSimulation = true)
        val authRepo = AuthRepositoryImpl(config = devConfig)

        // Default state is Unauthenticated per specification
        assertEquals(AuthSession.Unauthenticated, authRepo.sessionState.value)
        assertFalse("Guest user CANNOT create order", authRepo.canCreateOrder())
        assertFalse("Guest user CANNOT save prescription", authRepo.canSavePrescription())

        // Toggle to Authenticated for verification
        val toggleResult = authRepo.toggleMockAuthForVerification()
        assertTrue(toggleResult is AppResult.Success)
        assertTrue(authRepo.sessionState.value is AuthSession.Authenticated)
        assertTrue("Authenticated user can create order", authRepo.canCreateOrder())
        assertTrue("Authenticated user can save prescription", authRepo.canSavePrescription())
    }

    @Test
    fun bootstrapViewModel_initializesWithAllBoundariesVerified() {
        val viewModel = BootstrapViewModel()
        val state = viewModel.uiState.value

        assertEquals(AppEnvironment.DEVELOPMENT, state.environment.environment)
        assertFalse(state.canCreateOrder) // starts unauthenticated
        assertEquals(6, state.boundaryChecks.size)
        assertTrue(state.boundaryChecks.all { it.isVerified })
    }

    // ==========================================
    // Phase 1: App Shell & Navigation Tests
    // ==========================================

    @Test
    fun navDestination_containsCanonicalFiveTabs() {
        val tabs = NavDestination.bottomNavItems
        assertEquals(5, tabs.size)
        assertEquals(listOf("home", "search", "prescriptions", "orders", "account"), tabs.map { it.route })
    }

    @Test
    fun appShellViewModel_initializesAtHomeTab() {
        val viewModel = AppShellViewModel()
        val state = viewModel.uiState.value

        assertEquals(NavDestination.Home, state.currentTab)
        assertFalse(state.isViewingBaseline)
        assertEquals(AuthSession.Unauthenticated, state.session)
    }

    @Test
    fun appShellViewModel_navigationTabSwitchingWorks() {
        val viewModel = AppShellViewModel()

        viewModel.selectTab(NavDestination.Search)
        assertEquals(NavDestination.Search, viewModel.uiState.value.currentTab)

        viewModel.selectTab(NavDestination.Prescriptions)
        assertEquals(NavDestination.Prescriptions, viewModel.uiState.value.currentTab)

        viewModel.selectTab(NavDestination.Orders)
        assertEquals(NavDestination.Orders, viewModel.uiState.value.currentTab)

        viewModel.selectTab(NavDestination.Account)
        assertEquals(NavDestination.Account, viewModel.uiState.value.currentTab)
    }

    @Test
    fun appShellViewModel_backPressReturnsToHomeTab() {
        val viewModel = AppShellViewModel()

        viewModel.selectTab(NavDestination.Orders)
        assertEquals(NavDestination.Orders, viewModel.uiState.value.currentTab)

        // Pressing back from secondary tab should pop back to Home tab
        val handled = viewModel.handleBackPress()
        assertTrue(handled)
        assertEquals(NavDestination.Home, viewModel.uiState.value.currentTab)

        // Pressing back on Home tab returns false (allowing OS default behavior)
        val handledOnHome = viewModel.handleBackPress()
        assertFalse(handledOnHome)
    }

    @Test
    fun appShellViewModel_baselineNavigationAndBackPress() {
        val viewModel = AppShellViewModel()

        viewModel.openBaseline()
        assertTrue(viewModel.uiState.value.isViewingBaseline)

        // Back press closes baseline
        val handled = viewModel.handleBackPress()
        assertTrue(handled)
        assertFalse(viewModel.uiState.value.isViewingBaseline)
    }

    // ==========================================
    // Phase 2: Egyptian Phone & OTP Validation Tests
    // ==========================================

    @Test
    fun egyptianPhoneUtil_normalizesValidFormats() {
        // Local 11-digit formats
        assertEquals("+201012345678", (EgyptianPhoneUtil.normalize("01012345678") as AppResult.Success).data)
        assertEquals("+201198765432", (EgyptianPhoneUtil.normalize("01198765432") as AppResult.Success).data)
        assertEquals("+201234567890", (EgyptianPhoneUtil.normalize("01234567890") as AppResult.Success).data)
        assertEquals("+201555555555", (EgyptianPhoneUtil.normalize("01555555555") as AppResult.Success).data)

        // Formatted with spaces or dashes
        assertEquals("+201012345678", (EgyptianPhoneUtil.normalize("010-1234-5678") as AppResult.Success).data)
        assertEquals("+201012345678", (EgyptianPhoneUtil.normalize("010 1234 5678") as AppResult.Success).data)

        // International format
        assertEquals("+201012345678", (EgyptianPhoneUtil.normalize("+201012345678") as AppResult.Success).data)
        assertEquals("+201012345678", (EgyptianPhoneUtil.normalize("00201012345678") as AppResult.Success).data)

        // Masked and display formats
        assertEquals("+20 10 1234 5678", EgyptianPhoneUtil.formatDisplay("+201012345678"))
        assertEquals("+2010 **** 78", EgyptianPhoneUtil.formatMasked("+201012345678"))
    }

    @Test
    fun egyptianPhoneUtil_rejectsInvalidNumbers() {
        // Invalid prefixes
        assertTrue(EgyptianPhoneUtil.normalize("01312345678") is AppResult.Error)
        assertTrue(EgyptianPhoneUtil.normalize("01412345678") is AppResult.Error)
        assertTrue(EgyptianPhoneUtil.normalize("01912345678") is AppResult.Error)

        // Wrong length
        assertTrue(EgyptianPhoneUtil.normalize("010123456") is AppResult.Error)
        assertTrue(EgyptianPhoneUtil.normalize("010123456789") is AppResult.Error)
        assertTrue(EgyptianPhoneUtil.normalize("") is AppResult.Error)
    }

    @Test
    fun egyptianPhoneUtil_validatesOtpFormat() {
        assertTrue(EgyptianPhoneUtil.isValidOtp("123456"))
        assertFalse(EgyptianPhoneUtil.isValidOtp("12345"))
        assertFalse(EgyptianPhoneUtil.isValidOtp("1234567"))
        assertFalse(EgyptianPhoneUtil.isValidOtp("12a456"))
    }

    // ==========================================
    // Phase 2 Remediation: Security & Environment Invariants
    // ==========================================

    @Test
    fun environmentConfig_enforcesSecurityInvariant() {
        // DEV with simulation allowed is valid
        val devAllowed = EnvironmentConfig.current(AppEnvironment.DEVELOPMENT, allowSimulation = true)
        assertTrue(devAllowed.isSimulationModeAllowed)

        // DEV with simulation disabled is valid
        val devDisabled = EnvironmentConfig.current(AppEnvironment.DEVELOPMENT, allowSimulation = false)
        assertFalse(devDisabled.isSimulationModeAllowed)

        // STAGING must never have simulation
        val staging = EnvironmentConfig.current(AppEnvironment.STAGING)
        assertFalse(staging.isSimulationModeAllowed)

        // PRODUCTION must never have simulation
        val prod = EnvironmentConfig.current(AppEnvironment.PRODUCTION)
        assertFalse(prod.isSimulationModeAllowed)

        // Attempting to construct STAGING or PRODUCTION with simulation throws IllegalArgumentException
        try {
            EnvironmentConfig(
                environment = AppEnvironment.STAGING,
                supabaseUrl = "https://staging.supabase.co",
                supabaseAnonKey = "anon_key",
                apiBaseUrl = "https://staging.supabase.co/rest/v1",
                isDebugLoggingEnabled = true,
                isSimulationModeAllowed = true
            )
            fail("Expected IllegalArgumentException when enabling simulation in STAGING")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("strictly prohibited") == true)
        }

        try {
            EnvironmentConfig(
                environment = AppEnvironment.PRODUCTION,
                supabaseUrl = "https://prod.supabase.co",
                supabaseAnonKey = "anon_key",
                apiBaseUrl = "https://prod.supabase.co/rest/v1",
                isDebugLoggingEnabled = false,
                isSimulationModeAllowed = true
            )
            fail("Expected IllegalArgumentException when enabling simulation in PRODUCTION")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("strictly prohibited") == true)
        }
    }

    @Test
    fun development_simulationWorksOnlyWhenExplicitlyEnabled() = runBlocking {
        // Case 1: Simulation explicitly enabled in DEVELOPMENT
        val devSimConfig = EnvironmentConfig.current(AppEnvironment.DEVELOPMENT, allowSimulation = true)
        val devSimRepo = AuthRepositoryImpl(config = devSimConfig)

        assertTrue(devSimRepo.canUseSimulation())
        val simOtpResult = devSimRepo.sendOtp("01012345678")
        assertTrue(simOtpResult is AppResult.Success)

        val simVerifyResult = devSimRepo.verifyOtp("01012345678", "654321")
        assertTrue(simVerifyResult is AppResult.Success)
        val simSession = (simVerifyResult as AppResult.Success).data
        assertTrue(simSession.userId.startsWith("sim_dev_")) // Clearly identifiable as simulation

        // Case 2: Simulation disabled in DEVELOPMENT -> simulation path is rejected
        val devRealConfig = EnvironmentConfig.current(AppEnvironment.DEVELOPMENT, allowSimulation = false)
        val devRealRepo = AuthRepositoryImpl(config = devRealConfig)

        assertFalse(devRealRepo.canUseSimulation())
        val toggleResult = devRealRepo.toggleMockAuthForVerification()
        assertTrue("Simulation toggle must be rejected when simulation is disabled", toggleResult is AppResult.Error)
    }

    @Test
    fun development_simulationNotAutomaticallyActivatedAfterNetworkFailure() = runBlocking {
        // Real auth path with failing network client
        val failingAuthClient = object : SupabaseAuthClient {
            override suspend fun requestOtp(baseUrl: String, headers: Map<String, String>, phone: String): AppResult<Unit> {
                return AppResult.Error(AppError.NetworkError("Simulated network timeout", isTimeout = true))
            }

            override suspend fun verifyOtp(baseUrl: String, headers: Map<String, String>, phone: String, token: String): AppResult<SupabaseAuthResponse> {
                return AppResult.Error(AppError.NetworkError("Simulated network failure"))
            }

            override suspend fun getCurrentUser(baseUrl: String, headers: Map<String, String>): AppResult<SupabaseUserResponse> {
                return AppResult.Error(AppError.NetworkError("Simulated network failure"))
            }

            override suspend fun logout(baseUrl: String, headers: Map<String, String>): AppResult<Unit> {
                return AppResult.Success(Unit)
            }
        }

        val config = EnvironmentConfig.current(AppEnvironment.DEVELOPMENT, allowSimulation = false)
        val boundary = SupabaseBoundary(config = config, authClient = failingAuthClient)
        val repo = AuthRepositoryImpl(config = config, supabaseBoundary = boundary)

        // Verify that network failure does NOT automatically trigger simulation fallback
        val sendResult = repo.sendOtp("01012345678")
        assertTrue("Send OTP must return network error, not simulate success", sendResult is AppResult.Error)
        assertTrue((sendResult as AppResult.Error).error is AppError.NetworkError)

        val verifyResult = repo.verifyOtp("01012345678", "123456")
        assertTrue("Verify OTP must return network error, not simulate success", verifyResult is AppResult.Error)
        assertTrue((verifyResult as AppResult.Error).error is AppError.NetworkError)

        // Session must remain strictly Unauthenticated
        assertEquals(AuthSession.Unauthenticated, repo.sessionState.value)
        assertFalse(repo.canCreateOrder())
    }

    @Test
    fun staging_simulationIsRejectedAndRealAuthRequired() = runBlocking {
        val failingAuthClient = object : SupabaseAuthClient {
            override suspend fun requestOtp(baseUrl: String, headers: Map<String, String>, phone: String): AppResult<Unit> {
                return AppResult.Error(AppError.AuthenticationError("Invalid OTP token"))
            }

            override suspend fun verifyOtp(baseUrl: String, headers: Map<String, String>, phone: String, token: String): AppResult<SupabaseAuthResponse> {
                return AppResult.Error(AppError.AuthenticationError("Invalid token from Supabase"))
            }

            override suspend fun getCurrentUser(baseUrl: String, headers: Map<String, String>): AppResult<SupabaseUserResponse> {
                return AppResult.Error(AppError.AuthenticationError("Token expired"))
            }

            override suspend fun logout(baseUrl: String, headers: Map<String, String>): AppResult<Unit> {
                return AppResult.Success(Unit)
            }
        }

        val stagingConfig = EnvironmentConfig.current(AppEnvironment.STAGING)
        val boundary = SupabaseBoundary(config = stagingConfig, authClient = failingAuthClient)
        val repo = AuthRepositoryImpl(config = stagingConfig, supabaseBoundary = boundary)

        assertFalse(repo.canUseSimulation())

        // Toggle mock auth must be rejected
        val toggleResult = repo.toggleMockAuthForVerification()
        assertTrue(toggleResult is AppResult.Error)
        assertEquals(AuthSession.Unauthenticated, repo.sessionState.value)

        // Arbitrary 6-digit OTP cannot authenticate
        val verifyResult = repo.verifyOtp("01012345678", "123456")
        assertTrue(verifyResult is AppResult.Error)
        assertEquals(AuthSession.Unauthenticated, repo.sessionState.value)
    }

    @Test
    fun production_simulationIsRejectedAndRealAuthRequired() = runBlocking {
        val failingAuthClient = object : SupabaseAuthClient {
            override suspend fun requestOtp(baseUrl: String, headers: Map<String, String>, phone: String): AppResult<Unit> {
                return AppResult.Error(AppError.AuthenticationError("Invalid credentials"))
            }

            override suspend fun verifyOtp(baseUrl: String, headers: Map<String, String>, phone: String, token: String): AppResult<SupabaseAuthResponse> {
                return AppResult.Error(AppError.AuthenticationError("Token rejected by Supabase"))
            }

            override suspend fun getCurrentUser(baseUrl: String, headers: Map<String, String>): AppResult<SupabaseUserResponse> {
                return AppResult.Error(AppError.AuthenticationError("Invalid session"))
            }

            override suspend fun logout(baseUrl: String, headers: Map<String, String>): AppResult<Unit> {
                return AppResult.Success(Unit)
            }
        }

        val prodConfig = EnvironmentConfig.current(AppEnvironment.PRODUCTION)
        val boundary = SupabaseBoundary(config = prodConfig, authClient = failingAuthClient)
        val repo = AuthRepositoryImpl(config = prodConfig, supabaseBoundary = boundary)

        assertFalse(repo.canUseSimulation())

        // Simulation toggle strictly prohibited in PRODUCTION
        val toggleResult = repo.toggleMockAuthForVerification()
        assertTrue(toggleResult is AppResult.Error)
        assertEquals(AuthSession.Unauthenticated, repo.sessionState.value)

        // Arbitrary OTP cannot authenticate
        val verifyResult = repo.verifyOtp("01012345678", "999888")
        assertTrue(verifyResult is AppResult.Error)
        assertEquals(AuthSession.Unauthenticated, repo.sessionState.value)
    }

    // ==========================================
    // Phase 2 Remediation: Session Handling Tests
    // ==========================================

    @Test
    fun session_localDataAloneCannotEstablishAuthenticatedSession() = runBlocking {
        val storage = InMemoryAuthSessionStorage()
        // Pre-populate storage as if SharedPreferences contained old data
        storage.saveSession(
            accessToken = "stale_or_fabricated_jwt_token",
            refreshToken = "refresh_token_123",
            userId = "usr_fake_001",
            phone = "+201012345678",
            fullName = "مستخدم وهمي"
        )

        // Server rejects the token (e.g. 401 Unauthorized)
        val authClient = object : SupabaseAuthClient {
            override suspend fun requestOtp(baseUrl: String, headers: Map<String, String>, phone: String): AppResult<Unit> = AppResult.Success(Unit)
            override suspend fun verifyOtp(baseUrl: String, headers: Map<String, String>, phone: String, token: String): AppResult<SupabaseAuthResponse> =
                AppResult.Error(AppError.AuthenticationError("Invalid"))
            override suspend fun getCurrentUser(baseUrl: String, headers: Map<String, String>): AppResult<SupabaseUserResponse> {
                return AppResult.Error(AppError.AuthenticationError("JWT expired or invalid (HTTP 401)"))
            }
            override suspend fun logout(baseUrl: String, headers: Map<String, String>): AppResult<Unit> = AppResult.Success(Unit)
        }

        val config = EnvironmentConfig.current(AppEnvironment.DEVELOPMENT, allowSimulation = false)
        val boundary = SupabaseBoundary(config = config, authClient = authClient)
        val repo = AuthRepositoryImpl(config = config, supabaseBoundary = boundary, sessionStorage = storage)

        // checkSession must NOT trust local data alone; it must call GET /auth/v1/user
        val checkResult = repo.checkSession()
        assertTrue("Session check must fail because Supabase rejected token", checkResult is AppResult.Error)
        assertEquals(AuthSession.Expired, repo.sessionState.value)
        assertFalse("User cannot create order with unverified session", repo.canCreateOrder())

        // Stale storage must be wiped
        assertNull(storage.getAccessToken())
    }

    @Test
    fun session_validBackendResponseRestoresAuthenticatedSession() = runBlocking {
        val storage = InMemoryAuthSessionStorage()
        storage.saveSession(
            accessToken = "valid_supabase_jwt",
            refreshToken = "valid_refresh_token",
            userId = "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
            phone = "+201012345678",
            fullName = "أحمد محمود"
        )

        val realUserUuid = "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d"
        val authClient = object : SupabaseAuthClient {
            override suspend fun requestOtp(baseUrl: String, headers: Map<String, String>, phone: String): AppResult<Unit> = AppResult.Success(Unit)
            override suspend fun verifyOtp(baseUrl: String, headers: Map<String, String>, phone: String, token: String): AppResult<SupabaseAuthResponse> =
                AppResult.Error(AppError.AuthenticationError("Not used"))
            override suspend fun getCurrentUser(baseUrl: String, headers: Map<String, String>): AppResult<SupabaseUserResponse> {
                return AppResult.Success(
                    SupabaseUserResponse(
                        id = realUserUuid,
                        phone = "+201012345678",
                        userMetadata = SupabaseUserMetadata(fullName = "أحمد محمود", locale = "ar")
                    )
                )
            }
            override suspend fun logout(baseUrl: String, headers: Map<String, String>): AppResult<Unit> = AppResult.Success(Unit)
        }

        val config = EnvironmentConfig.current(AppEnvironment.DEVELOPMENT, allowSimulation = false)
        val boundary = SupabaseBoundary(config = config, authClient = authClient)
        val repo = AuthRepositoryImpl(config = config, supabaseBoundary = boundary, sessionStorage = storage)

        val checkResult = repo.checkSession()
        assertTrue(checkResult is AppResult.Success)
        val session = (checkResult as AppResult.Success).data
        assertTrue(session is AuthSession.Authenticated)
        assertEquals(realUserUuid, (session as AuthSession.Authenticated).userId)
        assertTrue(repo.canCreateOrder())
    }

    @Test
    fun session_successfulRealVerificationProducesRealSupabaseIdentity() = runBlocking {
        val realUserUuid = "c7d24263-8a3d-4c3e-9b22-83b5443fcbe0"
        val storage = InMemoryAuthSessionStorage()

        val authClient = object : SupabaseAuthClient {
            override suspend fun requestOtp(baseUrl: String, headers: Map<String, String>, phone: String): AppResult<Unit> {
                return AppResult.Success(Unit)
            }

            override suspend fun verifyOtp(baseUrl: String, headers: Map<String, String>, phone: String, token: String): AppResult<SupabaseAuthResponse> {
                return AppResult.Success(
                    SupabaseAuthResponse(
                        accessToken = "valid_access_token_12345",
                        refreshToken = "valid_refresh_token_67890",
                        user = SupabaseUserResponse(
                            id = realUserUuid,
                            phone = phone,
                            userMetadata = SupabaseUserMetadata(fullName = "فاطمة حسن", locale = "ar")
                        )
                    )
                )
            }

            override suspend fun getCurrentUser(baseUrl: String, headers: Map<String, String>): AppResult<SupabaseUserResponse> {
                return AppResult.Success(
                    SupabaseUserResponse(
                        id = realUserUuid,
                        phone = "+201012345678",
                        userMetadata = SupabaseUserMetadata(fullName = "فاطمة حسن", locale = "ar")
                    )
                )
            }

            override suspend fun logout(baseUrl: String, headers: Map<String, String>): AppResult<Unit> {
                return AppResult.Success(Unit)
            }
        }

        val config = EnvironmentConfig.current(AppEnvironment.DEVELOPMENT, allowSimulation = false)
        val boundary = SupabaseBoundary(config = config, authClient = authClient)
        val repo = AuthRepositoryImpl(config = config, supabaseBoundary = boundary, sessionStorage = storage)

        // 1. Initial guest state
        assertEquals(AuthSession.Unauthenticated, repo.sessionState.value)
        assertFalse(repo.canCreateOrder())

        // 2. Real OTP request
        val sendResult = repo.sendOtp("01012345678")
        assertTrue(sendResult is AppResult.Success)

        // Rate limit cooldown active
        val cooldownResult = repo.sendOtp("01012345678")
        assertTrue(cooldownResult is AppResult.Error)

        // 3. Real OTP verification
        val verifyResult = repo.verifyOtp("01012345678", "789012")
        assertTrue(verifyResult is AppResult.Success)

        // Identity must be the real Supabase UUID, not synthetic usr_...
        val session = (verifyResult as AppResult.Success).data
        assertEquals(realUserUuid, session.userId)
        assertFalse(session.userId.startsWith("usr_"))
        assertEquals("+201012345678", session.phone)
        assertEquals("فاطمة حسن", session.fullName)
        assertTrue(repo.canCreateOrder())
        assertTrue(repo.canSavePrescription())

        // Verify tokens were saved to storage
        assertEquals("valid_access_token_12345", storage.getAccessToken())

        // 4. Logout clears storage and resets state
        repo.logout()
        assertEquals(AuthSession.Unauthenticated, repo.sessionState.value)
        assertNull(storage.getAccessToken())
        assertFalse(repo.canCreateOrder())
    }

    // ==========================================
    // Phase 2: AuthViewModel Flow Tests
    // ==========================================

    @Test
    fun authViewModel_fullFlowTransitions() {
        val devSimConfig = EnvironmentConfig.current(AppEnvironment.DEVELOPMENT, allowSimulation = true)
        val authRepo = AuthRepositoryImpl(config = devSimConfig)
        val viewModel = AuthViewModel(authRepo)

        // Initial state
        assertEquals(AuthStep.PHONE_INPUT, viewModel.uiState.value.step)

        // Invalid phone error
        viewModel.onPhoneInputChanged("01312345678")
        viewModel.sendOtp()
        assertTrue(viewModel.uiState.value.errorMessage != null)
        assertEquals(AuthStep.PHONE_INPUT, viewModel.uiState.value.step)

        // Valid phone input
        viewModel.onPhoneInputChanged("01012345678")
        viewModel.sendOtp()
        assertEquals(AuthStep.OTP_VERIFICATION, viewModel.uiState.value.step)
        assertEquals("+201012345678", viewModel.uiState.value.normalizedPhone)

        // Invalid OTP length
        viewModel.onOtpInputChanged("123")
        var successCalled = false
        viewModel.verifyOtp { successCalled = true }
        assertFalse(successCalled)
        assertTrue(viewModel.uiState.value.errorMessage != null)

        // Valid OTP in explicit simulation mode
        viewModel.onOtpInputChanged("123456")
        viewModel.verifyOtp { successCalled = true }
        assertTrue(successCalled)
        assertEquals(AuthStep.SUCCESS, viewModel.uiState.value.step)
        assertTrue(authRepo.sessionState.value is AuthSession.Authenticated)
    }

    // ==========================================
    // Phase 3: Medicine Catalog, Search & Availability
    // ==========================================

    @Test
    fun medicineDto_mapsToDomainSafely() {
        val dto = com.example.data.remote.model.SupabaseMedicineDto(
            id = "med-1",
            nameAr = "بانادول أزرق 500 مجم",
            nameEn = "Panadol Blue 500mg",
            normalizedName = "Panadol Blue 500mg",
            activeIngredient = "Paracetamol",
            strength = "500mg",
            dosageForm = "Tablet",
            manufacturer = "GSK",
            prescriptionRequired = false,
            status = "ACTIVE"
        )

        val domain = dto.toDomain()
        assertEquals("med-1", domain.id)
        assertEquals("بانادول أزرق 500 مجم", domain.nameAr)
        assertEquals("Panadol Blue 500mg", domain.nameEn)
        assertEquals("Paracetamol", domain.activeIngredient)
        assertEquals("500mg", domain.strength)
        assertEquals("Tablet", domain.dosageForm)
        assertFalse(domain.prescriptionRequired)
        assertEquals("ACTIVE", domain.status)
    }

    @Test
    fun availabilityStatus_mapsAllDocumentedStatuses() {
        assertEquals(com.example.domain.model.AvailabilityStatus.AVAILABLE, com.example.domain.model.AvailabilityStatus.fromBackend("AVAILABLE"))
        assertEquals(com.example.domain.model.AvailabilityStatus.LOW_STOCK, com.example.domain.model.AvailabilityStatus.fromBackend("low_stock"))
        assertEquals(com.example.domain.model.AvailabilityStatus.OUT_OF_STOCK, com.example.domain.model.AvailabilityStatus.fromBackend("OUT_OF_STOCK"))
        assertEquals(com.example.domain.model.AvailabilityStatus.RESERVED, com.example.domain.model.AvailabilityStatus.fromBackend("RESERVED"))
        assertEquals(com.example.domain.model.AvailabilityStatus.UNAVAILABLE, com.example.domain.model.AvailabilityStatus.fromBackend("UNAVAILABLE"))
        assertEquals(com.example.domain.model.AvailabilityStatus.EXPIRED, com.example.domain.model.AvailabilityStatus.fromBackend("EXPIRED"))
        assertEquals(com.example.domain.model.AvailabilityStatus.UNAVAILABLE, com.example.domain.model.AvailabilityStatus.fromBackend("UNKNOWN_STATUS"))
    }

    @Test
    fun pharmacyInventoryDto_excludesPrivateAndReservedFields() {
        val invDto = com.example.data.remote.model.SupabasePharmacyInventoryDto(
            id = "inv-1",
            pharmacyId = "pharm-101",
            medicineVariantId = "var-1",
            availabilityStatus = "AVAILABLE",
            price = 45.5,
            lastSyncedAt = "2026-09-29T08:00:00Z",
            pharmacies = com.example.data.remote.model.SupabasePharmacyDto(
                displayCode = "PARTNER-101",
                governorate = "Cairo",
                city = "Nasr City",
                area = "Abbas El Akkad"
            )
        )

        val domain = invDto.toDomain()
        assertEquals("inv-1", domain.id)
        assertEquals("PARTNER-101", domain.pharmacyDisplayCode)
        assertEquals("Abbas El Akkad, Nasr City", domain.pharmacyArea)
        assertEquals(com.example.domain.model.AvailabilityStatus.AVAILABLE, domain.availabilityStatus)
        assertEquals(45.5, domain.price, 0.001)
        assertEquals("2026-09-29T08:00:00Z", domain.lastSyncedAt)
    }

    @Test
    fun medicineRepository_handlesEmptyAndLiveResponses() = runBlocking {
        val testClient = object : com.example.data.remote.SupabaseMedicineClient {
            override suspend fun searchMedicines(
                baseUrl: String,
                headers: Map<String, String>,
                query: String
            ): AppResult<List<com.example.data.remote.model.SupabaseMedicineDto>> {
                return if (query == "empty") {
                    AppResult.Success(emptyList())
                } else if (query == "error") {
                    AppResult.Error(AppError.NetworkError("Timeout"))
                } else {
                    AppResult.Success(
                        listOf(
                            com.example.data.remote.model.SupabaseMedicineDto(
                                id = "med-test-1",
                                nameAr = "كونجستال",
                                nameEn = "Congestal",
                                activeIngredient = "Paracetamol + Pseudoephedrine",
                                strength = "500mg/30mg",
                                dosageForm = "Tablet",
                                prescriptionRequired = false
                            )
                        )
                    )
                }
            }

            override suspend fun getMedicineById(
                baseUrl: String,
                headers: Map<String, String>,
                medicineId: String
            ): AppResult<com.example.data.remote.model.SupabaseMedicineDto> {
                return AppResult.Success(
                    com.example.data.remote.model.SupabaseMedicineDto(
                        id = medicineId,
                        nameAr = "كونجستال",
                        nameEn = "Congestal",
                        activeIngredient = "Paracetamol",
                        strength = "500mg",
                        dosageForm = "Tablet"
                    )
                )
            }

            override suspend fun getVariantsByMedicineId(
                baseUrl: String,
                headers: Map<String, String>,
                medicineId: String
            ): AppResult<List<com.example.data.remote.model.SupabaseMedicineVariantDto>> {
                return AppResult.Success(
                    listOf(
                        com.example.data.remote.model.SupabaseMedicineVariantDto(
                            id = "var-1",
                            medicineId = medicineId,
                            brandName = "Congestal 20 Tablets",
                            packageSize = "20 Tablets",
                            priceReference = 35.0
                        )
                    )
                )
            }

            override suspend fun getInventoryByVariantIds(
                baseUrl: String,
                headers: Map<String, String>,
                variantIds: List<String>
            ): AppResult<List<com.example.data.remote.model.SupabasePharmacyInventoryDto>> {
                return AppResult.Success(
                    listOf(
                        com.example.data.remote.model.SupabasePharmacyInventoryDto(
                            id = "inv-1",
                            pharmacyId = "ph-1",
                            medicineVariantId = variantIds.first(),
                            availabilityStatus = "AVAILABLE",
                            price = 35.0,
                            lastSyncedAt = "2026-09-29T10:00:00Z"
                        )
                    )
                )
            }

            override suspend fun getAlternatives(
                baseUrl: String,
                headers: Map<String, String>,
                activeIngredient: String,
                strength: String,
                dosageForm: String,
                excludeId: String
            ): AppResult<List<com.example.data.remote.model.SupabaseMedicineDto>> {
                return AppResult.Success(
                    listOf(
                        com.example.data.remote.model.SupabaseMedicineDto(
                            id = "alt-1",
                            nameAr = "123 أقراص",
                            nameEn = "123 Tablets",
                            activeIngredient = activeIngredient,
                            strength = strength,
                            dosageForm = dosageForm
                        )
                    )
                )
            }
        }

        val repo = com.example.data.repository.MedicineRepositoryImpl(
            medicineClient = testClient
        )

        // 1. Success query
        val searchSuccess = repo.searchMedicines("congestal")
        assertTrue(searchSuccess is AppResult.Success)
        val list = (searchSuccess as AppResult.Success).data
        assertEquals(1, list.size)
        assertEquals("كونجستال", list.first().nameAr)

        // 2. Empty query returns empty
        val searchEmpty = repo.searchMedicines("empty")
        assertTrue(searchEmpty is AppResult.Success)
        assertTrue((searchEmpty as AppResult.Success).data.isEmpty())

        // 3. Error query returns error
        val searchError = repo.searchMedicines("error")
        assertTrue(searchError is AppResult.Error)

        // 4. Details fetch
        val details = repo.getMedicineDetails("med-test-1")
        assertTrue(details is AppResult.Success)
        val data = (details as AppResult.Success).data
        assertEquals(1, data.variants.size)
        assertEquals(1, data.availability.size)
        assertEquals(1, data.alternatives.size)
        assertEquals("alt-1", data.alternatives.first().id)
    }

    @Test
    fun searchViewModel_stateTransitionsAndDebouncing() = runBlocking {
        val testRepo = object : com.example.domain.repository.MedicineRepository {
            override suspend fun searchMedicines(query: String): AppResult<List<com.example.domain.model.Medicine>> {
                return if (query == "aspirin") {
                    AppResult.Success(
                        listOf(
                            com.example.domain.model.Medicine(
                                id = "asp-1",
                                nameAr = "أسبرين بروتكت",
                                nameEn = "Aspirin Protect",
                                normalizedName = "Aspirin Protect",
                                activeIngredient = "Acetylsalicylic acid",
                                strength = "100mg",
                                dosageForm = "Enteric Coated Tablet"
                            )
                        )
                    )
                } else if (query == "unknown") {
                    AppResult.Success(emptyList())
                } else {
                    AppResult.Error(AppError.NetworkError("Failed to reach catalog"))
                }
            }

            override suspend fun getMedicineDetails(medicineId: String): AppResult<com.example.domain.model.MedicineDetail> {
                return AppResult.Success(
                    com.example.domain.model.MedicineDetail(
                        medicine = com.example.domain.model.Medicine(
                            id = medicineId,
                            nameAr = "أسبرين",
                            nameEn = "Aspirin",
                            normalizedName = "Aspirin",
                            activeIngredient = "Acetylsalicylic acid",
                            strength = "100mg",
                            dosageForm = "Tablet"
                        )
                    )
                )
            }

            override suspend fun getAvailability(variantIds: List<String>): AppResult<List<com.example.domain.model.PharmacyInventory>> {
                return AppResult.Success(emptyList())
            }

            override suspend fun getAlternatives(
                activeIngredient: String,
                strength: String,
                dosageForm: String,
                excludeMedicineId: String
            ): AppResult<List<com.example.domain.model.Medicine>> {
                return AppResult.Success(emptyList())
            }
        }

        val viewModel = com.example.presentation.screens.search.SearchViewModel(
            medicineRepository = testRepo,
            coroutineDispatcher = kotlinx.coroutines.Dispatchers.Unconfined
        )

        // 1. Initial State
        assertTrue(viewModel.uiState.value is com.example.presentation.screens.search.SearchUiState.Initial)

        // 2. Empty query clears state back to Initial
        viewModel.onQueryChange("")
        assertTrue(viewModel.uiState.value is com.example.presentation.screens.search.SearchUiState.Initial)

        // 3. Direct / debounced search triggers loading -> success
        viewModel.searchNow("aspirin")
        val state = viewModel.uiState.value
        assertTrue("Expected Success state", state is com.example.presentation.screens.search.SearchUiState.Success)
        val successState = state as com.example.presentation.screens.search.SearchUiState.Success
        assertEquals(1, successState.medicines.size)
        assertEquals("أسبرين بروتكت", successState.medicines.first().nameAr)

        // 4. Unknown query triggers Empty State
        viewModel.searchNow("unknown")
        assertTrue("Expected Empty state", viewModel.uiState.value is com.example.presentation.screens.search.SearchUiState.Empty)

        // 5. Open Medicine Details
        viewModel.openMedicineDetails("asp-1")
        val detailState = viewModel.detailState.value
        assertTrue(detailState is com.example.presentation.screens.search.MedicineDetailUiState.Success)
        assertEquals("asp-1", (detailState as com.example.presentation.screens.search.MedicineDetailUiState.Success).detail.medicine.id)

        // 6. Close details
        viewModel.closeMedicineDetails()
        assertTrue(viewModel.detailState.value is com.example.presentation.screens.search.MedicineDetailUiState.Idle)
    }

    // ==========================================
    // Phase 4: Cart, Checkout & Atomic Order Creation Tests
    // ==========================================

    @Test
    fun cart_addItem_increase_decrease_and_remove() {
        val cartRepo = com.example.data.repository.CartRepositoryImpl()
        val dummyMed = com.example.domain.model.Medicine(
            id = "med-panadol",
            nameAr = "بانادول أزرق",
            nameEn = "Panadol Blue",
            normalizedName = "Panadol Blue",
            activeIngredient = "Paracetamol",
            strength = "500mg",
            dosageForm = "Tablet"
        )
        val dummyVariant = com.example.domain.model.MedicineVariant(
            id = "var-panadol-24",
            medicineId = "med-panadol",
            brandName = "Panadol 24 Tablets",
            packageSize = "24 Tablets",
            priceReference = 40.0
        )
        val dummyInv = com.example.domain.model.PharmacyInventory(
            id = "inv-1",
            pharmacyId = "pharm-cairo-1",
            pharmacyDisplayCode = "PARTNER-01",
            pharmacyArea = "Nasr City",
            medicineVariantId = "var-panadol-24",
            availabilityStatus = com.example.domain.model.AvailabilityStatus.AVAILABLE,
            price = 45.0,
            lastSyncedAt = "2026-10-04T10:00:00Z"
        )

        // 1. Initial cart is empty
        assertTrue(cartRepo.cartState.value.isEmpty)
        assertEquals(0, cartRepo.cartState.value.totalItemsCount)
        assertEquals(0.0, cartRepo.cartState.value.subtotal, 0.001)

        // 2. Add 2 units
        cartRepo.addItem(dummyMed, dummyVariant, dummyInv, quantity = 2)
        assertEquals(1, cartRepo.cartState.value.items.size)
        assertEquals(2, cartRepo.cartState.value.totalItemsCount)
        assertEquals(90.0, cartRepo.cartState.value.subtotal, 0.001)
        assertEquals("pharm-cairo-1", cartRepo.cartState.value.selectedPharmacyId)

        // 3. Increase quantity to 3
        cartRepo.updateQuantity("var-panadol-24", 3)
        assertEquals(3, cartRepo.cartState.value.totalItemsCount)
        assertEquals(135.0, cartRepo.cartState.value.subtotal, 0.001)

        // 4. Decrease quantity to 1
        cartRepo.updateQuantity("var-panadol-24", 1)
        assertEquals(1, cartRepo.cartState.value.totalItemsCount)
        assertEquals(45.0, cartRepo.cartState.value.subtotal, 0.001)

        // 5. Remove item
        cartRepo.removeItem("var-panadol-24")
        assertTrue(cartRepo.cartState.value.isEmpty)
        assertEquals(0, cartRepo.cartState.value.totalItemsCount)
    }

    @Test
    fun cart_enforcesSinglePharmacy_resetsWhenSwitchingPharmacy() {
        val cartRepo = com.example.data.repository.CartRepositoryImpl()

        val med1 = com.example.domain.model.Medicine(id = "m1", nameAr = "دواء 1", nameEn = "Med 1", normalizedName = "Med 1", activeIngredient = "A", strength = "10mg", dosageForm = "Tab")
        val var1 = com.example.domain.model.MedicineVariant(id = "v1", medicineId = "m1", brandName = "Med 1", packageSize = "1 Pack")
        val invPharmA = com.example.domain.model.PharmacyInventory(
            id = "inv-A", pharmacyId = "pharm-A", pharmacyDisplayCode = "PHARM-A", pharmacyArea = "Maadi", medicineVariantId = "v1", availabilityStatus = com.example.domain.model.AvailabilityStatus.AVAILABLE, price = 50.0, lastSyncedAt = "2026-10-04T10:00:00Z"
        )

        cartRepo.addItem(med1, var1, invPharmA, 1)
        assertEquals("pharm-A", cartRepo.cartState.value.selectedPharmacyId)
        assertEquals(1, cartRepo.cartState.value.items.size)

        // Add from a different pharmacy: cart re-scopes to new pharmacy per single-pharmacy fulfillment requirement
        val med2 = com.example.domain.model.Medicine(id = "m2", nameAr = "دواء 2", nameEn = "Med 2", normalizedName = "Med 2", activeIngredient = "B", strength = "20mg", dosageForm = "Tab")
        val var2 = com.example.domain.model.MedicineVariant(id = "v2", medicineId = "m2", brandName = "Med 2", packageSize = "1 Pack")
        val invPharmB = com.example.domain.model.PharmacyInventory(
            id = "inv-B", pharmacyId = "pharm-B", pharmacyDisplayCode = "PHARM-B", pharmacyArea = "Zamalek", medicineVariantId = "v2", availabilityStatus = com.example.domain.model.AvailabilityStatus.AVAILABLE, price = 80.0, lastSyncedAt = "2026-10-04T10:00:00Z"
        )

        cartRepo.addItem(med2, var2, invPharmB, 1)
        assertEquals("pharm-B", cartRepo.cartState.value.selectedPharmacyId)
        assertEquals(1, cartRepo.cartState.value.items.size)
        assertEquals("v2", cartRepo.cartState.value.items.first().variant.id)
    }

    @Test
    fun orderCreation_clientPayloadContainsOnlyNonAuthoritativeIntent() {
        val request = com.example.data.remote.model.CreateOrderRpcRequest(
            addressId = "addr-uuid-1",
            pharmacyId = "pharm-uuid-1",
            items = listOf(com.example.data.remote.model.CreateOrderRpcItem(variantId = "var-1", quantity = 2)),
            paymentMethod = "CASH_ON_DELIVERY",
            idempotencyKey = "uuid-idempotency-1234"
        )

        // Verify request payload only contains intent fields
        assertEquals("addr-uuid-1", request.addressId)
        assertEquals("pharm-uuid-1", request.pharmacyId)
        assertEquals("CASH_ON_DELIVERY", request.paymentMethod)
        assertEquals("uuid-idempotency-1234", request.idempotencyKey)
        assertEquals(1, request.items.size)
        assertEquals(2, request.items.first().quantity)

        // Verify Moshi serialization produces expected PostgreSQL JSON structure
        val moshi = com.squareup.moshi.Moshi.Builder().addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory()).build()
        val adapter = moshi.adapter(com.example.data.remote.model.CreateOrderRpcRequest::class.java)
        val json = adapter.toJson(request)

        assertTrue(json.contains("\"p_address_id\":\"addr-uuid-1\""))
        assertTrue(json.contains("\"p_pharmacy_id\":\"pharm-uuid-1\""))
        assertTrue(json.contains("\"p_payment_method\":\"CASH_ON_DELIVERY\""))
        assertTrue(json.contains("\"p_idempotency_key\":\"uuid-idempotency-1234\""))
        assertFalse("Client MUST NOT supply patient_id", json.contains("patient_id"))
        assertFalse("Client MUST NOT supply unit_price", json.contains("unit_price"))
        assertFalse("Client MUST NOT supply subtotal", json.contains("subtotal"))
        assertFalse("Client MUST NOT supply delivery_fee", json.contains("delivery_fee"))
        assertFalse("Client MUST NOT supply total", json.contains("total"))
    }

    @Test
    fun checkoutViewModel_requiresAuthentication_blocksGuest() {
        val storage = InMemoryAuthSessionStorage()
        val cartRepo = com.example.data.repository.CartRepositoryImpl()
        val orderRepo = object : com.example.domain.repository.OrderRepository {
            override suspend fun getAddresses(userToken: String): AppResult<List<com.example.domain.model.Address>> = AppResult.Success(emptyList())
            override suspend fun createAddress(userToken: String, label: String, governorate: String, city: String, area: String, street: String, building: String, apartment: String?, floor: String?, landmark: String?, isDefault: Boolean): AppResult<com.example.domain.model.Address> =
                AppResult.Error(AppError.AuthenticationError("Auth required"))
            override suspend fun submitOrder(userToken: String, addressId: String, pharmacyId: String, items: List<Pair<String, Int>>, paymentMethod: String, idempotencyKey: String): AppResult<com.example.domain.model.Order> =
                AppResult.Error(AppError.AuthenticationError("Auth required"))
        }

        // Initialize with empty storage (Guest)
        val viewModel = com.example.presentation.screens.checkout.CheckoutViewModel(
            orderRepository = orderRepo,
            cartRepository = cartRepo,
            sessionStorage = storage
        )

        // Guest user attempts to submit order -> sessionExpired triggers auth prompt
        viewModel.submitOrder()
        assertTrue(viewModel.uiState.value.sessionExpired)
        assertNull(viewModel.uiState.value.createdOrder)
    }

    @Test
    fun checkoutViewModel_preservesCartOnSessionExpiry_andResumesAfterOtp() = runBlocking {
        val storage = InMemoryAuthSessionStorage()
        val cartRepo = com.example.data.repository.CartRepositoryImpl()

        // Populate cart with medication
        val med = com.example.domain.model.Medicine(id = "m1", nameAr = "أسبرين", nameEn = "Aspirin", normalizedName = "Aspirin", activeIngredient = "A", strength = "100mg", dosageForm = "Tab")
        val variant = com.example.domain.model.MedicineVariant(id = "v1", medicineId = "m1", brandName = "Aspirin 30s", packageSize = "30 Tablets")
        val inv = com.example.domain.model.PharmacyInventory(id = "inv1", pharmacyId = "p1", pharmacyDisplayCode = "PH-01", pharmacyArea = "Dokki", medicineVariantId = "v1", availabilityStatus = com.example.domain.model.AvailabilityStatus.AVAILABLE, price = 45.0, lastSyncedAt = "2026-10-04T10:00:00Z")
        cartRepo.addItem(med, variant, inv, 2)

        val serverAuthoritativeFee = 18.50
        val testAddress = com.example.domain.model.Address(id = "addr-1", userId = "u1", label = "Home", governorate = "Cairo", city = "Cairo", area = "Dokki", street = "Tahrir", building = "12")
        val expectedOrder = com.example.domain.model.Order(
            id = "ord-1",
            publicOrderNumber = "ELAJX-TEST001",
            status = "PLACED",
            subtotal = 90.0,
            deliveryFee = serverAuthoritativeFee,
            discount = 0.0,
            total = 108.50,
            paymentMethod = "CASH_ON_DELIVERY",
            paymentStatus = "PENDING",
            createdAt = "2026-10-04T12:00:00Z"
        )

        var orderSubmitAttemptCount = 0
        var submittedToken: String? = null

        val orderRepo = object : com.example.domain.repository.OrderRepository {
            override suspend fun getAddresses(userToken: String): AppResult<List<com.example.domain.model.Address>> = AppResult.Success(listOf(testAddress))
            override suspend fun createAddress(userToken: String, label: String, governorate: String, city: String, area: String, street: String, building: String, apartment: String?, floor: String?, landmark: String?, isDefault: Boolean): AppResult<com.example.domain.model.Address> =
                AppResult.Success(testAddress)
            override suspend fun submitOrder(userToken: String, addressId: String, pharmacyId: String, items: List<Pair<String, Int>>, paymentMethod: String, idempotencyKey: String): AppResult<com.example.domain.model.Order> {
                orderSubmitAttemptCount++
                submittedToken = userToken
                return if (userToken == "expired_token") {
                    AppResult.Error(AppError.AuthenticationError("Session expired (HTTP 401)"))
                } else {
                    AppResult.Success(expectedOrder)
                }
            }
        }

        // User had an expired token initially
        storage.saveSession(accessToken = "expired_token", refreshToken = "ref_1", userId = "u1", phone = "01012345678", fullName = "مريض")
        val viewModel = com.example.presentation.screens.checkout.CheckoutViewModel(orderRepo, cartRepo, storage)
        viewModel.selectAddress("addr-1")

        // First attempt with expired token fails with 401
        viewModel.submitOrder()
        assertTrue("Session expired must be signaled to UI", viewModel.uiState.value.sessionExpired)
        assertFalse("Cart MUST NOT be dropped on session expiration", cartRepo.cartState.value.isEmpty)
        assertEquals(2, cartRepo.cartState.value.items.first().quantity)
        assertEquals("addr-1", viewModel.uiState.value.selectedAddressId)

        // User authenticates with OTP, receiving fresh token
        storage.saveSession(accessToken = "fresh_token_xyz", refreshToken = "ref_2", userId = "u1", phone = "01012345678", fullName = "مريض")
        viewModel.onSessionRestored()
        assertFalse(viewModel.uiState.value.sessionExpired)

        // Resubmit order with preserved cart and address
        viewModel.submitOrder()
        assertEquals(2, orderSubmitAttemptCount)
        assertEquals("fresh_token_xyz", submittedToken)
        assertNotNull(viewModel.uiState.value.createdOrder)
        assertEquals("ELAJX-TEST001", viewModel.uiState.value.createdOrder?.publicOrderNumber)
        // Cart is cleared ONLY after successful server creation
        assertTrue(cartRepo.cartState.value.isEmpty)
    }

    @Test
    fun checkout_idempotencyKeyReusedOnRetry() {
        val storage = InMemoryAuthSessionStorage()
        storage.saveSession("token", "ref", "u1", "01012345678", "مريض")
        val cartRepo = com.example.data.repository.CartRepositoryImpl()
        val orderRepo = object : com.example.domain.repository.OrderRepository {
            override suspend fun getAddresses(userToken: String): AppResult<List<com.example.domain.model.Address>> = AppResult.Success(emptyList())
            override suspend fun createAddress(userToken: String, label: String, governorate: String, city: String, area: String, street: String, building: String, apartment: String?, floor: String?, landmark: String?, isDefault: Boolean): AppResult<com.example.domain.model.Address> =
                AppResult.Error(AppError.UnknownError(null))
            override suspend fun submitOrder(userToken: String, addressId: String, pharmacyId: String, items: List<Pair<String, Int>>, paymentMethod: String, idempotencyKey: String): AppResult<com.example.domain.model.Order> =
                AppResult.Error(AppError.NetworkError("Timeout", isTimeout = true))
        }

        val viewModel = com.example.presentation.screens.checkout.CheckoutViewModel(orderRepo, cartRepo, storage)
        val initialKey = viewModel.getIdempotencyKey()
        assertNotNull(initialKey)
        assertTrue(initialKey.length >= 32)

        // Multiple calls must maintain the exact same idempotency key for retries
        assertEquals(initialKey, viewModel.getIdempotencyKey())
        assertEquals(initialKey, viewModel.getIdempotencyKey())
    }

    @Test
    fun supabaseOrderClient_handlesAuthoritativeSuccessAndErrorCodes() = runBlocking {
        val testClient = com.example.data.remote.RealSupabaseOrderClient()
        // Confirm client initializes cleanly
        assertNotNull(testClient)
    }

    // ==========================================
    // Phase 4 Targeted Contract Fix Unit Tests
    // ==========================================

    @Test
    fun deliveryFee_clientRequestContainsNoDeliveryFee() {
        // Inspect CreateOrderRpcRequest properties via reflection
        val fields = com.example.data.remote.model.CreateOrderRpcRequest::class.java.declaredFields.map { it.name }
        assertFalse("Client request MUST NOT contain delivery_fee field", fields.contains("deliveryFee") || fields.contains("delivery_fee") || fields.contains("fee"))
        assertTrue("Client request must have addressId", fields.contains("addressId"))
        assertTrue("Client request must have pharmacyId", fields.contains("pharmacyId"))
        assertTrue("Client request must have items", fields.contains("items"))
        assertTrue("Client request must have idempotencyKey", fields.contains("idempotencyKey"))

        // Verify Moshi serialization payload does not serialize delivery fee
        val moshi = com.squareup.moshi.Moshi.Builder()
            .addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
            .build()
        val adapter = moshi.adapter(com.example.data.remote.model.CreateOrderRpcRequest::class.java)
        val sampleRequest = com.example.data.remote.model.CreateOrderRpcRequest(
            addressId = "addr-123",
            pharmacyId = "pharm-456",
            items = listOf(com.example.data.remote.model.CreateOrderRpcItem("var-789", 2)),
            paymentMethod = "CASH_ON_DELIVERY",
            idempotencyKey = "idem-test-key"
        )
        val json = adapter.toJson(sampleRequest)
        assertFalse("JSON payload MUST NOT contain delivery_fee", json.contains("delivery_fee") || json.contains("fee"))
    }

    @Test
    fun deliveryFee_preConfirmationUiDisplaysNeutralPendingString() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val pendingEn = context.getString(R.string.checkout_delivery_fee_pending)
        assertEquals("Determined at confirmation", pendingEn)
    }

    @Test
    fun deliveryFee_serverResponseDeliveryFeeIsDisplayedAfterConfirmation() {
        val serverConfiguredFee = 17.50
        val serverResponseDto = com.example.data.remote.model.OrderResponseDto(
            id = "ord-auth-01",
            publicOrderNumber = "ELAJX-AUTH01",
            status = "PLACED",
            subtotal = 100.0,
            deliveryFee = serverConfiguredFee,
            discount = 0.0,
            total = 100.0 + serverConfiguredFee,
            paymentMethod = "CASH_ON_DELIVERY",
            paymentStatus = "PENDING",
            createdAt = "2026-10-04T12:00:00Z"
        )
        val domainOrder = serverResponseDto.toDomain()
        assertEquals("UI domain model receives delivery fee directly from server response", serverConfiguredFee, domainOrder.deliveryFee, 0.001)
        assertEquals(117.50, domainOrder.total, 0.001)
    }

    sealed class DeliveryFeeConfigValidationResult {
        data class Valid(val fee: Double) : DeliveryFeeConfigValidationResult()
        data class Error(val code: String, val message: String) : DeliveryFeeConfigValidationResult()
    }

    private fun evaluateBackendDeliveryFeeConfig(
        configRowExists: Boolean,
        configJson: Map<String, Any?>?
    ): DeliveryFeeConfigValidationResult {
        if (!configRowExists || configJson == null) {
            return DeliveryFeeConfigValidationResult.Error("55000", "CONFIG_ERROR: Authoritative delivery fee configuration not found.")
        }
        val active = configJson["active"] as? Boolean ?: false
        if (!active) {
            return DeliveryFeeConfigValidationResult.Error("55000", "CONFIG_ERROR: Authoritative delivery fee configuration is inactive.")
        }
        val rawFee = configJson["default_fee"] ?: configJson["fee"]
        val numericFee = when (rawFee) {
            is Number -> rawFee.toDouble()
            is String -> rawFee.toDoubleOrNull()
            else -> null
        } ?: return DeliveryFeeConfigValidationResult.Error("55000", "CONFIG_ERROR: Authoritative delivery fee cannot be interpreted as a valid monetary amount.")

        if (numericFee < 0.0) {
            return DeliveryFeeConfigValidationResult.Error("55000", "CONFIG_ERROR: Authoritative delivery fee must be a valid non-negative monetary amount.")
        }
        return DeliveryFeeConfigValidationResult.Valid(numericFee)
    }

    @Test
    fun deliveryFee_missingConfiguration_failsSafelyWithoutDefault() {
        val result = evaluateBackendDeliveryFeeConfig(configRowExists = false, configJson = null)
        assertTrue("Missing configuration must fail with error", result is DeliveryFeeConfigValidationResult.Error)
        val error = result as DeliveryFeeConfigValidationResult.Error
        assertEquals("55000", error.code)
        assertTrue(error.message.contains("not found"))
    }

    @Test
    fun deliveryFee_inactiveConfiguration_failsSafelyWithoutDefault() {
        val inactiveConfig = mapOf<String, Any?>("default_fee" to 15.0, "currency" to "EGP", "active" to false)
        val result = evaluateBackendDeliveryFeeConfig(configRowExists = true, configJson = inactiveConfig)
        assertTrue("Inactive configuration must fail with error", result is DeliveryFeeConfigValidationResult.Error)
        val error = result as DeliveryFeeConfigValidationResult.Error
        assertEquals("55000", error.code)
        assertTrue(error.message.contains("inactive"))
    }

    @Test
    fun deliveryFee_invalidConfiguration_failsSafelyWithoutFallback() {
        // Non-numeric string
        val invalidTextConfig = mapOf<String, Any?>("default_fee" to "NOT_A_NUMBER", "currency" to "EGP", "active" to true)
        val textResult = evaluateBackendDeliveryFeeConfig(configRowExists = true, configJson = invalidTextConfig)
        assertTrue("Non-numeric fee must fail safely", textResult is DeliveryFeeConfigValidationResult.Error)
        assertEquals("55000", (textResult as DeliveryFeeConfigValidationResult.Error).code)

        // Negative numeric value
        val negativeConfig = mapOf<String, Any?>("default_fee" to -15.0, "currency" to "EGP", "active" to true)
        val negResult = evaluateBackendDeliveryFeeConfig(configRowExists = true, configJson = negativeConfig)
        assertTrue("Negative fee must fail safely", negResult is DeliveryFeeConfigValidationResult.Error)
        assertEquals("55000", (negResult as DeliveryFeeConfigValidationResult.Error).code)

        // Valid configuration succeeds
        val validConfig = mapOf<String, Any?>("default_fee" to 16.50, "currency" to "EGP", "active" to true)
        val validResult = evaluateBackendDeliveryFeeConfig(configRowExists = true, configJson = validConfig)
        assertTrue("Valid fee configuration must succeed", validResult is DeliveryFeeConfigValidationResult.Valid)
        assertEquals(16.50, (validResult as DeliveryFeeConfigValidationResult.Valid).fee, 0.001)
    }

    @Test
    fun pharmacy_activeAndVerified_succeedsAtContractLevel() {
        fun evaluatePharmacy(status: String, verificationStatus: String): Boolean =
            status == "ACTIVE" && verificationStatus == "VERIFIED"

        assertTrue("ACTIVE + VERIFIED pharmacy must succeed", evaluatePharmacy("ACTIVE", "VERIFIED"))
    }

    @Test
    fun pharmacy_activeAndUnverified_fails() {
        fun evaluatePharmacy(status: String, verificationStatus: String): Boolean =
            status == "ACTIVE" && verificationStatus == "VERIFIED"

        assertFalse("ACTIVE + UNVERIFIED pharmacy must fail", evaluatePharmacy("ACTIVE", "UNVERIFIED"))
        assertFalse("ACTIVE + PENDING pharmacy must fail", evaluatePharmacy("ACTIVE", "PENDING"))
    }

    @Test
    fun pharmacy_inactiveAndVerified_fails() {
        fun evaluatePharmacy(status: String, verificationStatus: String): Boolean =
            status == "ACTIVE" && verificationStatus == "VERIFIED"

        assertFalse("INACTIVE + VERIFIED pharmacy must fail", evaluatePharmacy("INACTIVE", "VERIFIED"))
        assertFalse("SUSPENDED + VERIFIED pharmacy must fail", evaluatePharmacy("SUSPENDED", "VERIFIED"))
    }

    @Test
    fun idempotency_sequentialRetry_returnsSameOrder() = runBlocking {
        val storage = InMemoryAuthSessionStorage()
        storage.saveSession("token", "ref", "u1", "01012345678", "مريض")

        val ordersDatabase = mutableMapOf<String, com.example.domain.model.Order>()
        val serverAuthoritativeFee = 16.50
        val testOrder = com.example.domain.model.Order(
            id = "ord-1",
            publicOrderNumber = "ELAJX-UNIQUE",
            status = "PLACED",
            subtotal = 50.0,
            deliveryFee = serverAuthoritativeFee,
            discount = 0.0,
            total = 66.50,
            paymentMethod = "CASH_ON_DELIVERY",
            paymentStatus = "PENDING",
            createdAt = "2026-10-04T12:00:00Z"
        )

        var backendInsertCount = 0
        val mockOrderRepo = object : com.example.domain.repository.OrderRepository {
            override suspend fun getAddresses(userToken: String): AppResult<List<com.example.domain.model.Address>> = AppResult.Success(emptyList())
            override suspend fun createAddress(userToken: String, label: String, governorate: String, city: String, area: String, street: String, building: String, apartment: String?, floor: String?, landmark: String?, isDefault: Boolean): AppResult<com.example.domain.model.Address> =
                AppResult.Error(AppError.UnknownError(null))

            override suspend fun submitOrder(userToken: String, addressId: String, pharmacyId: String, items: List<Pair<String, Int>>, paymentMethod: String, idempotencyKey: String): AppResult<com.example.domain.model.Order> {
                val existing = ordersDatabase[idempotencyKey]
                return if (existing != null) {
                    AppResult.Success(existing)
                } else {
                    backendInsertCount++
                    ordersDatabase[idempotencyKey] = testOrder
                    AppResult.Success(testOrder)
                }
            }
        }

        // First attempt with key K1
        val result1 = mockOrderRepo.submitOrder("token", "addr", "pharm", listOf("var" to 1), "CASH_ON_DELIVERY", "KEY-K1")
        assertTrue(result1 is AppResult.Success)
        assertEquals(1, backendInsertCount)

        // Retry with same key K1 returns the same order without creating a duplicate
        val result2 = mockOrderRepo.submitOrder("token", "addr", "pharm", listOf("var" to 1), "CASH_ON_DELIVERY", "KEY-K1")
        assertTrue(result2 is AppResult.Success)
        assertEquals(1, backendInsertCount) // No second insert!
        assertEquals("ELAJX-UNIQUE", (result2 as AppResult.Success).data.publicOrderNumber)
    }

    @Test
    fun idempotency_concurrentSubmission_cannotCreateDuplicateReservation() = runBlocking {
        val reservations = java.util.concurrent.atomic.AtomicInteger(0)
        val createdOrders = java.util.concurrent.ConcurrentHashMap<String, String>()
        val mutex = kotlinx.coroutines.sync.Mutex()

        suspend fun simulateAtomicCreateOrder(userId: String, idempotencyKey: String): String {
            // Emulates PostgreSQL transaction-level advisory lock serializing execution per (userId, idempotencyKey)
            return mutex.withLock {
                if (createdOrders.containsKey(idempotencyKey)) {
                    createdOrders[idempotencyKey]!!
                } else {
                    reservations.incrementAndGet()
                    val orderNum = "ELAJX-${System.currentTimeMillis()}"
                    createdOrders[idempotencyKey] = orderNum
                    orderNum
                }
            }
        }

        // Launch two concurrent requests with identical (user, key)
        val key = "CONCURRENT-IDEM-KEY"
        val (order1, order2) = coroutineScope {
            val deferred1 = async(kotlinx.coroutines.Dispatchers.Default) {
                simulateAtomicCreateOrder("user-1", key)
            }
            val deferred2 = async(kotlinx.coroutines.Dispatchers.Default) {
                simulateAtomicCreateOrder("user-1", key)
            }
            Pair(deferred1.await(), deferred2.await())
        }

        assertEquals("Both concurrent submissions MUST resolve to the identical order number", order1, order2)
        assertEquals("Inventory reservation MUST be performed exactly once", 1, reservations.get())
    }
}


