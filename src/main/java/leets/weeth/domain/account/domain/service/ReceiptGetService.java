package leets.weeth.domain.account.domain.service;

import leets.weeth.domain.account.domain.entity.Receipt;
import leets.weeth.domain.account.domain.repository.ReceiptRepository;
import leets.weeth.domain.account.application.exception.ReceiptNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReceiptGetService {

    private final ReceiptRepository receiptRepository;

    public Receipt find(Long id) {
        return receiptRepository.findById(id)
                .orElseThrow(ReceiptNotFoundException::new);
    }

    public List<Receipt> findAllByAccountId(Long accountId) {
        return receiptRepository.findAllByAccountIdOrderByCreatedAtDesc(accountId);
    }
}
