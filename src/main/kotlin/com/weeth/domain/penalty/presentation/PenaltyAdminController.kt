package com.weeth.domain.penalty.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import com.weeth.domain.penalty.application.dto.PenaltyDTO;
import com.weeth.domain.penalty.application.exception.PenaltyErrorCode;
import com.weeth.domain.penalty.application.usecase.PenaltyUsecase;
import com.weeth.global.common.exception.ApiErrorCodeExample;
import com.weeth.global.common.response.CommonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.weeth.domain.penalty.presentation.PenaltyResponseCode.*;

@Tag(name = "PENALTY ADMIN", description = "[ADMIN] 패널티 어드민 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/penalties")
@ApiErrorCodeExample(PenaltyErrorCode.class)
public class PenaltyAdminController {

    private final PenaltyUsecase penaltyUsecase;

    @PostMapping
    @Operation(summary="패널티 부여")
    public CommonResponse<String> assignPenalty(@Valid @RequestBody PenaltyDTO.Save dto){
        penaltyUsecase.save(dto);
        return CommonResponse.success(PENALTY_ASSIGN_SUCCESS);
    }

    @PatchMapping
    @Operation(summary = "패널티 수정")
    public CommonResponse<String> update(@Valid @RequestBody PenaltyDTO.Update dto){
        penaltyUsecase.update(dto);
        return CommonResponse.success(PENALTY_UPDATE_SUCCESS);
    }

    @GetMapping
    @Operation(summary="전체 패널티 조회")
    public CommonResponse<List<PenaltyDTO.ResponseAll>> findAll(@RequestParam(required = false) Integer cardinal){
        return CommonResponse.success(PENALTY_FIND_ALL_SUCCESS, penaltyUsecase.findAll(cardinal));
    }

    @DeleteMapping
    @Operation(summary="패널티 삭제")
    public CommonResponse<String> delete(@RequestParam Long penaltyId){
        penaltyUsecase.delete(penaltyId);
        return CommonResponse.success(PENALTY_DELETE_SUCCESS);
    }

}
