# Walkthrough - Admin Role & Match Open 기능 구현

## Description
관리자 역할 및 경기 오픈 기능 구현 문서. 사용자 역할(USER/ADMIN) 추가, 경기 상태(DRAFT/OPEN) 관리, 관리자 CRUD API 구현.

## 설계 결정

### 1. 인가(Authorization) 방식
- **Service Layer에서 Role 체크** (Spring Security 미사용)
- 현재 Spring Security 미적용 상태에서 단순한 role enum + 서비스 레이어 체크로 구현
- 추후 Spring Security 마이그레이션 가능

### 2. Match 상태 설계
- **Enum `MatchStatus`** (boolean 대신)
- `DRAFT`: 생성됨, 미오픈
- `OPEN`: 예매 가능
- 확장성 (CLOSED, CANCELLED 추가 가능)

### 3. 좌석 Pre-populate
- `SeatRepository.findAll()`로 전체 좌석 조회 후 Allocation 레코드 일괄 생성
- 현재 단일 경기장 구조 가정

---

## 구현 내용

### Phase 1: User Role

| 파일 | 작업 |
|------|------|
| `UserRole.java` (신규) | USER, ADMIN enum 생성 |
| `User.java` | role 필드, isAdmin() 메서드 추가 |
| `UserEntity.java` | role 컬럼 추가 (@Enumerated) |
| `UserResponse.java` | role 필드 포함 |

```java
// UserRole.java
public enum UserRole {
    USER,
    ADMIN
}

// User.java
public boolean isAdmin() {
    return this.role == UserRole.ADMIN;
}
```

### Phase 2: Match Status

| 파일 | 작업 |
|------|------|
| `MatchStatus.java` (신규) | DRAFT, OPEN enum 생성 |
| `Match.java` | status 필드, isOpen(), open(), update() 추가 |
| `MatchEntity.java` | status 컬럼 추가 |
| `MatchResponse.java` | status 필드 포함 |
| `MatchNotOpenException.java` (신규) | 미오픈 경기 접근 예외 |
| `MatchAlreadyOpenException.java` (신규) | 중복 오픈 방지 예외 |
| `UnauthorizedException.java` (신규) | 권한 없음 예외 |

```java
// MatchStatus.java
public enum MatchStatus {
    DRAFT,
    OPEN
}

// Match.java
public boolean isOpen() {
    return this.status == MatchStatus.OPEN;
}

public void open() {
    if (this.status == MatchStatus.OPEN) {
        throw new MatchAlreadyOpenException(this.id);
    }
    this.status = MatchStatus.OPEN;
}
```

### Phase 3: Admin Match CRUD

| 파일 | 작업 |
|------|------|
| `CreateMatchCommand.java` (신규) | 생성 커맨드 |
| `UpdateMatchCommand.java` (신규) | 수정 커맨드 |
| `CreateMatchUseCase.java` (신규) | 생성 인터페이스 |
| `UpdateMatchUseCase.java` (신규) | 수정 인터페이스 |
| `DeleteMatchUseCase.java` (신규) | 삭제 인터페이스 |
| `OpenMatchUseCase.java` (신규) | 오픈 인터페이스 |
| `RecordMatchPort.java` | delete() 메서드 추가 |
| `MatchPersistenceAdapter.java` | delete 구현 |
| `MatchService.java` | CRUD 구현 |

```java
// MatchService.java - openMatch
@Override
@Transactional
public Match openMatch(final Long userId, final Long matchId) {
    User user = loadUserPort.loadById(userId);
    if (!user.isAdmin()) {
        throw new UnauthorizedException("관리자만 경기를 오픈할 수 있습니다.");
    }

    Match match = loadMatchPort.loadById(matchId);
    match.open();

    List<Seat> allSeats = loadSeatPort.loadAllSeats();
    List<Allocation> allocations = allSeats.stream()
            .map(seat -> Allocation.availableForMatch(matchId, seat.getId()))
            .toList();
    recordAllocationPort.saveAll(allocations);

    return recordMatchPort.save(match);
}
```

### Phase 4: Match Open + Allocation Pre-populate

| 파일 | 작업 |
|------|------|
| `LoadSeatPort.java` | loadAllSeats() 추가 |
| `SitePersistenceAdapter.java` | loadAllSeats 구현 |
| `Allocation.java` | availableForMatch() 팩토리 메서드 추가 |
| `RecordAllocationPort.java` | saveAll() 추가 |
| `AllocationPersistenceAdapter.java` | bulk save 구현 |

