package com.weeth.domain.attendance.domain.port

interface SseBroadcastPort {
    fun broadcast(
        clubId: Long,
        eventName: String,
        data: Any?,
    )

    fun sendToUser(
        clubId: Long,
        userId: Long,
        eventName: String,
        data: Any?,
    )
}
