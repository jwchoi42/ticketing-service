package dev.ticketing.core.site.domain.hierarchy;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Section {

    private Long id;

    private Long areaId;
    private String name; // e.g., HOME, AWAY, LEFT, RIGHT

    public Section(final Long areaId, final String name) {
        this(null, areaId, name);
        validate(areaId, name);
    }

    private static void validate(final Long areaId, final String name) {
        if (areaId == null) {
            throw new IllegalArgumentException("Area ID cannot be null");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Section name cannot be empty");
        }
    }
}
