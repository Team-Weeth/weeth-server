package com.weeth.domain.session.presentation

import com.weeth.domain.session.application.dto.request.SessionCreateRequest
import com.weeth.domain.session.application.dto.request.SessionUpdateRequest
import com.weeth.domain.session.application.dto.response.SessionInfosResponse
import com.weeth.domain.session.application.exception.SessionErrorCode
import com.weeth.domain.session.application.usecase.command.CreateSessionUseCase
import com.weeth.domain.session.application.usecase.command.DeleteSessionUseCase
import com.weeth.domain.session.application.usecase.command.UpdateSessionUseCase
import com.weeth.domain.session.application.usecase.query.GetSessionQueryService
import com.weeth.domain.session.domain.enums.UpdateScope
import com.weeth.global.auth.annotation.CurrentUser
import com.weeth.global.common.exception.ApiErrorCodeExample
import com.weeth.global.common.response.CommonResponse
import com.weeth.global.common.web.TsidParam
import com.weeth.global.common.web.TsidPathVariable
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "SESSION ADMIN", description = "[ADMIN] 정기모임 어드민 API")
@RestController
@RequestMapping("/api/v4/admin/clubs/{clubId}/sessions")
@ApiErrorCodeExample(SessionErrorCode::class)
class SessionAdminController(
    private val createSessionUseCase: CreateSessionUseCase,
    private val updateSessionUseCase: UpdateSessionUseCase,
    private val deleteSessionUseCase: DeleteSessionUseCase,
    private val getSessionQueryService: GetSessionQueryService,
) {
    @PostMapping
    @Operation(summary = "정기모임 생성 (반복 지원)")
    fun create(
        @TsidParam
        @TsidPathVariable clubId: Long,
        @Valid @RequestBody dto: SessionCreateRequest,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<Void?> {
        createSessionUseCase.create(clubId, dto, userId)
        return CommonResponse.success(SessionResponseCode.SESSION_SAVE_SUCCESS)
    }

    @PatchMapping("/{sessionId}")
    @Operation(
        summary = "정기모임 수정",
        description = "scope=THIS_AND_FUTURE 시 이후 전체 세션 수정. CLOSED 세션 포함 시 force=true로 재요청 필요",
    )
    fun update(
        @TsidParam
        @TsidPathVariable clubId: Long,
        @PathVariable sessionId: Long,
        @Valid @RequestBody dto: SessionUpdateRequest,
        @RequestParam(defaultValue = "THIS_ONLY") scope: UpdateScope,
        @RequestParam(defaultValue = "false") force: Boolean,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<Void?> {
        updateSessionUseCase.update(clubId, sessionId, dto, userId, scope, force)
        return CommonResponse.success(SessionResponseCode.SESSION_UPDATE_SUCCESS)
    }

    @DeleteMapping("/{sessionId}")
    @Operation(
        summary = "정기모임 삭제",
        description = "scope=THIS_AND_FUTURE 시 이후 전체 세션 삭제. CLOSED 세션 포함 시 force=true로 재요청 필요",
    )
    fun delete(
        @TsidParam
        @TsidPathVariable clubId: Long,
        @PathVariable sessionId: Long,
        @RequestParam(defaultValue = "THIS_ONLY") scope: UpdateScope,
        @RequestParam(defaultValue = "false") force: Boolean,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<Void?> {
        deleteSessionUseCase.delete(clubId, sessionId, userId, scope, force)
        return CommonResponse.success(SessionResponseCode.SESSION_DELETE_SUCCESS)
    }

    @DeleteMapping("/groups/{groupId}")
    @Operation(
        summary = "세션 그룹 전체 삭제",
        description = "반복 세션 그룹과 소속 세션을 모두 삭제. CLOSED 세션 포함 시 force=true로 재요청 필요",
    )
    fun deleteGroup(
        @TsidParam
        @TsidPathVariable clubId: Long,
        @PathVariable groupId: Long,
        @RequestParam(defaultValue = "false") force: Boolean,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<Void?> {
        deleteSessionUseCase.deleteGroup(clubId, groupId, userId, force)
        return CommonResponse.success(SessionResponseCode.SESSION_DELETE_SUCCESS)
    }

    @GetMapping
    @Operation(summary = "정기모임 목록 조회 (반복 그룹 단위)")
    fun getSessionInfos(
        @TsidParam
        @TsidPathVariable clubId: Long,
        @RequestParam(required = false) cardinal: Int?,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<SessionInfosResponse> =
        CommonResponse.success(
            SessionResponseCode.SESSION_INFOS_FIND_SUCCESS,
            getSessionQueryService.findSessionInfos(clubId, userId, cardinal),
        )
}
