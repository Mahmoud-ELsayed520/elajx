package com.example.data.remote

import com.example.core.result.AppError
import com.example.core.result.AppResult
import com.example.core.security.SecureLogger
import com.example.data.remote.model.SupabaseMedicineDto
import com.example.data.remote.model.SupabaseMedicineVariantDto
import com.example.data.remote.model.SupabasePharmacyInventoryDto
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

/**
 * Low-level client for Supabase PostgREST medicine queries.
 * Ensures strictly URL-encoded queries, anonymous key usage, and zero leaks of internal/private fields.
 */
interface SupabaseMedicineClient {
    suspend fun searchMedicines(
        baseUrl: String,
        headers: Map<String, String>,
        query: String
    ): AppResult<List<SupabaseMedicineDto>>

    suspend fun getMedicineById(
        baseUrl: String,
        headers: Map<String, String>,
        medicineId: String
    ): AppResult<SupabaseMedicineDto>

    suspend fun getVariantsByMedicineId(
        baseUrl: String,
        headers: Map<String, String>,
        medicineId: String
    ): AppResult<List<SupabaseMedicineVariantDto>>

    suspend fun getInventoryByVariantIds(
        baseUrl: String,
        headers: Map<String, String>,
        variantIds: List<String>
    ): AppResult<List<SupabasePharmacyInventoryDto>>

    suspend fun getAlternatives(
        baseUrl: String,
        headers: Map<String, String>,
        activeIngredient: String,
        strength: String,
        dosageForm: String,
        excludeId: String
    ): AppResult<List<SupabaseMedicineDto>>
}

