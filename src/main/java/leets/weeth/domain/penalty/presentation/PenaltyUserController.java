package leets.weeth.domain.penalty.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import leets.weeth.domain.penalty.application.dto.PenaltyDTO;
import leets.weeth.domain.penalty.application.exception.PenaltyErrorCode;
import leets.weeth.domain.penalty.application.usecase.PenaltyUsecase;
import leets.weeth.global.auth.annotation.CurrentUser;
import leets.weeth.global.common.exception.ApiErrorCodeExample;
import leets.weeth.global.common.response.CommonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static leets.weeth.domain.penalty.presentation.ResponseMessage.PENALTY_USER_FIND_SUCCESS;

@Tag(name = "PENALTY", description = "패널티 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/penalties")
@ApiErrorCodeExample(PenaltyErrorCode.class)
public class PenaltyUserController {

    private final PenaltyUsecase penaltyUsecase;

    @GetMapping
    @Operation(summary="본인 패널티 조회")
    public CommonResponse<PenaltyDTO.Response> findAllPenalties(@Parameter(hidden = true) @CurrentUser Long userId) {
        PenaltyDTO.Response penalties = penaltyUsecase.find(userId);
        return CommonResponse.createSuccess(PENALTY_USER_FIND_SUCCESS.getMessage(),penalties);
    }

}
