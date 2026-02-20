package com.weeth.domain.user.domain.entity.enums

import com.weeth.domain.user.application.exception.StatusNotFoundException

enum class StatusPriority(
    val priority: Int,
) {
    ACTIVE(1),
    WAITING(2),
    LEFT(3),
    BANNED(4),
    ;

    companion object {
        @JvmStatic
        fun from(status: Status?): StatusPriority {
            if (status == null) {
                throw StatusNotFoundException()
            }
            return valueOf(status.name)
        }

        @JvmStatic
        fun fromStatus(status: Status?): StatusPriority = from(status)
    }
}
