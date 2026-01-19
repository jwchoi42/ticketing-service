package dev.ticketing.core.site.adapter.out.persistence.hierarchy.entity;

import dev.ticketing.core.site.domain.hierarchy.Seat;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name = "seats", indexes = {
    @Index(name = "idx_seats_block_id", columnList = "blockId")
})
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SeatEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long blockId;
    private Integer rowNumber;
    private Integer seatNumber;

    public static SeatEntity from(Seat seat) {
        return new SeatEntity(seat.getId(), seat.getBlockId(), seat.getRowNumber(), seat.getSeatNumber());
    }

    public Seat toDomain() {
        return new Seat(id, blockId, rowNumber, seatNumber);
    }
}
