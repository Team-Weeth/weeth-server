package com.weeth.global.common.converter

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter
abstract class JsonConverter<T>(
    private val typeRef: TypeReference<T>,
) : AttributeConverter<T, String> {
    companion object {
        private val objectMapper =
            ObjectMapper().apply {
                registerModule(KotlinModule.Builder().build())
            }
    }

    override fun convertToDatabaseColumn(attribute: T?): String? = attribute?.let { objectMapper.writeValueAsString(it) }

    override fun convertToEntityAttribute(dbData: String?): T? = dbData?.let { objectMapper.readValue(it, typeRef) }
}
