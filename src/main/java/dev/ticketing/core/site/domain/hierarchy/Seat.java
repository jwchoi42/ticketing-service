package dev.ticketing.core.site.domain.hierarchy;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Seat {
    private Long id;
    private Long blockId;
    private Integer rowNumber;
    private Integer seatNumber;

    public Seat(Long blockId, Integer rowNumber, Integer seatNumber) {
        this(null, blockId, rowNumber, seatNumber);
    }
}
