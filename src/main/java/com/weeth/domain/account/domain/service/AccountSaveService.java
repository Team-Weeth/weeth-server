package com.weeth.domain.account.domain.service;

import com.weeth.domain.account.domain.entity.Account;
import com.weeth.domain.account.domain.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountSaveService {

    private final AccountRepository accountRepository;

    public void save(Account account) {
        accountRepository.save(account);
    }
}
