package com.weeth.domain.file.infrastructure

import com.weeth.domain.file.application.exception.PresignedUrlGenerationException
import com.weeth.domain.file.domain.entity.FileOwnerType
import com.weeth.domain.file.domain.port.FileUploadUrl
import com.weeth.domain.file.domain.port.FileUploadUrlPort
import com.weeth.global.config.properties.AwsS3Properties
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest
import java.time.Duration
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

/** S3 기반 업로드용 presigned URL 생성 어댑터입니다. */
@Component
class S3FileUploadUrlAdapter(
    private val s3Presigner: S3Presigner,
    private val awsS3Properties: AwsS3Properties,
    @Value("\${app.file.presigned-url-expiration-minutes:5}")
    private val presignedUrlExpirationMinutes: Long,
) : FileUploadUrlPort {
    override fun generateUploadUrl(
        ownerType: FileOwnerType,
        fileName: String,
    ): FileUploadUrl =
        runCatching {
            val storageKey = generateStorageKey(ownerType, fileName)
            val putObjectRequest =
                PutObjectRequest
                    .builder()
                    .bucket(awsS3Properties.s3.bucket)
                    .key(storageKey)
                    .build()

            val request =
                PutObjectPresignRequest
                    .builder()
                    .signatureDuration(Duration.ofMinutes(presignedUrlExpirationMinutes))
                    .putObjectRequest(putObjectRequest)
                    .build()

            val presigned = s3Presigner.presignPutObject(request)
            FileUploadUrl(fileName = fileName, storageKey = storageKey, url = presigned.url().toString())
        }.getOrElse { e ->
            throw PresignedUrlGenerationException(cause = e)
        }

    private fun generateStorageKey(
        ownerType: FileOwnerType,
        fileName: String,
    ): String {
        val month = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
        val uuid = UUID.randomUUID().toString()
        val sanitized = fileName.trim().replace(Regex("""[\\/:*?"<>|]"""), "_")
        return "${ownerType.name}/$month/${uuid}_$sanitized"
    }
}
