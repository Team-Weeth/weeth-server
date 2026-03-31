package com.weeth.domain.user.application.usecase.command

import com.weeth.domain.user.application.dto.request.CreateInquiryRequest
import com.weeth.domain.user.domain.port.InquiryNotifyPort
import com.weeth.domain.user.domain.port.InquirySavePort
import org.springframework.stereotype.Service

@Service
class CreateInquiryUseCase(
    private val inquirySavePort: InquirySavePort,
    private val inquiryNotifyPort: InquiryNotifyPort,
) {
    fun execute(request: CreateInquiryRequest) {
        inquirySavePort.save(request.email, request.message)
        inquiryNotifyPort.notify(request.email, request.message)
    }
}
