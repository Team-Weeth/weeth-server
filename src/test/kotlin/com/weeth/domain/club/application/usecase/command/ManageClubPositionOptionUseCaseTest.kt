package com.weeth.domain.club.application.usecase.command

import com.weeth.domain.club.application.dto.request.ClubPositionOptionRequest
import com.weeth.domain.club.application.dto.request.SaveClubPositionOptionsRequest
import com.weeth.domain.club.application.exception.PositionOptionLimitExceededException
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
            it("옵션이 없던 동아리는 요청 순서대로 전체 옵션을 새로 생성한다") {
                val optionsSlot = slot<List<ClubPositionOption>>()
                every { clubPositionOptionRepository.saveAll(capture(optionsSlot)) } answers { firstArg() }

                useCase.save(
                    1L,
                    10L,
                    SaveClubPositionOptionsRequest(
                        options =
                            listOf(
                                ClubPositionOptionRequest(name = "백엔드", color = PositionColor.PRIMARY),
                                ClubPositionOptionRequest(name = "프론트엔드", color = PositionColor.SECONDARY),
                            ),
                    ),
                )

                val saved = optionsSlot.captured
                saved.size shouldBe 2
                saved[0].name shouldBe "백엔드"
                saved[0].displayOrder shouldBe 0
                saved[1].name shouldBe "프론트엔드"
                saved[1].displayOrder shouldBe 1
                // 옵션이 없던 동아리이므로 정리할 멤버 참조가 없다
                verify(exactly = 0) { clubMemberRepository.clearPositionOptionReferences(any()) }
                verify(exactly = 0) { clubPositionOptionRepository.deleteAllByIdInBatch(any()) }
            }

            it("요청에 동일한 name이 남아있으면 기존 옵션 엔티티를 재사용해 id를 보존한다") {
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
                        options = listOf(ClubPositionOptionRequest(name = "백엔드", color = PositionColor.SECONDARY)),
                    ),
                )

                existingOption.id shouldBe 50L
                existingOption.color shouldBe PositionColor.SECONDARY
                existingOption.displayOrder shouldBe 0
                // 이름이 그대로 유지된 옵션이므로 삭제도, 멤버 참조 정리도 일어나지 않는다
                verify(exactly = 0) { clubMemberRepository.clearPositionOptionReferences(any()) }
                verify(exactly = 0) { clubPositionOptionRepository.deleteAllByIdInBatch(any()) }
                verify(exactly = 0) { clubPositionOptionRepository.saveAll(any<List<ClubPositionOption>>()) }
            }

            it("요청에서 사라진 name의 기존 옵션만 삭제하고, 그 옵션을 참조하던 멤버만 정리한다") {
                val keptOption =
                    ClubPositionOptionTestFixture.createOption(id = 50L, club = club, name = "백엔드")
                val removedOption =
                    ClubPositionOptionTestFixture.createOption(id = 51L, club = club, name = "디자인")
                every {
                    clubPositionOptionRepository.findAllByClubIdOrderByDisplayOrderAsc(1L)
                } returns listOf(keptOption, removedOption)

                useCase.save(
                    1L,
                    10L,
                    SaveClubPositionOptionsRequest(
                        options = listOf(ClubPositionOptionRequest(name = "백엔드", color = PositionColor.PRIMARY)),
                    ),
                )

                verify(exactly = 1) { clubMemberRepository.clearPositionOptionReferences(listOf(51L)) }
                verify(exactly = 1) { clubPositionOptionRepository.deleteAllByIdInBatch(listOf(51L)) }
            }

            it("혼합 시나리오: 유지·삭제·신규 생성이 각각 올바르게 처리된다") {
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
                                ClubPositionOptionRequest(name = "백엔드", color = PositionColor.SECONDARY),
                                ClubPositionOptionRequest(name = "기획", color = PositionColor.PURPLE),
                            ),
                    ),
                )

                kept.id shouldBe 50L
                kept.displayOrder shouldBe 0
                optionsSlot.captured.single().name shouldBe "기획"
                optionsSlot.captured.single().displayOrder shouldBe 1
                verify(exactly = 1) { clubMemberRepository.clearPositionOptionReferences(listOf(51L)) }
                verify(exactly = 1) { clubPositionOptionRepository.deleteAllByIdInBatch(listOf(51L)) }
            }

            it("기존 옵션이 없으면 멤버 참조 정리를 호출하지 않는다") {
                every { clubPositionOptionRepository.findAllByClubIdOrderByDisplayOrderAsc(1L) } returns emptyList()

                useCase.save(
                    1L,
                    10L,
                    SaveClubPositionOptionsRequest(
                        options = listOf(ClubPositionOptionRequest(name = "디자인", color = PositionColor.PURPLE)),
                    ),
                )

                verify(exactly = 0) { clubMemberRepository.clearPositionOptionReferences(any()) }
            }

            it("옵션이 6개를 초과하면 PositionOptionLimitExceededException이 발생하고 아무 것도 변경하지 않는다") {
                val options = (1..7).map { ClubPositionOptionRequest(name = "옵션$it", color = PositionColor.PRIMARY) }

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
                            options = listOf(ClubPositionOptionRequest(name = "백엔드", color = PositionColor.PRIMARY)),
                        ),
                    )
                }

                verify(exactly = 0) { clubPositionOptionRepository.deleteAllByIdInBatch(any()) }
                verify(exactly = 0) { clubPositionOptionRepository.saveAll(any<List<ClubPositionOption>>()) }
            }
        }
    })
