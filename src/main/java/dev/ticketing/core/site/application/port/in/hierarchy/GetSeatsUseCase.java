package dev.ticketing.core.site.application.port.in.hierarchy;

import dev.ticketing.core.site.domain.hierarchy.Seat;
import java.util.List;

public interface GetSeatsUseCase {
    List<Seat> getSeats(Long blockId);
}
