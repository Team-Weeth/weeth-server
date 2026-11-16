package com.weeth.domain.releasenote.application.validator

import com.weeth.domain.releasenote.application.exception.NotDeveloperException
import com.weeth.domain.user.domain.repository.UserReader
import org.springframework.stereotype.Component

/**
 * 플랫폼 전역 권한(DEVELOPER) 검증. 릴리즈 노트 백오피스는 동아리 권한(MemberRole)이 아닌 이 검증으로 보호한다.
 * 권한은 반드시 서버에서 검증한다(프론트 버튼 숨김은 UX 용도일 뿐 보안 수단이 아니다).
 */
@Component
class PlatformPermissionValidator(
    private val userReader: UserReader,
) {
    /** DEVELOPER가 아니면 [NotDeveloperException](403)을 던진다. */
    fun requireDeveloper(userId: Long) {
        if (!userReader.getById(userId).isDeveloper()) {
            throw NotDeveloperException()
        }
    }
}
