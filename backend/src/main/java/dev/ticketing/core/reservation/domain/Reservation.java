package dev.ticketing.core.reservation.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Reservation {
    private Long id;
    private Long userId;
    private Long matchId;
    private ReservationStatus status;
    private List<Long> seatIds;

    public static Reservation create(final Long userId, final Long matchId, final ReservationStatus status) {
        validate(userId, matchId, status);
        return Reservation.builder()
                .userId(userId)
                .matchId(matchId)
                .status(status)
                .build();
    }

    public static Reservation withId(final Long id, final Long userId, final Long matchId,
            final ReservationStatus status) {
        validate(userId, matchId, status);
        return Reservation.builder()
                .id(id)
                .userId(userId)
                .matchId(matchId)
                .status(status)
                .build();
    }

    public static Reservation withSeatIds(final Long id, final Long userId, final Long matchId,
            final ReservationStatus status,
            final List<Long> seatIds) {
        validate(userId, matchId, status);
        if (seatIds == null || seatIds.isEmpty()) {
            throw new IllegalArgumentException("Seat IDs cannot be empty");
        }
        return Reservation.builder()
                .id(id)
                .userId(userId)
                .matchId(matchId)
                .status(status)
                .seatIds(seatIds)
                .build();
    }

    private static void validate(final Long userId, final Long matchId, final ReservationStatus status) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (matchId == null) {
            throw new IllegalArgumentException("Match ID cannot be null");
        }
        if (status == null) {
            throw new IllegalArgumentException("Reservation status cannot be null");
        }
    }

    public Reservation confirm() {
        return Reservation.builder()
                .id(this.id)
                .userId(this.userId)
                .matchId(this.matchId)
                .status(ReservationStatus.CONFIRMED)
                .seatIds(this.seatIds)
                .build();
    }
}
