package dev.ticketing.core.site.application.port.out.persistence.hierarchy;

import dev.ticketing.core.site.domain.hierarchy.Seat;
import java.util.List;
import java.util.Optional;

public interface LoadSeatPort {
    Optional<Seat> loadSeatById(Long seatId);

    List<Seat> loadSeatsByBlockId(Long blockId);

    Optional<Seat> loadSeatByBlockIdAndRowAndCol(Long blockId, int row, int col);

    List<Seat> loadAllSeats();
}
