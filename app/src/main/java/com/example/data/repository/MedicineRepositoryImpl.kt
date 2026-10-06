package com.example.data.repository

import com.example.core.config.EnvironmentConfig
import com.example.core.result.AppResult
import com.example.core.security.SecureLogger
import com.example.data.remote.RealSupabaseMedicineClient
import com.example.data.remote.SupabaseBoundary
import com.example.data.remote.SupabaseMedicineClient
import com.example.domain.model.Medicine
import com.example.domain.model.MedicineDetail
import com.example.domain.model.PharmacyInventory
import com.example.domain.repository.MedicineRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Production implementation of MedicineRepository communicating with Supabase PostgreSQL via PostgREST.
 *
 * Implements:
 * - Authoritative backend queries across Arabic, English, active ingredient, and strength.
 * - Clean safe read caching in-memory without masquerading as live authoritative stock.
 * - Strict field privacy (never exposes name_private or reserved_quantity).
 * - Exact matching for alternatives by active_ingredient, strength, and dosage_form.
 */
class MedicineRepositoryImpl(
    private val config: EnvironmentConfig = EnvironmentConfig.current(),
    private val supabaseBoundary: SupabaseBoundary = SupabaseBoundary(config),
    private val medicineClient: SupabaseMedicineClient = RealSupabaseMedicineClient()
) : MedicineRepository {

    private val tag = "MedicineRepositoryImpl"

    // Safe in-memory read cache to optimize repeated reads without masquerading as real-time stock
    private val medicineCache = mutableMapOf<String, Medicine>()

    override suspend fun searchMedicines(query: String): AppResult<List<Medicine>> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            return@withContext AppResult.Success(emptyList())
        }

        SecureLogger.d(tag, "Searching medicine catalog for query token: ${trimmed.take(3)}***")
        val headers = supabaseBoundary.getClientSafeHeaders()
        val result = medicineClient.searchMedicines(
            baseUrl = config.supabaseUrl,
            headers = headers,
            query = trimmed
        )

        result.map { dtoList ->
            dtoList.map { dto ->
                val med = dto.toDomain()
                medicineCache[med.id] = med
                med
            }
        }
    }

    override suspend fun getMedicineDetails(medicineId: String): AppResult<MedicineDetail> = withContext(Dispatchers.IO) {
        SecureLogger.d(tag, "Retrieving details for medicine ID: $medicineId")
        val headers = supabaseBoundary.getClientSafeHeaders()

        // 1. Fetch Medicine Master Record
        val medicineResult = medicineClient.getMedicineById(
            baseUrl = config.supabaseUrl,
            headers = headers,
            medicineId = medicineId
        )

        val medicine = when (medicineResult) {
            is AppResult.Success -> medicineResult.data.toDomain()
            is AppResult.Error -> return@withContext medicineResult
        }
        medicineCache[medicine.id] = medicine

        // 2. Fetch Package Variants
        val variantsResult = medicineClient.getVariantsByMedicineId(
            baseUrl = config.supabaseUrl,
            headers = headers,
            medicineId = medicineId
        )
        val variants = variantsResult.getOrNull()?.map { it.toDomain() } ?: emptyList()

        // 3. Fetch Pharmacy Availability for Variants
        val variantIds = variants.map { it.id }.ifEmpty { listOf(medicineId) }
        val inventoryResult = medicineClient.getInventoryByVariantIds(
            baseUrl = config.supabaseUrl,
            headers = headers,
            variantIds = variantIds
        )
        val availability = inventoryResult.getOrNull()?.map { it.toDomain() } ?: emptyList()

        // 4. Fetch Curated Alternatives (same active_ingredient, strength, and dosage_form)
        val alternativesResult = medicineClient.getAlternatives(
            baseUrl = config.supabaseUrl,
            headers = headers,
            activeIngredient = medicine.activeIngredient,
            strength = medicine.strength,
            dosageForm = medicine.dosageForm,
            excludeId = medicine.id
        )
        val alternatives = alternativesResult.getOrNull()?.map { it.toDomain() } ?: emptyList()

        AppResult.Success(
            MedicineDetail(
                medicine = medicine,
                variants = variants,
                availability = availability,
                alternatives = alternatives
            )
        )
    }

    override suspend fun getAvailability(variantIds: List<String>): AppResult<List<PharmacyInventory>> = withContext(Dispatchers.IO) {
        if (variantIds.isEmpty()) return@withContext AppResult.Success(emptyList())

        val headers = supabaseBoundary.getClientSafeHeaders()
        val result = medicineClient.getInventoryByVariantIds(
            baseUrl = config.supabaseUrl,
            headers = headers,
            variantIds = variantIds
        )
        result.map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getAlternatives(
        activeIngredient: String,
        strength: String,
        dosageForm: String,
        excludeMedicineId: String
    ): AppResult<List<Medicine>> = withContext(Dispatchers.IO) {
        val headers = supabaseBoundary.getClientSafeHeaders()
        val result = medicineClient.getAlternatives(
            baseUrl = config.supabaseUrl,
            headers = headers,
            activeIngredient = activeIngredient,
            strength = strength,
            dosageForm = dosageForm,
            excludeId = excludeMedicineId
        )
        result.map { list -> list.map { it.toDomain() } }
    }
}
