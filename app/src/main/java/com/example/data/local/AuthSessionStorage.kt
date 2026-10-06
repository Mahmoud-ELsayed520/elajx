package com.example.data.local

import android.content.Context
import android.content.SharedPreferences

/**
 * Storage contract for persisting authentication tokens and profile identifiers.
 *
 * CRITICAL ARCHITECTURAL RULE (06_SECURITY_PRIVACY_COMPLIANCE.md & Section 7):
 * Local data alone NEVER establishes an authenticated Supabase session.
 * Persisted tokens are loaded on cold start and must be validated with
 * Supabase Auth GET /auth/v1/user. If validation fails or token is expired,
 * local storage is wiped and the session remains Unauthenticated / Expired.
 */
interface AuthSessionStorage {
    fun getAccessToken(): String?
    fun getRefreshToken(): String?
    fun getUserId(): String?
    fun getPhone(): String?
    fun getFullName(): String?
    fun saveSession(
        accessToken: String,
        refreshToken: String?,
        userId: String,
        phone: String,
        fullName: String
    )
    fun clearSession()
}

/**
 * In-memory implementation of AuthSessionStorage for local JVM tests
 * and fallback instances.
 */
class InMemoryAuthSessionStorage : AuthSessionStorage {
    private var accessToken: String? = null
    private var refreshToken: String? = null
    private var userId: String? = null
    private var phone: String? = null
    private var fullName: String? = null

    override fun getAccessToken(): String? = accessToken
    override fun getRefreshToken(): String? = refreshToken
    override fun getUserId(): String? = userId
    override fun getPhone(): String? = phone
    override fun getFullName(): String? = fullName

    override fun saveSession(
        accessToken: String,
        refreshToken: String?,
        userId: String,
        phone: String,
        fullName: String
    ) {
        this.accessToken = accessToken
        this.refreshToken = refreshToken
        this.userId = userId
        this.phone = phone
        this.fullName = fullName
    }

    override fun clearSession() {
        accessToken = null
        refreshToken = null
        userId = null
        phone = null
        fullName = null
    }
}

/**
 * Android SharedPreferences-backed token storage.
 * Uses Context.MODE_PRIVATE.
 * Note: EncryptedSharedPreferences / Android Keystore integration for at-rest token
 * encryption is tracked as an architectural TBD for Phase 2 hardening.
 */
class SharedPreferencesAuthSessionStorage(
    context: Context
) : AuthSessionStorage {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    override fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)
    override fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)
    override fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)
    override fun getPhone(): String? = prefs.getString(KEY_PHONE, null)
    override fun getFullName(): String? = prefs.getString(KEY_FULL_NAME, null)

    override fun saveSession(
        accessToken: String,
        refreshToken: String?,
        userId: String,
        phone: String,
        fullName: String
    ) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .putString(KEY_USER_ID, userId)
            .putString(KEY_PHONE, phone)
            .putString(KEY_FULL_NAME, fullName)
            .apply()
    }

    override fun clearSession() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "elajx_auth_session_prefs"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_PHONE = "phone"
        private const val KEY_FULL_NAME = "full_name"
    }
}
