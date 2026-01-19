package dev.ticketing.core.site.adapter.out.persistence.hierarchy.entity;

import dev.ticketing.core.site.domain.hierarchy.Block;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
    private Long sectionId;
    private String name;

    public static BlockEntity from(Block block) {
        return new BlockEntity(block.getId(), block.getSectionId(), block.getName());
    }

    public Block toDomain() {
        return new Block(id, sectionId, name);
    }
}
