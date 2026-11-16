package com.weeth.domain.releasenote.application.usecase.command

import com.weeth.domain.releasenote.application.dto.request.CreateReleaseNoteRequest
import com.weeth.domain.releasenote.application.dto.request.UpdateReleaseNoteRequest
import com.weeth.domain.releasenote.application.dto.request.UpdateReleaseNoteStatusRequest
import com.weeth.domain.releasenote.application.exception.NotDeveloperException
import com.weeth.domain.releasenote.application.exception.ReleaseNoteNotFoundException
import com.weeth.domain.releasenote.application.mapper.ReleaseNoteMapper
import com.weeth.domain.releasenote.application.validator.PlatformPermissionValidator
import com.weeth.domain.releasenote.domain.entity.ReleaseNote
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
import io.mockk.slot
import io.mockk.verify
import java.util.Optional

class ManageReleaseNoteUseCaseTest :
    DescribeSpec({
        val releaseNoteRepository = mockk<ReleaseNoteRepository>()
        val platformPermissionValidator = mockk<PlatformPermissionValidator>()
        val releaseNoteMapper = ReleaseNoteMapper()
        val useCase = ManageReleaseNoteUseCase(releaseNoteRepository, platformPermissionValidator, releaseNoteMapper)

        val userId = 10L
        val noteId = 1L

        beforeTest {
            clearMocks(releaseNoteRepository, platformPermissionValidator)
            every { platformPermissionValidator.requireDeveloper(userId) } just Runs
        }

        describe("create") {
            it("DEVELOPER가 작성하면 노트를 저장하고 id를 반환한다") {
                val saved = ReleaseNoteTestFixture.draft(id = 12L)
                every { releaseNoteRepository.save(any()) } returns saved
                val request =
                    CreateReleaseNoteRequest(title = "제목", content = "<p>본문</p>", status = ReleaseNoteStatus.DRAFT)

                val result = useCase.create(userId, request)

                result.id shouldBe 12L
                verify(exactly = 1) { platformPermissionValidator.requireDeveloper(userId) }
                verify(exactly = 1) { releaseNoteRepository.save(any()) }
            }

            it("PUBLISHED로 작성하면 즉시 발행된 상태로 저장한다") {
                val slot = slot<ReleaseNote>()
                every { releaseNoteRepository.save(capture(slot)) } answers { firstArg() }
                val request =
                    CreateReleaseNoteRequest(title = "제목", content = "<p>본문</p>", status = ReleaseNoteStatus.PUBLISHED)

                useCase.create(userId, request)

                slot.captured.status shouldBe ReleaseNoteStatus.PUBLISHED
                (slot.captured.publishedAt != null) shouldBe true
            }

            it("DEVELOPER가 아니면 저장 없이 403 예외를 던진다") {
                every { platformPermissionValidator.requireDeveloper(userId) } throws NotDeveloperException()
                val request = CreateReleaseNoteRequest(title = "제목", content = "<p>본문</p>")

                shouldThrow<NotDeveloperException> { useCase.create(userId, request) }

                verify(exactly = 0) { releaseNoteRepository.save(any()) }
            }
        }

        describe("update") {
            it("전체 교체 수정하되 발행 상태는 유지한다") {
                val note = ReleaseNoteTestFixture.published(id = noteId)
                every { releaseNoteRepository.findById(noteId) } returns Optional.of(note)
                val request = UpdateReleaseNoteRequest(title = "새 제목", content = "<p>새 본문</p>")

                useCase.update(userId, noteId, request)

                note.title shouldBe "새 제목"
                note.content shouldBe "<p>새 본문</p>"
                note.status shouldBe ReleaseNoteStatus.PUBLISHED
                verify(exactly = 1) { platformPermissionValidator.requireDeveloper(userId) }
            }

            it("없는 노트면 404 예외를 던진다") {
                every { releaseNoteRepository.findById(noteId) } returns Optional.empty()

                shouldThrow<ReleaseNoteNotFoundException> {
                    useCase.update(userId, noteId, UpdateReleaseNoteRequest(title = "t", content = "<p>c</p>"))
                }
            }
        }

        describe("updateStatus") {
            it("DRAFT를 PUBLISHED로 전환하면 publishedAt이 기록된다") {
                val note = ReleaseNoteTestFixture.draft(id = noteId)
                every { releaseNoteRepository.findById(noteId) } returns Optional.of(note)

                useCase.updateStatus(userId, noteId, UpdateReleaseNoteStatusRequest(ReleaseNoteStatus.PUBLISHED))

                note.status shouldBe ReleaseNoteStatus.PUBLISHED
                (note.publishedAt != null) shouldBe true
            }

            it("PUBLISHED를 DRAFT로 전환하면 publishedAt이 초기화된다") {
                val note = ReleaseNoteTestFixture.published(id = noteId)
                every { releaseNoteRepository.findById(noteId) } returns Optional.of(note)

                useCase.updateStatus(userId, noteId, UpdateReleaseNoteStatusRequest(ReleaseNoteStatus.DRAFT))

                note.status shouldBe ReleaseNoteStatus.DRAFT
                note.publishedAt shouldBe null
            }
        }

        describe("delete") {
            it("노트를 조회해 삭제한다") {
                val note = ReleaseNoteTestFixture.draft(id = noteId)
                every { releaseNoteRepository.findById(noteId) } returns Optional.of(note)
                every { releaseNoteRepository.delete(note) } just Runs

                useCase.delete(userId, noteId)

                verify(exactly = 1) { releaseNoteRepository.delete(note) }
            }

            it("DEVELOPER가 아니면 삭제 없이 403 예외를 던진다") {
                every { platformPermissionValidator.requireDeveloper(userId) } throws NotDeveloperException()

                shouldThrow<NotDeveloperException> { useCase.delete(userId, noteId) }

                verify(exactly = 0) { releaseNoteRepository.delete(any()) }
            }
        }
    })
