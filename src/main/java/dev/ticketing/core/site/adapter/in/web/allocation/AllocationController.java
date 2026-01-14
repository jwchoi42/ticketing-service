package dev.ticketing.core.site.adapter.in.web.allocation;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import dev.ticketing.common.web.model.response.ErrorResponse;
import dev.ticketing.common.web.model.response.SuccessResponse;
import dev.ticketing.core.site.adapter.in.web.allocation.model.request.AllocateSeatRequest;
import dev.ticketing.core.site.adapter.in.web.allocation.model.request.ConfirmSeatsRequest;
import dev.ticketing.core.site.adapter.in.web.allocation.model.request.ReleaseSeatRequest;
import dev.ticketing.core.site.adapter.in.web.allocation.model.response.ConfirmSeatsResponse;
import dev.ticketing.core.site.application.port.in.allocation.AllocateSeatUseCase;
import dev.ticketing.core.site.application.port.in.allocation.ConfirmSeatsUseCase;
import dev.ticketing.core.site.application.port.in.allocation.ReleaseSeatUseCase;

@Tag(name = "Allocation", description = "좌석 배정 API")
@RestController
@RequestMapping("/api/matches/{matchId}/allocation")
@RequiredArgsConstructor
public class AllocationController {

    private final AllocateSeatUseCase allocateSeatUseCase;
    private final ReleaseSeatUseCase releaseSeatUseCase;
    private final ConfirmSeatsUseCase confirmSeatsUseCase;

    @Operation(summary = "좌석 점유 (Hold)", description = "사용자가 선택한 좌석을 임시로 점유합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "좌석 점유 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = SuccessResponse.class), examples = @ExampleObject(value = "{\"status\": 200, \"data\": null}"))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (이미 점유된 좌석, 존재하지 않는 좌석 등)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(value = "{\"status\": 400, \"message\": \"이미 점유된 좌석입니다.\"}")))
    })
    @PostMapping("/seats/{seatId}/hold")
    @ResponseStatus(HttpStatus.OK)
    public SuccessResponse<Void> holdSeat(
            @PathVariable Long matchId,
            @PathVariable Long seatId,
            @RequestBody AllocateSeatRequest request) {
        allocateSeatUseCase.allocateSeat(request.toCommand(matchId, seatId));
        return SuccessResponse.of(null);
    }

    @Operation(summary = "좌석 반환 (Release)", description = "점유 중인 좌석을 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "좌석 반환 성공 (응답 본문 없음)"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (점유하지 않은 좌석 등)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(value = "{\"status\": 400, \"message\": \"점유하지 않은 좌석입니다.\"}")))
    })
    @PostMapping("/seats/{seatId}/release")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public SuccessResponse<Void> releaseSeat(
            @PathVariable Long matchId,
            @PathVariable Long seatId,
            @RequestBody ReleaseSeatRequest request) {
        releaseSeatUseCase.releaseSeat(request.toCommand(matchId, seatId));
        return SuccessResponse.empty();
    }

    @Operation(summary = "좌석 선택 확정", description = "점유 중인 좌석들을 확정하여 예약을 진행합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "좌석 확정 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = SuccessResponse.class), examples = @ExampleObject(value = """
                    {
                      "status": 200,
                      "data": {
                        "confirmedSeats": [
                          {"seatId": 1, "status": "CONFIRMED"},
                          {"seatId": 2, "status": "CONFIRMED"}
                        ]
                      }
                    }
                    """))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (점유한 좌석이 없음 등)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(value = "{\"status\": 400, \"message\": \"점유한 좌석이 없습니다.\"}")))
    })
    @PostMapping("/seats/confirm")
    @ResponseStatus(HttpStatus.OK)
    public SuccessResponse<ConfirmSeatsResponse> confirmSeats(
            @PathVariable Long matchId,
            @RequestBody ConfirmSeatsRequest request) {
        var confirmedSeats = confirmSeatsUseCase.confirmSeats(request.toCommand(matchId));
        return SuccessResponse.of(ConfirmSeatsResponse.from(confirmedSeats));
    }
}
