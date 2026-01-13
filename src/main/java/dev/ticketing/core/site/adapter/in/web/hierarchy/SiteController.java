package dev.ticketing.core.site.adapter.in.web.hierarchy;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.ticketing.common.web.model.response.SuccessResponse;
import dev.ticketing.core.site.adapter.in.web.hierarchy.model.response.AreaListResponse;
import dev.ticketing.core.site.adapter.in.web.hierarchy.model.response.BlockListResponse;
import dev.ticketing.core.site.adapter.in.web.hierarchy.model.response.SectionListResponse;
import dev.ticketing.core.site.adapter.in.web.hierarchy.model.response.SeatListResponse;
import dev.ticketing.core.site.application.port.in.hierarchy.GetAreasUseCase;
import dev.ticketing.core.site.application.port.in.hierarchy.GetBlocksUseCase;
import dev.ticketing.core.site.application.port.in.hierarchy.GetSectionsUseCase;
import dev.ticketing.core.site.application.port.in.hierarchy.GetSeatsUseCase;

@Tag(name = "Site", description = "장소 API")
@RestController
@RequestMapping("/api/site")
@RequiredArgsConstructor
public class SiteController {

    private final GetAreasUseCase getAreasUseCase;
    private final GetSectionsUseCase getSectionsUseCase;
    private final GetBlocksUseCase getBlocksUseCase;
    private final GetSeatsUseCase getSeatsUseCase;

    @Operation(summary = "영역 목록 조회", description = "경기장의 영역(Area) 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "영역 목록 조회 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = SuccessResponse.class), examples = @ExampleObject(value = """
                    {
                      "status": 200,
                      "data": {
                        "areas": [
                          {"id": 1, "name": "1층"},
                          {"id": 2, "name": "2층"}
                        ]
                      }
                    }
                    """)))
    })
    @GetMapping("/areas")
    public SuccessResponse<AreaListResponse> getAreas() {
        return SuccessResponse.of(AreaListResponse.from(getAreasUseCase.getAreas()));
    }

    @Operation(summary = "영역의 진영 목록 조회", description = "특정 영역의 진영(Section) 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "진영 목록 조회 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = SuccessResponse.class), examples = @ExampleObject(value = """
                    {
                      "status": 200,
                      "data": {
                        "sections": [
                          {"id": 1, "name": "A구역"},
                          {"id": 2, "name": "B구역"}
                        ]
                      }
                    }
                    """)))
    })
    @GetMapping("/areas/{areaId}/sections")
    public SuccessResponse<SectionListResponse> getSections(@PathVariable Long areaId) {
        return SuccessResponse.of(SectionListResponse.from(getSectionsUseCase.getSections(areaId)));
    }

    @Operation(summary = "진영의 구간 목록 조회", description = "특정 진영의 구간(Block) 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "구간 목록 조회 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = SuccessResponse.class), examples = @ExampleObject(value = """
                    {
                      "status": 200,
                      "data": {
                        "blocks": [
                          {"id": 1, "name": "1열"},
                          {"id": 2, "name": "2열"}
                        ]
                      }
                    }
                    """)))
    })
    @GetMapping("/sections/{sectionId}/blocks")
    public SuccessResponse<BlockListResponse> getBlocks(@PathVariable Long sectionId) {
        return SuccessResponse.of(BlockListResponse.from(getBlocksUseCase.getBlocks(sectionId)));
    }

    @Operation(summary = "구간의 좌석 목록 조회", description = "특정 구간의 좌석(Seat) 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "좌석 목록 조회 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = SuccessResponse.class), examples = @ExampleObject(value = """
                    {
                      "status": 200,
                      "data": {
                        "seats": [
                          {"id": 1, "seatNumber": "1"},
                          {"id": 2, "seatNumber": "2"}
                        ]
                      }
                    }
                    """)))
    })
    @GetMapping("/blocks/{blockId}/seats")
    public SuccessResponse<SeatListResponse> getSeats(@PathVariable Long blockId) {
        return SuccessResponse.of(SeatListResponse.from(getSeatsUseCase.getSeats(blockId)));
    }
}
