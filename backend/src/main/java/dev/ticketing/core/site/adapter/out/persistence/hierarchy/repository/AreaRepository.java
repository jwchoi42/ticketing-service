package dev.ticketing.core.site.adapter.out.persistence.hierarchy.repository;

import dev.ticketing.core.site.adapter.out.persistence.hierarchy.entity.AreaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AreaRepository extends JpaRepository<AreaEntity, Long> {
}

