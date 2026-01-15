package dev.ticketing.core.site.domain.hierarchy;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Area {

    private Long id;
    private String name; // e.g., INFIELD, OUTFIELD

    public Area(final String name) {
        this(null, name);
        validate(name);
    }

    private static void validate(final String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Area name cannot be empty");
        }
    }

}
