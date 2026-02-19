package com.weeth.domain.account.application.usecase;

import com.weeth.domain.account.application.dto.request.AccountSaveRequest;
import com.weeth.domain.account.application.dto.response.AccountResponse;

public interface AccountUseCase {
    AccountResponse find(Integer cardinal);

    void save(AccountSaveRequest dto);
}
