package dev.ticketing.core.site.application.port.out.persistence.hierarchy;

import dev.ticketing.core.site.domain.hierarchy.Area;
import java.util.List;
import java.util.Optional;

public interface LoadAreaPort {
    Optional<Area> loadAreaById(Long areaId);

    List<Area> loadAllAreas();
}
