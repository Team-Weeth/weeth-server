package com.weeth.domain.user.domain.vo

data class PhoneNumber private constructor(
    val value: String,
) {
    companion object {
        fun from(raw: String): PhoneNumber {
            val normalized = raw.filter { it.isDigit() }
            if (normalized.isBlank()) {
                return PhoneNumber("")
            }
            require(normalized.length in 10..11) { "Invalid phone number format." }
            return PhoneNumber(normalized)
        }
    }
}

