package com.weeth.domain.account.application.mapper

import com.weeth.domain.account.application.dto.response.ReceiptResponse
import com.weeth.domain.account.domain.entity.Receipt
import com.weeth.domain.file.application.dto.response.FileResponse
import org.springframework.stereotype.Component

@Component
class ReceiptMapper {
    fun toResponse(
        receipt: Receipt,
        fileUrls: List<FileResponse>,
    ): ReceiptResponse =
        ReceiptResponse(
            id = receipt.id,
            description = receipt.description,
            source = receipt.source,
            amount = receipt.amount,
            date = receipt.date,
            fileUrls = fileUrls,
        )

    fun toResponses(
        receipts: List<Receipt>,
        filesByReceiptId: Map<Long, List<FileResponse>>,
    ): List<ReceiptResponse> =
        receipts.map { receipt ->
            toResponse(receipt, filesByReceiptId[receipt.id] ?: emptyList())
        }
}
