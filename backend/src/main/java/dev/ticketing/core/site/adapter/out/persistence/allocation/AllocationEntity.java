package dev.ticketing.core.site.adapter.out.persistence.allocation;

import dev.ticketing.core.site.domain.allocation.Allocation;
import dev.ticketing.core.site.domain.allocation.AllocationState;
import dev.ticketing.core.match.adapter.out.persistence.MatchEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import dev.ticketing.core.reservation.adapter.out.persistence.ReservationEntity;
import dev.ticketing.core.site.adapter.out.persistence.hierarchy.entity.BlockEntity;
import dev.ticketing.core.site.adapter.out.persistence.hierarchy.entity.SeatEntity;
import dev.ticketing.core.user.adapter.out.persistence.UserEntity;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Table(name = "allocations", indexes = {
        @Index(name = "idx_match_seat_unique", columnList = "match_id, seat_id", unique = true),
        @Index(name = "idx_match_block", columnList = "match_id, block_id"),
        @Index(name = "idx_match_block_updated", columnList = "match_id, block_id, updatedAt"),
        @Index(name = "idx_reservation_id", columnList = "reservation_id")
})
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AllocationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private MatchEntity match;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "block_id", nullable = false)
    private BlockEntity block;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id", nullable = false)
    private SeatEntity seat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id")
    private ReservationEntity reservation;

    @Enumerated(EnumType.STRING)
    private AllocationState status;

    private LocalDateTime holdExpiresAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
    }

    public static AllocationEntity from(Allocation allocation) {
        AllocationEntity entity = new AllocationEntity();
        entity.id = allocation.getId();
        entity.user = allocation.getUserId() != null ? UserEntity.fromId(allocation.getUserId()) : null;
        entity.match = MatchEntity.fromId(allocation.getMatchId());
        entity.block = BlockEntity.fromId(allocation.getBlockId());
        entity.seat = SeatEntity.fromId(allocation.getSeatId());
        entity.reservation = allocation.getReservationId() != null
                ? ReservationEntity.fromId(allocation.getReservationId())
                : null;
        entity.status = allocation.getState();
        entity.holdExpiresAt = allocation.getHoldExpiresAt();
        entity.updatedAt = allocation.getUpdatedAt() != null ? allocation.getUpdatedAt() : LocalDateTime.now();
        return entity;
    }

    public Allocation toDomain() {
        return Allocation.withId(
                id,
                user != null ? user.getId() : null,
                match != null ? match.getId() : null,
                block != null ? block.getId() : null,
                seat != null ? seat.getId() : null,
                reservation != null ? reservation.getId() : null,
                status,
                holdExpiresAt,
                updatedAt);
    }
}
