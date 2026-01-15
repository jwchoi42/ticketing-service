---
trigger: model_decision
description: mvp-stream-implementation-plan
---

MVP Status Stream Service 구현 계획 (Server-side Polling + SSE)
===

개요
---
RDB 기반 MVP 환경에서 **SSE + 서버 내부 DB 폴링** 방식으로 실시간 좌석 현황 기능을 구현합니다.

### 핵심 특징
- ✅ **SSE 사용**: 클라이언트는 한 번만 연결, 서버가 푸시
- ✅ **서버 폴링**: @Scheduled로 1초마다 DB에서 변경 감지
- ✅ **단순 인프라**: Redis 없이 RDB만으로 동작
- ⚠️ **폴링 지연**: 최대 1초 지연 (폴링 주기)

---

1. 구현 목표
---

### 완료 조건
- [x] SiteService: 좌석 계층 구조 조회 ✅
- [x] RdbAllocationService: 좌석 점유 처리 ✅
- [x] **RdbStatusStreamService**: SSE + 서버 내부 DB 폴링 ✅

### 구현 범위
1. **DB 스키마 변경**: allocations 테이블에 `updated_at` 컬럼 추가
2. **UseCase 정의**: `StatusStreamUseCase`
3. **Service 구현**: `RdbStatusStreamService` (SSE + @Scheduled 폴링)
4. **Port 구현**: `LoadSeatStatusesPort` 확장
5. **Adapter 구현**: `SeatStatusPersistenceAdapter`
6. **Controller 구현**: `GET /api/matches/{matchId}/blocks/{blockId}/seats/status-stream` SSE 엔드포인트
7. **Response DTO**: `AllocationStatusStreamInitResponse`, `AllocationStatusStreamUpdateResponse`

---

2. 아키텍처
---

### 전체 흐름
```
[Client]
   ↓
   └─ EventSource(/seats/status-stream) ──> [SiteController]
                                                ↓
                                                ↓
                                          [RdbStatusStreamService]
                                                ↓
                                                ├─ SSE 연결 유지 (SseEmitter)
                                                ├─ 초기 데이터 전송 (event: init)
                                                ↓
                                          [Spring Scheduler]
                                                ↓
                                         @Scheduled(fixedRate = 1000)
                                                ↓
                                                ↓
                                     SELECT * FROM allocations
                                     WHERE updated_at > last_check_time
                                                ↓
                                        변경 사항 감지 시
                                                ↓
                                                └──> SSE 푸시 (event: update)
```

### 데이터 흐름
1. 클라이언트가 `EventSource`로 SSE 연결
2. Controller가 `StatusStreamUseCase` 호출 후 `SseEmitter` 반환
3. Service가 초기 데이터 조회 후 `event: init` 전송
4. Service가 SSE 연결을 Map에 등록 (matchId:blockId 별 List<SseEmitter>)
5. @Scheduled 메서드가 1초마다 실행:
   - 모든 등록된 구간에 대해 DB 조회
   - `WHERE updated_at > last_check_time`로 변경분만 조회
   - 변경 사항이 있으면 해당 구간의 모든 SSE 연결에 `event: update` 전송
6. 클라이언트가 연결 종료 시 Map에서 제거

---

3. 상세 구현 계획
---

### Step 1: Database Schema

#### allocations 테이블에 updated_at 컬럼 추가
```sql
-- 1. 컬럼 추가
ALTER TABLE allocations
ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- 2. 인덱스 생성 (성능 최적화)
CREATE INDEX idx_allocations_updated_at ON allocations(updated_at);
```

#### AllocationEntity 수정
```java
@Entity
@Table(name = "allocations")
public class AllocationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "match_id", nullable = false)
    private Long matchId;

    @Column(name = "seat_id", nullable = false)
    private Long seatId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AllocationStatus status;

    @Column(name = "hold_expires_at")
    private LocalDateTime holdExpiresAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
    }
}
```

---

4. Application Layer
---

### 4.1 UseCase (In Port)
**파일**: `StatusStreamUseCase.java`

```java
package dev.ticketing.core.site.application.port.in.status;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import dev.ticketing.core.site.domain.allocation.Allocation;

import java.util.List;

public interface StatusStreamUseCase {
    /**
     * 특정 구간의 좌석 상태 스트림을 SSE로 제공합니다.
     *
     * @param matchId 경기 ID
     * @param blockId 구간 ID
     * @return SSE Emitter
     */
    SseEmitter getSeatStatusStream(Long matchId, Long blockId);

    List<Allocation> getSeatStatuses(Long matchId, Long blockId);
}
```

### 4.2 Service 구현체
**파일**: `RdbStatusStreamService.java`

```java
package dev.ticketing.core.site.application.service;

import dev.ticketing.core.site.application.port.in.status.StatusStreamUseCase;
// ... (기타 import 생략)

@Service
@RequiredArgsConstructor
public class RdbStatusStreamService implements StatusStreamUseCase {
    // 구현 내용 생략 (RdbStatusStreamService.java 참조)
}
```

---

5. API 명세
---

### 엔드포인트
```
GET /api/matches/{matchId}/blocks/{blockId}/seats/status-stream
Content-Type: text/event-stream
```

### Response (SSE Stream)

#### event: init
연결 시 전체 좌석 현황 전송
```
event: init
data: {"status":200,"data":{"seats":[{"id":101,"status":"AVAILABLE"},{"id":102,"status":"HOLD"}]}}
```

#### event: update
변경 시 변경된 좌석만 전송
```
event: update
data: {"status":200,"data":{"seatId":101,"status":"HOLD"}}
```

---

6. 구현 체크리스트 (Checklist)
---

### Database
- [x] allocations 테이블에 `updated_at` 컬럼 추가
- [x] `idx_allocations_updated_at` 인덱스 생성
- [x] AllocationEntity에 `updatedAt` 필드 및 `@PrePersist/@PreUpdate` 추가

### Application Layer
- [x] `StatusStreamUseCase` 인터페이스 작성
- [x] `LoadAllocationStatusPort.loadAllocationStatusesSince()` 메서드 추가
- [x] `RdbStatusStreamService` 구현 (SSE + @Scheduled)

### Adapter Layer
- [x] `AllocationPersistenceAdapter.loadAllocationStatusesSince()` 구현
- [x] `AllocationRepository.findByMatchIdAndSeatIdInAndUpdatedAtAfter()` 추가
- [x] `SiteController.getSeatStatusStream()` SSE 엔드포인트 추가

### Configuration
- [x] `@EnableScheduling` 추가 (Application 클래스)

### Documentation
- [x] 관련 규칙 문서 업데이트 (status stream 용어 통일)
