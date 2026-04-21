package com.weeth.global.logging

import com.fasterxml.jackson.core.JsonFactory
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.io.StringWriter

class MaskingJsonGeneratorTest :
    StringSpec({
        "snake_case token field values are masked" {
            val json = writeJson("access_token", "eyJabcdefghi.secret.payload")

            json shouldContain """"access_token":"***""""
            json shouldNotContain "eyJabcdefghi.secret.payload"
        }

        "email and phone number patterns are masked inside log messages" {
            val json = writeJson("message", "contact test@example.com or 010-1234-5678")

            json shouldContain "t***@example.com"
            json shouldContain "010-****-5678"
            json shouldNotContain "test@example.com"
            json shouldNotContain "010-1234-5678"
        }
    })

private fun writeJson(
    fieldName: String,
    value: String,
): String {
    val writer = StringWriter()
    val generator = MaskingJsonGenerator(JsonFactory().createGenerator(writer))

    generator.writeStartObject()
    generator.writeStringField(fieldName, value)
    generator.writeEndObject()
    generator.close()

    return writer.toString()
}
