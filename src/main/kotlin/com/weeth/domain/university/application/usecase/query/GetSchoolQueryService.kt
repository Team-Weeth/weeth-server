package com.weeth.domain.university.application.usecase.query

import com.weeth.domain.university.application.dto.response.SchoolResponse
import com.weeth.domain.university.application.mapper.UniversityMapper
import com.weeth.domain.university.domain.port.CareerNetPort
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class GetSchoolQueryService(
    private val careerNetPort: CareerNetPort,
    private val universityMapper: UniversityMapper,
) {
    @Cacheable(value = ["schools"], key = "'all'")
    fun getSchools(): List<SchoolResponse> =
        careerNetPort.getSchools().map(universityMapper::toSchoolResponse)
}
