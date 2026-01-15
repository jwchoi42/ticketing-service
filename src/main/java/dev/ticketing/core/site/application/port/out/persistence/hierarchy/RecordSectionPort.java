package dev.ticketing.core.site.application.port.out.persistence.hierarchy;

import dev.ticketing.core.site.domain.hierarchy.Section;

public interface RecordSectionPort {
    Section recordSection(Section section);
}
