package com.weeth.domain.user.application.mapper

import com.weeth.domain.file.domain.port.FileAccessUrlPort
import com.weeth.domain.user.application.dto.response.UserProfileClubResponse
import com.weeth.domain.user.domain.entity.UserProfile
import com.weeth.domain.user.fixture.UserTestFixture
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class UserProfileMapperTest :
    StringSpec({
        val fileAccessUrlPort = mockk<FileAccessUrlPort>()
        val mapper = UserProfileMapper(fileAccessUrlPort)

        "toResponse는 이미지 storageKey를 조회 URL로 변환한다" {
            val user = UserTestFixture.createActiveUser1()
            val profile =
                UserProfile.create(
                    user = user,
                    name = "길동",
                    profileImageStorageKey = "profile-key",
                    headerImageStorageKey = "header-key",
                    bio = "안녕하세요",
                )
            every { fileAccessUrlPort.resolve("profile-key") } returns "https://cdn.test/profile.png"
            every { fileAccessUrlPort.resolve("header-key") } returns "https://cdn.test/header.png"

            val response =
                mapper.toResponse(
                    profile = profile,
                    usingClubs = listOf(UserProfileClubResponse(clubId = "1A2b3C", name = "Leets")),
                )

            response.name shouldBe "길동"
            response.profileImageUrl shouldBe "https://cdn.test/profile.png"
            response.headerImageUrl shouldBe "https://cdn.test/header.png"
            response.bio shouldBe "안녕하세요"
            response.usingClubs shouldBe listOf(UserProfileClubResponse(clubId = "1A2b3C", name = "Leets"))
        }
    })
