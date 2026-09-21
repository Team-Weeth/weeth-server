package com.weeth.domain.club.application.usecase.command

import com.weeth.domain.club.application.dto.request.ClubPositionOptionRequest
import com.weeth.domain.club.application.dto.request.SaveClubPositionOptionsRequest
import com.weeth.domain.club.application.exception.PositionOptionLimitExceededException
import com.weeth.domain.club.domain.entity.ClubPositionOption
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
            every { clubPositionOptionRepository.hardDeleteAllByClubId(1L) } returns 0
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
                                ClubPositionOptionRequest(name = "백엔드", colorHex = "#4CAF50"),
                                ClubPositionOptionRequest(name = "프론트엔드", colorHex = "#FF5722"),
                            ),
                    ),
                )

                verify(exactly = 1) { clubPositionOptionRepository.hardDeleteAllByClubId(1L) }
                val saved = optionsSlot.captured
                saved.size shouldBe 2
                saved[0].name shouldBe "백엔드"
                saved[0].displayOrder shouldBe 0
                saved[1].name shouldBe "프론트엔드"
                saved[1].displayOrder shouldBe 1
                // 옵션이 없던 동아리이므로 정리할 멤버 참조가 없다
                verify(exactly = 0) { clubMemberRepository.clearPositionOptionReferences(any()) }
            }

            it("기존 옵션이 있는 동아리는 기존 옵션을 삭제하고 요청 순서대로 다시 생성한다") {
                val existingOption = ClubPositionOptionTestFixture.createOption(id = 50L, club = club)
                every {
                    clubPositionOptionRepository.findAllByClubIdOrderByDisplayOrderAsc(1L)
                } returns listOf(existingOption)
                val optionsSlot = slot<List<ClubPositionOption>>()
                every { clubPositionOptionRepository.saveAll(capture(optionsSlot)) } answers { firstArg() }

                useCase.save(
                    1L,
                    10L,
                    SaveClubPositionOptionsRequest(
                        options = listOf(ClubPositionOptionRequest(name = "디자인", colorHex = "#9C27B0")),
                    ),
                )

                verify(exactly = 1) { clubPositionOptionRepository.hardDeleteAllByClubId(1L) }
                optionsSlot.captured.single().name shouldBe "디자인"
            }

            it("옵션 삭제 전에 그 옵션을 참조하던 멤버들의 positionOption을 먼저 정리한다") {
                // save()는 id-diff가 아닌 hard-delete-all-then-recreate이므로,
                // 단순 이름/색만 바뀌는 것처럼 보이는 요청도 기존 옵션 id 전체가 clear 대상이어야 한다.
                val existingOption1 = ClubPositionOptionTestFixture.createOption(id = 50L, club = club)
                val existingOption2 = ClubPositionOptionTestFixture.createOption(id = 51L, club = club)
                every {
                    clubPositionOptionRepository.findAllByClubIdOrderByDisplayOrderAsc(1L)
                } returns listOf(existingOption1, existingOption2)

                useCase.save(
                    1L,
                    10L,
                    SaveClubPositionOptionsRequest(
                        options = listOf(ClubPositionOptionRequest(name = "디자인", colorHex = "#9C27B0")),
                    ),
                )

                verify(exactly = 1) { clubMemberRepository.clearPositionOptionReferences(listOf(50L, 51L)) }
            }

            it("기존 옵션이 없으면 멤버 참조 정리를 호출하지 않는다") {
                every { clubPositionOptionRepository.findAllByClubIdOrderByDisplayOrderAsc(1L) } returns emptyList()

                useCase.save(
                    1L,
                    10L,
                    SaveClubPositionOptionsRequest(
                        options = listOf(ClubPositionOptionRequest(name = "디자인", colorHex = "#9C27B0")),
                    ),
                )

                verify(exactly = 0) { clubMemberRepository.clearPositionOptionReferences(any()) }
            }

            it("옵션이 6개를 초과하면 PositionOptionLimitExceededException이 발생하고 삭제/저장하지 않는다") {
                val options = (1..7).map { ClubPositionOptionRequest(name = "옵션$it", colorHex = "#4CAF50") }

                shouldThrow<PositionOptionLimitExceededException> {
                    useCase.save(1L, 10L, SaveClubPositionOptionsRequest(options = options))
                }

                verify(exactly = 0) { clubPositionOptionRepository.hardDeleteAllByClubId(any()) }
                verify(exactly = 0) { clubPositionOptionRepository.saveAll(any<List<ClubPositionOption>>()) }
            }

            it("관리자가 아니면 requireAdmin에서 예외가 전파되고 이후 로직이 실행되지 않는다") {
                every { clubPermissionPolicy.requireAdmin(1L, 20L) } throws RuntimeException("관리자 아님")

                shouldThrow<RuntimeException> {
                    useCase.save(
                        1L,
                        20L,
                        SaveClubPositionOptionsRequest(
                            options = listOf(ClubPositionOptionRequest(name = "백엔드", colorHex = "#4CAF50")),
                        ),
                    )
                }

                verify(exactly = 0) { clubPositionOptionRepository.hardDeleteAllByClubId(any()) }
                verify(exactly = 0) { clubPositionOptionRepository.saveAll(any<List<ClubPositionOption>>()) }
            }
        }
    })
