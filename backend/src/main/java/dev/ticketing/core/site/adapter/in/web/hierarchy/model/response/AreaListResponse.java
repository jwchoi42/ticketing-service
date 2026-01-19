package dev.ticketing.core.site.adapter.in.web.hierarchy.model.response;

import dev.ticketing.core.site.domain.hierarchy.Area;
import java.util.List;

public record AreaListResponse(List<AreaResponse> areas) {
    public static AreaListResponse from(List<Area> areas) {
        return new AreaListResponse(areas.stream().map(AreaResponse::from).toList());
    }

    public record AreaResponse(Long id, String name) {
        public static AreaResponse from(Area area) {
            return new AreaResponse(area.getId(), area.getName());
        }
    }
}
