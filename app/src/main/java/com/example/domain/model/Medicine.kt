package com.example.domain.model

/**
 * Domain representation of Medicine master catalog.
 * Mapped to 04_DATA_MODEL.md (§4) and 13_API_CONTRACT.md (§16).
 */
data class Medicine(
    val id: String,
    val nameAr: String,
    val nameEn: String,
    val normalizedName: String,
    val activeIngredient: String,
    val strength: String,
    val dosageForm: String,
    val manufacturer: String? = null,
    val barcode: String? = null,
    val prescriptionRequired: Boolean = false,
    val status: String = "ACTIVE"
)

/**
 * Package variant of a medicine concept.
 * Mapped to 04_DATA_MODEL.md (§5).
 */
data class MedicineVariant(
    val id: String,
    val medicineId: String,
    val brandName: String,
    val packageSize: String,
    val barcode: String? = null,
    val priceReference: Double? = null,
    val active: Boolean = true
)

/**
 * Client-safe representation of a participating partner pharmacy.
 * Respects 01_PRODUCT_SPEC.md (§9): Hides name_private and internal contact numbers.
 */
data class Pharmacy(
    val id: String,
    val displayCode: String,
    val governorate: String,
    val city: String,
    val area: String,
    val status: String = "ACTIVE"
)

/**
 * Authoritative availability status values per 01_PRODUCT_SPEC.md (§8) and 04_DATA_MODEL.md (§7).
 */
enum class AvailabilityStatus {
    AVAILABLE,
    LOW_STOCK,
    OUT_OF_STOCK,
    RESERVED,
    UNAVAILABLE,
    EXPIRED;

    companion object {
        fun fromBackend(value: String?): AvailabilityStatus {
            return when (value?.uppercase()?.trim()) {
                "AVAILABLE" -> AVAILABLE
                "LOW_STOCK" -> LOW_STOCK
                "OUT_OF_STOCK" -> OUT_OF_STOCK
                "RESERVED" -> RESERVED
                "UNAVAILABLE" -> UNAVAILABLE
                "EXPIRED" -> EXPIRED
                else -> UNAVAILABLE
            }
        }
    }
}

/**
 * Pharmacy inventory item representation.
 * Excludes reserved_quantity to respect privacy and data boundaries.
 */
data class PharmacyInventory(
    val id: String,
    val pharmacyId: String,
    val pharmacyDisplayCode: String,
    val pharmacyArea: String,
    val medicineVariantId: String,
    val availabilityStatus: AvailabilityStatus,
    val price: Double,
    val lastSyncedAt: String
)

/**
 * Aggregate model combining medicine details, its package variants, and live partner pharmacy availability.
 */
data class MedicineDetail(
    val medicine: Medicine,
    val variants: List<MedicineVariant> = emptyList(),
    val availability: List<PharmacyInventory> = emptyList(),
    val alternatives: List<Medicine> = emptyList()
)
