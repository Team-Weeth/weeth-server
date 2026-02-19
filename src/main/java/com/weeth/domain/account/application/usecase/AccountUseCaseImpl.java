package com.weeth.domain.account.application.usecase;

import com.weeth.domain.account.application.dto.request.AccountSaveRequest;
import com.weeth.domain.account.application.dto.response.AccountResponse;
import com.weeth.domain.account.application.dto.response.ReceiptResponse;
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
import com.weeth.domain.file.domain.entity.FileOwnerType;
import com.weeth.domain.file.domain.repository.FileReader;
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
    private final FileReader fileReader;
    private final CardinalGetService cardinalGetService;

    private final AccountMapper accountMapper;
    private final ReceiptMapper receiptMapper;
    private final FileMapper fileMapper;

    @Override
    public AccountResponse find(Integer cardinal) {
        Account account = accountGetService.find(cardinal);
        List<Receipt> receipts = receiptGetService.findAllByAccountId(account.getId());
        List<ReceiptResponse> response = receipts.stream()
                .map(receipt -> receiptMapper.toResponse(receipt, getFiles(receipt.getId())))
                .toList();

        return accountMapper.toResponse(account, response);
    }

    @Override
    @Transactional
    public void save(AccountSaveRequest dto) {
        validate(dto);
        cardinalGetService.findByAdminSide(dto.getCardinal());

        accountSaveService.save(Account.create(dto.getDescription(), dto.getTotalAmount(), dto.getCardinal()));
    }

    private void validate(AccountSaveRequest dto) {
        if (accountGetService.validate(dto.getCardinal()))
            throw new AccountExistsException();
    }

    private List<FileResponse> getFiles(Long receiptId) {
        return fileReader.findAll(FileOwnerType.RECEIPT, receiptId, null).stream()
                .map(fileMapper::toFileResponse)
                .toList();
    }
}
