package com.weeth.domain.university.application.usecase.query

import com.weeth.domain.university.application.exception.CareerNetApiException
import com.weeth.domain.university.application.mapper.UniversityMapper
import com.weeth.domain.university.domain.port.UniversityInfoPort
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.every
import io.mockk.mockk

class GetUniversityQueryServiceTest :
    DescribeSpec({
        val universityInfoPort = mockk<UniversityInfoPort>()
        val universityMapper = mockk<UniversityMapper>()
        val queryService = GetUniversityQueryService(universityInfoPort, universityMapper)

        describe("getSchools") {
            context("커리어넷 API 오류 시") {
                it("CareerNetApiException을 전파한다") {
                    every { universityInfoPort.getSchools() } throws CareerNetApiException()

                    shouldThrow<CareerNetApiException> { queryService.getSchools() }
                }
            }
        }

        describe("getMajors") {
            context("커리어넷 API 오류 시") {
                it("CareerNetApiException을 전파한다") {
                    every { universityInfoPort.getMajors() } throws CareerNetApiException()

                    shouldThrow<CareerNetApiException> { queryService.getMajors() }
                }
            }
        }
    })
