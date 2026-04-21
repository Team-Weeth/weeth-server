package com.weeth.global.config

import io.micrometer.observation.ObservationPredicate
import io.micrometer.observation.ObservationRegistry
import org.springframework.boot.web.client.RestClientCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.server.observation.ServerRequestObservationContext

@Configuration
class ObservabilityConfig {
    @Bean
    fun restClientObservationCustomizer(observationRegistry: ObservationRegistry): RestClientCustomizer =
        RestClientCustomizer { builder ->
            builder.observationRegistry(observationRegistry)
        }

    @Bean
    fun actuatorObservationPredicate(): ObservationPredicate =
        ObservationPredicate { _, context ->
            if (context is ServerRequestObservationContext) {
                val path = context.carrier?.requestURI ?: return@ObservationPredicate true
                return@ObservationPredicate !path.startsWith("/actuator") && !path.startsWith("/health-check")
            }

            true
        }
}
