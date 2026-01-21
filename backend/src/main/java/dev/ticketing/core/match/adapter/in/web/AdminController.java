package dev.ticketing.core.match.adapter.in.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import dev.ticketing.common.exception.UnauthorizedException;
import dev.ticketing.common.web.model.response.SuccessResponse;
import dev.ticketing.core.match.adapter.in.web.model.CreateMatchRequest;
import dev.ticketing.core.match.adapter.in.web.model.UpdateMatchRequest;
import dev.ticketing.core.match.application.port.in.CreateMatchUseCase;
import dev.ticketing.core.match.application.port.in.DeleteMatchUseCase;
import dev.ticketing.core.match.application.port.in.OpenMatchUseCase;
import dev.ticketing.core.match.application.port.in.UpdateMatchUseCase;
import dev.ticketing.core.match.application.port.in.model.MatchResponse;
import dev.ticketing.core.user.application.port.out.LoadUserPort;
import dev.ticketing.core.user.domain.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Admin", description = "관리자 API")
@RestController
@RequestMapping("/api/admin/matches")
@RequiredArgsConstructor
public class AdminController {

    private final CreateMatchUseCase createMatchUseCase;
    private final UpdateMatchUseCase updateMatchUseCase;
    private final DeleteMatchUseCase deleteMatchUseCase;
    private final OpenMatchUseCase openMatchUseCase;
    private final LoadUserPort loadUserPort;

    @Operation(summary = "경기 생성", description = "새로운 경기를 생성합니다. (DRAFT 상태)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "생성 성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SuccessResponse<MatchResponse> createMatch(
            @RequestParam final Long userId,
            @RequestBody final CreateMatchRequest request) {
        validateAdmin(userId);
        MatchResponse response = createMatchUseCase.createMatch(request.toCommand());
        return SuccessResponse.of(response);
    }

    @Operation(summary = "경기 수정", description = "경기 정보를 수정합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "경기 없음")
    })
    @PutMapping("/{matchId}")
    @ResponseStatus(HttpStatus.OK)
    public SuccessResponse<MatchResponse> updateMatch(
            @RequestParam final Long userId,
            @PathVariable final Long matchId,
            @RequestBody final UpdateMatchRequest request) {
        validateAdmin(userId);
        MatchResponse response = updateMatchUseCase.updateMatch(request.toCommand(matchId));
        return SuccessResponse.of(response);
    }

    @Operation(summary = "경기 삭제", description = "경기를 삭제합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "경기 없음")
    })
    @DeleteMapping("/{matchId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMatch(
            @RequestParam final Long userId,
            @PathVariable final Long matchId) {
        validateAdmin(userId);
        deleteMatchUseCase.deleteMatch(matchId);
    }

    @Operation(summary = "경기 오픈", description = "경기를 오픈하여 예매 가능 상태로 전환합니다. 좌석 할당이 초기화됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "오픈 성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "경기 없음"),
            @ApiResponse(responseCode = "409", description = "이미 오픈된 경기")
    })
    @PostMapping("/{matchId}/open")
    @ResponseStatus(HttpStatus.OK)
    public SuccessResponse<MatchResponse> openMatch(
            @RequestParam final Long userId,
            @PathVariable final Long matchId) {
        validateAdmin(userId);
        MatchResponse response = openMatchUseCase.openMatch(matchId);
        return SuccessResponse.of(response);
    }

    private void validateAdmin(Long userId) {
        User user = loadUserPort.loadById(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));
        if (!user.isAdmin()) {
            throw new UnauthorizedException("Admin access required");
        }
    }
}
