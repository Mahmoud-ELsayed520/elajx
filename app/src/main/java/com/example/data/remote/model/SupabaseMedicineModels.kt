package com.example.data.remote.model

import com.example.domain.model.AvailabilityStatus
import com.example.domain.model.Medicine
import com.example.domain.model.MedicineVariant
import com.example.domain.model.PharmacyInventory
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SupabaseMedicineDto(
    @Json(name = "id") val id: String,
    @Json(name = "name_ar") val nameAr: String? = null,
    @Json(name = "name_en") val nameEn: String? = null,
    @Json(name = "normalized_name") val normalizedName: String? = null,
    @Json(name = "active_ingredient") val activeIngredient: String? = null,
    @Json(name = "strength") val strength: String? = null,
    @Json(name = "dosage_form") val dosageForm: String? = null,
    @Json(name = "manufacturer") val manufacturer: String? = null,
    @Json(name = "barcode") val barcode: String? = null,
    @Json(name = "prescription_required") val prescriptionRequired: Boolean? = false,
    @Json(name = "status") val status: String? = "ACTIVE"
) {
    fun toDomain(): Medicine {
        return Medicine(
            id = id,
            nameAr = nameAr ?: nameEn ?: "",
            nameEn = nameEn ?: nameAr ?: "",
            normalizedName = normalizedName ?: (nameEn ?: nameAr ?: ""),
            activeIngredient = activeIngredient ?: "",
            strength = strength ?: "",
            dosageForm = dosageForm ?: "",
            manufacturer = manufacturer,
            barcode = barcode,
            prescriptionRequired = prescriptionRequired ?: false,
            status = status ?: "ACTIVE"
        )
    }
}

@JsonClass(generateAdapter = true)
data class SupabaseMedicineVariantDto(
    @Json(name = "id") val id: String,
    @Json(name = "medicine_id") val medicineId: String,
    @Json(name = "brand_name") val brandName: String? = null,
    @Json(name = "package_size") val packageSize: String? = null,
    @Json(name = "barcode") val barcode: String? = null,
    @Json(name = "price_reference") val priceReference: Double? = null,
    @Json(name = "active") val active: Boolean? = true
) {
    fun toDomain(): MedicineVariant {
        return MedicineVariant(
            id = id,
            medicineId = medicineId,
            brandName = brandName ?: "",
            packageSize = packageSize ?: "",
            barcode = barcode,
            priceReference = priceReference,
            active = active ?: true
        )
    }
}

@JsonClass(generateAdapter = true)
data class SupabasePharmacyDto(
    @Json(name = "display_code") val displayCode: String? = null,
    @Json(name = "governorate") val governorate: String? = null,
    @Json(name = "city") val city: String? = null,
    @Json(name = "area") val area: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabasePharmacyInventoryDto(
    @Json(name = "id") val id: String,
    @Json(name = "pharmacy_id") val pharmacyId: String,
    @Json(name = "medicine_variant_id") val medicineVariantId: String,
    @Json(name = "availability_status") val availabilityStatus: String? = null,
    @Json(name = "price") val price: Double? = null,
    @Json(name = "last_synced_at") val lastSyncedAt: String? = null,
    @Json(name = "pharmacies") val pharmacies: SupabasePharmacyDto? = null
) {
    fun toDomain(): PharmacyInventory {
        return PharmacyInventory(
            id = id,
            pharmacyId = pharmacyId,
            pharmacyDisplayCode = pharmacies?.displayCode ?: "PARTNER-${pharmacyId.take(4).uppercase()}",
            pharmacyArea = listOfNotNull(pharmacies?.area, pharmacies?.city).joinToString(", ").ifBlank { "Cairo" },
            medicineVariantId = medicineVariantId,
            availabilityStatus = AvailabilityStatus.fromBackend(availabilityStatus),
            price = price ?: 0.0,
            lastSyncedAt = lastSyncedAt ?: ""
        )
    }
}
