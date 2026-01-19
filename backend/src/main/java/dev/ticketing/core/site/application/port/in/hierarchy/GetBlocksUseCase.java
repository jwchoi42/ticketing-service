package dev.ticketing.core.site.application.port.in.hierarchy;

import dev.ticketing.core.site.domain.hierarchy.Block;
import java.util.List;

public interface GetBlocksUseCase {
    List<Block> getBlocks(Long sectionId);
}
