package com.weeth.domain.university.domain.port

import com.weeth.domain.university.application.dto.response.MajorResponse
import com.weeth.domain.university.application.dto.response.SchoolResponse

interface CareerNetPort {
    fun getSchools(): List<SchoolResponse>

    fun getMajors(): List<MajorResponse>
}
