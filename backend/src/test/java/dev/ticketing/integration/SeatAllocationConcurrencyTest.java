package dev.ticketing.integration;

import dev.ticketing.configuration.TestContainerConfiguration;
import dev.ticketing.core.site.application.port.in.allocation.AllocateSeatCommand;
import dev.ticketing.core.site.application.port.in.allocation.AllocateSeatUseCase;
import dev.ticketing.core.site.application.service.exception.SeatAlreadyHeldException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestContainerConfiguration.class)
@ActiveProfiles("test")
@DisplayName("Seat Allocation Concurrency Tests")
class SeatAllocationConcurrencyTest {

    @Autowired
    private AllocateSeatUseCase allocateSeatUseCase;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long matchId;
    private Long seatId;

    @BeforeEach
    void setUp() {
        // Clean up existing allocations
        jdbcTemplate.execute("DELETE FROM allocations");

        // Get a valid match ID and seat ID from the database
        matchId = jdbcTemplate.queryForObject(
                "SELECT id FROM matches LIMIT 1",
                Long.class
        );

        seatId = jdbcTemplate.queryForObject(
                "SELECT id FROM seats LIMIT 1",
                Long.class
        );

        // Ensure match is OPEN for seat allocation
        jdbcTemplate.update(
                "UPDATE matches SET status = 'OPEN' WHERE id = ?",
                matchId
        );

        // Pre-create AVAILABLE allocation (simulating MatchService.openMatch() behavior)
        jdbcTemplate.update("""
                INSERT INTO allocations (user_id, match_id, seat_id, status, hold_expires_at, updated_at)
                VALUES (NULL, ?, ?, 'AVAILABLE', NULL, NOW())
                ON CONFLICT (match_id, seat_id) DO UPDATE SET status = 'AVAILABLE', user_id = NULL, hold_expires_at = NULL
                """, matchId, seatId);
    }

    @Test
    @DisplayName("When two users simultaneously try to allocate the same seat, exactly one should succeed")
    void concurrentSeatAllocation_onlyOneSucceeds() throws InterruptedException {
        // Given
        Long user1Id = 1L;
        Long user2Id = 2L;

        AllocateSeatCommand command1 = new AllocateSeatCommand(user1Id, matchId, seatId);
        AllocateSeatCommand command2 = new AllocateSeatCommand(user2Id, matchId, seatId);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        AtomicReference<Exception> unexpectedException = new AtomicReference<>();

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(2);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        // When - Both threads try to allocate at the same time
        executor.submit(() -> {
            try {
                startLatch.await();
                allocateSeatUseCase.allocateSeat(command1);
                successCount.incrementAndGet();
            } catch (SeatAlreadyHeldException e) {
                conflictCount.incrementAndGet();
            } catch (Exception e) {
                unexpectedException.set(e);
            } finally {
                endLatch.countDown();
            }
        });

        executor.submit(() -> {
            try {
                startLatch.await();
                allocateSeatUseCase.allocateSeat(command2);
                successCount.incrementAndGet();
            } catch (SeatAlreadyHeldException e) {
                conflictCount.incrementAndGet();
            } catch (Exception e) {
                unexpectedException.set(e);
            } finally {
                endLatch.countDown();
            }
        });

        // Release both threads simultaneously
        startLatch.countDown();

        // Wait for both to complete
        endLatch.await();
        executor.shutdown();

        // Then
        assertThat(unexpectedException.get())
                .as("No unexpected exceptions should occur")
                .isNull();

        assertThat(successCount.get())
                .as("Exactly one user should successfully allocate the seat")
                .isEqualTo(1);

        assertThat(conflictCount.get())
                .as("Exactly one user should receive a conflict error")
                .isEqualTo(1);

        // Verify only one allocation exists in the database
        Integer allocationCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM allocations WHERE match_id = ? AND seat_id = ?",
                Integer.class,
                matchId, seatId
        );

        assertThat(allocationCount)
                .as("Only one allocation record should exist")
                .isEqualTo(1);
    }

    @Test
    @DisplayName("When multiple users try to allocate the same seat with higher concurrency, exactly one should succeed")
    void highConcurrencySeatAllocation_onlyOneSucceeds() throws InterruptedException {
        // Given
        int threadCount = 10;

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        AtomicReference<Exception> unexpectedException = new AtomicReference<>();

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        // When - All threads try to allocate at the same time
        for (int i = 0; i < threadCount; i++) {
            Long userId = (long) (i + 1);
            AllocateSeatCommand command = new AllocateSeatCommand(userId, matchId, seatId);

            executor.submit(() -> {
                try {
                    startLatch.await();
                    allocateSeatUseCase.allocateSeat(command);
                    successCount.incrementAndGet();
                } catch (SeatAlreadyHeldException e) {
                    conflictCount.incrementAndGet();
                } catch (Exception e) {
                    if (unexpectedException.get() == null) {
                        unexpectedException.set(e);
                    }
                } finally {
                    endLatch.countDown();
                }
            });
        }

        // Release all threads simultaneously
        startLatch.countDown();

        // Wait for all to complete
        endLatch.await();
        executor.shutdown();

        // Then
        assertThat(unexpectedException.get())
                .as("No unexpected exceptions should occur: " +
                        (unexpectedException.get() != null ? unexpectedException.get().getMessage() : ""))
                .isNull();

        assertThat(successCount.get())
                .as("Exactly one user should successfully allocate the seat")
                .isEqualTo(1);

        assertThat(conflictCount.get())
                .as("All other users should receive a conflict error")
                .isEqualTo(threadCount - 1);

        // Verify only one allocation exists in the database
        Integer allocationCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM allocations WHERE match_id = ? AND seat_id = ?",
                Integer.class,
                matchId, seatId
        );

        assertThat(allocationCount)
                .as("Only one allocation record should exist")
                .isEqualTo(1);
    }
}
