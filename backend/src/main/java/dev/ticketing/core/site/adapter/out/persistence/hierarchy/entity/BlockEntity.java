package dev.ticketing.core.site.adapter.out.persistence.hierarchy.entity;

import dev.ticketing.core.site.domain.hierarchy.Block;
import jakarta.persistence.Entity;
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

@Table(name = "blocks")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BlockEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", nullable = false)
    private SectionEntity section;

    private String name;

    public static BlockEntity from(Block block) {
        BlockEntity entity = new BlockEntity();
        entity.id = block.getId();
        entity.section = SectionEntity.fromId(block.getSectionId());
        entity.name = block.getName();
        return entity;
    }

    public static BlockEntity fromId(final Long id) {
        BlockEntity entity = new BlockEntity();
        entity.id = id;
        return entity;
    }

    public Block toDomain() {
        return new Block(id, section != null ? section.getId() : null, name);
    }
}
