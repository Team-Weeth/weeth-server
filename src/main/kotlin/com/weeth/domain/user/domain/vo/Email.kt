package com.weeth.domain.user.domain.vo

data class Email private constructor(
    val value: String,
) {
    companion object {
        fun from(raw: String): Email {
            val normalized = raw.trim().lowercase()
            if (normalized.isBlank()) {
                return Email("")
            }
            require(EMAIL_REGEX.matches(normalized)) { "Invalid email format." }
            return Email(normalized)
        }

        private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")
    }
}
