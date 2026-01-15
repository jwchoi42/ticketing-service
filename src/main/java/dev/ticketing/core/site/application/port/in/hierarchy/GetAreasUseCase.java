package dev.ticketing.core.site.application.port.in.hierarchy;

import dev.ticketing.core.site.domain.hierarchy.Area;
import java.util.List;

public interface GetAreasUseCase {
    List<Area> getAreas();
}
