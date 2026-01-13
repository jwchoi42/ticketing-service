package dev.ticketing.core.match.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchRepository extends JpaRepository<MatchEntity, Long> {
}
