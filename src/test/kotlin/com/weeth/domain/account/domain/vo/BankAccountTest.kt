package com.weeth.domain.account.domain.vo

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class BankAccountTest :
    StringSpec({
        "of는 계좌 정보를 trim해서 보관한다" {
            val bankAccount =
                BankAccount.of(
                    bankName = " 국민은행 ",
                    accountNumber = " 123-456 ",
                    holder = " 가천대 검도부 ",
                    guide = " 회비 입금 ",
                )

            bankAccount.bankName shouldBe "국민은행"
            bankAccount.accountNumber shouldBe "123-456"
            bankAccount.holder shouldBe "가천대 검도부"
            bankAccount.guide shouldBe "회비 입금"
        }

        "생성자는 계좌 정보를 trim해서 보관한다" {
            val bankAccount =
                BankAccount(
                    bankName = " 국민은행 ",
                    accountNumber = " 123-456 ",
                    holder = " 가천대 검도부 ",
                    guide = " 회비 입금 ",
                )

            bankAccount.bankName shouldBe "국민은행"
            bankAccount.accountNumber shouldBe "123-456"
            bankAccount.holder shouldBe "가천대 검도부"
            bankAccount.guide shouldBe "회비 입금"
        }

        "은행명은 비어 있을 수 없다" {
            shouldThrow<IllegalArgumentException> {
                BankAccount.of(
                    bankName = " ",
                    accountNumber = "123-456",
                    holder = "가천대 검도부",
                )
            }
        }
    })
