package com.weeth.domain.account.domain.service;

import com.weeth.domain.account.domain.entity.Receipt;
import com.weeth.domain.account.domain.repository.ReceiptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReceiptSaveService {

    private final ReceiptRepository receiptRepository;

    public Receipt save(Receipt receipt) {
        return receiptRepository.save(receipt);
    }
}
