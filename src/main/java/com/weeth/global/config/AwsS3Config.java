package com.weeth.global.config;

import com.weeth.global.config.properties.AwsS3Properties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@RequiredArgsConstructor
public class AwsS3Config {

    private final AwsS3Properties awsS3Properties;

    @Bean
    public S3Presigner s3Presigner() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(
                awsS3Properties.getCredentials().getAccessKey(),
                awsS3Properties.getCredentials().getSecretKey()
        );
        return S3Presigner.builder()
                .region(Region.of(awsS3Properties.getRegion().getStatic()))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
    }
}
