package com.weeth.domain.university.application.usecase.query

import com.weeth.domain.university.application.exception.CareerNetApiException
import com.weeth.domain.university.application.mapper.UniversityMapper
import com.weeth.domain.university.domain.port.CareerNetPort
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.every
import io.mockk.mockk

class GetMajorQueryServiceTest :
    DescribeSpec({
        val careerNetPort = mockk<CareerNetPort>()
        val universityMapper = mockk<UniversityMapper>()
        val queryService = GetMajorQueryService(careerNetPort, universityMapper)

        describe("getMajors") {
            context("커리어넷 API 오류 시") {
                it("CareerNetApiException을 전파한다") {
                    every { careerNetPort.getMajors() } throws CareerNetApiException()

                    shouldThrow<CareerNetApiException> { queryService.getMajors() }
                }
            }
        }
    })
