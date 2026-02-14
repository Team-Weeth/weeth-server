package leets.weeth.domain.account.domain.service;

import leets.weeth.domain.account.application.dto.ReceiptDTO;
import leets.weeth.domain.account.domain.entity.Receipt;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReceiptUpdateService {
    public void update(Receipt receipt, ReceiptDTO.Update dto) {
        receipt.update(dto);
    }
}