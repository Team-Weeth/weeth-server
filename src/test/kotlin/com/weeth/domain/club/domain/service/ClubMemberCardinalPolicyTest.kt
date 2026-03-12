package com.weeth.domain.club.domain.service

import com.weeth.domain.cardinal.application.exception.CardinalNotFoundException
import com.weeth.domain.cardinal.fixture.CardinalTestFixture
import com.weeth.domain.club.domain.entity.ClubMemberCardinal
import com.weeth.domain.club.domain.repository.ClubMemberCardinalReader
import com.weeth.domain.club.fixture.ClubMemberTestFixture
import com.weeth.domain.club.fixture.ClubTestFixture
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk

class ClubMemberCardinalPolicyTest :
    DescribeSpec({
        val clubMemberCardinalReader = mockk<ClubMemberCardinalReader>()
        val policy = ClubMemberCardinalPolicy(clubMemberCardinalReader)

        val club = ClubTestFixture.createClub()
        val member = ClubMemberTestFixture.createActiveMember(club = club)

        beforeTest {
            clearMocks(clubMemberCardinalReader)
        }

        describe("getCurrentCardinal") {
            context("기수가 존재하는 경우") {
                it("최신 기수의 Cardinal을 반환해야 한다") {
                    val cardinal =
                        CardinalTestFixture.createCardinal(
                            club = club,
                            cardinalNumber = 5,
                            year = 2026,
                            semester = 1,
                        )
                    val memberCardinal = ClubMemberCardinal.create(clubMember = member, cardinal = cardinal)

                    every { clubMemberCardinalReader.findLatestCardinalByClubMember(member) } returns memberCardinal

                    val result = policy.getCurrentCardinal(member)

                    result shouldBe cardinal
                }
            }

            context("기수가 존재하지 않는 경우") {
                it("CardinalNotFoundException을 발생시켜야 한다") {
                    every { clubMemberCardinalReader.findLatestCardinalByClubMember(member) } returns null

                    shouldThrow<CardinalNotFoundException> {
                        policy.getCurrentCardinal(member)
                    }
                }
            }
        }

        describe("notContains") {
            val cardinal =
                CardinalTestFixture.createCardinal(
                    id = 10L,
                    club = club,
                    cardinalNumber = 3,
                    year = 2025,
                    semester = 1,
                )

            context("멤버가 해당 기수에 속하지 않는 경우") {
                it("true를 반환해야 한다") {
                    every {
                        clubMemberCardinalReader.existsByClubMemberAndCardinalId(member, cardinal.id)
                    } returns false

                    policy.notContains(member, cardinal) shouldBe true
                }
            }

            context("멤버가 해당 기수에 이미 속한 경우") {
                it("false를 반환해야 한다") {
                    every {
                        clubMemberCardinalReader.existsByClubMemberAndCardinalId(member, cardinal.id)
                    } returns true

                    policy.notContains(member, cardinal) shouldBe false
                }
            }
        }

        describe("isCurrent") {
            context("기수 이력이 없는 경우") {
                it("true를 반환해야 한다 (첫 기수 등록)") {
                    val cardinal =
                        CardinalTestFixture.createCardinal(
                            club = club,
                            cardinalNumber = 1,
                            year = 2024,
                            semester = 1,
                        )

                    every { clubMemberCardinalReader.findLatestCardinalByClubMember(member) } returns null

                    policy.isCurrent(member, cardinal) shouldBe true
                }
            }

            context("전달된 기수가 최신 기수보다 높은 경우") {
                it("true를 반환해야 한다") {
                    val latestCardinal =
                        CardinalTestFixture.createCardinal(
                            club = club,
                            cardinalNumber = 3,
                            year = 2025,
                            semester = 1,
                        )
                    val newCardinal =
                        CardinalTestFixture.createCardinal(
                            club = club,
                            cardinalNumber = 4,
                            year = 2025,
                            semester = 2,
                        )
                    val latestMemberCardinal =
                        ClubMemberCardinal.create(clubMember = member, cardinal = latestCardinal)

                    every {
                        clubMemberCardinalReader.findLatestCardinalByClubMember(member)
                    } returns latestMemberCardinal

                    policy.isCurrent(member, newCardinal) shouldBe true
                }
            }

            context("전달된 기수가 최신 기수와 같은 경우") {
                it("false를 반환해야 한다") {
                    val cardinal =
                        CardinalTestFixture.createCardinal(
                            club = club,
                            cardinalNumber = 3,
                            year = 2025,
                            semester = 1,
                        )
                    val memberCardinal = ClubMemberCardinal.create(clubMember = member, cardinal = cardinal)

                    every {
                        clubMemberCardinalReader.findLatestCardinalByClubMember(member)
                    } returns memberCardinal

                    policy.isCurrent(member, cardinal) shouldBe false
                }
            }

            context("전달된 기수가 최신 기수보다 낮은 경우") {
                it("false를 반환해야 한다 (OB 기수 등록 시나리오)") {
                    val latestCardinal =
                        CardinalTestFixture.createCardinal(
                            club = club,
                            cardinalNumber = 5,
                            year = 2026,
                            semester = 1,
                        )
                    val oldCardinal =
                        CardinalTestFixture.createCardinal(
                            club = club,
                            cardinalNumber = 2,
                            year = 2024,
                            semester = 2,
                        )
                    val latestMemberCardinal =
                        ClubMemberCardinal.create(clubMember = member, cardinal = latestCardinal)

                    every {
                        clubMemberCardinalReader.findLatestCardinalByClubMember(member)
                    } returns latestMemberCardinal

                    policy.isCurrent(member, oldCardinal) shouldBe false
                }
            }
        }
    })
