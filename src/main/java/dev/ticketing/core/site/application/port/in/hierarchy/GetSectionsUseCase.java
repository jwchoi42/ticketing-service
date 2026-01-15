package dev.ticketing.core.site.application.port.in.hierarchy;

import dev.ticketing.core.site.domain.hierarchy.Section;
import java.util.List;

public interface GetSectionsUseCase {
    List<Section> getSections(Long areaId);
}
