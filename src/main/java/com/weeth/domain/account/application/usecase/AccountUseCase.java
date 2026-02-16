package com.weeth.domain.account.application.usecase;

import com.weeth.domain.account.application.dto.AccountDTO;

public interface AccountUseCase {
    AccountDTO.Response find(Integer cardinal);

    void save(AccountDTO.Save dto);
}
