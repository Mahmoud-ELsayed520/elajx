package com.example.data.remote

import com.example.core.result.AppError
import com.example.core.result.AppResult
import com.example.core.security.SecureLogger
import com.example.data.remote.model.SupabaseAuthResponse
import com.example.data.remote.model.SupabaseErrorPayload
import com.example.data.remote.model.SupabaseOtpRequestBody
import com.example.data.remote.model.SupabaseUserResponse
import com.example.data.remote.model.SupabaseVerifyRequestBody
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Contract for executing low-level Supabase Auth REST network operations.
 */
interface SupabaseAuthClient {
    suspend fun requestOtp(baseUrl: String, headers: Map<String, String>, phone: String): AppResult<Unit>
    suspend fun verifyOtp(baseUrl: String, headers: Map<String, String>, phone: String, token: String): AppResult<SupabaseAuthResponse>
    suspend fun getCurrentUser(baseUrl: String, headers: Map<String, String>): AppResult<SupabaseUserResponse>
    suspend fun logout(baseUrl: String, headers: Map<String, String>): AppResult<Unit>
}

/**
 * Production implementation of SupabaseAuthClient using OkHttp and Moshi.
 * Strictly respects security constraints: never logs raw tokens or authorization headers.
 */
class RealSupabaseAuthClient(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
) : SupabaseAuthClient {

    private val tag = "RealSupabaseAuthClient"
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val otpRequestAdapter = moshi.adapter(SupabaseOtpRequestBody::class.java)
    private val verifyRequestAdapter = moshi.adapter(SupabaseVerifyRequestBody::class.java)
    private val authResponseAdapter = moshi.adapter(SupabaseAuthResponse::class.java)
    private val userResponseAdapter = moshi.adapter(SupabaseUserResponse::class.java)
    private val errorPayloadAdapter = moshi.adapter(SupabaseErrorPayload::class.java)

    override suspend fun requestOtp(
        baseUrl: String,
        headers: Map<String, String>,
        phone: String
    ): AppResult<Unit> = withContext(Dispatchers.IO) {
        val url = cleanUrl(baseUrl) + "/auth/v1/otp"
        val payload = otpRequestAdapter.toJson(SupabaseOtpRequestBody(phone = phone))
        val body = payload.toRequestBody(jsonMediaType)

        val requestBuilder = Request.Builder().url(url).post(body)
        headers.forEach { (key, value) -> requestBuilder.header(key, value) }

        SecureLogger.d(tag, "Executing POST /auth/v1/otp for phone: $phone")
        try {
            httpClient.newCall(requestBuilder.build()).execute().use { response ->
                if (response.isSuccessful) {
                    SecureLogger.d(tag, "Supabase /auth/v1/otp responded with HTTP ${response.code}")
                    AppResult.Success(Unit)
                } else {
                    val errorBody = response.body?.string().orEmpty()
                    handleHttpError(response.code, errorBody)
                }
            }
        } catch (e: IOException) {
            SecureLogger.w(tag, "Network failure calling /auth/v1/otp: ${e.message}")
            AppResult.Error(AppError.NetworkError("Network connection failed. Please check your internet connection."))
        } catch (e: Exception) {
            SecureLogger.e(tag, "Unexpected error in /auth/v1/otp", e)
            AppResult.Error(AppError.UnknownError(e, "Unexpected error during authentication request"))
        }
    }

    override suspend fun verifyOtp(
        baseUrl: String,
        headers: Map<String, String>,
        phone: String,
        token: String
    ): AppResult<SupabaseAuthResponse> = withContext(Dispatchers.IO) {
        val url = cleanUrl(baseUrl) + "/auth/v1/verify"
        val payload = verifyRequestAdapter.toJson(
            SupabaseVerifyRequestBody(type = "sms", phone = phone, token = token)
        )
        val body = payload.toRequestBody(jsonMediaType)

        val requestBuilder = Request.Builder().url(url).post(body)
        headers.forEach { (key, value) -> requestBuilder.header(key, value) }

        SecureLogger.d(tag, "Executing POST /auth/v1/verify for phone: $phone")
        try {
            httpClient.newCall(requestBuilder.build()).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val authResponse = try {
                        authResponseAdapter.fromJson(responseBody)
                    } catch (e: Exception) {
                        null
                    }
                    if (authResponse?.user != null) {
                        SecureLogger.i(tag, "Supabase /auth/v1/verify succeeded for user ${authResponse.user.id}")
                        AppResult.Success(authResponse)
                    } else {
                        SecureLogger.e(tag, "Supabase Auth returned 200 but null or invalid user object")
                        AppResult.Error(AppError.ServerError(500, "Invalid authentication response structure from server"))
                    }
                } else {
                    handleHttpError(response.code, responseBody)
                }
            }
        } catch (e: IOException) {
            SecureLogger.w(tag, "Network failure calling /auth/v1/verify: ${e.message}")
            AppResult.Error(AppError.NetworkError("Network connection failed. Please check your internet connection."))
        } catch (e: Exception) {
            SecureLogger.e(tag, "Unexpected error in /auth/v1/verify", e)
            AppResult.Error(AppError.UnknownError(e, "Unexpected error during OTP verification"))
        }
    }

    override suspend fun getCurrentUser(
        baseUrl: String,
        headers: Map<String, String>
    ): AppResult<SupabaseUserResponse> = withContext(Dispatchers.IO) {
        val url = cleanUrl(baseUrl) + "/auth/v1/user"
        val requestBuilder = Request.Builder().url(url).get()
        headers.forEach { (key, value) -> requestBuilder.header(key, value) }

        SecureLogger.d(tag, "Executing GET /auth/v1/user for session validation")
        try {
            httpClient.newCall(requestBuilder.build()).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val userResponse = try {
                        userResponseAdapter.fromJson(responseBody)
                    } catch (e: Exception) {
                        null
                    }
                    if (userResponse != null && userResponse.id.isNotBlank()) {
                        SecureLogger.d(tag, "Supabase session validation succeeded for user ${userResponse.id}")
                        AppResult.Success(userResponse)
                    } else {
                        AppResult.Error(AppError.ServerError(500, "Malformed user payload from Supabase Auth"))
                    }
                } else {
                    if (response.code == 401 || response.code == 403) {
                        SecureLogger.d(tag, "Supabase token expired or invalid (HTTP ${response.code})")
                        AppResult.Error(AppError.AuthenticationError("Session expired or invalid"))
                    } else {
                        handleHttpError(response.code, responseBody)
                    }
                }
            }
        } catch (e: IOException) {
            SecureLogger.w(tag, "Network failure calling /auth/v1/user: ${e.message}")
            AppResult.Error(AppError.NetworkError("Network connection failed while verifying session."))
        } catch (e: Exception) {
            SecureLogger.e(tag, "Unexpected error in /auth/v1/user", e)
            AppResult.Error(AppError.UnknownError(e, "Unexpected error during session validation"))
        }
    }

    override suspend fun logout(
        baseUrl: String,
        headers: Map<String, String>
    ): AppResult<Unit> = withContext(Dispatchers.IO) {
        val url = cleanUrl(baseUrl) + "/auth/v1/logout"
        val emptyBody = "".toRequestBody(jsonMediaType)
        val requestBuilder = Request.Builder().url(url).post(emptyBody)
        headers.forEach { (key, value) -> requestBuilder.header(key, value) }

        SecureLogger.d(tag, "Executing POST /auth/v1/logout")
        try {
            httpClient.newCall(requestBuilder.build()).execute().use { response ->
                SecureLogger.d(tag, "Supabase /auth/v1/logout response code: ${response.code}")
                AppResult.Success(Unit)
            }
        } catch (e: Exception) {
            SecureLogger.w(tag, "Non-critical error during remote logout: ${e.message}")
            AppResult.Success(Unit)
        }
    }

    private fun cleanUrl(url: String): String {
        return url.trimEnd('/')
    }

    private fun <T> handleHttpError(code: Int, errorBody: String): AppResult<T> {
        val parsed = try {
            errorPayloadAdapter.fromJson(errorBody)
        } catch (e: Exception) {
            null
        }

        val rawMsg = parsed?.errorDescription ?: parsed?.msg ?: parsed?.message ?: parsed?.error ?: ""
        SecureLogger.w(tag, "HTTP error $code from Supabase: $rawMsg")

        return when {
            code == 429 || rawMsg.contains("rate", ignoreCase = true) || rawMsg.contains("limit", ignoreCase = true) -> {
                AppResult.Error(AppError.RateLimitedError(60))
            }
            code in 400..499 -> {
                val userFacing = when {
                    rawMsg.contains("invalid", ignoreCase = true) && rawMsg.contains("token", ignoreCase = true) ->
                        "Invalid or expired verification code"
                    rawMsg.contains("expired", ignoreCase = true) ->
                        "Verification code has expired. Please request a new one."
                    rawMsg.contains("phone", ignoreCase = true) ->
                        "Invalid phone number format"
                    else ->
                        "Authentication failed. Please verify your phone number and code."
                }
                AppResult.Error(AppError.AuthenticationError(userFacing))
            }
            code in 500..599 -> {
                AppResult.Error(AppError.ServerError(code, "Authentication server is currently unavailable. Please try again later."))
            }
            else -> {
                AppResult.Error(AppError.UnknownError(null, "Authentication request failed (HTTP $code)"))
            }
        }
    }
}
