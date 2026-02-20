package com.weeth.domain.account.domain.vo

@JvmInline
value class Money(
    val value: Int,
) {
    init {
        require(value >= 0) { "금액은 0 이상이어야 합니다: $value" }
    }

    operator fun plus(other: Money) = Money(value + other.value)

    operator fun minus(other: Money) = Money(value - other.value)

    companion object {
        val ZERO = Money(0)

        fun of(value: Int) = Money(value)
    }
}
