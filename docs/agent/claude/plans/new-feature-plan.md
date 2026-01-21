Implement the following plan:

  # Admin Role 및 Match Open 기능 구현

  ## 요구사항
  1. User 테이블에 role 추가 ('USER', 'ADMIN')
  2. Match 테이블에 open 상태 추가 (오픈 이후에만 좌석 선택/예매 가능)
  3. Admin 기능: 경기 생성, 수정, 삭제, 오픈 (오픈 시 allocation pre-populate)

  ---

  ## 설계 결정

  ### 1. 인가(Authorization) 방식
  **선택: Service Layer에서 Role 체크** (Spring Security 미사용)
  - 현재 Spring Security 미적용 상태
  - 단순한 role enum + 서비스 레이어 체크로 충분
  - 추후 Spring Security 마이그레이션 가능

  ### 2. Match 상태 설계
  **선택: Enum `MatchStatus`** (boolean 대신)
  - DRAFT: 생성됨, 미오픈
  - OPEN: 예매 가능
  - 확장성 (CLOSED, CANCELLED 추가 가능)

  ### 3. 좌석 Pre-populate
  **방식: `SeatRepository.findAll()`로 전체 좌석 조회**
  - 현재 단일 경기장 구조 가정
  - 추후 다중 경기장 시 Area에 venueId 추가

  ### 4. Admin 부여 방식
  **방식: 초기 DB에 admin@email.com / admin 계정 생성**
  - API로 role 변경 기능 없음
  - 운영자가 DB에서 직접 관리

  ### 5. 기존 데이터 마이그레이션
  - 기존 User → `USER` role (default)
  - 기존 Match → `DRAFT` status (오픈 필요)

  ---

  ## 수정 파일 목록

  ### User Module
  | 파일 | 작업 |
  |------|------|
  | `UserRole.java` (신규) | USER, ADMIN enum 생성 |
  | `User.java` | role 필드, isAdmin() 추가 |
  | `UserEntity.java` | role 컬럼 추가 |
  | `UserResponse.java` | role 포함 |

  ### Match Module
  | 파일 | 작업 |
  |------|------|
  | `MatchStatus.java` (신규) | DRAFT, OPEN enum 생성 |
  | `Match.java` | status 필드, isOpen() 추가 |
  | `MatchEntity.java` | status 컬럼 추가 |
  | `MatchResponse.java` | status 포함 |
  | `MatchService.java` | create, update, delete, open 구현 |
  | `AdminMatchController.java` (신규) | Admin API endpoints |
  | `CreateMatchUseCase.java` (신규) | 생성 인터페이스 |
  | `UpdateMatchUseCase.java` (신규) | 수정 인터페이스 |
  | `DeleteMatchUseCase.java` (신규) | 삭제 인터페이스 |
  | `OpenMatchUseCase.java` (신규) | 오픈 인터페이스 |

  ### Allocation Module
  | 파일 | 작업 |
  |------|------|
  | `Allocation.java` | availableForMatch(matchId, seatId) 팩토리 메서드 |
  | `RecordAllocationPort.java` | saveAll() 메서드 추가 |
  | `AllocationPersistenceAdapter.java` | bulk save 구현 |
  | `AllocationService.java` | match.isOpen() 체크 추가 |

  ### Site Module
  | 파일 | 작업 |
  |------|------|
  | `LoadSeatPort.java` | loadAllSeats() 추가 |
  | `SitePersistenceAdapter.java` | loadAllSeats 구현 |

  ### Exception
  | 파일 | 작업 |
  |------|------|
  | `UnauthorizedException.java` (신규) | 권한 없음 예외 |
  | `MatchNotOpenException.java` (신규) | 미오픈 경기 접근 예외 |
  | `MatchAlreadyOpenException.java` (신규) | 중복 오픈 방지 예외 |

  ---

  ## 구현 순서

  ### Phase 1: User Role
  1. `UserRole.java` enum 생성
  2. `User.java` - role 필드 추가
  3. `UserEntity.java` - role 컬럼 추가
  4. `UserResponse.java` 수정

  ### Phase 2: Match Status
  1. `MatchStatus.java` enum 생성
  2. `Match.java` - status 필드, isOpen() 추가
  3. `MatchEntity.java` - status 컬럼 추가
  4. `MatchResponse.java` 수정
  5. 예외 클래스 생성

  ### Phase 3: Admin Match CRUD
  1. Use Case 인터페이스 및 Command 생성
  2. `RecordMatchPort.java` - delete 메서드 추가
  3. `MatchPersistenceAdapter.java` - delete 구현
  4. `MatchService.java` - CRUD 구현

  ### Phase 4: Match Open + Allocation Pre-populate
  1. `LoadSeatPort.java` - loadAllSeats() 추가
  2. `SitePersistenceAdapter.java` - 구현
  3. `Allocation.java` - availableForMatch() 추가
  4. `RecordAllocationPort.java` - saveAll() 추가
  5. `AllocationPersistenceAdapter.java` - bulk save 구현
  6. `MatchService.openMatch()` - 오픈 + pre-populate 구현

  ### Phase 5: Admin Controller
  1. `UnauthorizedException.java` 생성
  2. `AdminMatchController.java` 생성
  - `POST /api/admin/matches` - 생성
  - `PUT /api/admin/matches/{matchId}` - 수정
  - `DELETE /api/admin/matches/{matchId}` - 삭제
  - `POST /api/admin/matches/{matchId}/open` - 오픈

  ### Phase 6: 기존 서비스에 Open 체크 추가
  1. `AllocationService.allocateSeat()` - match open 체크
  2. `ReservationService` - match open 체크

  ---

  ## 초기 데이터 (data.sql)
  ```sql
  -- Admin 계정 생성
  INSERT INTO users (email, password, role) VALUES ('admin@email.com', 'admin', 'ADMIN');
  ```

  ---

  ## 검증
  1. `./gradlew test` - 기존 테스트 통과 확인
  2. Admin API 테스트 - 생성/수정/삭제/오픈
  3. 미오픈 경기 좌석 선점 시도 → 에러 확인
  4. 오픈 후 allocation 레코드 생성 확인