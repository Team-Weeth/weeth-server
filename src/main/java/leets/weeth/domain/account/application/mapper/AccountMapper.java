package leets.weeth.domain.account.application.mapper;

import leets.weeth.domain.account.application.dto.AccountDTO;
import leets.weeth.domain.account.application.dto.ReceiptDTO;
import leets.weeth.domain.account.domain.entity.Account;
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
