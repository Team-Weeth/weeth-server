package com.weeth.domain.account.application.usecase;

import com.weeth.domain.account.application.dto.request.ReceiptSaveRequest;
import com.weeth.domain.account.application.dto.request.ReceiptUpdateRequest;

public interface ReceiptUseCase {
    void save(ReceiptSaveRequest dto);

    void update(Long receiptId, ReceiptUpdateRequest dto);

    void delete(Long id);
}
