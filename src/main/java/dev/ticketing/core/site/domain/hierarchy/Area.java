package dev.ticketing.core.site.domain.hierarchy;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Area {

    private Long id;
    private String name; // e.g., INFIELD, OUTFIELD

    public Area(String name) {
        this(null, name);
    }

}
