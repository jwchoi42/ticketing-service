package dev.ticketing.core.site.adapter.out.persistence.allocation;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import dev.ticketing.core.site.domain.allocation.AllocationState;
import dev.ticketing.core.site.domain.allocation.AllocationStatus;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

import static dev.ticketing.core.site.adapter.out.persistence.allocation.QAllocationEntity.allocationEntity;
import static dev.ticketing.core.site.adapter.out.persistence.hierarchy.entity.QSeatEntity.seatEntity;

@RequiredArgsConstructor
public class AllocationRepositoryQueryImpl implements AllocationRepositoryQuery {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<AllocationStatus> findAllocationStatusesByMatchIdAndBlockId(Long matchId, Long blockId) {
        return queryFactory
                .select(Projections.constructor(AllocationStatus.class,
                        allocationEntity.id,
                        allocationEntity.match.id,
                        allocationEntity.block.id,
                        allocationEntity.seat.id,
                        allocationEntity.status,
                        allocationEntity.holdExpiresAt,
                        allocationEntity.updatedAt))
                .from(allocationEntity)
                .where(
                        allocationEntity.match.id.eq(matchId),
                        allocationEntity.block.id.eq(blockId))
                .fetch();
    }

    @Override
    public List<AllocationStatus> findAllocationStatusesByMatchIdAndBlockIdWithJoin(Long matchId, Long blockId) {
        return queryFactory
                .select(Projections.constructor(AllocationStatus.class,
                        allocationEntity.id,
                        allocationEntity.match.id,
                        seatEntity.block.id,
                        allocationEntity.seat.id,
                        allocationEntity.status,
                        allocationEntity.holdExpiresAt,
                        allocationEntity.updatedAt))
                .from(allocationEntity)
                .join(allocationEntity.seat, seatEntity)
                .where(
                        allocationEntity.match.id.eq(matchId),
                        seatEntity.block.id.eq(blockId))
                .fetch();
    }

    @Override
    public List<AllocationStatus> findAllocationStatusesByBlockIdAndUpdatedAtAfter(
            Long matchId, Long blockId, LocalDateTime since) {
        return queryFactory
                .select(Projections.constructor(AllocationStatus.class,
                        allocationEntity.id,
                        allocationEntity.match.id,
                        allocationEntity.block.id,
                        allocationEntity.seat.id,
                        allocationEntity.status,
                        allocationEntity.holdExpiresAt,
                        allocationEntity.updatedAt))
                .from(allocationEntity)
                .where(
                        allocationEntity.match.id.eq(matchId),
                        allocationEntity.block.id.eq(blockId),
                        allocationEntity.updatedAt.after(since))
                .fetch();
    }
}
