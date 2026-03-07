package com.weeth.global.common.id

/**
 * TSID를 Base62로 인코딩/디코딩하는 유틸리티
 * Base62 알파벳: 0-9a-zA-Z (총 62자)
 */
object TsidBase62Encoder {
    private const val BASE62_ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private const val BASE = 62

    /**
     * Long을 Base62 String으로 인코딩
     */
    fun encode(id: Long): String {
        if (id == 0L) return "0"

        val result = StringBuilder()
        var num = id

        while (num > 0) {
            result.append(BASE62_ALPHABET[(num % BASE).toInt()])
            num /= BASE
        }

        return result.reverse().toString()
    }

    /**
     * Base62 String을 Long으로 디코딩
     */
    fun decode(encoded: String): Long {
        if (encoded.isEmpty()) throw IllegalArgumentException("Base62 인코딩된 문자열은 비어 있을 수 없습니다.")

        var result = 0L

        for (char in encoded) {
            val digit = BASE62_ALPHABET.indexOf(char)
            if (digit == -1) {
                throw IllegalArgumentException("유효하지 않은 Base62 문자: $char")
            }
            result = result * BASE + digit
        }

        return result
    }
}
