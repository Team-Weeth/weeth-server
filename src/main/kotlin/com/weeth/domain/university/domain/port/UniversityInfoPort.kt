package com.weeth.domain.university.domain.port

import com.weeth.domain.university.domain.model.MajorData
import com.weeth.domain.university.domain.model.SchoolData

interface UniversityInfoPort {
    fun getSchools(): List<SchoolData>

    fun getMajors(): List<MajorData>
}
