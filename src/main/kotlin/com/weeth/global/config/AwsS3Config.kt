package com.weeth.global.config

import com.weeth.global.config.properties.AwsS3Properties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.presigner.S3Presigner

@Configuration
class AwsS3Config(
    private val awsS3Properties: AwsS3Properties,
) {
    @Bean
    fun s3Presigner(): S3Presigner {
        val credentials =
            AwsBasicCredentials.create(
                awsS3Properties.credentials.accessKey,
                awsS3Properties.credentials.secretKey,
            )
        return S3Presigner
            .builder()
            .region(Region.of(awsS3Properties.region.static))
            .credentialsProvider(StaticCredentialsProvider.create(credentials))
            .build()
    }
}
