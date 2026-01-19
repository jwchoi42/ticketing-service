package dev.ticketing.global.init;

import dev.ticketing.core.match.adapter.out.persistence.MatchEntity;
import dev.ticketing.core.match.adapter.out.persistence.MatchRepository;
import dev.ticketing.core.match.domain.Match;
import dev.ticketing.core.site.adapter.out.persistence.hierarchy.entity.AreaEntity;
import dev.ticketing.core.site.adapter.out.persistence.hierarchy.entity.BlockEntity;
import dev.ticketing.core.site.adapter.out.persistence.hierarchy.entity.SeatEntity;
import dev.ticketing.core.site.adapter.out.persistence.hierarchy.entity.SectionEntity;
import dev.ticketing.core.site.adapter.out.persistence.hierarchy.repository.AreaRepository;
import dev.ticketing.core.site.adapter.out.persistence.hierarchy.repository.BlockRepository;
import dev.ticketing.core.site.adapter.out.persistence.hierarchy.repository.SeatRepository;
import dev.ticketing.core.site.adapter.out.persistence.hierarchy.repository.SectionRepository;
import dev.ticketing.core.site.domain.hierarchy.Area;
import dev.ticketing.core.site.domain.hierarchy.Block;
import dev.ticketing.core.site.domain.hierarchy.Seat;
import dev.ticketing.core.site.domain.hierarchy.Section;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final AreaRepository areaRepository;
    private final SectionRepository sectionRepository;
    private final BlockRepository blockRepository;
    private final SeatRepository seatRepository;
    private final MatchRepository matchRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (areaRepository.count() == 0) {
            long startTime = System.currentTimeMillis();
            log.info("Initializing Stadium Data...");
            initStadium();
            long endTime = System.currentTimeMillis();
            log.info("Stadium Data Initialized. Took {} ms", endTime - startTime);
        }

        if (matchRepository.count() == 0) {
            log.info("Initializing Match Data...");
            initMatch();
            log.info("Match Data Initialized.");
        }
    }

    private void initStadium() {
        // Areas
        Area infield = saveArea("INFIELD");
        Area outfield = saveArea("OUTFIELD");

        // Sections
        // Infield
        Section home = saveSection(infield, "HOME");
        Section away = saveSection(infield, "AWAY");

        // Outfield
        Section left = saveSection(outfield, "LEFT");
        Section right = saveSection(outfield, "RIGHT");

        // Blocks & Seats
        List<Section> sections = List.of(home, away, left, right);
        for (Section section : sections) {
            // 25 blocks per section
            for (int i = 1; i <= 25; i++) {
                Block block = saveBlock(section, String.format("BLOCK-%s-%d", section.getName(), i));
                saveSeats(block);
            }
        }
    }

    private void initMatch() {
        // Adding a future game for testing
        // You can change teams and dates as needed
        Match match = Match.create(
                "Jamsil Baseball Stadium",
                "Doosan Bears",
                "LG Twins",
                LocalDateTime.now().plusDays(7));
        matchRepository.save(MatchEntity.from(match));
    }

    private Area saveArea(String name) {
        Area area = new Area(name);
        AreaEntity entity = AreaEntity.from(area);
        AreaEntity saved = areaRepository.save(entity);
        return saved.toDomain();
    }

    private Section saveSection(Area area, String name) {
        Section section = new Section(area.getId(), name);
        SectionEntity entity = SectionEntity.from(section);
        SectionEntity saved = sectionRepository.save(entity);
        return saved.toDomain();
    }

    private Block saveBlock(Section section, String name) {
        Block block = new Block(section.getId(), name);
        BlockEntity entity = BlockEntity.from(block);
        BlockEntity saved = blockRepository.save(entity);
        return saved.toDomain();
    }

    private void saveSeats(Block block) {
        List<SeatEntity> seats = new ArrayList<>();
        // 100 seats: 10 rows x 10 seats
        int rows = 10;
        int seatsPerRow = 10;

        for (int r = 1; r <= rows; r++) {
            for (int s = 1; s <= seatsPerRow; s++) {
                Seat seat = new Seat(block.getId(), r, s);
                seats.add(SeatEntity.from(seat));
            }
        }
        seatRepository.saveAll(seats);
    }
}
