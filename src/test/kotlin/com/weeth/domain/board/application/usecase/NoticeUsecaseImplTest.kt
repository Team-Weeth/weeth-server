package com.weeth.domain.board.application.usecase

import com.weeth.domain.board.application.dto.NoticeDTO
import com.weeth.domain.board.application.mapper.NoticeMapper
import com.weeth.domain.board.domain.entity.Notice
import com.weeth.domain.board.domain.service.NoticeDeleteService
import com.weeth.domain.board.domain.service.NoticeFindService
import com.weeth.domain.board.domain.service.NoticeSaveService
import com.weeth.domain.board.domain.service.NoticeUpdateService
import com.weeth.domain.board.fixture.NoticeTestFixture
import com.weeth.domain.comment.application.mapper.CommentMapper
import com.weeth.domain.file.application.dto.request.FileSaveRequest
import com.weeth.domain.file.application.mapper.FileMapper
import com.weeth.domain.file.domain.entity.File
import com.weeth.domain.file.domain.entity.FileOwnerType
import com.weeth.domain.file.domain.repository.FileReader
import com.weeth.domain.file.domain.repository.FileRepository
import com.weeth.domain.file.fixture.FileTestFixture
import com.weeth.domain.user.domain.entity.User
import com.weeth.domain.user.domain.entity.enums.Department
import com.weeth.domain.user.domain.entity.enums.Position
import com.weeth.domain.user.domain.entity.enums.Role
import com.weeth.domain.user.domain.service.UserGetService
import com.weeth.domain.user.fixture.UserTestFixture
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.SliceImpl
import org.springframework.data.domain.Sort
import org.springframework.test.util.ReflectionTestUtils

