package com.weeth.domain.account.application.mapper;

import com.weeth.domain.account.application.dto.ReceiptDTO;
import com.weeth.domain.account.domain.entity.Account;
import com.weeth.domain.account.domain.entity.Receipt;
import com.weeth.domain.file.application.dto.response.FileResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ReceiptMapper {

    List<ReceiptDTO.Response> to(List<Receipt> account);

    ReceiptDTO.Response to(Receipt receipt, List<FileResponse> fileUrls);

}
