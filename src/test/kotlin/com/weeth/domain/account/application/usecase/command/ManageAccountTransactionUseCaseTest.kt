package com.weeth.domain.account.application.usecase.command

import com.weeth.domain.account.application.dto.request.SaveAccountTransactionRequest
import com.weeth.domain.account.application.dto.request.UpdateAccountTransactionRequest
import com.weeth.domain.account.application.exception.AccountNotActiveException
import com.weeth.domain.account.application.exception.AccountNotFoundException
import com.weeth.domain.account.application.exception.AccountTransactionNotFoundException
import com.weeth.domain.account.application.exception.AccountTransactionTypeNotAllowedException
import com.weeth.domain.account.application.mapper.AccountTransactionMapper
import com.weeth.domain.account.domain.entity.Account
import com.weeth.domain.account.domain.entity.AccountTransaction
import com.weeth.domain.account.domain.enums.AccountStatus
import com.weeth.domain.account.domain.enums.AccountTransactionType
import com.weeth.domain.account.domain.repository.AccountRepository
import com.weeth.domain.account.domain.repository.AccountTransactionRepository
import com.weeth.domain.account.domain.vo.Money
import com.weeth.domain.account.fixture.AccountTestFixture
import com.weeth.domain.club.domain.service.ClubPermissionPolicy
import com.weeth.domain.club.fixture.ClubMemberTestFixture
import com.weeth.domain.file.application.dto.request.FileSaveRequest
import com.weeth.domain.file.application.dto.response.FileResponse
import com.weeth.domain.file.application.mapper.FileMapper
import com.weeth.domain.file.domain.entity.File
import com.weeth.domain.file.domain.enums.FileOwnerType
import com.weeth.domain.file.domain.enums.FileStatus
import com.weeth.domain.file.domain.repository.FileReader
import com.weeth.domain.file.domain.repository.FileRepository
import com.weeth.domain.file.domain.vo.StorageKey
import com.weeth.domain.file.fixture.FileTestFixture
import com.weeth.domain.user.fixture.UserTestFixture
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDateTime

