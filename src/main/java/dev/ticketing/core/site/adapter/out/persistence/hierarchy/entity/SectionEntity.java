package dev.ticketing.core.site.adapter.out.persistence.hierarchy.entity;

import dev.ticketing.core.site.domain.hierarchy.Section;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
    private Long areaId;
    private String name;

    public static SectionEntity from(Section section) {
        return new SectionEntity(section.getId(), section.getAreaId(), section.getName());
    }

    public Section toDomain() {
        return new Section(id, areaId, name);
    }
}
