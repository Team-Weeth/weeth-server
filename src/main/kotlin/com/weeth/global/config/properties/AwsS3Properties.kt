package com.weeth.global.config.properties

import jakarta.validation.constraints.NotBlank
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@Validated
@ConfigurationProperties(prefix = "cloud.aws")
data class AwsS3Properties(
    val s3: S3Properties,
    val credentials: CredentialsProperties,
    val region: RegionProperties,
) {
    data class S3Properties(
        @field:NotBlank
        val bucket: String,
    )

    data class CredentialsProperties(
        @field:NotBlank
        val accessKey: String,
        @field:NotBlank
        val secretKey: String,
    )

    data class RegionProperties(
        @field:NotBlank
        val static: String,
    )
}
