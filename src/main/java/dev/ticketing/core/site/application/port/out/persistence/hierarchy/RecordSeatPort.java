package dev.ticketing.core.site.application.port.out.persistence.hierarchy;

import dev.ticketing.core.site.domain.hierarchy.Seat;

public interface RecordSeatPort {
    Seat recordSeat(Seat seat);
}
