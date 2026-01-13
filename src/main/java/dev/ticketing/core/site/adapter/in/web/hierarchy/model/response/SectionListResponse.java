package dev.ticketing.core.site.adapter.in.web.hierarchy.model.response;

import dev.ticketing.core.site.domain.hierarchy.Section;
import java.util.List;

public record SectionListResponse(List<SectionResponse> sections) {
    public static SectionListResponse from(List<Section> sections) {
        return new SectionListResponse(sections.stream().map(SectionResponse::from).toList());
    }

    public record SectionResponse(Long id, String name) {
        public static SectionResponse from(Section section) {
            return new SectionResponse(section.getId(), section.getName());
        }
    }
}
