package com.weeth.domain.club.application.usecase.command

import com.weeth.domain.club.application.dto.request.ClubPositionOptionRequest
import com.weeth.domain.club.application.dto.request.SaveClubPositionOptionsRequest
import com.weeth.domain.club.application.exception.PositionOptionLimitExceededException
import com.weeth.domain.club.application.exception.PositionOptionNotFoundException
import com.weeth.domain.club.application.exception.PositionOptionUpdateDeleteConflictException
import com.weeth.domain.club.domain.entity.ClubPositionOption
import com.weeth.domain.club.domain.enums.PositionColor
import com.weeth.domain.club.domain.repository.ClubMemberRepository
import com.weeth.domain.club.domain.repository.ClubPositionOptionRepository
import com.weeth.domain.club.domain.repository.ClubReader
import com.weeth.domain.club.domain.service.ClubPermissionPolicy
import com.weeth.domain.club.fixture.ClubMemberTestFixture
import com.weeth.domain.club.fixture.ClubPositionOptionTestFixture
import com.weeth.domain.club.fixture.ClubTestFixture
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify

class ManageClubPositionOptionUseCaseTest :
    DescribeSpec({
        val clubReader = mockk<ClubReader>()
        val clubPositionOptionRepository = mockk<ClubPositionOptionRepository>()
        val clubMemberRepository = mockk<ClubMemberRepository>(relaxed = true)
        val clubPermissionPolicy = mockk<ClubPermissionPolicy>()
        val useCase =
            ManageClubPositionOptionUseCase(
                clubReader = clubReader,
                clubPositionOptionRepository = clubPositionOptionRepository,
                clubMemberRepository = clubMemberRepository,
                clubPermissionPolicy = clubPermissionPolicy,
            )

        val club = ClubTestFixture.createClub(id = 1L)
        val adminMember = ClubMemberTestFixture.createAdminMember(club = club)

        beforeTest {
            clearMocks(clubReader, clubPositionOptionRepository, clubMemberRepository, clubPermissionPolicy)
            every { clubPermissionPolicy.requireAdmin(1L, 10L) } returns adminMember
            every { clubReader.getClubById(1L) } returns club
            every { clubPositionOptionRepository.findAllByClubIdOrderByDisplayOrderAsc(1L) } returns emptyList()
            every { clubPositionOptionRepository.deleteAllByIdInBatch(any()) } returns Unit
            every { clubPositionOptionRepository.saveAll(any<List<ClubPositionOption>>()) } answers { firstArg() }
        }

        describe("save") {
            it("id가 없는 옵션은 요청 순서대로 신규 생성된다") {
                val optionsSlot = slot<List<ClubPositionOption>>()
                every { clubPositionOptionRepository.saveAll(capture(optionsSlot)) } answers { firstArg() }

                useCase.save(
                    1L,
                    10L,
                    SaveClubPositionOptionsRequest(
                        options =
                            listOf(
                                ClubPositionOptionRequest(id = null, name = "백엔드", color = PositionColor.PRIMARY),
                                ClubPositionOptionRequest(id = null, name = "프론트엔드", color = PositionColor.SECONDARY),
                            ),
                    ),
                )

                val saved = optionsSlot.captured
                saved.size shouldBe 2
                saved[0].name shouldBe "백엔드"
                saved[0].displayOrder shouldBe 0
                saved[1].name shouldBe "프론트엔드"
                saved[1].displayOrder shouldBe 1
                verify(exactly = 0) { clubMemberRepository.clearPositionOptionReferences(any()) }
                verify(exactly = 0) { clubPositionOptionRepository.deleteAllByIdInBatch(any()) }
            }

            it("id가 있는 옵션은 기존 엔티티를 재사용해 update만 호출하고 삭제/생성하지 않는다") {
                val existingOption =
                    ClubPositionOptionTestFixture.createOption(
                        id = 50L,
                        club = club,
                        name = "백엔드",
                        color = PositionColor.PRIMARY,
                        displayOrder = 0,
                    )
                every {
                    clubPositionOptionRepository.findAllByClubIdOrderByDisplayOrderAsc(1L)
                } returns listOf(existingOption)

                useCase.save(
                    1L,
                    10L,
                    SaveClubPositionOptionsRequest(
                        options =
                            listOf(
                                ClubPositionOptionRequest(id = 50L, name = "백엔드팀", color = PositionColor.SECONDARY),
                            ),
                    ),
                )

                existingOption.id shouldBe 50L
                existingOption.name shouldBe "백엔드팀"
                existingOption.color shouldBe PositionColor.SECONDARY
                existingOption.displayOrder shouldBe 0
                verify(exactly = 0) { clubMemberRepository.clearPositionOptionReferences(any()) }
                verify(exactly = 0) { clubPositionOptionRepository.deleteAllByIdInBatch(any()) }
                verify(exactly = 0) { clubPositionOptionRepository.saveAll(any<List<ClubPositionOption>>()) }
            }

            it("deletedPositionIds에 있는 id만 멤버 참조 정리 후 삭제된다") {
                val kept = ClubPositionOptionTestFixture.createOption(id = 50L, club = club, name = "백엔드")
                val removed = ClubPositionOptionTestFixture.createOption(id = 51L, club = club, name = "디자인")
                every {
                    clubPositionOptionRepository.findAllByClubIdOrderByDisplayOrderAsc(1L)
                } returns listOf(kept, removed)

                useCase.save(
                    1L,
                    10L,
                    SaveClubPositionOptionsRequest(
                        options =
                            listOf(
                                ClubPositionOptionRequest(id = 50L, name = "백엔드", color = PositionColor.PRIMARY),
                            ),
                        deletedPositionIds = listOf(51L),
                    ),
                )

                verify(exactly = 1) { clubMemberRepository.clearPositionOptionReferences(listOf(51L)) }
                verify(exactly = 1) { clubPositionOptionRepository.deleteAllByIdInBatch(listOf(51L)) }
            }

            it("혼합 시나리오: 수정·삭제·신규 생성이 각각 올바르게 처리된다") {
                val kept = ClubPositionOptionTestFixture.createOption(id = 50L, club = club, name = "백엔드")
                val removed = ClubPositionOptionTestFixture.createOption(id = 51L, club = club, name = "디자인")
                every {
                    clubPositionOptionRepository.findAllByClubIdOrderByDisplayOrderAsc(1L)
                } returns listOf(kept, removed)
                val optionsSlot = slot<List<ClubPositionOption>>()
                every { clubPositionOptionRepository.saveAll(capture(optionsSlot)) } answers { firstArg() }

                useCase.save(
                    1L,
                    10L,
                    SaveClubPositionOptionsRequest(
                        options =
                            listOf(
                                ClubPositionOptionRequest(id = 50L, name = "백엔드", color = PositionColor.SECONDARY),
                                ClubPositionOptionRequest(id = null, name = "기획", color = PositionColor.PURPLE),
                            ),
                        deletedPositionIds = listOf(51L),
                    ),
                )

                kept.id shouldBe 50L
                kept.displayOrder shouldBe 0
                optionsSlot.captured.single().name shouldBe "기획"
                optionsSlot.captured.single().displayOrder shouldBe 1
                verify(exactly = 1) { clubMemberRepository.clearPositionOptionReferences(listOf(51L)) }
                verify(exactly = 1) { clubPositionOptionRepository.deleteAllByIdInBatch(listOf(51L)) }
            }

            it("같은 id가 options(수정)와 deletedPositionIds(삭제)에 동시에 있으면 예외가 발생하고 아무 것도 변경하지 않는다") {
                val existing = ClubPositionOptionTestFixture.createOption(id = 50L, club = club, name = "백엔드")
                every {
                    clubPositionOptionRepository.findAllByClubIdOrderByDisplayOrderAsc(1L)
                } returns listOf(existing)

                shouldThrow<PositionOptionUpdateDeleteConflictException> {
                    useCase.save(
                        1L,
                        10L,
                        SaveClubPositionOptionsRequest(
                            options =
                                listOf(
                                    ClubPositionOptionRequest(id = 50L, name = "백엔드", color = PositionColor.PRIMARY),
                                ),
                            deletedPositionIds = listOf(50L),
                        ),
                    )
                }

                verify(exactly = 0) { clubMemberRepository.clearPositionOptionReferences(any()) }
                verify(exactly = 0) { clubPositionOptionRepository.deleteAllByIdInBatch(any()) }
                verify(exactly = 0) { clubPositionOptionRepository.saveAll(any<List<ClubPositionOption>>()) }
            }

            it("options의 id가 이 동아리에 존재하지 않으면 PositionOptionNotFoundException이 발생한다") {
                every { clubPositionOptionRepository.findAllByClubIdOrderByDisplayOrderAsc(1L) } returns emptyList()

                shouldThrow<PositionOptionNotFoundException> {
                    useCase.save(
                        1L,
                        10L,
                        SaveClubPositionOptionsRequest(
                            options =
                                listOf(
                                    ClubPositionOptionRequest(id = 999L, name = "백엔드", color = PositionColor.PRIMARY),
                                ),
                        ),
                    )
                }

                verify(exactly = 0) { clubPositionOptionRepository.saveAll(any<List<ClubPositionOption>>()) }
            }

            it("deletedPositionIds의 id가 이 동아리에 존재하지 않으면 PositionOptionNotFoundException이 발생하고 아무 것도 삭제하지 않는다") {
                every { clubPositionOptionRepository.findAllByClubIdOrderByDisplayOrderAsc(1L) } returns emptyList()

                shouldThrow<PositionOptionNotFoundException> {
                    useCase.save(
                        1L,
                        10L,
                        SaveClubPositionOptionsRequest(options = emptyList(), deletedPositionIds = listOf(999L)),
                    )
                }

                verify(exactly = 0) { clubMemberRepository.clearPositionOptionReferences(any()) }
                verify(exactly = 0) { clubPositionOptionRepository.deleteAllByIdInBatch(any()) }
            }

            it("옵션이 6개를 초과하면 PositionOptionLimitExceededException이 발생하고 아무 것도 변경하지 않는다") {
                val options =
                    (1..7).map {
                        ClubPositionOptionRequest(
                            id = null,
                            name = "옵션$it",
                            color = PositionColor.PRIMARY,
                        )
                    }

                shouldThrow<PositionOptionLimitExceededException> {
                    useCase.save(1L, 10L, SaveClubPositionOptionsRequest(options = options))
                }

                verify(exactly = 0) { clubPositionOptionRepository.deleteAllByIdInBatch(any()) }
                verify(exactly = 0) { clubPositionOptionRepository.saveAll(any<List<ClubPositionOption>>()) }
                verify(exactly = 0) { clubMemberRepository.clearPositionOptionReferences(any()) }
            }

            it("관리자가 아니면 requireAdmin에서 예외가 전파되고 이후 로직이 실행되지 않는다") {
                every { clubPermissionPolicy.requireAdmin(1L, 20L) } throws RuntimeException("관리자 아님")

                shouldThrow<RuntimeException> {
                    useCase.save(
                        1L,
                        20L,
                        SaveClubPositionOptionsRequest(
                            options =
                                listOf(
                                    ClubPositionOptionRequest(id = null, name = "백엔드", color = PositionColor.PRIMARY),
                                ),
                        ),
                    )
                }

                verify(exactly = 0) { clubPositionOptionRepository.deleteAllByIdInBatch(any()) }
                verify(exactly = 0) { clubPositionOptionRepository.saveAll(any<List<ClubPositionOption>>()) }
            }
        }
    })
