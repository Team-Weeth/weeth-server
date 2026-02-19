package com.weeth.domain.account.domain.service;

import com.weeth.domain.account.application.dto.ReceiptDTO;
import com.weeth.domain.account.domain.entity.Receipt;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReceiptUpdateService {
    public void update(Receipt receipt, ReceiptDTO.Update dto) {
        receipt.update(dto.description(), dto.source(), dto.amount(), dto.date());
    }
}