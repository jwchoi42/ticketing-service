package dev.ticketing.core.site.application.port.out.persistence.hierarchy;

import dev.ticketing.core.site.domain.hierarchy.Section;
import java.util.List;
import java.util.Optional;

public interface LoadSectionPort {
    Optional<Section> loadSectionById(Long sectionId);

    List<Section> loadSectionsByAreaId(Long areaId);
}
