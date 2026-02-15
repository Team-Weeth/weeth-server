package com.weeth.domain.user.domain.repository

import com.weeth.config.TestContainersConfig
import com.weeth.domain.user.domain.entity.enums.Status
import com.weeth.domain.user.fixture.CardinalTestFixture
import com.weeth.domain.user.fixture.UserCardinalTestFixture
import com.weeth.domain.user.fixture.UserTestFixture
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageRequest

@DataJpaTest
@Import(TestContainersConfig::class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest(
    private val userRepository: UserRepository,
    private val userCardinalRepository: UserCardinalRepository,
    private val cardinalRepository: CardinalRepository,
) : DescribeSpec({

        lateinit var cardinal7: com.weeth.domain.user.domain.entity.Cardinal
        lateinit var cardinal8: com.weeth.domain.user.domain.entity.Cardinal

        beforeEach {
            cardinal7 = cardinalRepository.save(CardinalTestFixture.createCardinal(cardinalNumber = 7, year = 2026, semester = 1))
            cardinal8 = cardinalRepository.save(CardinalTestFixture.createCardinal(cardinalNumber = 8, year = 2026, semester = 2))

            val user1 = userRepository.save(UserTestFixture.createActiveUser1())
            val user2 = userRepository.save(UserTestFixture.createActiveUser2())
            val user3 = userRepository.save(UserTestFixture.createWaitingUser1())

            user1.accept()
            user2.accept()
            userCardinalRepository.flush()

            userCardinalRepository.save(UserCardinalTestFixture.linkUserCardinal(user1, cardinal7))
            userCardinalRepository.save(UserCardinalTestFixture.linkUserCardinal(user2, cardinal8))
            userCardinalRepository.save(UserCardinalTestFixture.linkUserCardinal(user3, cardinal7))
        }

        describe("findAllByCardinalAndStatus") {
            it("특정 기수 + 상태에 맞는 유저만 조회된다") {
                val result = userRepository.findAllByCardinalAndStatus(cardinal7, Status.ACTIVE)

                result shouldHaveSize 1
                result.map { it.name } shouldContainExactly listOf("적순")
            }
        }

        describe("findAllByStatusOrderedByCardinalAndName") {
            it("상태별로 최신 기수순 + 이름 오름차순으로 정렬된다") {
                val pageable = PageRequest.of(0, 10)

                val resultSlice = userRepository.findAllByStatusOrderedByCardinalAndName(Status.ACTIVE, pageable)
                val result = resultSlice.content

                result shouldHaveSize 2
                result.map { it.name } shouldContainExactly listOf("적순2", "적순")
            }
        }

        describe("findAllByCardinalOrderByNameAsc") {
            it("Active인 유저들 중 특정 기수 + 이름 오름차순으로 정렬한다") {
                val pageable = PageRequest.of(0, 10)

                val resultSlice = userRepository.findAllByCardinalOrderByNameAsc(Status.ACTIVE, cardinal7, pageable)
                val result = resultSlice.content

                result shouldHaveSize 1
                result.map { it.name } shouldContainExactly listOf("적순")
            }
        }
    })
