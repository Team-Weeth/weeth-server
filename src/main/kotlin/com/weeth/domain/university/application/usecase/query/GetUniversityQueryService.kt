package com.weeth.domain.university.application.usecase.query

import com.weeth.domain.university.application.dto.response.MajorResponse
import com.weeth.domain.university.application.dto.response.SchoolResponse
import com.weeth.domain.university.application.mapper.UniversityMapper
import com.weeth.domain.university.domain.model.MajorData
import com.weeth.domain.university.domain.port.UniversityInfoPort
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class GetUniversityQueryService(
    private val universityInfoPort: UniversityInfoPort,
    private val universityMapper: UniversityMapper,
) {
    @Cacheable(value = ["schools"], key = "'all'")
    fun getSchools(): List<SchoolResponse> =
        universityInfoPort
            .getSchools()
            .sortedWith(koreanFirstComparator { it.name })
            .map(universityMapper::toSchoolResponse)

    /**
     * 커리어넷 오픈API에 아직 등록되지 않은 신설 학과("인공지능학과")를 보정 추가한다.
     * API가 이미 해당 학과를 포함하게 되더라도 이름 기준으로 중복 추가되지 않는다.
     */
    @Cacheable(value = ["majors"], key = "'all'")
    fun getMajors(): List<MajorResponse> {
        val majors = universityInfoPort.getMajors()
        val withManualAdditions =
            if (majors.any { it.name == MANUALLY_ADDED_MAJOR.name }) {
                majors
            } else {
                majors + MANUALLY_ADDED_MAJOR
            }

        return withManualAdditions
            .sortedWith(koreanFirstComparator { it.name })
            .map(universityMapper::toMajorResponse)
    }

    private fun <T> koreanFirstComparator(selector: (T) -> String): Comparator<T> =
        compareBy(
            { selector(it).firstOrNull()?.let { c -> c !in '가'..'힣' } ?: true },
            { selector(it) },
        )

    companion object {
        private val MANUALLY_ADDED_MAJOR = MajorData(name = "인공지능학과", category = "공학계열")
    }
}
