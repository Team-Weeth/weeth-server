package com.weeth.domain.university.application.usecase.query

import com.weeth.domain.university.application.dto.response.MajorResponse
import com.weeth.domain.university.application.dto.response.SchoolResponse
import com.weeth.domain.university.application.mapper.UniversityMapper
import com.weeth.domain.university.domain.port.CareerNetPort
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class GetUniversityQueryService(
    private val careerNetPort: CareerNetPort,
    private val universityMapper: UniversityMapper,
) {
    @Cacheable(value = ["schools"], key = "'all'")
    fun getSchools(): List<SchoolResponse> =
        careerNetPort
            .getSchools()
            .sortedWith(koreanFirstComparator { it.name })
            .map(universityMapper::toSchoolResponse)

    @Cacheable(value = ["majors"], key = "'all'")
    fun getMajors(): List<MajorResponse> =
        careerNetPort
            .getMajors()
            .sortedWith(koreanFirstComparator { it.name })
            .map(universityMapper::toMajorResponse)

    private fun <T> koreanFirstComparator(selector: (T) -> String): Comparator<T> =
        compareBy(
            { selector(it).firstOrNull()?.let { c -> c !in '가'..'힣' } ?: true },
            { selector(it) },
        )
}
