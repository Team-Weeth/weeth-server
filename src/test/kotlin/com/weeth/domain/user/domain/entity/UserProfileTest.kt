package com.weeth.domain.user.domain.entity

import com.weeth.domain.user.fixture.UserTestFixture
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class UserProfileTest :
    StringSpec({
        "create는 이름을 trim하고 공개 프로필 정보를 저장한다" {
            val user = UserTestFixture.createActiveUser1()

            val profile =
                UserProfile.create(
                    user = user,
                    name = "  길동  ",
                    profileImageStorageKey = "USER_PROFILE_IMAGE/2026-07/profile.png",
                    headerImageStorageKey = "USER_PROFILE_HEADER/2026-07/header.png",
                    bio = "  안녕하세요  ",
                )

            profile.user shouldBe user
            profile.name shouldBe "길동"
            profile.profileImageStorageKey shouldBe "USER_PROFILE_IMAGE/2026-07/profile.png"
            profile.headerImageStorageKey shouldBe "USER_PROFILE_HEADER/2026-07/header.png"
            profile.bio shouldBe "안녕하세요"
        }

        "create는 빈 이름을 허용하지 않는다" {
            val user = UserTestFixture.createActiveUser1()

            shouldThrow<IllegalArgumentException> {
                UserProfile.create(user = user, name = "   ")
            }
        }

        "create는 20자를 초과하는 이름을 허용하지 않는다" {
            val user = UserTestFixture.createActiveUser1()

            shouldThrow<IllegalArgumentException> {
                UserProfile.create(user = user, name = "가".repeat(21))
            }
        }

        "update는 null이 아닌 필드만 변경한다" {
            val user = UserTestFixture.createActiveUser1()
            val profile =
                UserProfile.create(
                    user = user,
                    name = "길동",
                    profileImageStorageKey = "old-profile",
                    headerImageStorageKey = "old-header",
                    bio = "기존 소개",
                )

            profile.update(
                name = "새 이름",
                profileImageStorageKey = null,
                headerImageStorageKey = "new-header",
                bio = "새 소개",
            )

            profile.name shouldBe "새 이름"
            profile.profileImageStorageKey shouldBe "old-profile"
            profile.headerImageStorageKey shouldBe "new-header"
            profile.bio shouldBe "새 소개"
        }

        "이미지는 명시적 삭제 메서드로 제거한다" {
            val user = UserTestFixture.createActiveUser1()
            val profile =
                UserProfile.create(
                    user = user,
                    name = "길동",
                    profileImageStorageKey = "profile",
                    headerImageStorageKey = "header",
                )

            profile.removeProfileImage()
            profile.removeHeaderImage()

            profile.profileImageStorageKey shouldBe null
            profile.headerImageStorageKey shouldBe null
        }
    })
