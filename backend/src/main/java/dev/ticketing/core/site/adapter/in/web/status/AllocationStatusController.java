package dev.ticketing.core.site.adapter.in.web.status;

import dev.ticketing.common.web.model.response.SuccessResponse;
import dev.ticketing.core.site.application.port.in.allocation.status.GetAllocationStatusSnapShotUseCase;
import dev.ticketing.core.site.domain.allocation.Allocation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@Tag(name = "StatusStream", description = "좌석 현황 API")
@RestController
@RequestMapping("/api/matches/{matchId}")
@RequiredArgsConstructor
public class AllocationStatusController {

    private final GetAllocationStatusSnapShotUseCase snapshotUseCase;
    private final SseAllocationStatusBroadcaster broadcaster;

    @Operation(summary = "실시간 좌석 현황 스트림 (SSE)")
    @GetMapping("/blocks/{blockId}/seats/events")
    public SseEmitter getSeatStatusStream(@PathVariable final Long matchId, @PathVariable final Long blockId) {
        return broadcaster.subscribe(matchId, blockId);
    }

    @Operation(summary = "좌석 현황 조회 (SnapShot)")
    @GetMapping("/blocks/{blockId}/seats")
    public SuccessResponse<AllocationStatusSnapShot> getSeatStatuses(@PathVariable final Long matchId,
            @PathVariable final Long blockId) {
        List<Allocation> allocations = snapshotUseCase.getSnapshot(matchId, blockId);
        return SuccessResponse.of(AllocationStatusSnapShot.from(allocations));
    }
}
