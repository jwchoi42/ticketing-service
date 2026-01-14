package dev.ticketing.core.site.domain.hierarchy;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Block {
    private Long id;
    private Long sectionId;
    private String name;

    public Block(final Long sectionId, final String name) {
        this(null, sectionId, name);
        validate(sectionId, name);
    }

    private static void validate(final Long sectionId, final String name) {
        if (sectionId == null) {
            throw new IllegalArgumentException("Section ID cannot be null");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Block name cannot be empty");
        }
    }
}
