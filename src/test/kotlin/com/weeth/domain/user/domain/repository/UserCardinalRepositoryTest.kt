package com.weeth.domain.user.domain.repository

import com.weeth.config.TestContainersConfig
import com.weeth.domain.user.domain.entity.UserCardinal
import com.weeth.domain.user.fixture.CardinalTestFixture
import com.weeth.domain.user.fixture.UserTestFixture
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import

@DataJpaTest
@Import(TestContainersConfig::class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserCardinalRepositoryTest(
    private val userRepository: UserRepository,
    private val cardinalRepository: CardinalRepository,
    private val userCardinalRepository: UserCardinalRepository,
) : DescribeSpec({

        describe("findAllByUserOrderByCardinalCardinalNumberDesc") {
            it("유저별 기수가 내림차순으로 조회된다") {
                val user = UserTestFixture.createActiveUser1()
                userRepository.save(user)

                val cardinal1 = cardinalRepository.save(CardinalTestFixture.createCardinal(cardinalNumber = 5, year = 2023, semester = 1))
                val cardinal2 = cardinalRepository.save(CardinalTestFixture.createCardinal(cardinalNumber = 6, year = 2023, semester = 2))
                val cardinal3 = cardinalRepository.save(CardinalTestFixture.createCardinal(cardinalNumber = 7, year = 2024, semester = 1))

                userCardinalRepository.saveAll(
                    listOf(
                        UserCardinal(user, cardinal1),
                        UserCardinal(user, cardinal2),
                        UserCardinal(user, cardinal3),
                    ),
                )

                val result = userCardinalRepository.findAllByUserOrderByCardinalCardinalNumberDesc(user)

                result shouldHaveSize 3
                result[0].cardinal.cardinalNumber shouldBe 7
                result[1].cardinal.cardinalNumber shouldBe 6
                result[2].cardinal.cardinalNumber shouldBe 5
            }
        }

        describe("findAllByUsers") {
            it("여러 유저의 기수를 유저별 내림차순으로 조회한다") {
                val user1 = UserTestFixture.createActiveUser1()
                val user2 = UserTestFixture.createActiveUser2()
                userRepository.save(user1)
                userRepository.save(user2)

                val c1 = cardinalRepository.save(CardinalTestFixture.createCardinal(cardinalNumber = 5, year = 2023, semester = 1))
                val c2 = cardinalRepository.save(CardinalTestFixture.createCardinal(cardinalNumber = 6, year = 2023, semester = 2))
                val c3 = cardinalRepository.save(CardinalTestFixture.createCardinal(cardinalNumber = 7, year = 2024, semester = 1))
                val c4 = cardinalRepository.save(CardinalTestFixture.createCardinal(cardinalNumber = 8, year = 2024, semester = 2))

                userCardinalRepository.saveAll(
                    listOf(
                        UserCardinal(user1, c3),
                        UserCardinal(user1, c2),
                    ),
                )
                userCardinalRepository.saveAll(
                    listOf(
                        UserCardinal(user2, c4),
                        UserCardinal(user2, c1),
                    ),
                )

                val result = userCardinalRepository.findAllByUsers(listOf(user1, user2))

                result shouldHaveSize 4
                result[0].user.id shouldBe user1.id
                result[0].cardinal.cardinalNumber shouldBe 7
                result[1].cardinal.cardinalNumber shouldBe 6

                result[2].user.id shouldBe user2.id
                result[2].cardinal.cardinalNumber shouldBe 8
                result[3].cardinal.cardinalNumber shouldBe 5
            }
        }
    })
