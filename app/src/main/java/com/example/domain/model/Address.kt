package com.example.domain.model

/**
 * Domain representation of a patient delivery address.
 * Conforms to 04_DATA_MODEL.md (§3).
 */
data class Address(
    val id: String,
    val userId: String,
    val label: String,
    val governorate: String,
    val city: String,
    val area: String,
    val street: String,
    val building: String,
    val apartment: String? = null,
    val floor: String? = null,
    val landmark: String? = null,
    val isDefault: Boolean = false
) {
    val formattedAddress: String
        get() = buildString {
            append("$building $street, $area, $city, $governorate")
            if (!apartment.isNullOrBlank()) append(" (Apt $apartment)")
            if (!floor.isNullOrBlank()) append(" (Floor $floor)")
            if (!landmark.isNullOrBlank()) append(" - Near $landmark")
        }
}
