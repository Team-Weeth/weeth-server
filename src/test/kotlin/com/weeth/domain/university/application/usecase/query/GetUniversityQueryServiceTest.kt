package com.weeth.domain.university.application.usecase.query

import com.weeth.domain.university.application.dto.response.MajorResponse
import com.weeth.domain.university.application.exception.CareerNetApiException
import com.weeth.domain.university.application.mapper.UniversityMapper
import com.weeth.domain.university.domain.model.MajorData
import com.weeth.domain.university.domain.port.UniversityInfoPort
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
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

            it("API 응답에 인공지능학과가 없으면 보정 추가한다") {
                val computerScience = MajorData(name = "컴퓨터공학과", category = "공학계열")
                every { universityInfoPort.getMajors() } returns listOf(computerScience)
                every { universityMapper.toMajorResponse(computerScience) } returns
                    MajorResponse(majorName = "컴퓨터공학과", category = "공학계열")
                every { universityMapper.toMajorResponse(MajorData(name = "인공지능학과", category = "공학계열")) } returns
                    MajorResponse(majorName = "인공지능학과", category = "공학계열")

                val result = queryService.getMajors()

                result.map { it.majorName } shouldContainExactlyInAnyOrder listOf("컴퓨터공학과", "인공지능학과")
            }

            it("API 응답에 인공지능학과가 이미 있으면 중복 추가하지 않는다") {
                val aiMajor = MajorData(name = "인공지능학과", category = "공학계열")
                every { universityInfoPort.getMajors() } returns listOf(aiMajor)
                every { universityMapper.toMajorResponse(aiMajor) } returns
                    MajorResponse(majorName = "인공지능학과", category = "공학계열")

                val result = queryService.getMajors()

                result.size shouldBe 1
                result.first().majorName shouldBe "인공지능학과"
            }
        }
    })
