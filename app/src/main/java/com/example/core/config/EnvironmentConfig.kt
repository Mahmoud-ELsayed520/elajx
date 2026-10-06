package com.example.core.config

/**
 * Environment configuration for ElajX.
 *
 * Adheres to 12_ENVIRONMENT_CONFIG.md, 06_SECURITY_PRIVACY_COMPLIANCE.md, and Phase 2 remediation:
 * CRITICAL SECURITY INVARIANT:
 * 1. The Android client is an untrusted environment.
 * 2. ONLY client-safe anonymous keys and public endpoints are ever configured here.
 * 3. Never embed SUPABASE_SERVICE_ROLE_KEY, payment secrets, or server API keys.
 * 4. isSimulationModeAllowed == true is strictly prohibited in STAGING and PRODUCTION.
 */
enum class AppEnvironment {
    DEVELOPMENT,
    STAGING,
    PRODUCTION
}

data class EnvironmentConfig(
    val environment: AppEnvironment,
    val supabaseUrl: String,
    val supabaseAnonKey: String,
    val apiBaseUrl: String,
    val isDebugLoggingEnabled: Boolean,
    val isSimulationModeAllowed: Boolean
) {
    init {
        // Enforce the security invariant in executable code:
        // isSimulationModeAllowed == true must be possible ONLY for AppEnvironment.DEVELOPMENT
        require(environment == AppEnvironment.DEVELOPMENT || !isSimulationModeAllowed) {
            "CRITICAL SECURITY VIOLATION: Simulation mode is strictly prohibited in ${environment.name} environment."
        }
    }

    companion object {
        /**
         * Resolves the active environment configuration.
         * Default is DEVELOPMENT with simulation explicitly disabled (requiring real auth).
         * Simulation mode can ONLY be enabled in DEVELOPMENT via allowSimulation = true.
         */
        fun current(
            env: AppEnvironment = AppEnvironment.DEVELOPMENT,
            allowSimulation: Boolean = false
        ): EnvironmentConfig {
            return when (env) {
                AppEnvironment.DEVELOPMENT -> EnvironmentConfig(
                    environment = AppEnvironment.DEVELOPMENT,
                    supabaseUrl = "https://elajk-db.supabase.co",
                    supabaseAnonKey = "sb_anon_client_safe_dev_placeholder",
                    apiBaseUrl = "https://elajk-db.supabase.co/rest/v1",
                    isDebugLoggingEnabled = true,
                    isSimulationModeAllowed = allowSimulation
                )
                AppEnvironment.STAGING -> EnvironmentConfig(
                    environment = AppEnvironment.STAGING,
                    supabaseUrl = "https://staging-elajk.supabase.co",
                    supabaseAnonKey = "sb_anon_client_safe_staging_placeholder",
                    apiBaseUrl = "https://staging-elajk.supabase.co/rest/v1",
                    isDebugLoggingEnabled = true,
                    isSimulationModeAllowed = false
                )
                AppEnvironment.PRODUCTION -> EnvironmentConfig(
                    environment = AppEnvironment.PRODUCTION,
                    supabaseUrl = "https://elajk-db.supabase.co",
                    supabaseAnonKey = "sb_anon_client_safe_prod_placeholder",
                    apiBaseUrl = "https://elajk-db.supabase.co/rest/v1",
                    isDebugLoggingEnabled = false,
                    isSimulationModeAllowed = false
                )
            }
        }
    }
}
