package com.weeth.domain.board.domain.repository

import com.weeth.config.TestContainersConfig
import com.weeth.domain.board.fixture.BoardTestFixture
import com.weeth.domain.club.domain.repository.ClubMemberRepository
import com.weeth.domain.club.domain.repository.ClubRepository
import com.weeth.domain.club.fixture.ClubTestFixture
import com.weeth.domain.user.domain.repository.UserRepository
import com.weeth.domain.user.fixture.UserTestFixture
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import java.time.LocalDateTime

@DataJpaTest
@Import(TestContainersConfig::class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class LastNoticeReadRepositoryTest(
    private val lastNoticeReadRepository: LastNoticeReadRepository,
    private val boardRepository: BoardRepository,
    private val clubRepository: ClubRepository,
    private val clubMemberRepository: ClubMemberRepository,
    private val userRepository: UserRepository,
) : StringSpec({

        "markRead는 최초 호출 시 읽음 기록을 생성하고 재호출 시 lastReadAt을 갱신한다" {
            val user = userRepository.save(UserTestFixture.createActiveUser1())
            val club = clubRepository.save(ClubTestFixture.createClub())
            val clubMember = clubMemberRepository.save(ClubTestFixture.createClubMember(club = club, user = user))
            val board = boardRepository.save(BoardTestFixture.createNoticeBoard(club = club))
            val firstReadAt = LocalDateTime.of(2026, 5, 7, 10, 0)
            val secondReadAt = LocalDateTime.of(2026, 5, 7, 10, 5)

            lastNoticeReadRepository.markRead(clubMember.id, board.id, firstReadAt)
            lastNoticeReadRepository.markRead(clubMember.id, board.id, secondReadAt)

            val result = lastNoticeReadRepository.findByClubMemberIdAndBoardId(clubMember.id, board.id)

            result.shouldNotBeNull()
            result.lastReadAt shouldBe secondReadAt
            lastNoticeReadRepository.findAll() shouldHaveSize 1
        }
    })
