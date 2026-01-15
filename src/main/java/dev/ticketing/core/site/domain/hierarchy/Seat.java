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

    public Seat(final Long blockId, final Integer rowNumber, final Integer seatNumber) {
        this(null, blockId, rowNumber, seatNumber);
        validate(blockId, rowNumber, seatNumber);
    }

    private static void validate(final Long blockId, final Integer rowNumber, final Integer seatNumber) {
        if (blockId == null) {
            throw new IllegalArgumentException("Block ID cannot be null");
        }
        if (rowNumber == null || rowNumber <= 0) {
            throw new IllegalArgumentException("Row number must be positive");
        }
        if (seatNumber == null || seatNumber <= 0) {
            throw new IllegalArgumentException("Seat number must be positive");
        }
    }
}
