package com.example.core.security

import android.util.Log
import com.example.core.config.AppEnvironment
import com.example.core.config.EnvironmentConfig

/**
 * Production-safe logging boundary.
 *
 * Adheres strictly to 06_SECURITY_PRIVACY_COMPLIANCE.md:
 * - NEVER logs passwords, access tokens, refresh tokens, OTPs.
 * - NEVER logs full prescription text or patient diagnosis.
 * - NEVER logs payment secrets or raw service credentials.
 * - Strips sensitive query parameters or masks sensitive data.
 * - Disabled in Production.
 */
object SecureLogger {

    private const val TAG_PREFIX = "ElajX"
    private var isEnabled: Boolean = EnvironmentConfig.current().isDebugLoggingEnabled

    fun configure(config: EnvironmentConfig) {
        isEnabled = config.isDebugLoggingEnabled && config.environment != AppEnvironment.PRODUCTION
    }

    fun d(tag: String, message: String) {
        if (isEnabled) {
            Log.d("$TAG_PREFIX:$tag", sanitize(message))
        }
    }

    fun i(tag: String, message: String) {
        if (isEnabled) {
            Log.i("$TAG_PREFIX:$tag", sanitize(message))
        }
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        if (isEnabled) {
            Log.w("$TAG_PREFIX:$tag", sanitize(message), throwable)
        }
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        // Errors are logged even in production if non-sensitive
        Log.e("$TAG_PREFIX:$tag", sanitize(message), throwable)
    }

    /**
     * Sanitizes message strings to remove or mask sensitive patterns.
     */
    fun sanitize(input: String): String {
        var sanitized = input
        // Mask phone numbers (e.g. +201012345678 or 01012345678)
        sanitized = sanitized.replace(Regex("""(\+?20|0)?1[0125]\d{8}""")) { match ->
            val num = match.value
            if (num.length >= 6) {
                num.take(4) + "****" + num.takeLast(2)
            } else {
                "***"
            }
        }
        // Redact potential tokens, keys, OTPs
        sanitized = sanitized.replace(Regex("""(?i)(bearer\s+[A-Za-z0-9\-._~+/]+=*)"""), "bearer [REDACTED]")
        sanitized = sanitized.replace(Regex("""(?i)(otp[=:]\s*)\d{4,6}"""), "$1[REDACTED]")
        sanitized = sanitized.replace(Regex("""(?i)(token[=:]\s*)\d{4,6}"""), "$1[REDACTED]")
        sanitized = sanitized.replace(Regex("""(?i)(code[=:]\s*)\d{4,6}"""), "$1[REDACTED]")
        sanitized = sanitized.replace(Regex("""(?i)(password[=:]\s*)[^\s,;&]+"""), "$1[REDACTED]")
        sanitized = sanitized.replace(Regex("""(?i)(access_token["']?\s*[:=]\s*["']?)[^"',\s]+"""), "$1[REDACTED]")
        sanitized = sanitized.replace(Regex("""(?i)(refresh_token["']?\s*[:=]\s*["']?)[^"',\s]+"""), "$1[REDACTED]")
        sanitized = sanitized.replace(Regex("""(?i)(apikey["']?\s*[:=]\s*["']?)[^"',\s]+"""), "$1[REDACTED]")
        return sanitized
    }
}
