package com.weeth.domain.board.domain.repository

import com.weeth.config.TestContainersConfig
import com.weeth.domain.board.fixture.NoticeTestFixture
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort

@DataJpaTest
@Import(TestContainersConfig::class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class NoticeRepositoryTest(
    private val noticeRepository: NoticeRepository,
) : DescribeSpec({

        describe("findPageBy") {
            it("공지 id 내림차순으로 조회") {
                val notices =
                    (0 until 5).map { i ->
                        NoticeTestFixture.createNotice(title = "공지$i")
                    }
                noticeRepository.saveAll(notices)

                val pageable = PageRequest.of(0, 3, Sort.by(Sort.Direction.DESC, "id"))

                val pagedNotices = noticeRepository.findPageBy(pageable)

                pagedNotices.size shouldBe 3
                pagedNotices.map { it.title } shouldContainExactly
                    listOf(notices[4].title, notices[3].title, notices[2].title)
                pagedNotices.hasNext().shouldBeTrue()
            }
        }

        describe("search") {
            it("검색어가 포함된 공지를 id 내림차순으로 조회") {
                val notices =
                    (0 until 6).map { i ->
                        if (i % 2 == 0) {
                            NoticeTestFixture.createNotice(title = "공지$i")
                        } else {
                            NoticeTestFixture.createNotice(title = "검색$i")
                        }
                    }
                noticeRepository.saveAll(notices)

                val pageable = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "id"))

                val searchedNotices = noticeRepository.search("검색", pageable)

                searchedNotices.content shouldHaveSize 3
                searchedNotices.content.map { it.title } shouldContainExactly
                    listOf(notices[5].title, notices[3].title, notices[1].title)
                searchedNotices.hasNext().shouldBeFalse()
            }
        }
    })
