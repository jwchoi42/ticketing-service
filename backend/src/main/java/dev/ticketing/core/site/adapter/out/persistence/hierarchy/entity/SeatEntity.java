package dev.ticketing.core.site.adapter.out.persistence.hierarchy.entity;

import dev.ticketing.core.site.domain.hierarchy.Seat;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name = "seats", indexes = {
        @Index(name = "idx_seats_block_id", columnList = "block_id"),
        @Index(name = "idx_seats_block_row_seat", columnList = "block_id, rowNumber, seatNumber")
})
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SeatEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "block_id", nullable = false)
    private BlockEntity block;

    private Integer rowNumber;
    private Integer seatNumber;

    public static SeatEntity from(Seat seat) {
        SeatEntity entity = new SeatEntity();
        entity.id = seat.getId();
        entity.block = BlockEntity.fromId(seat.getBlockId());
        entity.rowNumber = seat.getRowNumber();
        entity.seatNumber = seat.getSeatNumber();
        return entity;
    }

    public static SeatEntity fromId(final Long id) {
        SeatEntity entity = new SeatEntity();
        entity.id = id;
        return entity;
    }

    public Seat toDomain() {
        return new Seat(id, block != null ? block.getId() : null, rowNumber, seatNumber);
    }
}
