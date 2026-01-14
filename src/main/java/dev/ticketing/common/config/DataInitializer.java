package dev.ticketing.common.config;

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
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final MatchRepository matchRepository;
    private final AreaRepository areaRepository;
    private final SectionRepository sectionRepository;
    private final BlockRepository blockRepository;
    private final SeatRepository seatRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (matchRepository.count() > 0) {
            log.info("데이터가 이미 존재하므로 초기화를 건너뜁니다.");
            return;
        }

        log.info("데이터 초기화 시작...");

        // 1. 경기 데이터 생성
        Match match = Match.create("잠실야구장", "LG 트윈스", "두산 베어스", LocalDateTime.now().plusDays(7));
        MatchEntity matchEntity = matchRepository.save(MatchEntity.from(match));
        log.info("경기 데이터 생성 완료: ID={}", matchEntity.getId());

        // 2. 영역(Area) 생성: 내야, 외야
        AreaEntity infieldEntity = areaRepository.save(AreaEntity.from(new Area("내야")));
        AreaEntity outfieldEntity = areaRepository.save(AreaEntity.from(new Area("외야")));
        log.info("영역 데이터 생성 완료: 내야, 외야");

        // 3. 진영(Section) 생성
        // 내야: 연고, 원정
        SectionEntity homeInfieldEntity = sectionRepository
                .save(SectionEntity.from(new Section(infieldEntity.getId(), "연고")));
        SectionEntity awayInfieldEntity = sectionRepository
                .save(SectionEntity.from(new Section(infieldEntity.getId(), "원정")));
        // 외야: 좌측, 우측
        SectionEntity leftOutfieldEntity = sectionRepository
                .save(SectionEntity.from(new Section(outfieldEntity.getId(), "좌측")));
        SectionEntity rightOutfieldEntity = sectionRepository
                .save(SectionEntity.from(new Section(outfieldEntity.getId(), "우측")));
        log.info("진영 데이터 생성 완료: 연고, 원정, 좌측, 우측");

        // 4. 구역(Block) 및 좌석(Seat) 생성
        initBlocksAndSeats(homeInfieldEntity.getId(), "내야-연고");
        initBlocksAndSeats(awayInfieldEntity.getId(), "내야-원정");
        initBlocksAndSeats(leftOutfieldEntity.getId(), "외야-좌측");
        initBlocksAndSeats(rightOutfieldEntity.getId(), "외야-우측");

        log.info("모든 데이터 초기화 완료.");
    }

    private void initBlocksAndSeats(Long sectionId, String prefix) {
        log.info("{} 구역 및 좌석 생성 중...", prefix);
        for (int b = 1; b <= 25; b++) {
            BlockEntity blockEntity = blockRepository.save(BlockEntity.from(new Block(sectionId, prefix + "-" + b)));

            List<SeatEntity> seats = new ArrayList<>();
            for (int s = 1; s <= 100; s++) {
                int row = (s - 1) / 10 + 1;
                seats.add(SeatEntity.from(new Seat(blockEntity.getId(), row, s)));
            }
            seatRepository.saveAll(seats);
        }
    }
}
