package com.weeth.domain.club.domain.vo

/**
 * 포지션 옵션의 색상 값을 나타내는 VO.
 * `#RRGGBB` 형식의 6자리 hex 색상만 허용한다.
 */
@JvmInline
value class ColorHex(
    val value: String,
) {
    init {
        require(HEX_PATTERN.matches(value)) { "색상은 #RRGGBB 형식의 hex 값이어야 합니다." }
    }

    companion object {
        private val HEX_PATTERN = Regex("^#[0-9A-Fa-f]{6}$")
    }
}
