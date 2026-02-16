package com.weeth.domain.account.domain.service;

import com.weeth.domain.account.domain.entity.Receipt;
import com.weeth.domain.account.domain.repository.ReceiptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReceiptDeleteService {

    private final ReceiptRepository receiptRepository;

    public void delete(Receipt receipt) {
        receiptRepository.delete(receipt);
    }
}
