package dev.ticketing.core.match.adapter.in.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import dev.ticketing.common.web.model.response.SuccessResponse;
import dev.ticketing.core.match.application.port.in.GetMatchesUseCase;
import dev.ticketing.core.match.application.port.in.model.GetMatchesQuery;
import dev.ticketing.core.match.application.port.in.model.MatchListResponse;

@Tag(name = "Match", description = "경기 API")
@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
public class MatchController {

    private final GetMatchesUseCase getMatchesUseCase;

    @Operation(summary = "경기 목록 조회", description = "예매 가능한 경기 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public SuccessResponse<MatchListResponse> getMatches() {
        GetMatchesQuery query = new GetMatchesQuery();
        MatchListResponse response = getMatchesUseCase.getMatches(query);
        return SuccessResponse.of(response);
    }
}
