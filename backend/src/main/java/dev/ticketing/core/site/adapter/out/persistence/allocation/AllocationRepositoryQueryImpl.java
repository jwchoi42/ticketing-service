package dev.ticketing.core.site.adapter.out.persistence.allocation;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import dev.ticketing.core.site.domain.allocation.AllocationState;
import dev.ticketing.core.site.domain.allocation.AllocationStatus;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

import static dev.ticketing.core.site.adapter.out.persistence.allocation.QAllocationEntity.allocationEntity;

@RequiredArgsConstructor
public class AllocationRepositoryQueryImpl implements AllocationRepositoryQuery {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<AllocationStatus> findAllocationStatusesByMatchIdAndBlockId(Long matchId, Long blockId) {
        return queryFactory
                .select(Projections.constructor(AllocationStatus.class,
                        allocationEntity.id,
                        allocationEntity.matchId,
                        allocationEntity.blockId,
                        allocationEntity.seatId,
                        allocationEntity.status,
                        allocationEntity.holdExpiresAt,
                        allocationEntity.updatedAt
                ))
                .from(allocationEntity)
                .where(
                        allocationEntity.matchId.eq(matchId),
                        allocationEntity.blockId.eq(blockId)
                )
                .fetch();
    }

    @Override
    public List<AllocationStatus> findAllocationStatusesByBlockIdAndUpdatedAtAfter(
            Long matchId, Long blockId, LocalDateTime since) {
        return queryFactory
                .select(Projections.constructor(AllocationStatus.class,
                        allocationEntity.id,
                        allocationEntity.matchId,
                        allocationEntity.blockId,
                        allocationEntity.seatId,
                        allocationEntity.status,
                        allocationEntity.holdExpiresAt,
                        allocationEntity.updatedAt
                ))
                .from(allocationEntity)
                .where(
                        allocationEntity.matchId.eq(matchId),
                        allocationEntity.blockId.eq(blockId),
                        allocationEntity.updatedAt.after(since)
                )
                .fetch();
    }
}
