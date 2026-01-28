package dev.ticketing.core.site.domain.allocation;

import java.time.LocalDateTime;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Allocation {

    private Long id;
    private Long userId;
    private Long matchId;
    private Long blockId;
    private Long seatId;
    private Long reservationId;
    private AllocationState state;
    private LocalDateTime holdExpiresAt;
    private LocalDateTime updatedAt;

    public static Allocation of(final Long seatId, final AllocationState status) {
        return Allocation.builder()
                .seatId(seatId)
                .state(status)
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public static Allocation available(final Long seatId) {
        return Allocation.builder()
                .seatId(seatId)
                .state(AllocationState.AVAILABLE)
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public static Allocation availableForMatch(final Long matchId, final Long blockId, final Long seatId) {
        return Allocation.builder()
                .matchId(matchId)
                .blockId(blockId)
                .seatId(seatId)
                .state(AllocationState.AVAILABLE)
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public static Allocation withId(
            final Long id,
            final Long userId,
            final Long matchId,
            final Long blockId,
            final Long seatId,
            final Long reservationId,
            final AllocationState status,
            final LocalDateTime holdExpiresAt,
            final LocalDateTime updatedAt) {
        return Allocation.builder()
                .id(id)
                .userId(userId)
                .matchId(matchId)
                .blockId(blockId)
                .seatId(seatId)
                .reservationId(reservationId)
                .state(status)
                .holdExpiresAt(holdExpiresAt)
                .updatedAt(updatedAt)
                .build();
    }

    public boolean isHeldBy(final Long userId) {
        return this.state == AllocationState.HOLD && this.userId != null && this.userId.equals(userId);
    }

    public Allocation hold(final Long userId, final Long matchId, final LocalDateTime expiresAt) {
        return Allocation.builder()
                .id(this.id)
                .userId(userId)
                .matchId(matchId)
                .blockId(this.blockId)
                .seatId(this.seatId)
                .reservationId(null)
                .state(AllocationState.HOLD)
                .holdExpiresAt(expiresAt)
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public Allocation release() {
        return Allocation.builder()
                .id(this.id)
                .userId(null)
                .matchId(this.matchId)
                .blockId(this.blockId)
                .seatId(this.seatId)
                .reservationId(null)
                .state(AllocationState.AVAILABLE)
                .holdExpiresAt(null)
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public Allocation occupy() {
        return Allocation.builder()
                .id(this.id)
                .userId(this.userId)
                .matchId(this.matchId)
                .blockId(this.blockId)
                .seatId(this.seatId)
                .reservationId(this.reservationId)
                .state(AllocationState.OCCUPIED)
                .holdExpiresAt(null)
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public Allocation assignReservation(final Long reservationId) {
        return Allocation.builder()
                .id(this.id)
                .userId(this.userId)
                .matchId(this.matchId)
                .blockId(this.blockId)
                .seatId(this.seatId)
                .reservationId(reservationId)
                .state(this.state)
                .holdExpiresAt(this.holdExpiresAt)
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