```java
// Allocation.java
public static Allocation availableForMatch(final Long matchId, final Long seatId) {
    return Allocation.builder()
            .matchId(matchId)
            .seatId(seatId)
            .status(AllocationStatus.AVAILABLE)
            .build();
}
```

### Phase 5: Admin Controller

| 파일 | 작업 |
|------|------|
| `AdminController.java` (신규) | Admin API endpoints |
| `CreateMatchRequest.java` (신규) | 생성 요청 DTO |
| `UpdateMatchRequest.java` (신규) | 수정 요청 DTO |

```java
// AdminController.java
@Tag(name = "Admin", description = "관리자 API")
@RestController
@RequestMapping("/api/admin/matches")
@RequiredArgsConstructor
public class AdminController {
    // POST /api/admin/matches - 경기 생성
    // PUT /api/admin/matches/{matchId} - 경기 수정
    // DELETE /api/admin/matches/{matchId} - 경기 삭제
    // POST /api/admin/matches/{matchId}/open - 경기 오픈
}
```

### Phase 6: Open 체크 추가

| 파일 | 작업 |
|------|------|
| `AllocationService.java` | match.isOpen() 체크 추가 |
| `ReservationService.java` | match.isOpen() 체크 추가 |

```java
// AllocationService.allocateSeat() 내
Match match = loadMatchPort.loadById(matchId);
if (!match.isOpen()) {
    throw new MatchNotOpenException(matchId);
}
```

---

## 테스트 구현

### Acceptance Test (Cucumber BDD)

| 파일 | 설명 |
|------|------|
| `admin.feature` | 관리자 기능 시나리오 정의 |
| `AdminTestRunner.java` | @admin 태그 테스트 실행기 |
| `AdminClient.java` | Admin API 호출 클라이언트 |
| `AdminSteps.java` | Step definitions |
| `AcceptanceTestConfig.java` | AdminClient 빈 등록 |

### 테스트 시나리오 (admin.feature)

```gherkin
@admin
Feature: 관리자 경기 관리

  Scenario: 관리자가 새 경기를 생성한다.
  Scenario: 관리자가 경기 정보를 수정한다.
  Scenario: 관리자가 경기를 삭제한다.
  Scenario: 관리자가 경기를 오픈한다.
  Scenario: 이미 오픈된 경기를 다시 오픈하면 실패한다.
  Scenario: 일반 사용자는 경기를 생성할 수 없다.
  Scenario: 오픈된 경기만 좌석 선점이 가능하다.
```

### 기존 테스트 수정

| 파일 | 작업 |
|------|------|
| `MatchSteps.java` | 경기 생성 후 자동 오픈 처리 |
| `SeatAllocationConcurrencyTest.java` | setUp에서 match OPEN 상태로 변경 |

---

## API 명세

### Admin Match API

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/api/admin/matches?userId={id}` | 경기 생성 |
| PUT | `/api/admin/matches/{matchId}?userId={id}` | 경기 수정 |
| DELETE | `/api/admin/matches/{matchId}?userId={id}` | 경기 삭제 |
| POST | `/api/admin/matches/{matchId}/open?userId={id}` | 경기 오픈 |

### Request/Response

```json
// POST /api/admin/matches - Request
{
  "stadium": "잠실 야구장",
  "homeTeam": "LG 트윈스",
  "awayTeam": "두산 베어스",
  "dateTime": "2026-05-01T18:30:00"
}

// Response
{
  "status": "success",
  "data": {
    "id": 1,
    "stadium": "잠실 야구장",
    "homeTeam": "LG 트윈스",
    "awayTeam": "두산 베어스",
    "dateTime": "2026-05-01T18:30:00",
    "status": "DRAFT"
  }
}
```

---

## 검증 결과

### 테스트 실행
- `./gradlew test` - 전체 테스트 통과
- `./gradlew test --tests "*AdminTestRunner"` - Admin 인수 테스트 통과

### 검증 항목
- [x] Admin 계정으로 경기 생성/수정/삭제/오픈 가능
- [x] 일반 사용자가 Admin API 접근 시 403 Forbidden
- [x] DRAFT 상태 경기에 좌석 선점 시 400 Bad Request
- [x] 경기 오픈 시 모든 좌석에 대한 Allocation 레코드 생성
- [x] 이미 오픈된 경기 재오픈 시 409 Conflict
