package com.weeth.domain.board.domain.converter

import com.weeth.domain.board.domain.vo.BoardConfig
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class BoardConfigConverterTest :
    StringSpec({
        val converter = BoardConfigConverter()

        "BoardConfig를 JSON 문자열로 변환하고 역직렬화한다" {
            val config =
                BoardConfig(
                    commentEnabled = false,
                    writePermission = BoardConfig.WritePermission.ADMIN,
                    isPrivate = true,
                )

            val json = converter.convertToDatabaseColumn(config)
            val restored = converter.convertToEntityAttribute(json)

            restored shouldBe config
        }

        "null DB 값은 null로 변환한다" {
            converter.convertToEntityAttribute(null).shouldBeNull()
        }
    })
