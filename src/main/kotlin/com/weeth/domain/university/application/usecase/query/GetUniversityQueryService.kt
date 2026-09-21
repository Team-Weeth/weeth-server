package com.weeth.domain.university.application.usecase.query

import com.weeth.domain.university.application.dto.response.MajorResponse
import com.weeth.domain.university.application.dto.response.SchoolResponse
import com.weeth.domain.university.application.mapper.UniversityMapper
import com.weeth.domain.university.domain.model.MajorData
import com.weeth.domain.university.domain.port.UniversityInfoPort
import com.weeth.global.config.properties.UniversityProperties
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class GetUniversityQueryService(
    private val universityInfoPort: UniversityInfoPort,
    private val universityMapper: UniversityMapper,
    private val universityProperties: UniversityProperties,
) {
    @Cacheable(value = ["schools"], key = "'all'")
    fun getSchools(): List<SchoolResponse> =
        universityInfoPort
            .getSchools()
            .sortedWith(koreanFirstComparator { it.name })
            .map(universityMapper::toSchoolResponse)

    /**
     * 커리어넷 오픈API에 아직 등록되지 않은 신설 학과를 application.yml(university.manual-majors)
     * 설정 기준으로 보정 추가한다. API가 이미 해당 학과를 포함하게 되더라도 이름 기준으로 중복 추가되지 않는다.
     */
    @Cacheable(value = ["majors"], key = "'all'")
    fun getMajors(): List<MajorResponse> {
        val majors = universityInfoPort.getMajors()
        val existingNames = majors.map { it.name }.toSet()
        val manualAdditions =
            universityProperties.manualMajors
                .filter { it.name !in existingNames }
                .map { MajorData(name = it.name, category = it.category) }

        return (majors + manualAdditions)
            .sortedWith(koreanFirstComparator { it.name })
            .map(universityMapper::toMajorResponse)
    }

    private fun <T> koreanFirstComparator(selector: (T) -> String): Comparator<T> =
        compareBy(
            { selector(it).firstOrNull()?.let { c -> c !in '가'..'힣' } ?: true },
            { selector(it) },
        )
}
