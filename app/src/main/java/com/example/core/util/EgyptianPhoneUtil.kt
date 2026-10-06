package com.example.core.util

import com.example.core.result.AppError
import com.example.core.result.AppResult

/**
 * Egyptian mobile number validation and E.164 normalization.
 *
 * Sourced from 02_UX_UI_SPEC.md (Section 13) & 06_SECURITY_PRIVACY_COMPLIANCE.md:
 * - Egyptian mobile networks: 010 (Vodafone), 011 (Etisalat), 012 (Orange), 015 (WE).
 * - Normalized canonical format: +201XXXXXXXXX (13 characters).
 * - Presentation format: +20 1X XXXX XXXX.
 * - Masked format: +20 1X **** XX.
 */
object EgyptianPhoneUtil {

    // Regex matching normalized E.164 Egyptian mobile number: +2010, +2011, +2012, +2015 followed by 8 digits
    private val EGYPT_PHONE_REGEX = Regex("""^\+201[0125]\d{8}$""")

    /**
     * Normalizes a raw input string into canonical E.164 format (+201XXXXXXXXX).
     */
    fun normalize(rawInput: String): AppResult<String> {
        if (rawInput.isBlank()) {
            return AppResult.Error(AppError.ValidationError("Phone number cannot be empty", "phone"))
        }

        // Strip whitespace, hyphens, parentheses, and convert Arabic-Indic digits to ASCII
        var cleaned = rawInput.trim()
            .replace(" ", "")
            .replace("-", "")
            .replace("(", "")
            .replace(")", "")
            .replace(Regex("[٠-٩]")) { match ->
                (match.value[0].code - '٠'.code + '0'.code).toChar().toString()
            }

        // Normalize leading prefixes
        val normalized = when {
            cleaned.startsWith("+20") -> cleaned
            cleaned.startsWith("0020") -> "+20" + cleaned.removePrefix("0020")
            cleaned.startsWith("20") -> "+$cleaned"
            cleaned.startsWith("01") -> "+20" + cleaned.removePrefix("0")
            cleaned.startsWith("1") && cleaned.length == 10 -> "+20$cleaned"
            else -> "+20$cleaned"
        }

        return if (EGYPT_PHONE_REGEX.matches(normalized)) {
            AppResult.Success(normalized)
        } else {
            AppResult.Error(
                AppError.ValidationError(
                    "Invalid Egyptian mobile number. Must start with 010, 011, 012, or 015 and be 11 digits.",
                    "phone"
                )
            )
        }
    }

    /**
     * Formats normalized number for display: +20 1X XXXX XXXX
     */
    fun formatDisplay(normalizedPhone: String): String {
        if (!EGYPT_PHONE_REGEX.matches(normalizedPhone)) return normalizedPhone
        // +20 1X XXXX XXXX
        val country = normalizedPhone.substring(0, 3) // +20
        val operator = normalizedPhone.substring(3, 5) // 10, 11, 12, 15
        val part1 = normalizedPhone.substring(5, 9)
        val part2 = normalizedPhone.substring(9, 13)
        return "$country $operator $part1 $part2"
    }

    /**
     * Formats normalized number with middle digits masked for privacy: +20 1X **** XX
     */
    fun formatMasked(normalizedPhone: String): String {
        if (!EGYPT_PHONE_REGEX.matches(normalizedPhone)) return normalizedPhone
        val prefix = normalizedPhone.substring(0, 5) // +201X
        val suffix = normalizedPhone.takeLast(2)
        return "$prefix **** $suffix"
    }

    /**
     * Validates a 6-digit SMS OTP token.
     */
    fun isValidOtp(otpToken: String): Boolean {
        return otpToken.length == 6 && otpToken.all { it.isDigit() }
    }
}
