package com.weeth.domain.account.application.mapper;

import com.weeth.domain.account.application.dto.AccountDTO;
import com.weeth.domain.account.application.dto.ReceiptDTO;
import com.weeth.domain.account.domain.entity.Account;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AccountMapper {

    @Mapping(target = "accountId", source = "account.id")
    @Mapping(target = "receipts", source = "receipts")
    @Mapping(target = "time", source = "account.modifiedAt")
    AccountDTO.Response to(Account account, List<ReceiptDTO.Response> receipts);

    @Mapping(target = "currentAmount", source = "totalAmount")
    Account from(AccountDTO.Save dto);
}
