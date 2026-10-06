package com.example.data.remote.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Request payload for Supabase Auth OTP initiation.
 * Spec: POST /auth/v1/otp
 */
@JsonClass(generateAdapter = true)
data class SupabaseOtpRequestBody(
    @Json(name = "phone") val phone: String
)

/**
 * Request payload for Supabase Auth OTP verification.
 * Spec: POST /auth/v1/verify
 * Exact payload required by 03_TECH_ARCHITECTURE.md & 13_API_CONTRACT.md:
 * {
 *   "type": "sms",
 *   "phone": normalizedPhone,
 *   "token": otpToken
 * }
 */
@JsonClass(generateAdapter = true)
data class SupabaseVerifyRequestBody(
    @Json(name = "type") val type: String = "sms",
    @Json(name = "phone") val phone: String,
    @Json(name = "token") val token: String
)

/**
 * User metadata container from Supabase Auth user object.
 */
@JsonClass(generateAdapter = true)
data class SupabaseUserMetadata(
    @Json(name = "full_name") val fullName: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "locale") val locale: String? = null
)

/**
 * Supabase Auth user object representation.
 */
@JsonClass(generateAdapter = true)
data class SupabaseUserResponse(
    @Json(name = "id") val id: String,
    @Json(name = "phone") val phone: String? = null,
    @Json(name = "email") val email: String? = null,
    @Json(name = "user_metadata") val userMetadata: SupabaseUserMetadata? = null
)

/**
 * Successful authentication response returned by Supabase /auth/v1/verify.
 */
@JsonClass(generateAdapter = true)
data class SupabaseAuthResponse(
    @Json(name = "access_token") val accessToken: String? = null,
    @Json(name = "token_type") val tokenType: String? = null,
    @Json(name = "expires_in") val expiresIn: Long? = null,
    @Json(name = "refresh_token") val refreshToken: String? = null,
    @Json(name = "user") val user: SupabaseUserResponse? = null
)

/**
 * Standard Supabase Auth error payload structure.
 */
@JsonClass(generateAdapter = true)
data class SupabaseErrorPayload(
    @Json(name = "code") val code: Int? = null,
    @Json(name = "msg") val msg: String? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "error") val error: String? = null,
    @Json(name = "error_description") val errorDescription: String? = null
)
