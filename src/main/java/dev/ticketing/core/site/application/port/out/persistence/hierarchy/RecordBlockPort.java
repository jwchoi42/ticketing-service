package dev.ticketing.core.site.application.port.out.persistence.hierarchy;

import dev.ticketing.core.site.domain.hierarchy.Block;

public interface RecordBlockPort {
    Block recordBlock(Block block);
}
