package dev.ticketing.core.site.adapter.out.persistence.hierarchy;

import dev.ticketing.core.site.adapter.out.persistence.hierarchy.repository.AreaRepository;
import dev.ticketing.core.site.adapter.out.persistence.hierarchy.repository.BlockRepository;
import dev.ticketing.core.site.adapter.out.persistence.hierarchy.repository.SeatRepository;
import dev.ticketing.core.site.adapter.out.persistence.hierarchy.repository.SectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import dev.ticketing.core.site.adapter.out.persistence.hierarchy.entity.AreaEntity;
import dev.ticketing.core.site.adapter.out.persistence.hierarchy.entity.BlockEntity;
import dev.ticketing.core.site.adapter.out.persistence.hierarchy.entity.SeatEntity;
import dev.ticketing.core.site.adapter.out.persistence.hierarchy.entity.SectionEntity;
import dev.ticketing.core.site.application.port.out.persistence.hierarchy.*;
import dev.ticketing.core.site.domain.hierarchy.Area;
import dev.ticketing.core.site.domain.hierarchy.Block;
import dev.ticketing.core.site.domain.hierarchy.Seat;
import dev.ticketing.core.site.domain.hierarchy.Section;

@Component
@RequiredArgsConstructor
public class SitePersistenceAdapter
        implements LoadAreaPort, LoadSectionPort, LoadBlockPort, LoadSeatPort,
        RecordAreaPort, RecordSectionPort, RecordBlockPort, RecordSeatPort {

    private final AreaRepository areaRepository;
    private final SectionRepository sectionRepository;
    private final BlockRepository blockRepository;
    private final SeatRepository seatRepository;

    @Override
    public Optional<Area> loadAreaById(final Long areaId) {
        return areaRepository.findById(areaId).map(AreaEntity::toDomain);
    }

    @Override
    public List<Area> loadAllAreas() {
        return areaRepository.findAll().stream().map(AreaEntity::toDomain).collect(Collectors.toList());
    }

    @Override
    public Optional<Section> loadSectionById(final Long sectionId) {
        return sectionRepository.findById(sectionId).map(SectionEntity::toDomain);
    }

    @Override
    public List<Section> loadSectionsByAreaId(final Long areaId) {
        return sectionRepository.findByAreaId(areaId).stream().map(SectionEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Block> loadBlockById(final Long blockId) {
        return blockRepository.findById(blockId).map(BlockEntity::toDomain);
    }

    @Override
    public List<Block> loadBlocksBySectionId(final Long sectionId) {
        return blockRepository.findBySectionId(sectionId).stream().map(BlockEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Seat> loadSeatById(final Long seatId) {
        return seatRepository.findById(seatId).map(SeatEntity::toDomain);
    }

    @Override
    public List<Seat> loadSeatsByBlockId(final Long blockId) {
        return seatRepository.findByBlockId(blockId).stream().map(SeatEntity::toDomain).collect(Collectors.toList());
    }

    @Override
    public Optional<Seat> loadSeatByBlockIdAndRowAndCol(final Long blockId, final int row, final int col) {
        return seatRepository.findByBlockIdAndRowNumberAndSeatNumber(blockId, row, col)
                .map(SeatEntity::toDomain);
    }

    @Override
    public List<Seat> loadAllSeats() {
        return seatRepository.findAll().stream()
                .map(SeatEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Area recordArea(final Area area) {
        return areaRepository.save(AreaEntity.from(area)).toDomain();
    }

    @Override
    public Section recordSection(final Section section) {
        return sectionRepository.save(SectionEntity.from(section)).toDomain();
    }

    @Override
    public Block recordBlock(final Block block) {
        return blockRepository.save(BlockEntity.from(block)).toDomain();
    }

    @Override
    public Seat recordSeat(final Seat seat) {
        return seatRepository.save(SeatEntity.from(seat)).toDomain();
    }
}
