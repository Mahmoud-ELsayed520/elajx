package com.example.data.remote

import com.example.core.result.AppError
import com.example.core.result.AppResult
import com.example.core.security.SecureLogger
import com.example.data.remote.model.AddressDto
import com.example.data.remote.model.CreateAddressRequestDto
import com.example.data.remote.model.CreateOrderRpcRequest
import com.example.data.remote.model.OrderResponseDto
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

interface SupabaseOrderClient {
    suspend fun createOrder(
        baseUrl: String,
        headers: Map<String, String>,
        request: CreateOrderRpcRequest
    ): AppResult<OrderResponseDto>

    suspend fun getUserAddresses(
        baseUrl: String,
        headers: Map<String, String>
    ): AppResult<List<AddressDto>>

    suspend fun createAddress(
        baseUrl: String,
        headers: Map<String, String>,
        request: CreateAddressRequestDto
    ): AppResult<AddressDto>
}

class RealSupabaseOrderClient(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
) : SupabaseOrderClient {

    private val tag = "RealSupabaseOrderClient"
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val createOrderRequestAdapter = moshi.adapter(CreateOrderRpcRequest::class.java)
    private val orderResponseAdapter = moshi.adapter(OrderResponseDto::class.java)
    private val addressListAdapter = moshi.adapter<List<AddressDto>>(
        Types.newParameterizedType(List::class.java, AddressDto::class.java)
    )
    private val createAddressRequestAdapter = moshi.adapter(CreateAddressRequestDto::class.java)
    private val addressAdapter = moshi.adapter(AddressDto::class.java)

    override suspend fun createOrder(
        baseUrl: String,
        headers: Map<String, String>,
        request: CreateOrderRpcRequest
    ): AppResult<OrderResponseDto> = withContext(Dispatchers.IO) {
        val url = baseUrl.trimEnd('/') + "/rest/v1/rpc/create_order"
        val payload = createOrderRequestAdapter.toJson(request)
        val body = payload.toRequestBody(jsonMediaType)

        val requestBuilder = Request.Builder().url(url).post(body)
        headers.forEach { (key, value) -> requestBuilder.header(key, value) }

        SecureLogger.i(tag, "Executing atomic create_order RPC (Idempotency Key: ${request.idempotencyKey.take(8)}...)")
        try {
            httpClient.newCall(requestBuilder.build()).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val orderDto = try {
                        orderResponseAdapter.fromJson(responseBody)
                    } catch (e: Exception) {
                        null
                    }
                    if (orderDto != null) {
                        SecureLogger.i(tag, "create_order RPC succeeded: Order #${orderDto.publicOrderNumber}, Total=${orderDto.total}")
                        AppResult.Success(orderDto)
                    } else {
                        SecureLogger.e(tag, "Failed to parse create_order RPC response")
                        AppResult.Error(AppError.ServerError(500, "Invalid order confirmation response format from server"))
                    }
                } else {
                    handleHttpError(response.code, responseBody)
                }
            }
        } catch (e: IOException) {
            SecureLogger.w(tag, "Network failure calling create_order: ${e.message}")
            AppResult.Error(AppError.NetworkError("Network connection failed during order submission. Please retry."))
        } catch (e: Exception) {
            SecureLogger.e(tag, "Unexpected error in create_order RPC", e)
            AppResult.Error(AppError.UnknownError(e, "Unexpected error creating order"))
        }
    }

    override suspend fun getUserAddresses(
        baseUrl: String,
        headers: Map<String, String>
    ): AppResult<List<AddressDto>> = withContext(Dispatchers.IO) {
        val url = baseUrl.trimEnd('/') + "/rest/v1/addresses?select=*&order=is_default.desc,created_at.desc"
        val requestBuilder = Request.Builder().url(url).get()
        headers.forEach { (key, value) -> requestBuilder.header(key, value) }

        try {
            httpClient.newCall(requestBuilder.build()).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val addresses = addressListAdapter.fromJson(responseBody) ?: emptyList()
                    AppResult.Success(addresses)
                } else {
                    handleHttpError(response.code, responseBody)
                }
            }
        } catch (e: IOException) {
            AppResult.Error(AppError.NetworkError("Network connection failed fetching addresses."))
        } catch (e: Exception) {
            AppResult.Error(AppError.UnknownError(e))
        }
    }

    override suspend fun createAddress(
        baseUrl: String,
        headers: Map<String, String>,
        request: CreateAddressRequestDto
    ): AppResult<AddressDto> = withContext(Dispatchers.IO) {
        val url = baseUrl.trimEnd('/') + "/rest/v1/addresses"
        val payload = createAddressRequestAdapter.toJson(request)
        val body = payload.toRequestBody(jsonMediaType)

        val requestBuilder = Request.Builder()
            .url(url)
            .post(body)
            .header("Prefer", "return=representation")
        headers.forEach { (key, value) -> requestBuilder.header(key, value) }

        try {
            httpClient.newCall(requestBuilder.build()).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    // PostgREST return=representation returns a JSON array with the created row
                    val created = try {
                        val list = addressListAdapter.fromJson(responseBody)
                        list?.firstOrNull() ?: addressAdapter.fromJson(responseBody)
                    } catch (e: Exception) {
                        addressAdapter.fromJson(responseBody)
                    }
                    if (created != null) {
                        AppResult.Success(created)
                    } else {
                        AppResult.Error(AppError.ServerError(500, "Unable to parse created address response"))
                    }
                } else {
                    handleHttpError(response.code, responseBody)
                }
            }
        } catch (e: IOException) {
            AppResult.Error(AppError.NetworkError("Network connection failed saving address."))
        } catch (e: Exception) {
            AppResult.Error(AppError.UnknownError(e))
        }
    }

    private fun <T> handleHttpError(code: Int, errorBody: String): AppResult<T> {
        SecureLogger.w(tag, "HTTP error $code from Supabase: $errorBody")
        return when {
            code == 401 || code == 403 || errorBody.contains("AUTH_REQUIRED", ignoreCase = true) -> {
                AppResult.Error(AppError.AuthenticationError("Session expired or authentication required"))
            }
            errorBody.contains("INSUFFICIENT_STOCK", ignoreCase = true) || errorBody.contains("OUT_OF_STOCK", ignoreCase = true) -> {
                AppResult.Error(AppError.ValidationError("Requested medicine is currently out of stock or exceeds available quantity", "inventory"))
            }
            errorBody.contains("ADDRESS_NOT_FOUND", ignoreCase = true) -> {
                AppResult.Error(AppError.ValidationError("Delivery address not found or does not belong to you", "address"))
            }
            errorBody.contains("PHARMACY_UNAVAILABLE", ignoreCase = true) -> {
                AppResult.Error(AppError.ValidationError("Selected pharmacy is currently unavailable", "pharmacy"))
            }
            errorBody.contains("CART_EMPTY", ignoreCase = true) -> {
                AppResult.Error(AppError.ValidationError("Cannot place order with an empty cart", "cart"))
            }
            code == 409 || errorBody.contains("conflict", ignoreCase = true) -> {
                AppResult.Error(AppError.ConflictError("Order submission conflict. Please retry."))
            }
            code in 400..499 -> {
                AppResult.Error(AppError.ValidationError("Order request could not be processed. Please verify your details."))
            }
            else -> {
                AppResult.Error(AppError.ServerError(code, "Server error processing order. Please try again later."))
            }
        }
    }
}
