package dev.ticketing.core.site.adapter.out.persistence.hierarchy.repository;

import dev.ticketing.core.site.adapter.out.persistence.hierarchy.entity.BlockEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BlockRepository extends JpaRepository<BlockEntity, Long> {
    List<BlockEntity> findBySectionId(Long sectionId);
}