class RealSupabaseMedicineClient(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
) : SupabaseMedicineClient {

    private val tag = "RealSupabaseMedicineClient"
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val medicineListType = Types.newParameterizedType(List::class.java, SupabaseMedicineDto::class.java)
    private val medicineListAdapter = moshi.adapter<List<SupabaseMedicineDto>>(medicineListType)
    private val variantListType = Types.newParameterizedType(List::class.java, SupabaseMedicineVariantDto::class.java)
    private val variantListAdapter = moshi.adapter<List<SupabaseMedicineVariantDto>>(variantListType)
    private val inventoryListType = Types.newParameterizedType(List::class.java, SupabasePharmacyInventoryDto::class.java)
    private val inventoryListAdapter = moshi.adapter<List<SupabasePharmacyInventoryDto>>(inventoryListType)

    override suspend fun searchMedicines(
        baseUrl: String,
        headers: Map<String, String>,
        query: String
    ): AppResult<List<SupabaseMedicineDto>> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return@withContext AppResult.Success(emptyList())

        // Sanitize and URL encode search token
        val encodedToken = URLEncoder.encode("*$trimmed*", StandardCharsets.UTF_8.name())
        val encodedOrFilter = "or=(name_ar.ilike.$encodedToken,name_en.ilike.$encodedToken,active_ingredient.ilike.$encodedToken,normalized_name.ilike.$encodedToken)"
        val url = "$baseUrl/rest/v1/medicines?$encodedOrFilter&status=eq.ACTIVE&select=id,name_ar,name_en,normalized_name,active_ingredient,strength,dosage_form,manufacturer,barcode,prescription_required,status&limit=50"

        executeGetList(url, headers, medicineListAdapter)
    }

    override suspend fun getMedicineById(
        baseUrl: String,
        headers: Map<String, String>,
        medicineId: String
    ): AppResult<SupabaseMedicineDto> = withContext(Dispatchers.IO) {
        val encodedId = URLEncoder.encode(medicineId, StandardCharsets.UTF_8.name())
        val url = "$baseUrl/rest/v1/medicines?id=eq.$encodedId&select=id,name_ar,name_en,normalized_name,active_ingredient,strength,dosage_form,manufacturer,barcode,prescription_required,status&limit=1"

        when (val result = executeGetList(url, headers, medicineListAdapter)) {
            is AppResult.Success -> {
                val med = result.data.firstOrNull()
                if (med != null) AppResult.Success(med) else AppResult.Error(AppError.NotFoundError("Medicine with ID $medicineId not found"))
            }
            is AppResult.Error -> result
        }
    }

    override suspend fun getVariantsByMedicineId(
        baseUrl: String,
        headers: Map<String, String>,
        medicineId: String
    ): AppResult<List<SupabaseMedicineVariantDto>> = withContext(Dispatchers.IO) {
        val encodedId = URLEncoder.encode(medicineId, StandardCharsets.UTF_8.name())
        val url = "$baseUrl/rest/v1/medicine_variants?medicine_id=eq.$encodedId&active=eq.true&select=id,medicine_id,brand_name,package_size,barcode,price_reference,active"
        executeGetList(url, headers, variantListAdapter)
    }

    override suspend fun getInventoryByVariantIds(
        baseUrl: String,
        headers: Map<String, String>,
        variantIds: List<String>
    ): AppResult<List<SupabasePharmacyInventoryDto>> = withContext(Dispatchers.IO) {
        if (variantIds.isEmpty()) return@withContext AppResult.Success(emptyList())
        val encodedIds = variantIds.joinToString(",") { URLEncoder.encode(it, StandardCharsets.UTF_8.name()) }
        // Excludes reserved_quantity and name_private per specifications
        val url = "$baseUrl/rest/v1/pharmacy_inventory?medicine_variant_id=in.($encodedIds)&select=id,pharmacy_id,medicine_variant_id,availability_status,price,last_synced_at,pharmacies(display_code,governorate,city,area)&order=price.asc"
        executeGetList(url, headers, inventoryListAdapter)
    }

    override suspend fun getAlternatives(
        baseUrl: String,
        headers: Map<String, String>,
        activeIngredient: String,
        strength: String,
        dosageForm: String,
        excludeId: String
    ): AppResult<List<SupabaseMedicineDto>> = withContext(Dispatchers.IO) {
        if (activeIngredient.isBlank()) return@withContext AppResult.Success(emptyList())

        val encodedIngredient = URLEncoder.encode(activeIngredient, StandardCharsets.UTF_8.name())
        val encodedExcludeId = URLEncoder.encode(excludeId, StandardCharsets.UTF_8.name())
        val encodedStrength = URLEncoder.encode(strength, StandardCharsets.UTF_8.name())
        val encodedDosageForm = URLEncoder.encode(dosageForm, StandardCharsets.UTF_8.name())

        // Query active medicines with exact active_ingredient, strength, and dosage_form, excluding current id
        val url = "$baseUrl/rest/v1/medicines?active_ingredient=eq.$encodedIngredient&strength=eq.$encodedStrength&dosage_form=eq.$encodedDosageForm&id=neq.$encodedExcludeId&status=eq.ACTIVE&select=id,name_ar,name_en,normalized_name,active_ingredient,strength,dosage_form,manufacturer,barcode,prescription_required,status&limit=10"
        executeGetList(url, headers, medicineListAdapter)
    }

    private fun <T> executeGetList(
        url: String,
        headers: Map<String, String>,
        adapter: com.squareup.moshi.JsonAdapter<List<T>>
    ): AppResult<List<T>> {
        return try {
            val requestBuilder = Request.Builder().url(url)
            headers.forEach { (key, value) -> requestBuilder.addHeader(key, value) }

            val response = httpClient.newCall(requestBuilder.build()).execute()
            val bodyString = response.body?.string()

            if (response.isSuccessful && bodyString != null) {
                val list = adapter.fromJson(bodyString) ?: emptyList()
                AppResult.Success(list)
            } else {
                SecureLogger.e(tag, "PostgREST request failed with HTTP ${response.code}")
                AppResult.Error(AppError.ServerError(response.code, "Backend catalog request failed with status ${response.code}"))
            }
        } catch (e: IOException) {
            SecureLogger.w(tag, "Network exception querying catalog: ${e.message}")
            AppResult.Error(AppError.NetworkError("Network connectivity error: ${e.localizedMessage}"))
        } catch (e: Exception) {
            SecureLogger.e(tag, "Unexpected error parsing catalog response", e)
            AppResult.Error(AppError.UnknownError(e))
        }
    }
}
