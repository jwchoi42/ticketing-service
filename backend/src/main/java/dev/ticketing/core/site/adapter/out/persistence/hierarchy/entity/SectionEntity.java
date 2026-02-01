package dev.ticketing.core.site.adapter.out.persistence.hierarchy.entity;

import dev.ticketing.core.site.domain.hierarchy.Section;
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

@Table(name = "sections")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SectionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "area_id", nullable = false)
    private AreaEntity area;

    private String name;

    public static SectionEntity from(Section section) {
        SectionEntity entity = new SectionEntity();
        entity.id = section.getId();
        entity.area = AreaEntity.fromId(section.getAreaId());
        entity.name = section.getName();
        return entity;
    }

    public static SectionEntity fromId(final Long id) {
        SectionEntity entity = new SectionEntity();
        entity.id = id;
        return entity;
    }

    public Section toDomain() {
        return new Section(id, area != null ? area.getId() : null, name);
    }
}
