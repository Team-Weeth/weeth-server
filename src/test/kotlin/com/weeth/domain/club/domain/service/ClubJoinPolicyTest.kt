package com.weeth.domain.club.domain.service

import com.weeth.domain.club.application.exception.ClubCreateLimitExceededException
import com.weeth.domain.club.application.exception.ClubJoinLimitExceededException
import com.weeth.domain.club.domain.enums.MemberRole
import com.weeth.domain.club.domain.enums.MemberStatus
import com.weeth.domain.club.domain.repository.ClubMemberReader
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk

class ClubJoinPolicyTest :
    DescribeSpec({
        val clubMemberReader = mockk<ClubMemberReader>()
        val policy = ClubJoinPolicy(clubMemberReader)

        beforeTest {
            clearMocks(clubMemberReader)
        }

        describe("validateJoinLimit") {
            context("USER로 가입한 동아리가 없는 경우") {
                it("검증을 통과해야 한다") {
                    every {
                        clubMemberReader.countByUserIdAndMemberStatusAndMemberRole(
                            1L,
                            MemberStatus.ACTIVE,
                            MemberRole.USER,
                        )
                    } returns 0L

                    shouldNotThrowAny {
                        policy.validateJoinLimit(1L)
                    }
                }
            }

            context("이미 USER로 1개 동아리에 가입한 경우") {
                it("ClubJoinLimitExceededException을 발생시켜야 한다") {
                    every {
                        clubMemberReader.countByUserIdAndMemberStatusAndMemberRole(
                            1L,
                            MemberStatus.ACTIVE,
                            MemberRole.USER,
                        )
                    } returns 1L

                    shouldThrow<ClubJoinLimitExceededException> {
                        policy.validateJoinLimit(1L)
                    }
                }
            }

            context("LEAD로 1개 동아리를 생성했지만 USER 가입은 없는 경우") {
                it("검증을 통과해야 한다 (역할이 다르므로 허용)") {
                    every {
                        clubMemberReader.countByUserIdAndMemberStatusAndMemberRole(
                            1L,
                            MemberStatus.ACTIVE,
                            MemberRole.USER,
                        )
                    } returns 0L

                    shouldNotThrowAny {
                        policy.validateJoinLimit(1L)
                    }
                }
            }
        }

        describe("validateCreateLimit") {
            context("LEAD로 생성한 동아리가 없는 경우") {
                it("검증을 통과해야 한다") {
                    every {
                        clubMemberReader.countByUserIdAndMemberStatusAndMemberRole(
                            1L,
                            MemberStatus.ACTIVE,
                            MemberRole.LEAD,
                        )
                    } returns 0L

                    shouldNotThrowAny {
                        policy.validateCreateLimit(1L)
                    }
                }
            }

            context("이미 LEAD로 1개 동아리를 생성한 경우") {
                it("ClubCreateLimitExceededException을 발생시켜야 한다") {
                    every {
                        clubMemberReader.countByUserIdAndMemberStatusAndMemberRole(
                            1L,
                            MemberStatus.ACTIVE,
                            MemberRole.LEAD,
                        )
                    } returns 1L

                    shouldThrow<ClubCreateLimitExceededException> {
                        policy.validateCreateLimit(1L)
                    }
                }
            }
        }
    })
