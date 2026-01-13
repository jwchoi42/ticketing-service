package dev.ticketing.core.site.domain.hierarchy;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Block {
    private Long id;
    private Long sectionId;
    private String name;

    public Block(Long sectionId, String name) {
        this(null, sectionId, name);
    }
}
