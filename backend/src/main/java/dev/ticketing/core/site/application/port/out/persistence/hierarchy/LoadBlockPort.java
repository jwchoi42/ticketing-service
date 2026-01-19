package dev.ticketing.core.site.application.port.out.persistence.hierarchy;

import dev.ticketing.core.site.domain.hierarchy.Block;
import java.util.List;
import java.util.Optional;

public interface LoadBlockPort {
    Optional<Block> loadBlockById(Long blockId);

    List<Block> loadBlocksBySectionId(Long sectionId);
}
