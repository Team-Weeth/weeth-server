package com.weeth.domain.account.domain.entity

import com.weeth.domain.account.application.dto.ReceiptDTO
import com.weeth.domain.account.fixture.ReceiptTestFixture
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate

class ReceiptTest :
    StringSpec({
        "update는 영수증 필드를 갱신한다" {
            val receipt =
                ReceiptTestFixture.createReceipt(
                    description = "기존 설명",
                    source = "기존 출처",
                    amount = 5_000,
                    date = LocalDate.of(2024, 1, 1),
                )
            val dto = ReceiptDTO.Update("새로운 설명", "새 출처", 20_000, LocalDate.of(2025, 6, 1), 40, emptyList())

            receipt.update(dto)

            receipt.description shouldBe "새로운 설명"
            receipt.source shouldBe "새 출처"
            receipt.amount shouldBe 20_000
            receipt.date shouldBe LocalDate.of(2025, 6, 1)
        }
    })
