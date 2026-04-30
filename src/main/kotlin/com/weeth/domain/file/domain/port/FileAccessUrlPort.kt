package com.weeth.domain.file.domain.port

/**
 * 저장된 storageKey를 조회 가능한 URL로 변환하는 포트입니다.
 */
interface FileAccessUrlPort {
    /**
     * storageKey를 조회용 URL로 변환합니다.
     * 기본 구현은 S3 공개 URL을 사용하고, 설정에 따라 CDN URL로 교체될 수 있습니다.
     */
    fun resolve(storageKey: String): String
}
