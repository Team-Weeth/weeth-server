package com.weeth.domain.releasenote.application.usecase.query

import com.weeth.domain.file.domain.port.FileAccessUrlPort
import com.weeth.domain.releasenote.application.exception.ReleaseNoteNotFoundException
import com.weeth.domain.releasenote.application.mapper.ReleaseNoteMapper
import com.weeth.domain.releasenote.domain.enums.ReleaseNoteStatus
import com.weeth.domain.releasenote.domain.repository.ReleaseNoteRepository
import com.weeth.domain.releasenote.fixture.ReleaseNoteTestFixture
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.SliceImpl

class GetReleaseNoteQueryServiceTest :
    DescribeSpec({
        val releaseNoteRepository = mockk<ReleaseNoteRepository>()
        val service = GetReleaseNoteQueryService(releaseNoteRepository, ReleaseNoteMapper(mockk<FileAccessUrlPort>()))

        beforeTest { clearMocks(releaseNoteRepository) }

        describe("findAll") {
            it("발행 노트만 조회하고 본문에서 추출한 요약으로 매핑한다") {
                val note =
                    ReleaseNoteTestFixture.create(
                        id = 12L,
                        content = "<h3>개선된 기능</h3><p>이미지가 중복으로 보이지 않아요.</p>",
                        status = ReleaseNoteStatus.PUBLISHED,
                    )
                val statusSlot = slot<ReleaseNoteStatus>()
                every {
                    releaseNoteRepository.findPublic(capture(statusSlot), any(), any())
                } returns SliceImpl(listOf(note), Pageable.ofSize(12), false)

                val result = service.findAll(keyword = null, page = 0, size = 12)

                statusSlot.captured shouldBe ReleaseNoteStatus.PUBLISHED
                result.content.single().id shouldBe 12L
                result.content.single().summary shouldBe "개선된 기능 이미지가 중복으로 보이지 않아요."
                result.hasNext shouldBe false
            }

            it("keyword가 null이면 빈 문자열로 조회한다") {
                val keywordSlot = slot<String>()
                every {
                    releaseNoteRepository.findPublic(any(), capture(keywordSlot), any())
                } returns SliceImpl(emptyList(), Pageable.ofSize(12), false)

                service.findAll(keyword = null, page = 0, size = 12)

                keywordSlot.captured shouldBe ""
            }
        }

        describe("find") {
            it("발행된 노트 상세를 반환한다") {
                every { releaseNoteRepository.findByIdAndStatus(12L, ReleaseNoteStatus.PUBLISHED) } returns
                    ReleaseNoteTestFixture.published(id = 12L)

                service.find(12L).id shouldBe 12L
                verify(exactly = 1) { releaseNoteRepository.findByIdAndStatus(12L, ReleaseNoteStatus.PUBLISHED) }
            }

            it("DRAFT/없는 노트면 404 예외를 전달한다") {
                every { releaseNoteRepository.findByIdAndStatus(99L, ReleaseNoteStatus.PUBLISHED) } returns null

                shouldThrow<ReleaseNoteNotFoundException> { service.find(99L) }
            }
        }
    })
