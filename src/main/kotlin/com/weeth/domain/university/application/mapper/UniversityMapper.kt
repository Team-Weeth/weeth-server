package com.weeth.domain.university.application.mapper

import com.weeth.domain.university.application.dto.response.MajorResponse
import com.weeth.domain.university.application.dto.response.SchoolResponse
import com.weeth.domain.university.domain.model.MajorData
import com.weeth.domain.university.domain.model.SchoolData
import org.springframework.stereotype.Component

@Component
class UniversityMapper {
    fun toSchoolResponse(data: SchoolData) = SchoolResponse(
        schoolName = data.name,
        region = data.region,
    )

    fun toMajorResponse(data: MajorData) = MajorResponse(
        majorName = data.name,
        category = data.category,
    )
}
