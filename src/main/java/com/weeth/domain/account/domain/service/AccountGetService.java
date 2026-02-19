package com.weeth.domain.account.domain.service;

import com.weeth.domain.account.domain.entity.Account;
import com.weeth.domain.account.domain.repository.AccountRepository;
import com.weeth.domain.account.application.exception.AccountNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountGetService {

    private final AccountRepository accountRepository;

    public Account find(Integer cardinal) {
        Account account = accountRepository.findByCardinal(cardinal);
        if (account == null) throw new AccountNotFoundException();
        return account;
    }

    public boolean validate(Integer cardinal) {
        return accountRepository.existsByCardinal(cardinal);
    }
}
