package com.weeth.domain.file.infrastructure

import com.weeth.domain.file.domain.port.FileAccessUrlPort
import com.weeth.global.config.properties.AwsS3Properties
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(
    prefix = "app.file",
    name = ["url-provider"],
    havingValue = "S3", // S3로 설정된 경우 이 어댑터를 사용합니다.
    matchIfMissing = true,
)
class S3FileAccessUrlAdapter(
    private val awsS3Properties: AwsS3Properties,
) : FileAccessUrlPort {
    /** storageKey를 S3 공개 조회 URL로 변환합니다. */
    override fun resolve(storageKey: String): String {
        val bucket = awsS3Properties.s3.bucket
        val region = awsS3Properties.region.static
        return "https://$bucket.s3.$region.amazonaws.com/$storageKey"
    }
}