class ManageAccountTransactionUseCaseTest :
    DescribeSpec({
        val accountRepository = mockk<AccountRepository>()
        val transactionRepository = mockk<AccountTransactionRepository>(relaxed = true)
        val clubPermissionPolicy = mockk<ClubPermissionPolicy>(relaxed = true)
        val fileRepository = mockk<FileRepository>()
        val fileReader = mockk<FileReader>()
        val fileMapper = mockk<FileMapper>()
        val useCase =
            ManageAccountTransactionUseCase(
                accountRepository,
                transactionRepository,
                clubPermissionPolicy,
                fileRepository,
                fileReader,
                fileMapper,
                AccountTransactionMapper(),
            )

        val userId = 10L
        val accountId = 1L
        val transactionId = 100L
        val date = LocalDateTime.of(2026, 7, 20, 0, 0)
        val adminMember = ClubMemberTestFixture.createAdminMember(user = UserTestFixture.createAdmin(id = userId))
        val receiptRequest =
            FileSaveRequest(
                fileName = "receipt.png",
                storageKey = "ACCOUNT_TRANSACTION/2026-07/550e8400-e29b-41d4-a716-446655440000_receipt.png",
                fileSize = 10_240,
                contentType = "image/png",
            )
        val receiptResponse =
            FileResponse(
                fileId = 200L,
                fileName = "receipt.png",
                fileUrl = "https://cdn.weeth/receipt.png",
                storageKey = receiptRequest.storageKey,
                fileSize = receiptRequest.fileSize,
                contentType = receiptRequest.contentType,
                status = FileStatus.UPLOADED,
            )

        fun receiptFile(
            id: Long = 200L,
            ownerId: Long = transactionId,
            fileName: String = "receipt.png",
        ): File {
            val uuid =
                when (id) {
                    199L -> "550e8400-e29b-41d4-a716-446655440199"
                    else -> "550e8400-e29b-41d4-a716-446655440200"
                }
            return FileTestFixture.createFile(
                id = id,
                fileName = fileName,
                storageKey = StorageKey("ACCOUNT_TRANSACTION/2026-07/${uuid}_$fileName"),
                ownerType = FileOwnerType.ACCOUNT_TRANSACTION,
                ownerId = ownerId,
            )
        }

        beforeTest {
            clearMocks(
                accountRepository,
                transactionRepository,
                clubPermissionPolicy,
                fileRepository,
                fileReader,
                fileMapper,
            )
            every { transactionRepository.save(any()) } answers { firstArg() }
            every { fileMapper.toFileList(any(), any(), any()) } returns emptyList()
            every { fileMapper.toFileResponse(any()) } returns receiptResponse
            every { fileRepository.saveAll(any<List<File>>()) } answers { firstArg() }
            every { fileRepository.hardDeleteActiveByOwnerTypeAndOwnerId(any(), any()) } returns 0
            every { fileReader.findAll(any(), any<Long>(), any()) } returns emptyList()
            every { clubPermissionPolicy.requireAdmin(any(), userId) } returns adminMember
        }

        fun appliedTransaction(
            account: Account,
            type: AccountTransactionType,
            amount: Int,
        ): AccountTransaction =
            AccountTransaction
                .create(
                    account = account,
                    type = type,
                    title = "기존 거래",
                    source = null,
                    amount = Money.of(amount),
                    transactedAt = date,
                ).also { account.applyTransaction(it) }

        describe("save") {
            it("지출 거래를 생성하고 잔액을 차감하며 수정자를 기록한다") {
                val account = AccountTestFixture.createAccount(id = accountId, currentBalance = 100_000)
                every { accountRepository.findByIdWithLock(accountId) } returns account
                val request =
                    SaveAccountTransactionRequest(AccountTransactionType.EXPENSE, 30_000, "스터디 지원금", "인프런", date, null)

                val response = useCase.save(account.club.id, accountId, request, userId)

                response.amount shouldBe 30_000
                response.type shouldBe AccountTransactionType.EXPENSE
                account.currentBalance shouldBe 70_000
                account.lastModifiedBy shouldBe userId
                verify(exactly = 1) { transactionRepository.save(match { it.registeredByName == "적순" }) }
            }

            it("영수증 파일이 있으면 거래 ID를 ownerId로 파일 메타데이터를 저장하고 응답에 포함한다") {
                val account = AccountTestFixture.createAccount(id = accountId, currentBalance = 100_000)
                val savedReceipt = receiptFile()
                every { accountRepository.findByIdWithLock(accountId) } returns account
                every { transactionRepository.save(any()) } answers {
                    firstArg<AccountTransaction>().also {
                        ReflectionTestUtils.setField(it, "id", transactionId)
                    }
                }
                every {
                    fileMapper.toFileList(listOf(receiptRequest), FileOwnerType.ACCOUNT_TRANSACTION, transactionId)
                } returns listOf(savedReceipt)
                every { fileRepository.saveAll(listOf(savedReceipt)) } returns listOf(savedReceipt)
                val request =
                    SaveAccountTransactionRequest(
                        type = AccountTransactionType.EXPENSE,
                        amount = 30_000,
                        title = "스터디 지원금",
                        source = "인프런",
                        transactedAt = date,
                        memo = null,
                        files = listOf(receiptRequest),
                    )

                val response = useCase.save(account.club.id, accountId, request, userId)

                response.hasReceipt shouldBe true
                response.receipts shouldBe listOf(receiptResponse)
                verify(exactly = 1) {
                    fileMapper.toFileList(listOf(receiptRequest), FileOwnerType.ACCOUNT_TRANSACTION, transactionId)
                }
                verify(exactly = 1) { fileRepository.saveAll(listOf(savedReceipt)) }
            }

            it("시스템 타입(DUES) 생성 요청은 거부한다") {
                val account = AccountTestFixture.createAccount(id = accountId)
                every { accountRepository.findByIdWithLock(accountId) } returns account
                val request = SaveAccountTransactionRequest(AccountTransactionType.DUES, 10_000, "회비", "출처", date, null)

                shouldThrow<AccountTransactionTypeNotAllowedException> {
                    useCase.save(account.club.id, accountId, request, userId)
                }
            }

            it("초안 상태 장부면 운영을 거부한다") {
                val account = AccountTestFixture.createAccount(id = accountId, status = AccountStatus.DRAFT)
                every { accountRepository.findByIdWithLock(accountId) } returns account
                val request =
                    SaveAccountTransactionRequest(AccountTransactionType.EXPENSE, 1_000, "지출", "출처", date, null)

                shouldThrow<AccountNotActiveException> { useCase.save(account.club.id, accountId, request, userId) }
            }

            it("존재하지 않는 장부면 NotFound를 던진다") {
                every { accountRepository.findByIdWithLock(accountId) } returns null
                val request =
                    SaveAccountTransactionRequest(AccountTransactionType.EXPENSE, 1_000, "지출", "출처", date, null)

                shouldThrow<AccountNotFoundException> { useCase.save(1L, accountId, request, userId) }
            }
        }

        describe("update") {
            it("금액 수정 시 잔액을 재계산한다") {
                val account = AccountTestFixture.createAccount(id = accountId, currentBalance = 100_000)
                val transaction = appliedTransaction(account, AccountTransactionType.EXPENSE, 30_000)
                account.currentBalance shouldBe 70_000
                every { accountRepository.findByIdWithLock(accountId) } returns account
                every { transactionRepository.findByIdAndDeletedAtIsNull(transactionId) } returns transaction
                val request =
                    UpdateAccountTransactionRequest(
                        AccountTransactionType.EXPENSE,
                        50_000,
                        "수정",
                        null,
                        date.toLocalDate(),
                        null,
                    )

                val response = useCase.update(account.club.id, accountId, transactionId, request, userId)

                response.amount shouldBe 50_000
                account.currentBalance shouldBe 50_000
            }

            it("일부 필드만 보낸 PATCH 는 나머지 기존 값을 유지한다") {
                val account = AccountTestFixture.createAccount(id = accountId, currentBalance = 100_000)
                val transaction = appliedTransaction(account, AccountTransactionType.EXPENSE, 30_000)
                every { accountRepository.findByIdWithLock(accountId) } returns account
                every { transactionRepository.findByIdAndDeletedAtIsNull(transactionId) } returns transaction
                // title 만 변경, 나머지는 null=변경 안 함
                val request = UpdateAccountTransactionRequest(title = "제목만 수정")

                val response = useCase.update(account.club.id, accountId, transactionId, request, userId)

                response.title shouldBe "제목만 수정"
                response.type shouldBe AccountTransactionType.EXPENSE
                response.amount shouldBe 30_000
                account.currentBalance shouldBe 70_000
            }

            it("files가 null이면 기존 영수증을 유지하고 응답에 포함한다") {
                val account = AccountTestFixture.createAccount(id = accountId, currentBalance = 100_000)
                val transaction = appliedTransaction(account, AccountTransactionType.EXPENSE, 30_000)
                ReflectionTestUtils.setField(transaction, "id", transactionId)
                val oldReceipt = receiptFile(ownerId = transactionId)
                every { accountRepository.findByIdWithLock(accountId) } returns account
                every { transactionRepository.findByIdAndDeletedAtIsNull(transactionId) } returns transaction
                every { fileReader.findAll(FileOwnerType.ACCOUNT_TRANSACTION, transactionId, any()) } returns
                    listOf(oldReceipt)
                val request = UpdateAccountTransactionRequest(title = "제목만 수정", files = null)

                val response = useCase.update(account.club.id, accountId, transactionId, request, userId)

                response.hasReceipt shouldBe true
                response.receipts shouldBe listOf(receiptResponse)
                verify(exactly = 0) { fileRepository.hardDeleteActiveByOwnerTypeAndOwnerId(any(), any()) }
                verify(exactly = 0) { fileRepository.saveAll(any<List<File>>()) }
            }

            it("files가 빈 배열이면 기존 영수증을 삭제한다") {
                val account = AccountTestFixture.createAccount(id = accountId, currentBalance = 100_000)
                val transaction = appliedTransaction(account, AccountTransactionType.EXPENSE, 30_000)
                ReflectionTestUtils.setField(transaction, "id", transactionId)
                every { accountRepository.findByIdWithLock(accountId) } returns account
                every { transactionRepository.findByIdAndDeletedAtIsNull(transactionId) } returns transaction
                val request = UpdateAccountTransactionRequest(files = emptyList())

                val response = useCase.update(account.club.id, accountId, transactionId, request, userId)

                response.hasReceipt shouldBe false
                response.receipts shouldBe emptyList()
                verify(exactly = 1) {
                    fileRepository.hardDeleteActiveByOwnerTypeAndOwnerId(
                        FileOwnerType.ACCOUNT_TRANSACTION,
                        transactionId,
                    )
                }
                verify(exactly = 0) { fileRepository.saveAll(any<List<File>>()) }
            }

            it("files가 있으면 기존 영수증을 삭제하고 새 영수증으로 교체한다") {
                val account = AccountTestFixture.createAccount(id = accountId, currentBalance = 100_000)
                val transaction = appliedTransaction(account, AccountTransactionType.EXPENSE, 30_000)
                ReflectionTestUtils.setField(transaction, "id", transactionId)
                val newReceipt = receiptFile(id = 200L, ownerId = transactionId, fileName = "receipt.png")
                every { accountRepository.findByIdWithLock(accountId) } returns account
                every { transactionRepository.findByIdAndDeletedAtIsNull(transactionId) } returns transaction
                every {
                    fileMapper.toFileList(listOf(receiptRequest), FileOwnerType.ACCOUNT_TRANSACTION, transactionId)
                } returns listOf(newReceipt)
                every { fileRepository.saveAll(listOf(newReceipt)) } returns listOf(newReceipt)
                val request = UpdateAccountTransactionRequest(files = listOf(receiptRequest))

                val response = useCase.update(account.club.id, accountId, transactionId, request, userId)

                response.hasReceipt shouldBe true
                response.receipts shouldBe listOf(receiptResponse)
                verify(exactly = 1) {
                    fileRepository.hardDeleteActiveByOwnerTypeAndOwnerId(
                        FileOwnerType.ACCOUNT_TRANSACTION,
                        transactionId,
                    )
                }
                verify(exactly = 1) { fileRepository.saveAll(listOf(newReceipt)) }
            }

            it("시스템 거래(DUES) 수정 요청은 거부한다") {
                val account = AccountTestFixture.createAccount(id = accountId)
                val transaction = appliedTransaction(account, AccountTransactionType.DUES, 10_000)
                every { accountRepository.findByIdWithLock(accountId) } returns account
                every { transactionRepository.findByIdAndDeletedAtIsNull(transactionId) } returns transaction
                val request =
                    UpdateAccountTransactionRequest(
                        AccountTransactionType.EXPENSE,
                        1_000,
                        "수정",
                        null,
                        date.toLocalDate(),
                        null,
                    )

                shouldThrow<AccountTransactionTypeNotAllowedException> {
                    useCase.update(account.club.id, accountId, transactionId, request, userId)
                }
            }

            it("존재하지 않는 거래면 NotFound를 던진다") {
                val account = AccountTestFixture.createAccount(id = accountId)
                every { accountRepository.findByIdWithLock(accountId) } returns account
                every { transactionRepository.findByIdAndDeletedAtIsNull(transactionId) } returns null
                val request =
                    UpdateAccountTransactionRequest(
                        AccountTransactionType.EXPENSE,
                        1_000,
                        "수정",
                        null,
                        date.toLocalDate(),
                        null,
                    )

                shouldThrow<AccountTransactionNotFoundException> {
                    useCase.update(account.club.id, accountId, transactionId, request, userId)
                }
            }
        }

        describe("delete") {
            it("삭제 시 잔액을 원복하고 소프트 삭제한다") {
                val account = AccountTestFixture.createAccount(id = accountId, currentBalance = 100_000)
                val transaction = appliedTransaction(account, AccountTransactionType.EXPENSE, 30_000)
                account.currentBalance shouldBe 70_000
                every { accountRepository.findByIdWithLock(accountId) } returns account
                every { transactionRepository.findByIdAndDeletedAtIsNull(transactionId) } returns transaction

                useCase.delete(account.club.id, accountId, transactionId, userId)

                account.currentBalance shouldBe 100_000
                transaction.deletedAt.shouldNotBeNull()
                verify(exactly = 0) { fileRepository.hardDeleteActiveByOwnerTypeAndOwnerId(any(), any()) }
            }

            it("시스템 거래(CARRY_OVER) 삭제 요청은 거부한다") {
                val account = AccountTestFixture.createAccount(id = accountId)
                val transaction = appliedTransaction(account, AccountTransactionType.CARRY_OVER, 10_000)
                every { accountRepository.findByIdWithLock(accountId) } returns account
                every { transactionRepository.findByIdAndDeletedAtIsNull(transactionId) } returns transaction

                shouldThrow<AccountTransactionTypeNotAllowedException> {
                    useCase.delete(account.club.id, accountId, transactionId, userId)
                }
            }
        }
    })
