package com.weeth.domain.account.application.dto.request

import com.weeth.domain.account.domain.enums.AccountTransactionType
import com.weeth.domain.file.application.dto.request.FileSaveRequest
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.booleans.shouldBeTrue
import jakarta.validation.Validation
import java.time.LocalDate

class AccountTransactionRequestValidationTest :
    StringSpec({
        val validator = Validation.buildDefaultValidatorFactory().validator
        val receipt1 =
            FileSaveRequest(
                fileName = "receipt-1.png",
                storageKey = "ACCOUNT_TRANSACTION/2026-07/550e8400-e29b-41d4-a716-446655440001_receipt-1.png",
                fileSize = 1024,
                contentType = "image/png",
            )
        val receipt2 =
            FileSaveRequest(
                fileName = "receipt-2.png",
                storageKey = "ACCOUNT_TRANSACTION/2026-07/550e8400-e29b-41d4-a716-446655440002_receipt-2.png",
                fileSize = 1024,
                contentType = "image/png",
            )

        "거래 생성 요청의 영수증 파일은 최대 1개만 허용한다" {
            val request =
                SaveAccountTransactionRequest(
                    type = AccountTransactionType.EXPENSE,
                    amount = 10_000,
                    title = "스터디 지원금",
                    source = "인프런",
                    transactedAt = LocalDate.of(2026, 7, 20),
                    files = listOf(receipt1, receipt2),
                )

            validator
                .validate(request)
                .any { it.propertyPath.toString() == "files" }
                .shouldBeTrue()
        }

        "거래 수정 요청의 영수증 파일은 최대 1개만 허용한다" {
            val request = UpdateAccountTransactionRequest(files = listOf(receipt1, receipt2))

            validator
                .validate(request)
                .any { it.propertyPath.toString() == "files" }
                .shouldBeTrue()
        }
    })
