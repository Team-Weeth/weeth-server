package com.weeth.domain.account.application.usecase.command

import com.weeth.domain.account.domain.entity.AccountSetting
import com.weeth.domain.account.domain.repository.AccountSettingRepository
import com.weeth.domain.club.domain.service.ClubPermissionPolicy
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify

class ManageAccountUseCaseTest :
    DescribeSpec({
        val accountSettingRepository = mockk<AccountSettingRepository>(relaxed = true)
        val clubPermissionPolicy = mockk<ClubPermissionPolicy>(relaxed = true)
        val useCase =
            ManageAccountUseCase(
                accountSettingRepository = accountSettingRepository,
                clubPermissionPolicy = clubPermissionPolicy,
            )

        val clubId = 1L
        val userId = 100L

        beforeTest {
            clearMocks(accountSettingRepository, clubPermissionPolicy)
        }

        describe("updateMemberVisibility") {
            it("어드민 권한을 확인하고 club 단위 공개 여부를 저장한다") {
                every { accountSettingRepository.findByClubId(clubId) } returns null
                val saved = slot<AccountSetting>()
                every { accountSettingRepository.save(capture(saved)) } answers { firstArg() }

                useCase.updateMemberVisibility(clubId = clubId, visible = true, userId = userId)

                verify { clubPermissionPolicy.requireAdmin(clubId, userId) }
                saved.captured.clubId shouldBe clubId
                saved.captured.memberVisible shouldBe true
            }

            it("기존 설정이 있으면 해당 설정을 갱신한다") {
                val existing = AccountSetting.createDefault(clubId).apply { showToMembers() }
                every { accountSettingRepository.findByClubId(clubId) } returns existing
                val saved = slot<AccountSetting>()
                every { accountSettingRepository.save(capture(saved)) } answers { firstArg() }

                useCase.updateMemberVisibility(clubId = clubId, visible = false, userId = userId)

                saved.captured shouldBe existing
                existing.memberVisible shouldBe false
            }
        }
    })
