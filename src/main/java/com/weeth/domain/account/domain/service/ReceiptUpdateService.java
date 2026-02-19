package com.weeth.domain.account.domain.service;

import com.weeth.domain.account.application.dto.request.ReceiptUpdateRequest;
import com.weeth.domain.account.domain.entity.Receipt;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReceiptUpdateService {
    public void update(Receipt receipt, ReceiptUpdateRequest dto) {
        receipt.update(dto.getDescription(), dto.getSource(), dto.getAmount(), dto.getDate());
    }
}
