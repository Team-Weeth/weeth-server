package leets.weeth.domain.account.domain.service;

import leets.weeth.domain.account.domain.entity.Account;
import leets.weeth.domain.account.domain.repository.AccountRepository;
import leets.weeth.domain.account.application.exception.AccountNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountGetService {

    private final AccountRepository accountRepository;

    public Account find(Integer cardinal) {
        return accountRepository.findByCardinal(cardinal)
                .orElseThrow(AccountNotFoundException::new);
    }

    public boolean validate(Integer cardinal) {
        return accountRepository.existsByCardinal(cardinal);
    }
}
