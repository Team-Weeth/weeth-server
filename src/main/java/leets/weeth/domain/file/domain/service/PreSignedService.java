package leets.weeth.domain.file.domain.service;

import leets.weeth.domain.file.application.dto.response.UrlResponse;
import leets.weeth.domain.file.application.mapper.FileMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PreSignedService {

    private final S3Presigner s3Presigner;
    private final FileMapper fileMapper;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    public UrlResponse generateUrl(String fileName) {
        String key = generateKey(fileName);

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        PutObjectPresignRequest request = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(5))
                .putObjectRequest(putObjectRequest)
                .build();

        PresignedPutObjectRequest presignedUrlRequest = s3Presigner.presignPutObject(request);

        String putUrl = presignedUrlRequest.url().toString();

        return fileMapper.toUrlResponse(fileName, putUrl);
    }

    // 파일 이름을 고유하게 생성하는 메서드(확장자 포함)
    private String generateKey(String fileName) {
        String key = UUID.randomUUID().toString();
        String extension = fileName.substring(fileName.lastIndexOf(".") + 1);

        return key + "." + extension;
    }
}
