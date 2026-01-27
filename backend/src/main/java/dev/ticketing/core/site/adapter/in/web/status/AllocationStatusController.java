package dev.ticketing.core.site.adapter.in.web.status;

import dev.ticketing.common.web.model.response.SuccessResponse;
import dev.ticketing.core.site.application.port.in.allocation.status.GetAllocationStatusSnapShotUseCase;
import dev.ticketing.core.site.domain.allocation.AllocationStatusSnapShot;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(name = "Allocation Status", description = "자리 배정 현황 API")
@RestController
@RequestMapping("/api/matches/{matchId}/blocks/{blockId}/seats")
@RequiredArgsConstructor
public class AllocationStatusController {

    private final GetAllocationStatusSnapShotUseCase getAllocationStatusSnapShotUseCase;
    private final SseAllocationStatusBroadcaster broadcaster;

    @Operation(summary = "자리 배정 현황 조회")
    @GetMapping
    public SuccessResponse<AllocationStatusSnapShot> getAllocationStatusSnapShotByMatchIdAndBlockId(
            @PathVariable final Long matchId, @PathVariable final Long blockId) {
        AllocationStatusSnapShot snapshot
                = getAllocationStatusSnapShotUseCase.getAllocationStatusSnapShotByMatchIdAndBlockId(matchId, blockId);
        return SuccessResponse.of(snapshot);
    }

    @Operation(summary = "자리 배정 변경 내역 발생")
    @GetMapping("/events")
    public SseEmitter getSeatStatusStreamByMatchIdAndBlockId(
            @PathVariable final Long matchId, @PathVariable final Long blockId) {
        return broadcaster.subscribe(matchId, blockId);
    }

}
