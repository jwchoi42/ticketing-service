package dev.ticketing.core.site.application.port.out.persistence.hierarchy;

import dev.ticketing.core.site.domain.hierarchy.Area;

public interface RecordAreaPort {
    Area recordArea(Area area);
}
