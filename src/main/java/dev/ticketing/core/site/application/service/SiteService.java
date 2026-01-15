package dev.ticketing.core.site.application.service;

import dev.ticketing.core.site.application.port.in.hierarchy.GetAreasUseCase;
import dev.ticketing.core.site.application.port.in.hierarchy.GetBlocksUseCase;
import dev.ticketing.core.site.application.port.in.hierarchy.GetSectionsUseCase;
import dev.ticketing.core.site.application.port.in.hierarchy.GetSeatsUseCase;
import dev.ticketing.core.site.application.port.out.persistence.hierarchy.LoadAreaPort;
import dev.ticketing.core.site.application.port.out.persistence.hierarchy.LoadBlockPort;
import dev.ticketing.core.site.application.port.out.persistence.hierarchy.LoadSeatPort;
import dev.ticketing.core.site.application.port.out.persistence.hierarchy.LoadSectionPort;
import dev.ticketing.core.site.domain.hierarchy.Area;
import dev.ticketing.core.site.domain.hierarchy.Block;
import dev.ticketing.core.site.domain.hierarchy.Seat;
import dev.ticketing.core.site.domain.hierarchy.Section;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SiteService implements GetAreasUseCase, GetSectionsUseCase, GetBlocksUseCase, GetSeatsUseCase {

    private final LoadAreaPort loadAreaPort;
    private final LoadSectionPort loadSectionPort;
    private final LoadBlockPort loadBlockPort;
    private final LoadSeatPort loadSeatPort;

    @Override
    public List<Area> getAreas() {
        return loadAreaPort.loadAllAreas();
    }

    @Override
    public List<Section> getSections(final Long areaId) {
        return loadSectionPort.loadSectionsByAreaId(areaId);
    }

    @Override
    public List<Block> getBlocks(final Long sectionId) {
        return loadBlockPort.loadBlocksBySectionId(sectionId);
    }

    @Override
    public List<Seat> getSeats(final Long blockId) {
        return loadSeatPort.loadSeatsByBlockId(blockId);
    }
}
