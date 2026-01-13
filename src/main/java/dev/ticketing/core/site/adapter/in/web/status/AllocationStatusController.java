package dev.ticketing.core.site.adapter.in.web.status;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import dev.ticketing.common.web.model.response.SuccessResponse;
import dev.ticketing.core.site.application.port.in.allocation.status.GetAllocationStatusSnapShotUseCase;
import dev.ticketing.core.site.application.port.in.allocation.status.SubscribeAllocationStatusChangesStreamUseCase;

@Tag(name = "StatusStream", description = "좌석 현황 API")
@RestController
@RequestMapping("/api/matches/{matchId}")
@RequiredArgsConstructor
public class AllocationStatusController {

    private final SubscribeAllocationStatusChangesStreamUseCase subscribeAllocationStatusChangesStreamUseCase;
    private final GetAllocationStatusSnapShotUseCase getAllocationStatusSnapShotUseCase;

    @Operation(summary = "실시간 좌석 현황 스트림 (SSE)")
    @GetMapping("/blocks/{blockId}/seats/events")
    public SseEmitter getSeatStatusStream(@PathVariable Long matchId, @PathVariable Long blockId) {
        return subscribeAllocationStatusChangesStreamUseCase.subscribeAllocationStatusChangesStream(matchId, blockId);
    }

    @Operation(summary = "좌석 현황 조회 (SnapShot)")
    @GetMapping("/blocks/{blockId}/seats")
    public SuccessResponse<AllocationStatusSnapShot> getSeatStatuses(@PathVariable Long matchId,
            @PathVariable Long blockId) {
        return SuccessResponse.of(getAllocationStatusSnapShotUseCase.getAllocationStatusSnapShot(matchId, blockId));
    }
}
