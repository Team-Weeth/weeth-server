package com.weeth.global.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

/**
 * 커리어넷 오픈API에 아직 등록되지 않은 신설 학과를 운영상 보정 추가하기 위한 설정.
 * 코드 재배포 없이 application.yml만 수정해 학과를 추가/삭제할 수 있도록 외부화했다.
 */
@Validated
@ConfigurationProperties(prefix = "university")
data class UniversityProperties(
    val manualMajors: List<ManualMajor> = emptyList(),
) {
    data class ManualMajor(
        val name: String,
        val category: String,
    )
}
