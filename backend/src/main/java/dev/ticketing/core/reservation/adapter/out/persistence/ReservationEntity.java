package dev.ticketing.core.reservation.adapter.out.persistence;

import dev.ticketing.core.reservation.domain.Reservation;
import dev.ticketing.core.reservation.domain.ReservationStatus;
import dev.ticketing.core.match.adapter.out.persistence.MatchEntity;
import dev.ticketing.core.user.adapter.out.persistence.UserEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name = "reservations")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ReservationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private MatchEntity match;

    @Enumerated(EnumType.STRING)
    private ReservationStatus status;

    public static ReservationEntity from(final Reservation reservation) {
        ReservationEntity entity = new ReservationEntity();
        entity.id = reservation.getId();
        entity.user = UserEntity.fromId(reservation.getUserId());
        entity.match = MatchEntity.fromId(reservation.getMatchId());
        entity.status = reservation.getStatus();
        return entity;
    }

    public static ReservationEntity fromId(final Long id) {
        ReservationEntity entity = new ReservationEntity();
        entity.id = id;
        return entity;
    }

    public Reservation toDomain() {
        return Reservation.withId(
                id,
                user != null ? user.getId() : null,
                match != null ? match.getId() : null,
                status);
    }
}
