package com.weeth

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity

@EnableAsync
@EnableScheduling
@EnableJpaAuditing
@EnableWebSecurity
@SpringBootApplication
@ConfigurationPropertiesScan
class WeethApplication

fun main(args: Array<String>) {
    runApplication<WeethApplication>(*args)
}
