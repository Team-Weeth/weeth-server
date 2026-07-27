package com.weeth.domain.user.application.usecase.command

import com.weeth.domain.club.domain.enums.MemberRole
import com.weeth.domain.club.domain.enums.MemberStatus
import com.weeth.domain.club.domain.repository.ClubMemberRepository
import com.weeth.domain.club.domain.service.ClubActivityDeletionPolicy
import com.weeth.domain.club.fixture.ClubMemberTestFixture
import com.weeth.domain.file.domain.enums.FileOwnerType
import com.weeth.domain.file.domain.repository.FileRepository
import com.weeth.domain.user.application.exception.UserHasLeadClubException
import com.weeth.domain.user.domain.entity.UserProfile
import com.weeth.domain.user.domain.enums.Status
import com.weeth.domain.user.domain.repository.UserProfileRepository
import com.weeth.domain.user.domain.repository.UserReader
import com.weeth.domain.user.fixture.UserTestFixture
import com.weeth.global.auth.jwt.application.usecase.JwtManageUseCase
import com.weeth.global.auth.jwt.domain.port.AccessTokenBlacklistStorePort
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class LeaveUserUseCaseTest :
    DescribeSpec({
        val userReader = mockk<UserReader>()
        val clubMemberRepository = mockk<ClubMemberRepository>()
        val clubActivityDeletionPolicy = mockk<ClubActivityDeletionPolicy>()
        val fileRepository = mockk<FileRepository>()
        val userProfileRepository = mockk<UserProfileRepository>()
        val jwtManageUseCase = mockk<JwtManageUseCase>()
        val accessTokenBlacklistStore = mockk<AccessTokenBlacklistStorePort>()
        val meterRegistry = SimpleMeterRegistry()
        val clock = Clock.fixed(Instant.parse("2026-06-12T03:00:00Z"), ZoneId.of("Asia/Seoul"))
        val useCase =
            LeaveUserUseCase(
                userReader = userReader,
                clubMemberRepository = clubMemberRepository,
                clubActivityDeletionPolicy = clubActivityDeletionPolicy,
                fileRepository = fileRepository,
                userProfileRepository = userProfileRepository,
                jwtManageUseCase = jwtManageUseCase,
                accessTokenBlacklistStore = accessTokenBlacklistStore,
                meterRegistry = meterRegistry,
                clock = clock,
            )

        beforeTest {
            clearMocks(
                userReader,
                clubMemberRepository,
                clubActivityDeletionPolicy,
                fileRepository,
                userProfileRepository,
                jwtManageUseCase,
                accessTokenBlacklistStore,
            )
            every { fileRepository.hardDeleteActiveByOwnerTypeAndOwnerId(any(), any()) } returns 0
            every { fileRepository.hardDeleteActiveByOwnerTypeAndOwnerIdIn(any(), any()) } returns 0
            every { userProfileRepository.findAllByUserIdOrderByIdAsc(any()) } returns emptyList()
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.clearSynchronization()
            }
            meterRegistry.clear()
        }

        afterTest {
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.clearSynchronization()
            }
        }

        describe("execute") {
            it("ACTIVE 멤버십이 없으면 사용자만 탈퇴하고 커밋 후 refresh token을 삭제한다") {
                val user = UserTestFixture.createRegisteredUser(1L)
                val now = LocalDateTime.now(clock)
                every { userReader.getByIdWithLock(1L) } returns user
                every { clubMemberRepository.findAllActiveByUserIdWithLock(1L) } returns emptyList()
                justRun { jwtManageUseCase.deleteRefreshToken(1L) }
                justRun { accessTokenBlacklistStore.blacklist(1L) }
                TransactionSynchronizationManager.initSynchronization()

                useCase.execute(1L)

                user.status shouldBe Status.LEFT
                user.leftAt shouldBe now
                user.hardDeleteAfter shouldBe now.plusDays(30)
                verify(exactly = 0) { jwtManageUseCase.deleteRefreshToken(any()) }
                verify(exactly = 0) { accessTokenBlacklistStore.blacklist(any()) }

                TransactionSynchronizationManager.getSynchronizations().forEach { it.afterCommit() }

                verify(exactly = 1) { jwtManageUseCase.deleteRefreshToken(1L) }
                verify(exactly = 1) { accessTokenBlacklistStore.blacklist(1L) }
            }

            it("커밋 후 refresh token 삭제가 일시 실패하면 재시도한다") {
                val user = UserTestFixture.createRegisteredUser(1L)
                var attempts = 0
                every { userReader.getByIdWithLock(1L) } returns user
                every { clubMemberRepository.findAllActiveByUserIdWithLock(1L) } returns emptyList()
                every { jwtManageUseCase.deleteRefreshToken(1L) } answers {
                    attempts++
                    if (attempts < 3) throw RuntimeException("temporary redis failure")
                }
                justRun { accessTokenBlacklistStore.blacklist(1L) }
                TransactionSynchronizationManager.initSynchronization()

                useCase.execute(1L)

                TransactionSynchronizationManager.getSynchronizations().forEach { it.afterCommit() }

                attempts shouldBe 3
                verify(exactly = 3) { jwtManageUseCase.deleteRefreshToken(1L) }
            }

            it("커밋 후 access token blacklist 등록이 일시 실패하면 재시도한다") {
                val user = UserTestFixture.createRegisteredUser(1L)
                var attempts = 0
                every { userReader.getByIdWithLock(1L) } returns user
                every { clubMemberRepository.findAllActiveByUserIdWithLock(1L) } returns emptyList()
                justRun { jwtManageUseCase.deleteRefreshToken(1L) }
                every { accessTokenBlacklistStore.blacklist(1L) } answers {
                    attempts++
                    if (attempts < 3) throw RuntimeException("temporary redis failure")
                }
                TransactionSynchronizationManager.initSynchronization()

                useCase.execute(1L)

                TransactionSynchronizationManager.getSynchronizations().forEach { it.afterCommit() }

                attempts shouldBe 3
                verify(exactly = 3) { accessTokenBlacklistStore.blacklist(1L) }
            }

            it("커밋 후 토큰 폐기가 모두 실패하면 실패 metric을 기록한다") {
                val user = UserTestFixture.createRegisteredUser(1L)
                every { userReader.getByIdWithLock(1L) } returns user
                every { clubMemberRepository.findAllActiveByUserIdWithLock(1L) } returns emptyList()
                every { jwtManageUseCase.deleteRefreshToken(1L) } throws RuntimeException("redis down")
                every { accessTokenBlacklistStore.blacklist(1L) } throws RuntimeException("redis down")
                TransactionSynchronizationManager.initSynchronization()

                useCase.execute(1L)

                TransactionSynchronizationManager.getSynchronizations().forEach { it.afterCommit() }

                meterRegistry
                    .counter(
                        "user.leave.token_revoke.failure",
                        "action",
                        "refresh_token_delete",
                    ).count() shouldBe 1.0
                meterRegistry
                    .counter(
                        "user.leave.token_revoke.failure",
                        "action",
                        "access_token_blacklist",
                    ).count() shouldBe 1.0
            }

            it("USER와 ADMIN ACTIVE 멤버십을 모두 탈퇴 처리한다") {
                val user = UserTestFixture.createRegisteredUser(1L)
                val userMember =
                    ClubMemberTestFixture.createActiveMember(
                        id = 10L,
                        user = user,
                        memberRole = MemberRole.USER,
                    )
                val adminMember =
                    ClubMemberTestFixture.createActiveMember(
                        id = 11L,
                        user = user,
                        memberRole = MemberRole.ADMIN,
                    )
                val now = LocalDateTime.now(clock)
                every { userReader.getByIdWithLock(1L) } returns user
                every { clubMemberRepository.findAllActiveByUserIdWithLock(1L) } returns listOf(userMember, adminMember)
                justRun { clubActivityDeletionPolicy.markMembersActivitiesDeleted(any(), any()) }
                justRun { jwtManageUseCase.deleteRefreshToken(1L) }
                justRun { accessTokenBlacklistStore.blacklist(1L) }
                TransactionSynchronizationManager.initSynchronization()

                useCase.execute(1L)

                userMember.memberStatus shouldBe MemberStatus.LEFT
                userMember.leftAt shouldBe now
                adminMember.memberStatus shouldBe MemberStatus.LEFT
                adminMember.leftAt shouldBe now
                user.status shouldBe Status.LEFT
                verify(exactly = 1) {
                    clubActivityDeletionPolicy.markMembersActivitiesDeleted(listOf(userMember, adminMember), now)
                }
                verify(exactly = 0) { clubActivityDeletionPolicy.markMemberActivitiesDeleted(any(), any()) }
            }

            it("위드 탈퇴 시 멤버 프로필 파일을 하드 딜리트한다") {
                val user = UserTestFixture.createRegisteredUser(1L)
                every { userReader.getByIdWithLock(1L) } returns user
                every { clubMemberRepository.findAllActiveByUserIdWithLock(1L) } returns emptyList()
                justRun { jwtManageUseCase.deleteRefreshToken(1L) }
                justRun { accessTokenBlacklistStore.blacklist(1L) }
                TransactionSynchronizationManager.initSynchronization()

                useCase.execute(1L)

                verify(exactly = 1) {
                    fileRepository.hardDeleteActiveByOwnerTypeAndOwnerId(FileOwnerType.CLUB_MEMBER_PROFILE, 1L)
                }
            }

            it("위드 탈퇴 시 멀티프로필 이미지와 헤더 파일도 하드 딜리트한다") {
                val user = UserTestFixture.createRegisteredUser(1L)
                val profile1 = UserProfile.create(user = user, name = "프로필1")
                val profile2 = UserProfile.create(user = user, name = "프로필2")
                org.springframework.test.util.ReflectionTestUtils
                    .setField(profile1, "id", 10L)
                org.springframework.test.util.ReflectionTestUtils
                    .setField(profile2, "id", 11L)
                every { userReader.getByIdWithLock(1L) } returns user
                every { clubMemberRepository.findAllActiveByUserIdWithLock(1L) } returns emptyList()
                every { userProfileRepository.findAllByUserIdOrderByIdAsc(1L) } returns listOf(profile1, profile2)
                justRun { jwtManageUseCase.deleteRefreshToken(1L) }
                justRun { accessTokenBlacklistStore.blacklist(1L) }
                TransactionSynchronizationManager.initSynchronization()

                useCase.execute(1L)

                verify(exactly = 1) {
                    fileRepository.hardDeleteActiveByOwnerTypeAndOwnerIdIn(
                        FileOwnerType.USER_PROFILE_IMAGE,
                        listOf(10L, 11L),
                    )
                }
                verify(exactly = 1) {
                    fileRepository.hardDeleteActiveByOwnerTypeAndOwnerIdIn(
                        FileOwnerType.USER_PROFILE_HEADER,
                        listOf(10L, 11L),
                    )
                }
                verify(exactly = 0) {
                    fileRepository.hardDeleteActiveByOwnerTypeAndOwnerId(FileOwnerType.USER_PROFILE_IMAGE, any())
                }
                verify(exactly = 0) {
                    fileRepository.hardDeleteActiveByOwnerTypeAndOwnerId(FileOwnerType.USER_PROFILE_HEADER, any())
                }
            }

            it("ACTIVE LEAD 멤버십이 있으면 탈퇴를 차단하고 상태를 변경하지 않는다") {
                val user = UserTestFixture.createRegisteredUser(1L)
                val leadMember =
                    ClubMemberTestFixture.createActiveMember(
                        id = 10L,
                        user = user,
                        memberRole = MemberRole.LEAD,
                    )
                every { userReader.getByIdWithLock(1L) } returns user
                every { clubMemberRepository.findAllActiveByUserIdWithLock(1L) } returns listOf(leadMember)

                shouldThrow<UserHasLeadClubException> {
                    useCase.execute(1L)
                }

                user.status shouldBe Status.ACTIVE
                leadMember.memberStatus shouldBe MemberStatus.ACTIVE
                verify(exactly = 0) { clubActivityDeletionPolicy.markMemberActivitiesDeleted(any(), any()) }
                verify(
                    exactly = 0,
                ) { fileRepository.hardDeleteActiveByOwnerTypeAndOwnerId(any(), any()) }
                verify(exactly = 0) { jwtManageUseCase.deleteRefreshToken(any()) }
            }
        }
    })
