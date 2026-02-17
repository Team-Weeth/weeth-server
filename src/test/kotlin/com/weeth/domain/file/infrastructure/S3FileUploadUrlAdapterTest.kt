package com.weeth.domain.file.infrastructure

import com.weeth.domain.file.application.exception.PresignedUrlGenerationException
import com.weeth.domain.file.domain.entity.FileOwnerType
import com.weeth.global.config.properties.AwsS3Properties
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith
import io.mockk.every
import io.mockk.mockk
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest
import java.net.URL

class S3FileUploadUrlAdapterTest :
    DescribeSpec({
        describe("generateUploadUrl") {
            val awsS3Properties =
                AwsS3Properties(
                    s3 = AwsS3Properties.S3Properties(bucket = "weeth-bucket"),
                    credentials = AwsS3Properties.CredentialsProperties(accessKey = "test", secretKey = "test"),
                    region = AwsS3Properties.RegionProperties(static = "ap-northeast-2"),
                )

            it("ownerType/fileName 기반 storageKey와 presigned URL을 반환한다") {
                val s3Presigner = mockk<S3Presigner>()
                val presignedRequest = mockk<PresignedPutObjectRequest>()
                val adapter = S3FileUploadUrlAdapter(s3Presigner, awsS3Properties, 5)

                every { s3Presigner.presignPutObject(any<PutObjectPresignRequest>()) } returns presignedRequest
                every { presignedRequest.url() } returns URL("https://presigned.example.com/upload")

                val result = adapter.generateUploadUrl(FileOwnerType.POST, "file.png")

                result.fileName shouldBe "file.png"
                result.storageKey shouldStartWith "POST/"
                result.storageKey shouldContain "_file.png"
                result.url shouldBe "https://presigned.example.com/upload"
            }

            it("presigner 오류가 발생하면 PresignedUrlGenerationException으로 변환한다") {
                val s3Presigner = mockk<S3Presigner>()
                val adapter = S3FileUploadUrlAdapter(s3Presigner, awsS3Properties, 5)

                every { s3Presigner.presignPutObject(any<PutObjectPresignRequest>()) } throws RuntimeException("s3 unavailable")

                shouldThrow<PresignedUrlGenerationException> {
                    adapter.generateUploadUrl(FileOwnerType.POST, "file.png")
                }
            }
        }
    })
