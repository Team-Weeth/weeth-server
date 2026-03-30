package com.weeth.domain.session.domain.enums

enum class UpdateScope {
    THIS_ONLY, // 해당 세션만
    THIS_AND_FUTURE, // 해당 세션을 포함한 이후 세션 전체
}
