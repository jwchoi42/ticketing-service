package dev.ticketing.core.site.domain.hierarchy;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Section {

    private Long id;

    private Long areaId;
    private String name; // e.g., HOME, AWAY, LEFT, RIGHT

    public Section(Long areaId, String name) {
        this(null, areaId, name);
    }
}
