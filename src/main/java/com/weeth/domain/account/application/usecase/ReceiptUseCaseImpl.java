package com.weeth.domain.account.application.usecase;

import jakarta.transaction.Transactional;
import com.weeth.domain.account.application.dto.ReceiptDTO;
import com.weeth.domain.account.application.mapper.ReceiptMapper;
import com.weeth.domain.account.domain.entity.Account;
import com.weeth.domain.account.domain.entity.Receipt;
import com.weeth.domain.account.domain.service.*;
import com.weeth.domain.file.application.mapper.FileMapper;
import com.weeth.domain.file.domain.entity.File;
import com.weeth.domain.file.domain.service.FileDeleteService;
import com.weeth.domain.file.domain.service.FileGetService;
import com.weeth.domain.file.domain.service.FileSaveService;
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

    private final FileGetService fileGetService;
    private final FileSaveService fileSaveService;
    private final FileDeleteService fileDeleteService;

    private final CardinalGetService cardinalGetService;

    private final ReceiptMapper mapper;
    private final FileMapper fileMapper;


    @Override
    @Transactional
    public void save(ReceiptDTO.Save dto) {
        cardinalGetService.findByAdminSide(dto.cardinal());

        Account account = accountGetService.find(dto.cardinal());
        Receipt receipt = receiptSaveService.save(mapper.from(dto, account));
        account.spend(receipt);

        List<File> files = fileMapper.toFileList(dto.files(), receipt);
        fileSaveService.save(files);
    }

    @Override
    @Transactional
    public void update(Long receiptId, ReceiptDTO.Update dto){
        Account account = accountGetService.find(dto.cardinal());
        Receipt receipt = receiptGetService.find(receiptId);
        account.cancel(receipt);

        if(!dto.files().isEmpty()){ // 업데이트하려는 파일이 있다면 파일을 전체 삭제한 뒤 저장
            List<File> fileList = getFiles(receiptId);
            fileDeleteService.delete(fileList);

            List<File> files = fileMapper.toFileList(dto.files(), receipt);
            fileSaveService.save(files);
        }
        receiptUpdateService.update(receipt, dto);
        account.spend(receipt);
    }

    private List<File> getFiles(Long receiptId) {
        return fileGetService.findAllByReceipt(receiptId);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Receipt receipt = receiptGetService.find(id);
        List<File> fileList = fileGetService.findAllByReceipt(id);

        receipt.getAccount().cancel(receipt);

        fileDeleteService.delete(fileList);
        receiptDeleteService.delete(receipt);
    }
}
