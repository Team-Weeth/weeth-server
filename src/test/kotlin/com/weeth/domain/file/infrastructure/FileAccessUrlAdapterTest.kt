package com.weeth.domain.file.infrastructure

import com.weeth.global.config.properties.AwsS3Properties
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class FileAccessUrlAdapterTest :
    DescribeSpec({
        describe("S3FileAccessUrlAdapter") {
            it("storageKey를 S3 URL로 변환한다") {
                val awsS3Properties =
                    AwsS3Properties(
                        s3 = AwsS3Properties.S3Properties(bucket = "weeth-bucket"),
                        credentials = AwsS3Properties.CredentialsProperties(accessKey = "test", secretKey = "test"),
                        region = AwsS3Properties.RegionProperties(static = "ap-northeast-2"),
                    )
                val adapter = S3FileAccessUrlAdapter(awsS3Properties)

                val result = adapter.resolve("POST/2026-02/file.png")

                result shouldBe "https://weeth-bucket.s3.ap-northeast-2.amazonaws.com/POST/2026-02/file.png"
            }
        }

        describe("CdnFileAccessUrlAdapter") {
            it("cdn base url이 있으면 CDN URL로 변환한다") {
                val adapter = CdnFileAccessUrlAdapter("https://cdn.example.com")

                val result = adapter.resolve("POST/2026-02/file.png")

                result shouldBe "https://cdn.example.com/POST/2026-02/file.png"
            }

            it("cdn base url이 없으면 storageKey를 그대로 반환한다") {
                val adapter = CdnFileAccessUrlAdapter("")

                val result = adapter.resolve("POST/2026-02/file.png")

                result shouldBe "POST/2026-02/file.png"
            }
        }
    })
