package leets.weeth.domain.account.application.usecase;

import leets.weeth.domain.account.application.dto.ReceiptDTO;

public interface ReceiptUseCase {
    void save(ReceiptDTO.Save dto);

    void update(Long receiptId, ReceiptDTO.Update dto);

    void delete(Long id);
}
