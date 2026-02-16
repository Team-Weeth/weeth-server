package com.weeth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@EnableScheduling
@EnableJpaAuditing
@EnableWebSecurity
@SpringBootApplication
@ConfigurationPropertiesScan
public class WeethApplication {

    public static void main(String[] args) {
        SpringApplication.run(WeethApplication.class, args);
    }

}
