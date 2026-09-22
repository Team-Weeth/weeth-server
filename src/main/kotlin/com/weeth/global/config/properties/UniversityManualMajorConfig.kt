package com.weeth.global.config.properties

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component

@Component
class UniversityManualMajorConfig(
    properties: UniversityProperties,
    objectMapper: ObjectMapper,
) {
    val manualMajors: List<UniversityProperties.ManualMajor> =
        runCatching {
            objectMapper.readValue(
                properties.manualMajorsJson,
                object : TypeReference<List<UniversityProperties.ManualMajor>>() {},
            )
        }.getOrElse { cause ->
            throw IllegalArgumentException("UNIVERSITY_MANUAL_MAJORS must be a valid JSON array", cause)
        }
}
