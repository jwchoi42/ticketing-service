package dev.ticketing.core.site.adapter.out.persistence.allocation;

import dev.ticketing.core.site.domain.allocation.Allocation;
import dev.ticketing.core.site.domain.allocation.AllocationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Table(name = "allocations", indexes = {
        @Index(name = "idx_match_seat_unique", columnList = "matchId, seatId", unique = true),
        @Index(name = "idx_allocations_updated_at", columnList = "updatedAt")
})
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AllocationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private Long matchId;
    private Long seatId;
    private Long reservationId;

    @Enumerated(EnumType.STRING)
    private AllocationStatus status;

    private LocalDateTime holdExpiresAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
    }

    public static AllocationEntity from(Allocation allocation) {
        AllocationEntity entity = new AllocationEntity(
                allocation.getId(),
                allocation.getUserId(),
                allocation.getMatchId(),
                allocation.getSeatId(),
                allocation.getReservationId(),
                allocation.getStatus(),
                allocation.getHoldExpiresAt(),
                allocation.getUpdatedAt() != null ? allocation.getUpdatedAt() : LocalDateTime.now());
        return entity;
    }

    public Allocation toDomain() {
        return Allocation.withId(id, userId, matchId, seatId, reservationId, status, holdExpiresAt, updatedAt);
    }
}
