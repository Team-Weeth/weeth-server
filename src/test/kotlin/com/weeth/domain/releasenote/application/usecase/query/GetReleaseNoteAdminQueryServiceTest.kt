package com.weeth.domain.releasenote.application.usecase.query

import com.weeth.domain.releasenote.application.exception.NotDeveloperException
import com.weeth.domain.releasenote.application.mapper.ReleaseNoteMapper
import com.weeth.domain.releasenote.application.validator.PlatformPermissionValidator
import com.weeth.domain.releasenote.domain.enums.ReleaseNoteStatus
import com.weeth.domain.releasenote.domain.repository.ReleaseNoteRepository
import com.weeth.domain.releasenote.fixture.ReleaseNoteTestFixture
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.SliceImpl
import java.util.Optional

class GetReleaseNoteAdminQueryServiceTest :
    DescribeSpec({
        val releaseNoteRepository = mockk<ReleaseNoteRepository>()
        val platformPermissionValidator = mockk<PlatformPermissionValidator>()
        val service =
            GetReleaseNoteAdminQueryService(releaseNoteRepository, platformPermissionValidator, ReleaseNoteMapper())

        val userId = 10L

        beforeTest {
            clearMocks(releaseNoteRepository, platformPermissionValidator)
            every { platformPermissionValidator.requireDeveloper(userId) } just Runs
        }

        describe("findAll") {
            it("DEVELOPER 검증 후 status 필터로 DRAFT 포함 목록을 조회한다") {
                every {
                    releaseNoteRepository.findForAdmin(ReleaseNoteStatus.DRAFT, any())
                } returns SliceImpl(listOf(ReleaseNoteTestFixture.draft(id = 1L)), Pageable.ofSize(12), false)

                val result = service.findAll(userId, status = ReleaseNoteStatus.DRAFT, page = 0, size = 12)

                result.content.single().id shouldBe 1L
                verify(exactly = 1) { platformPermissionValidator.requireDeveloper(userId) }
                verify(exactly = 1) { releaseNoteRepository.findForAdmin(ReleaseNoteStatus.DRAFT, any()) }
            }

            it("DEVELOPER가 아니면 조회 없이 403 예외를 던진다") {
                every { platformPermissionValidator.requireDeveloper(userId) } throws NotDeveloperException()

                shouldThrow<NotDeveloperException> { service.findAll(userId, null, 0, 12) }

                verify(exactly = 0) { releaseNoteRepository.findForAdmin(any(), any()) }
            }
        }

        describe("find") {
            it("DEVELOPER 검증 후 DRAFT 포함 상세를 반환한다") {
                every { releaseNoteRepository.findById(1L) } returns Optional.of(ReleaseNoteTestFixture.draft(id = 1L))

                service.find(userId, 1L).id shouldBe 1L
                verify(exactly = 1) { platformPermissionValidator.requireDeveloper(userId) }
            }
        }
    })