class NoticeUsecaseImplTest :
    DescribeSpec({

        val noticeSaveService = mockk<NoticeSaveService>(relaxUnitFun = true)
        val noticeFindService = mockk<NoticeFindService>()
        val noticeUpdateService = mockk<NoticeUpdateService>(relaxUnitFun = true)
        val noticeDeleteService = mockk<NoticeDeleteService>(relaxUnitFun = true)
        val userGetService = mockk<UserGetService>()
        val fileRepository = mockk<FileRepository>(relaxed = true)
        val fileReader = mockk<FileReader>()
        val noticeMapper = mockk<NoticeMapper>()
        val commentMapper = mockk<CommentMapper>()
        val fileMapper = mockk<FileMapper>()

        val noticeUsecase =
            NoticeUsecaseImpl(
                noticeSaveService,
                noticeFindService,
                noticeUpdateService,
                noticeDeleteService,
                userGetService,
                fileRepository,
                fileReader,
                noticeMapper,
                commentMapper,
                fileMapper,
            )

        describe("findNotices") {
            it("공지사항이 최신순으로 정렬된다") {
                val user =
                    User
                        .builder()
                        .email("abc@test.com")
                        .name("홍길동")
                        .position(Position.BE)
                        .department(Department.SW)
                        .role(Role.USER)
                        .build()

                val notices =
                    (0 until 5).map { i ->
                        NoticeTestFixture.createNotice(title = "공지$i", user = user).also {
                            ReflectionTestUtils.setField(it, "id", (i + 1).toLong())
                        }
                    }

                val pageable = PageRequest.of(0, 3, Sort.by(Sort.Direction.DESC, "id"))
                val slice = SliceImpl(listOf(notices[4], notices[3], notices[2]), pageable, true)

                every { noticeFindService.findRecentNotices(any()) } returns slice
                every { fileReader.exists(FileOwnerType.NOTICE, any(), null) } returns false
                every { noticeMapper.toAll(any<Notice>(), any<Boolean>()) } answers {
                    val notice = firstArg<Notice>()
                    NoticeDTO.ResponseAll(
                        notice.id,
                        notice.user?.name ?: "",
                        notice.user?.position ?: Position.BE,
                        notice.user?.role ?: Role.USER,
                        notice.title,
                        notice.content,
                        notice.createdAt,
                        notice.commentCount,
                        false,
                    )
                }

                val noticeResponses = noticeUsecase.findNotices(0, 3)

                noticeResponses.shouldNotBeNull()
                noticeResponses.content shouldHaveSize 3
                noticeResponses.content.map { it.title() } shouldContainExactly
                    listOf(notices[4].title, notices[3].title, notices[2].title)
                noticeResponses.hasNext().shouldBeTrue()

                verify(exactly = 1) { noticeFindService.findRecentNotices(pageable) }
            }
        }

        describe("searchNotice") {
            it("공지사항 검색시 결과와 파일 존재여부가 정상적으로 반환") {
                val user =
                    User
                        .builder()
                        .email("abc@test.com")
                        .name("홍길동")
                        .position(Position.BE)
                        .department(Department.SW)
                        .role(Role.USER)
                        .build()

                val notices = mutableListOf<Notice>()
                for (i in 0 until 3) {
                    val notice = NoticeTestFixture.createNotice(title = "공지$i", user = user)
                    ReflectionTestUtils.setField(notice, "id", (i + 1).toLong())
                    notices.add(notice)
                }
                for (i in 3 until 6) {
                    val notice = NoticeTestFixture.createNotice(title = "검색$i", user = user)
                    ReflectionTestUtils.setField(notice, "id", (i + 1).toLong())
                    notices.add(notice)
                }

                val pageable = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "id"))
                val slice = SliceImpl(listOf(notices[5], notices[4], notices[3]), pageable, false)

                every { noticeFindService.search(any<String>(), any()) } returns slice
                every { fileReader.exists(FileOwnerType.NOTICE, any(), null) } answers {
                    val noticeId = secondArg<Long>()
                    noticeId % 2 == 0L
                }
                every { noticeMapper.toAll(any<Notice>(), any<Boolean>()) } answers {
                    val notice = firstArg<Notice>()
                    val fileExists = secondArg<Boolean>()
                    NoticeDTO.ResponseAll(
                        notice.id,
                        notice.user?.name ?: "",
                        notice.user?.position ?: Position.BE,
                        notice.user?.role ?: Role.USER,
                        notice.title,
                        notice.content,
                        notice.createdAt,
                        notice.commentCount,
                        fileExists,
                    )
                }

                val noticeResponses = noticeUsecase.searchNotice("검색", 0, 5)

                noticeResponses.shouldNotBeNull()
                noticeResponses.content shouldHaveSize 3
                noticeResponses.content.map { it.title() } shouldContainExactly
                    listOf(notices[5].title, notices[4].title, notices[3].title)
                noticeResponses.hasNext().shouldBeFalse()

                noticeResponses.content[0].hasFile().shouldBeTrue()
                noticeResponses.content[1].hasFile().shouldBeFalse()

                verify(exactly = 1) { noticeFindService.search("검색", pageable) }
            }
        }

        describe("update") {
            it("공지사항 수정 시 기존 파일 삭제 후 새 파일로 업데이트된다") {
                val noticeId = 1L
                val userId = 1L

                val user = UserTestFixture.createActiveUser1(userId)
                val notice = NoticeTestFixture.createNotice(id = noticeId, title = "기존 제목", user = user)

                val oldFile =
                    FileTestFixture.createFile(
                        1L,
                        "old.pdf",
                        storageKey = "NOTICE/2026-02/old.pdf",
                        ownerType = FileOwnerType.NOTICE,
                        ownerId = noticeId,
                        contentType = "application/pdf",
                    )
                val oldFiles = listOf(oldFile)

                val dto =
                    NoticeDTO.Update(
                        "수정된 제목",
                        "수정된 내용",
                        listOf(FileSaveRequest("new.pdf", "NOTICE/2026-02/new.pdf", 100L, "application/pdf")),
                    )

                val newFile =
                    FileTestFixture.createFile(
                        2L,
                        "new.pdf",
                        storageKey = "NOTICE/2026-02/new.pdf",
                        ownerType = FileOwnerType.NOTICE,
                        ownerId = noticeId,
                        contentType = "application/pdf",
                    )
                val newFiles = listOf(newFile)

                val expectedResponse = NoticeDTO.SaveResponse(noticeId)

                every { noticeFindService.find(noticeId) } returns notice
                every { fileReader.findAll(FileOwnerType.NOTICE, noticeId, null) } returns oldFiles
                every { fileMapper.toFileList(dto.files(), FileOwnerType.NOTICE, noticeId) } returns newFiles
                every { noticeMapper.toSaveResponse(notice) } returns expectedResponse

                val response = noticeUsecase.update(noticeId, dto, userId)

                response shouldBe expectedResponse

                verify { noticeFindService.find(noticeId) }
                verify { fileReader.findAll(FileOwnerType.NOTICE, noticeId, null) }
                verify { fileRepository.deleteAll(oldFiles) }
                verify { fileMapper.toFileList(dto.files(), FileOwnerType.NOTICE, noticeId) }
                verify { fileRepository.saveAll(newFiles) }
                verify { noticeUpdateService.update(notice, dto) }
            }

            it("공지사항 엔티티 update() 호출 시 제목과 내용이 변경된다") {
                val userId = 1L
                val user = UserTestFixture.createActiveUser1(userId)
                val notice = NoticeTestFixture.createNotice(id = 1L, title = "기존 제목", user = user)
                val dto = NoticeDTO.Update("수정된 제목", "수정된 내용", listOf())

                notice.update(dto)

                notice.title shouldBe dto.title()
                notice.content shouldBe dto.content()
            }
        }
    })
