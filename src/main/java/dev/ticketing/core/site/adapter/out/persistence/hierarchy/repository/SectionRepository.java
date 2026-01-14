package dev.ticketing.core.site.adapter.out.persistence.hierarchy.repository;

import dev.ticketing.core.site.adapter.out.persistence.hierarchy.entity.SectionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SectionRepository extends JpaRepository<SectionEntity, Long> {
    List<SectionEntity> findByAreaId(Long areaId);
}
