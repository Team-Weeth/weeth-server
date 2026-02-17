package com.weeth.domain.board.application.usecase

import com.weeth.domain.board.application.dto.PartPostDTO
import com.weeth.domain.board.application.dto.PostDTO
import com.weeth.domain.board.application.exception.CategoryAccessDeniedException
import com.weeth.domain.board.application.mapper.PostMapper
import com.weeth.domain.board.domain.entity.enums.Category
import com.weeth.domain.board.domain.entity.enums.Part
import com.weeth.domain.board.domain.service.PostDeleteService
import com.weeth.domain.board.domain.service.PostFindService
import com.weeth.domain.board.domain.service.PostSaveService
import com.weeth.domain.board.domain.service.PostUpdateService
import com.weeth.domain.board.fixture.PostTestFixture
import com.weeth.domain.comment.application.mapper.CommentMapper
import com.weeth.domain.file.application.mapper.FileMapper
import com.weeth.domain.file.domain.entity.FileOwnerType
import com.weeth.domain.file.domain.repository.FileReader
import com.weeth.domain.file.domain.repository.FileRepository
import com.weeth.domain.file.fixture.FileTestFixture
import com.weeth.domain.user.domain.service.CardinalGetService
import com.weeth.domain.user.domain.service.UserCardinalGetService
import com.weeth.domain.user.domain.service.UserGetService
import com.weeth.domain.user.fixture.CardinalTestFixture
import com.weeth.domain.user.fixture.UserTestFixture
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.SliceImpl
import org.springframework.data.domain.Sort

