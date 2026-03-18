package com.weeth.domain.university.application.usecase.query

import com.weeth.domain.university.application.exception.CareerNetApiException
import com.weeth.domain.university.domain.port.CareerNetPort
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.every
import io.mockk.mockk

class GetSchoolQueryServiceTest :
    DescribeSpec({
        val careerNetPort = mockk<CareerNetPort>()
        val queryService = GetSchoolQueryService(careerNetPort)

        describe("getSchools") {
            context("커리어넷 API 오류 시") {
                it("CareerNetApiException을 전파한다") {
                    every { careerNetPort.getSchools() } throws CareerNetApiException()

                    shouldThrow<CareerNetApiException> { queryService.getSchools() }
                }
            }
        }
    })
