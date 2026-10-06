package com.example.domain.repository

import com.example.core.result.AppResult
import com.example.domain.model.Medicine
import com.example.domain.model.MedicineDetail

/**
 * Domain repository contract for medicine discovery, search, details, and partner pharmacy availability.
 * Sourced directly from 03_TECH_ARCHITECTURE.md (§9), 10_FEATURE_REQUIREMENT_MATRIX.md (F-002, F-004, F-005, F-008),
 * and 13_API_CONTRACT.md (§15, §16).
 */
interface MedicineRepository {

    /**
     * Searches authoritative backend medicine catalog across Arabic name, English name,
     * normalized name, active ingredient, and strength.
     * Guaranteed URL/query parameter safety and no fabricated records.
     */
    suspend fun searchMedicines(query: String): AppResult<List<Medicine>>

    /**
     * Retrieves full medicine details including package variants, partner pharmacy availability,
     * and strictly matched alternatives by active ingredient, strength, and dosage form.
     */
    suspend fun getMedicineDetails(medicineId: String): AppResult<MedicineDetail>

    /**
     * Retrieves partner pharmacy availability for a specific medicine variant or set of variants.
     */
    suspend fun getAvailability(variantIds: List<String>): AppResult<List<com.example.domain.model.PharmacyInventory>>

    /**
     * Retrieves generic alternatives matching active ingredient, strength, and dosage form.
     * Excludes current medicine ID and enforces safety disclaimer.
     */
    suspend fun getAlternatives(
        activeIngredient: String,
        strength: String,
        dosageForm: String,
        excludeMedicineId: String
    ): AppResult<List<Medicine>>
}
