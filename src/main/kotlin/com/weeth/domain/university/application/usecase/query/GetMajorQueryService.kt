package com.weeth.domain.university.application.usecase.query

import com.weeth.domain.university.application.dto.response.MajorResponse
import com.weeth.domain.university.domain.port.CareerNetPort
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class GetMajorQueryService(
    private val careerNetPort: CareerNetPort,
) {
    @Cacheable(value = ["majors"], key = "'all'")
    fun getMajors(): List<MajorResponse> = careerNetPort.getMajors()
}
