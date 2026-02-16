package com.weeth.domain.account.application.usecase;

import com.weeth.domain.account.application.dto.AccountDTO;
import com.weeth.domain.account.application.dto.ReceiptDTO;
import com.weeth.domain.account.application.exception.AccountExistsException;
import com.weeth.domain.account.application.mapper.AccountMapper;
import com.weeth.domain.account.application.mapper.ReceiptMapper;
import com.weeth.domain.account.domain.entity.Account;
import com.weeth.domain.account.domain.entity.Receipt;
import com.weeth.domain.account.domain.service.AccountGetService;
import com.weeth.domain.account.domain.service.AccountSaveService;
import com.weeth.domain.account.domain.service.ReceiptGetService;
import com.weeth.domain.file.application.dto.response.FileResponse;
import com.weeth.domain.file.application.mapper.FileMapper;
import com.weeth.domain.file.domain.service.FileGetService;
import com.weeth.domain.user.domain.service.CardinalGetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountUseCaseImpl implements AccountUseCase {

    private final AccountGetService accountGetService;
    private final AccountSaveService accountSaveService;
    private final ReceiptGetService receiptGetService;
    private final FileGetService fileGetService;
    private final CardinalGetService cardinalGetService;

    private final AccountMapper accountMapper;
    private final ReceiptMapper receiptMapper;
    private final FileMapper fileMapper;

    @Override
    public AccountDTO.Response find(Integer cardinal) {
        Account account = accountGetService.find(cardinal);
        List<Receipt> receipts = receiptGetService.findAllByAccountId(account.getId());
        List<ReceiptDTO.Response> response = receipts.stream()
                .map(receipt -> receiptMapper.to(receipt, getFiles(receipt.getId())))
                .toList();

        return accountMapper.to(account, response);
    }

    @Override
    @Transactional
    public void save(AccountDTO.Save dto) {
        validate(dto);
        cardinalGetService.findByAdminSide(dto.cardinal());

        accountSaveService.save(accountMapper.from(dto));
    }

    private void validate(AccountDTO.Save dto) {
        if (accountGetService.validate(dto.cardinal()))
            throw new AccountExistsException();
    }

    private List<FileResponse> getFiles(Long receiptId) {
        return fileGetService.findAllByReceipt(receiptId).stream()
                .map(fileMapper::toFileResponse)
                .toList();
    }
}
