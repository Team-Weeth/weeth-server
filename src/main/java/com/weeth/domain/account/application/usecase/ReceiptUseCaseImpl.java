package com.weeth.domain.account.application.usecase;

import jakarta.transaction.Transactional;
import com.weeth.domain.account.application.dto.ReceiptDTO;
import com.weeth.domain.account.application.mapper.ReceiptMapper;
import com.weeth.domain.account.domain.entity.Account;
import com.weeth.domain.account.domain.entity.Receipt;
import com.weeth.domain.account.domain.service.*;
import com.weeth.domain.file.application.mapper.FileMapper;
import com.weeth.domain.file.domain.entity.File;
import com.weeth.domain.file.domain.entity.FileOwnerType;
import com.weeth.domain.file.domain.repository.FileReader;
import com.weeth.domain.file.domain.repository.FileRepository;
import com.weeth.domain.user.domain.service.CardinalGetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReceiptUseCaseImpl implements ReceiptUseCase {

    private final ReceiptGetService receiptGetService;
    private final ReceiptDeleteService receiptDeleteService;
    private final ReceiptSaveService receiptSaveService;
    private final ReceiptUpdateService receiptUpdateService;
    private final AccountGetService accountGetService;

    private final FileReader fileReader;
    private final FileRepository fileRepository;

    private final CardinalGetService cardinalGetService;

    private final ReceiptMapper mapper;
    private final FileMapper fileMapper;


    @Override
    @Transactional
    public void save(ReceiptDTO.Save dto) {
        cardinalGetService.findByAdminSide(dto.cardinal());

        Account account = accountGetService.find(dto.cardinal());
        Receipt receipt = receiptSaveService.save(
                Receipt.Companion.create(dto.description(), dto.source(), dto.amount(), dto.date(), account)
        );
        account.spend(dto.amount());

        List<File> files = fileMapper.toFileList(dto.files(), FileOwnerType.RECEIPT, receipt.getId());
        fileRepository.saveAll(files);
    }

    @Override
    @Transactional
    public void update(Long receiptId, ReceiptDTO.Update dto) {
        Account account = accountGetService.find(dto.cardinal());
        Receipt receipt = receiptGetService.find(receiptId);
        account.adjustSpend(receipt.getAmount(), dto.amount());

        if (!dto.files().isEmpty()) { // 업데이트하려는 파일이 있다면 파일을 전체 삭제한 뒤 저장
            List<File> fileList = getFiles(receiptId);
            fileRepository.deleteAll(fileList);

            List<File> files = fileMapper.toFileList(dto.files(), FileOwnerType.RECEIPT, receipt.getId());
            fileRepository.saveAll(files);
        }
        receiptUpdateService.update(receipt, dto);
    }

    private List<File> getFiles(Long receiptId) {
        return fileReader.findAll(FileOwnerType.RECEIPT, receiptId, null);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Receipt receipt = receiptGetService.find(id);
        List<File> fileList = fileReader.findAll(FileOwnerType.RECEIPT, id, null);

        receipt.getAccount().cancelSpend(receipt.getAmount());

        fileRepository.deleteAll(fileList);
        receiptDeleteService.delete(receipt);
    }
}