class PostUseCaseImplTest :
    DescribeSpec({

        val postSaveService = mockk<PostSaveService>()
        val postFindService = mockk<PostFindService>()
        val postUpdateService = mockk<PostUpdateService>()
        val postDeleteService = mockk<PostDeleteService>()
        val userGetService = mockk<UserGetService>()
        val userCardinalGetService = mockk<UserCardinalGetService>()
        val cardinalGetService = mockk<CardinalGetService>()
        val fileRepository = mockk<FileRepository>(relaxed = true)
        val fileReader = mockk<FileReader>()
        val mapper = mockk<PostMapper>()
        val fileMapper = mockk<FileMapper>()
        val commentMapper = mockk<CommentMapper>()

        val postUseCase =
            PostUseCaseImpl(
                postSaveService,
                postFindService,
                postUpdateService,
                postDeleteService,
                userGetService,
                userCardinalGetService,
                cardinalGetService,
                fileRepository,
                fileReader,
                mapper,
                fileMapper,
                commentMapper,
            )

        describe("saveEducation") {
            it("교육 게시글 저장 성공") {
                val userId = 1L
                val postId = 1L

                val request = PostDTO.SaveEducation("제목1", "내용", listOf(Part.BE), 1, listOf())
                val user = UserTestFixture.createActiveUser1(1L)
                val post = PostTestFixture.createPost(postId, "제목1", Category.Education)

                every { userGetService.find(userId) } returns user
                every { mapper.fromEducationDto(request, user) } returns post
                every { postSaveService.save(post) } returns post
                every { fileMapper.toFileList(request.files(), FileOwnerType.POST, postId) } returns listOf()
                every { mapper.toSaveResponse(post) } returns PostDTO.SaveResponse(postId)

                val response = postUseCase.saveEducation(request, userId)

                response.id() shouldBe postId
                verify { userGetService.find(userId) }
                verify { postSaveService.save(post) }
                verify { mapper.toSaveResponse(post) }
            }
        }

        describe("save") {
            context("관리자 권한이 없는 사용자가 교육 게시글 생성 시") {
                it("예외를 던진다") {
                    val userId = 1L
                    val request = PostDTO.Save("제목", "내용", Category.Education, null, 1, Part.BE, 1, listOf())
                    val user = UserTestFixture.createActiveUser1(1L)

                    every { userGetService.find(userId) } returns user

                    shouldThrow<CategoryAccessDeniedException> {
                        postUseCase.save(request, userId)
                    }
                }
            }
        }

        describe("findPartPosts") {
            it("특정 파트와 주차 조건으로 게시글 목록 조회 성공") {
                val dto = PartPostDTO(Part.BE, Category.Education, 1, 2, "스터디1")
                val pageNumber = 0
                val pageSize = 5
                val user = UserTestFixture.createActiveUser1()

                val post2 =
                    PostTestFixture.createEducationPost(
                        2L,
                        user,
                        "게시글2",
                        Category.Education,
                        listOf(Part.BE),
                        1,
                        2,
                    )
                val postSlice = SliceImpl(listOf(post2))
                val response2 = PostTestFixture.createResponseAll(post2)

                every {
                    postFindService.findByPartAndOptionalFilters(
                        dto.part(),
                        dto.category(),
                        dto.cardinalNumber(),
                        dto.studyName(),
                        dto.week(),
                        PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "id")),
                    )
                } returns postSlice

                every { mapper.toAll(post2, false) } returns response2
                every { fileReader.exists(FileOwnerType.POST, post2.id, null) } returns false

                val result = postUseCase.findPartPosts(dto, pageNumber, pageSize)

                result.shouldNotBeNull()
                result.content shouldHaveSize 1
                result.content[0].title() shouldBe "게시글2"
                result.content[0].hasFile().shouldBeFalse()

                verify {
                    postFindService.findByPartAndOptionalFilters(
                        dto.part(),
                        dto.category(),
                        dto.cardinalNumber(),
                        dto.studyName(),
                        dto.week(),
                        PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "id")),
                    )
                }
            }
        }

        describe("findEducationPosts") {
            it("관리자 권한 사용자가 교육 게시글 목록 조회 시 성공적으로 반환한다") {
                val userId = 1L
                val part = Part.BE
                val cardinalNumber = 1
                val pageNumber = 0
                val pageSize = 5

                val adminUser = UserTestFixture.createAdmin(userId)

                val post1 =
                    PostTestFixture.createEducationPost(
                        1L,
                        adminUser,
                        "교육글1",
                        Category.Education,
                        listOf(Part.BE),
                        1,
                        1,
                    )
                val post2 =
                    PostTestFixture.createEducationPost(
                        2L,
                        adminUser,
                        "교육글2",
                        Category.Education,
                        listOf(Part.BE),
                        1,
                        2,
                    )
                val postSlice = SliceImpl(listOf(post1, post2))

                val response1 = PostTestFixture.createResponseEducationAll(post1, false)
                val response2 = PostTestFixture.createResponseEducationAll(post2, false)

                every { userGetService.find(userId) } returns adminUser
                every { postFindService.findByCategory(part, Category.Education, cardinalNumber, pageNumber, pageSize) } returns postSlice
                every { mapper.toEducationAll(post1, false) } returns response1
                every { mapper.toEducationAll(post2, false) } returns response2
                every { fileReader.exists(FileOwnerType.POST, post1.id, null) } returns false
                every { fileReader.exists(FileOwnerType.POST, post2.id, null) } returns false

                val result = postUseCase.findEducationPosts(userId, part, cardinalNumber, pageNumber, pageSize)

                result.shouldNotBeNull()
                result.content shouldHaveSize 2
                result.content.map { it.title() } shouldContainExactly listOf("교육글1", "교육글2")

                verify { postFindService.findByCategory(part, Category.Education, cardinalNumber, pageNumber, pageSize) }
                verify { mapper.toEducationAll(post1, false) }
                verify { mapper.toEducationAll(post2, false) }
            }

            it("본인이 속하지 않은 교육 자료를 검색하면 빈 리스트를 반환한다") {
                val userId = 1L
                val part = Part.BE
                val cardinalNumber = 3
                val pageNumber = 0
                val pageSize = 5

                val user = UserTestFixture.createActiveUser1(userId)
                val cardinal = CardinalTestFixture.createCardinal(cardinalNumber = 1, year = 2025, semester = 1)

                every { userGetService.find(userId) } returns user
                every { cardinalGetService.findByUserSide(cardinalNumber) } returns cardinal
                every { userCardinalGetService.notContains(user, cardinal) } returns true

                val result = postUseCase.findEducationPosts(userId, part, cardinalNumber, pageNumber, pageSize)

                result.shouldNotBeNull()
                result.content.shouldBeEmpty()
                result.hasNext().shouldBeFalse()

                verify { userGetService.find(userId) }
                verify { cardinalGetService.findByUserSide(cardinalNumber) }
                verify { userCardinalGetService.notContains(user, cardinal) }
                verify(exactly = 0) { postFindService.findEducationByCardinal(any(), any(), any<Pageable>()) }
            }
        }

        describe("findStudyNames") {
            it("스터디가 없을 시 예외가 발생하지 않는다") {
                val part = Part.BE
                val emptyNames = listOf<String>()
                val expectedResponse = PostDTO.ResponseStudyNames(emptyNames)

                every { postFindService.findByPart(part) } returns emptyNames
                every { mapper.toStudyNames(emptyNames) } returns expectedResponse

                shouldNotThrowAny {
                    postUseCase.findStudyNames(part)
                }

                verify { postFindService.findByPart(part) }
                verify { mapper.toStudyNames(emptyNames) }
            }
        }

        describe("checkFileExistsByPost") {
            it("파일이 존재하는 경우 true를 반환한다") {
                val postId = 1L
                val file =
                    FileTestFixture.createFile(
                        postId,
                        "파일1",
                        storageKey = "POST/2026-02/url1",
                        ownerType = FileOwnerType.POST,
                        ownerId = postId,
                    )

                every { fileReader.exists(FileOwnerType.POST, postId, null) } returns true

                val fileExists = postUseCase.checkFileExistsByPost(postId)

                fileExists.shouldBeTrue()
                verify { fileReader.exists(FileOwnerType.POST, postId, null) }
            }
        }
    })
