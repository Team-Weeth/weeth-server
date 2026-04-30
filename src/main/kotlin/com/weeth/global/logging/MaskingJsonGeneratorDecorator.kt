package com.weeth.global.logging

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.util.JsonGeneratorDelegate
import net.logstash.logback.decorate.JsonGeneratorDecorator

class MaskingJsonGeneratorDecorator : JsonGeneratorDecorator {
    override fun decorate(generator: JsonGenerator): JsonGenerator = MaskingJsonGenerator(generator)
}

class MaskingJsonGenerator(
    delegate: JsonGenerator,
) : JsonGeneratorDelegate(delegate) {
    private val sensitiveFields = setOf("password", "token", "accesstoken", "refreshtoken", "secret", "authorization")
    private var currentFieldName: String? = null

    override fun writeFieldName(name: String?) {
        currentFieldName = name?.lowercase()?.filter(Char::isLetterOrDigit)
        super.writeFieldName(name)
    }

    override fun writeString(text: String?) {
        if (text == null) {
            super.writeString(null as String?)
            return
        }

        val masked =
            when {
                currentFieldName in sensitiveFields -> "***"
                else -> maskPatterns(text)
            }
        currentFieldName = null
        super.writeString(masked as String?)
    }

    private fun maskPatterns(value: String): String =
        value
            .replace(EMAIL_PATTERN) { "${it.groupValues[1]}***${it.groupValues[3]}" }
            .replace(PHONE_PATTERN) { "${it.groupValues[1]}-****-${it.groupValues[3]}" }
            .replace(TOKEN_PATTERN) { "${it.groupValues[1]}***" }

    companion object {
        private val EMAIL_PATTERN =
            Regex("""([a-zA-Z0-9._%+-])([a-zA-Z0-9._%+-]*)(@[a-zA-Z0-9.-]+)""")
        private val PHONE_PATTERN =
            Regex("""(01[0-9])-?(\d{3,4})-?(\d{4})""")
        private val TOKEN_PATTERN =
            Regex("""(eyJ[a-zA-Z0-9_-]{7})[a-zA-Z0-9_.-]+""")
    }
}
