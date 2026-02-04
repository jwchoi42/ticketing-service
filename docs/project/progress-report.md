# 티켓팅 서비스 - 현재 개발 상황 정리

## 1. 프로젝트 배경

야구 좌석 예매 시 동시 접속자가 몰리면서 "이미 선점된 좌석입니다" 메시지를 반복적으로 마주치는 불쾌한 경험을 줄이기 위한 티켓팅 서비스.

핵심 문제의식: **좌석 선점 실패 경험을 최소화**하는 시스템 설계

---

## 2. 기술 스택

| 영역 | 기술 |
|------|------|
| Backend | Spring Boot 3.5 (Java 17), Spring Data JPA, QueryDSL |
| Frontend | Next.js 16 (TypeScript), Tailwind CSS, Zustand, React Query |
| DB/Cache | PostgreSQL 17, Redis 7, Caffeine (로컬 캐시) |
| 테스트 | Cucumber (BDD), JUnit 5, Testcontainers, K6 (부하 테스트) |
| 인프라 | Docker Compose, Nginx, Terraform, AWS CodeDeploy |
| 모니터링 | Prometheus, Grafana |
| CI/CD | GitHub Actions |

---

## 3. 아키텍처

- **헥사고날 아키텍처** (Ports & Adapters) 적용
- 도메인별 독립 패키지: `match`, `site`, `reservation`, `payment`, `ticketing`, `user`
- 각 도메인은 `adapter(in/out)` → `application(port/service)` → `domain` 계층 구조

---

## 4. 전체 워크플로우 (구현된 흐름)

```
1. [관리자] 경기 생성 (DRAFT) → 경기 오픈 (OPEN, 좌석 Allocation 사전 생성)
2. [사용자] SSE 연결 → 실시간 좌석 현황 수신
3. [사용자] 좌석 선점 (Hold, TTL 10분, 비관적 락)
4. [사용자] 예약 생성 (선점된 좌석 기반)
5. [사용자] 결제 요청 → 결제 확인
6. [시스템] 결제 완료 → 예약 확정 → 좌석 OCCUPIED
```

---

## 5. 기능별 완성도

### 범례
- ✅ 완성 (프론트 + 백엔드 + 테스트)
- 🔧 백엔드만 완성 (프론트 미연동 또는 부분 연동)
- ⚠️ 부분 구현
- ❌ 미구현 (설계만 존재하거나 아예 없음)

---

### 5-1. 경기(Match) 관리

| 기능 | 백엔드 | 프론트엔드 | 테스트 | 종합 |
|------|--------|-----------|--------|------|
| 경기 생성/수정/삭제 (관리자) | ✅ 팩토리 메서드 + 불변 빌더 | ✅ 모달 폼 (MatchFormSheet) | ✅ BDD Cucumber | ✅ |
| 경기 목록 조회 | ✅ JpaRepository.findAll() | ✅ SSR (force-dynamic) + 스켈레톤 (animate-pulse) | ✅ BDD Cucumber | ✅ |
| 경기 단건 조회 | ✅ loadById + MatchNotFoundException | ✅ React Query (useQuery) | ✅ BDD Cucumber | ✅ |
| 경기 오픈 (DRAFT→OPEN) | ✅ 비관적 락 + 전좌석 Allocation 일괄 생성 | ✅ 관리자 버튼 + 확인 다이얼로그 | ✅ BDD Cucumber | ✅ |

### 5-2. 좌석 구조(Site Hierarchy)

| 기능 | 백엔드 | 프론트엔드 | 테스트 | 종합 |
|------|--------|-----------|--------|------|
| 4단계 계층 조회 (Area→Section→Block→Seat) | ✅ 계층별 Port 분리 조회 | ✅ 캐스케이딩 셀렉터 (React Query enabled 체이닝) | ✅ BDD Cucumber | ✅ |

- Area: INFIELD, OUTFIELD
- Section: HOME(연고), AWAY(원정), LEFT(좌측), RIGHT(우측)
- Block → Seat: 행번호 + 좌석번호

### 5-3. 좌석 배정(Allocation) — 핵심 기능

| 기능 | 백엔드 | 프론트엔드 | 테스트 | 종합 |
|------|--------|-----------|--------|------|
| 좌석 선점 (AVAILABLE→HOLD) | ✅ 비관적 락 (`PESSIMISTIC_WRITE`) + TTL 10분 | ✅ 낙관적 업데이트 (즉시 UI 반영) | ✅ BDD + CountDownLatch 동시성 | ✅ |
| 좌석 해제 (HOLD→AVAILABLE) | ✅ `isHeldBy()` 본인 검증 | ✅ 선점 해제 버튼 | ✅ BDD Cucumber | ✅ |
| 좌석 확정 (HOLD→OCCUPIED) | ✅ 복수 좌석 루프 + 개별 락 | ✅ 확인 다이얼로그 (ConfirmDialog) | ✅ BDD Cucumber | ⚠️ |
| 실시간 좌석 현황 (SSE) | ✅ @Scheduled 1초 폴링, 스냅샷+델타 | ✅ EventSource 자동 재연결, Map 기반 상태 | ✅ BDD Cucumber | ✅ |
| 동시성 제어 | ✅ `SELECT ... FOR UPDATE` (행 수준 락) | — | ✅ 2명/10명 CountDownLatch 동시 선점 | ✅ |
| 만료 좌석 자동 회수 (배치잡) | ⚠️ 반응적 회수만 (선점 시도 시 만료 체크) | — | — | ⚠️ |

> **동시성 제어 상세**: 같은 좌석에 10명이 동시 접근해도 정확히 1명만 성공하도록 검증됨

### 5-4. 예약(Reservation)

| 기능 | 백엔드 | 프론트엔드 | 테스트 | 종합 |
|------|--------|-----------|--------|------|
| 예약 생성 (선점 좌석 기반) | ✅ 좌석 락 검증 + TTL 만료 체크 | ❌ FE 미호출 + seatIds 누락 | ✅ BDD Cucumber | ⚠️ |
| 예약 목록 조회 (사용자별) | ✅ findByUserId 조회 | ✅ React Query + 상태 필터링 + 빈 상태 UI | — | ✅ |
| 예약 단건 조회 | ✅ loadById + Allocation 조인 | ✅ 상세 카드 UI | — | ✅ |
| 예약 취소 | ❌ | ❌ | ❌ | ❌ |

### 5-5. 결제(Payment)

| 기능 | 백엔드 | 프론트엔드 | 테스트 | 종합 |
|------|--------|-----------|--------|------|
| 결제 요청 (PENDING) | ✅ 예약 상태 검증 + Payment 엔티티 생성 | ❌ 결제 페이지 미존재 | ✅ BDD Cucumber | ❌ |
| 결제 확인 → 예약 확정 | ✅ TicketingService 3도메인 오케스트레이션 | ❌ 결제 페이지 미존재, 흐름 단절 | ✅ BDD Cucumber | ❌ |
| PG 연동 (토스페이먼츠) | ⚠️ Mock 어댑터 (시뮬레이션) | ❌ | — | ⚠️ |
| 환불 | ❌ | ❌ | ❌ | ❌ |

> **결제 게이트웨이**: `TossPaymentGatewayAdapter`가 존재하나 실제 PG 연동 없이 내부 시뮬레이션만 수행

### 5-6. 사용자(User)

| 기능 | 백엔드 | 프론트엔드 | 테스트 | 종합 |
|------|--------|-----------|--------|------|
| 회원가입 | ✅ `existsByEmail()` 중복 검증 | ✅ HTML5 유효성 검사 + 토스트 알림 | ✅ BDD Cucumber | ✅ |
| 로그인 | ✅ `loadByEmail()` + `matchPassword()` | ✅ Zustand 상태 관리 | ✅ BDD Cucumber | ✅ |
| 프로필 / 로그아웃 | — | ✅ Zustand 상태 초기화 | — | ⚠️ |
| 비밀번호 해싱 | ❌ 평문 비교 | — | — | ❌ |
| JWT/세션 기반 인증 | ❌ | ❌ (userId 파라미터 전달 방식) | — | ❌ |
| Spring Security | ❌ | — | — | ❌ |

### 5-7. 프론트엔드 전용

| 기능 | 상태 | 비고 |
|------|------|------|
| 모바일 퍼스트 레이아웃 (480px) | ✅ | Tailwind 반응형, 하단 네비게이션, 헤더 |
| SSE 실시간 훅 (`useSSE`) | ✅ | EventSource, 자동 재연결 (최대 3회), 연결 상태 추적 |
| 좌석 선택 UI (그리드 + 애니메이션) | ✅ | ResizeObserver 반응형 스케일링, cubic-bezier 슬라이드 애니메이션 |
| 관리자 기능 (경기 CRUD) | ✅ | `user.role === 'ADMIN'` 분기, MatchFormSheet 모달 |
| 티켓(구매 완료) 페이지 | ⚠️ 껍데기만 존재 | API 연동 없음 |
| 결제 전용 페이지 | ❌ | 예약 페이지에서 바로 호출 |
| 좌석 가격 표시 | ⚠️ 하드코딩 (170,000원) | 가격 체계 미설계 |

---

## 6. 백엔드 작동 상태

### 기술적 상세 (Section 5 기능표에 없는 내용)

- **동시성 제어**: `SELECT ... FOR UPDATE` 행 수준 비관적 락. 같은 좌석에 10명 동시 접근 시 정확히 1명만 성공 (CountDownLatch 검증)
- **SSE 실시간 방송**: `ConcurrentHashMap` + `CopyOnWriteArrayList` 기반 스레드 안전 구현. @Scheduled 1초 폴링, 스냅샷+델타 전송
- **크로스 도메인 오케스트레이션**: `TicketingService`가 결제→예약→좌석 확정 3도메인 흐름 조율
- **예외 처리**: 22개 도메인 예외 클래스 + 글로벌/도메인별 핸들러 완비
- **헥사고날 아키텍처**: 모든 도메인이 `adapter(in/out)` → `application(port/service)` → `domain` 계층 구조. Port 인터페이스 완전 구현

### 부하 테스트 결과

**채택 전략**: `denormalized + collapsing` (비정규화 쿼리 + 동일 요청 병합)

#### 성능 목표

| 구분 | 항목 | 기준 |
|------|------|------|
| **테스트 임계값** (K6 Threshold) | p95 응답 시간 | < 1,000ms |
| | p99 응답 시간 | < 3,000ms |
| | 에러율 | < 1% |
| **운영 목표** (TPS) | 처리량 | > 500 TPS |

#### 4전략 비교 결과 (VU 1,000, 5회 측정 중앙값 기준)

| 전략 | TPS | avg (ms) | p95 (ms) | 에러율 (%) |
|------|-----|----------|----------|-----------|
| normalized + none | 355 | 1,112 | 2,008 | 0.010 |
| normalized + collapsing | 566 | 319 | 1,009 | 0.008 |
| denormalized + none | 523 | 429 | 1,322 | 0.011 |
| **denormalized + collapsing** | **611** | **224** | **623** | **0.008** |

#### 채택 전략 실측 결과 (평균값)

| 지표 | 목표 | 실측 | 달성 |
|------|------|------|------|
| TPS | > 500 | 573 | O |
| avg 응답 시간 | < 1,000ms | 292ms | O |
| p95 응답 시간 | < 1,000ms | 1,095ms | X (초과) |
| 에러율 | < 1% | 0.008% | O |

> **p95 미달 분석**: 평균 5회 측정 중 p95가 1,095ms로 임계값(1,000ms)을 소폭 초과. 중앙값 기준으로는 623ms로 통과. 분산이 큰 원인은 cold start 및 GC 영향으로 추정.
>
> 상세 결과: [`docs/technical-articles/allocation-status/load-test/result-summarys/20260201-171759-summary.md`](../technical-articles/allocation-status/load-test/result-summarys/20260201-171759-summary.md)

### 미구현 / 제한 사항
- **만료 좌석 자동 회수**: 능동적 배치잡 없음 (다른 사용자가 해당 좌석 선점 시도 시에만 만료 체크)
- **인증/인가**: Spring Security, JWT, 세션 관리 없음 — 요청 파라미터로 userId 전달
- **비밀번호 해싱**: 평문 비교
- **결제 PG**: Mock 어댑터 (토스페이먼츠 시뮬레이션)

---

## 7. 프론트엔드 기술 상세 (Section 5 기능표에 없는 내용)

- **SSE 훅 (`useSSE`)**: EventSource 기반 자동 재연결 (최대 3회), 연결 상태 추적, Map 기반 상태 관리
- **React 최적화**: memo, useCallback, useMemo 적극 활용
- **서버 상태 관리**: Axios 인터셉터 + React Query (캐시, 재시도, enabled 체이닝)
- **클라이언트 상태 관리**: Zustand (인증 상태, UI 상태)
- **모바일 퍼스트**: 480px 기준 반응형 레이아웃, ResizeObserver 기반 좌석 그리드 스케일링
- **좌석 선택 UI**: cubic-bezier 슬라이드 애니메이션, 낙관적 업데이트 (즉시 UI 반영)
- **TypeScript**: 전체 타입 안전성 확보

---

## 8. 테스트 현황

| 종류 | 내용 | 상태 |
|------|------|------|
| BDD (Cucumber) | 좌석 선점/해제/확인, 예약 생성, 결제, SSE 등 9개 Feature | ✅ 전체 구현 |
| 동시성 테스트 | 2명/10명 동시 같은 좌석 선점 → 정확히 1명만 성공 | ✅ 구현 |
| 부하 테스트 (K6) | 좌석 배정 동시성, SSE 대량 연결, E2E 플로우 | ✅ 스크립트 + 실행 |
| 테스트 인프라 | Testcontainers (PostgreSQL, Redis), 자동 DB 클린업 | ✅ 구현 |

---

## 9. 인프라/운영

- Docker Compose: PostgreSQL, Redis, Prometheus, Grafana, Nginx
- GitHub Actions CI/CD 파이프라인
- AWS CodeDeploy 배포 설정
- Terraform 인프라 코드
- Grafana 대시보드 (Spring Boot 메트릭)
- Swagger UI API 문서 자동 생성

---

## 10. 기획이 필요한 부분 (우선순위별)

> 핵심 역량 우선순위: 문제 해결 과정 → 성능 최적화 → 아키텍처 설계력 → 테스트 → 인프라

### P0: 데모/포트폴리오에 필수

| 항목 | 현재 상태 | 기획 필요 사항 | 근거 |
|------|----------|---------------|------|
| **E2E 사용자 여정 완결** | 결제 후 화면 없음, 티켓 페이지 껍데기 | 결제 → 티켓 확인까지 흐름 완성 | 데모 완성도를 좌우. 미완성 인상 방지 |
| **만료 좌석 능동적 회수** | 반응적 회수만 (선점 시도 시에만 만료 체크) | 능동적 회수 배치잡 + SSE 상태 반영 | 프로젝트 핵심 주장("선점 실패 최소화")과의 일관성 |
| **MVP 범위 정의** | 미정의 | In-scope / Out-of-scope / 완성 기준 | → Section 12에 정의 |

### P1: 완성도를 높이는 항목

| 항목 | 현재 상태 | 기획 필요 사항 | 근거 |
|------|----------|---------------|------|
| **인증 (JWT)** | userId 파라미터 전달 | JWT 토큰 기반 인증/인가 | "왜 안 했나?" 면접 질문 방어 + API 보안 최소 수준 |
| **가격 체계** | 170,000원 하드코딩 | Block 단위 가격 시딩 + 서버 금액 계산 | 결제 도메인 정합성 확보 |
| **결제 UI 플로우** | 결제 전용 페이지 미존재 | 결제 진행 화면 + 결과 확인 | 사용자 인지 경험 개선 |

### P2: 시간 여유가 있을 때

| 항목 | 현재 상태 | 기획 필요 사항 | 근거 |
|------|----------|---------------|------|
| **예약 취소/환불** | 미구현 | 취소 정책, 환불 프로세스 | 데모에서 필수는 아님 |
| **PG 실연동** | Mock 시뮬레이션 | 토스페이먼츠 실연동 범위 | Mock으로 충분히 데모 가능 |
| **비밀번호 해싱** | 평문 비교 | BCrypt 적용 | 구현 간단, 보안 인식 어필 |
| **알림 시스템** | 없음 | 예매 확정/취소 알림 채널 | 부가 기능 |
| **페르소나/유스케이스** | 핵심 문제의식만 정의 | 구체적 사용자 시나리오 | 포트폴리오에 큰 영향 없음 |

---

## 11. 사용자 흐름 검토 결과

전체 사용자 흐름(좌석 선택 → 확정 → 예약 → 결제 → 티켓)을 검토한 결과, 설계 의도와 실제 구현 사이에 다수의 불일치가 발견되었다.

### 올바른 흐름 vs 현재 흐름

**설계 의도 (TicketingService 기준)**
```
1. 좌석 선점      AVAILABLE → HOLD (TTL 10분)
2. 예약 생성      Reservation PENDING 생성 + Allocation에 reservationId 연결 (상태 HOLD 유지)
3. 결제 요청      Payment PENDING 생성
4. 결제 확인      Payment PAID → Reservation CONFIRMED → Allocation OCCUPIED
```

**현재 실제 흐름 (프론트엔드 기준)**
```
1. 좌석 선점      AVAILABLE → HOLD                               ✅
2. 좌석 확정      HOLD → OCCUPIED                                ❌ (너무 이른 OCCUPIED 전환)
3. /reservation   예약 조회 → 없음                                ❌ (예약 생성 API 호출 누락)
4. 결제 시도      /payment/{id} → 404                            ❌ (결제 페이지 미존재)
```

### 🔴 CRITICAL — 흐름이 끊기는 치명적 문제

#### C1. confirmSeats가 바로 OCCUPIED로 전환 → 예약 생성 불가
- **위치**: `AllocationService.confirmSeats()` (line 153)
- **현상**: 좌석 확정 시 `allocation.occupy()` 호출 → 상태가 `HOLD → OCCUPIED`
- **문제**: `ReservationService.createReservation()`은 `isHeldBy(userId)` 검증 → `HOLD` 상태만 통과
- **결과**: 확정 후 예약을 만들 수 없는 논리적 불가능 상태
- **설계 의도와의 충돌**: `TicketingService`는 결제 완료 후에야 OCCUPIED로 전환하도록 설계됨

#### C2. 프론트엔드에서 예약 생성 API를 호출하지 않음
- **위치**: `frontend/app/matches/[matchId]/page.tsx` (line 214)
- **현상**: `confirmSeats` 성공 후 `router.push('/reservation')`만 실행
- **문제**: `reservationApi.createReservation()`이 정의만 되어 있고 어디서도 호출되지 않음
- **결과**: 예약 페이지에 도착해도 예약이 없어 "No pending reservations" 표시

#### C3. 결제 페이지 미존재
- **위치**: `frontend/app/reservation/page.tsx` (line 37-39)
- **현상**: "Pay Now" 클릭 시 `/payment/${reservationId}`로 라우팅
- **문제**: `frontend/app/payment/[id]/page.tsx` 파일 자체가 존재하지 않음
- **결과**: 404 에러 → 결제 플로우 완전 단절

### 🟠 HIGH — 데이터 불일치 또는 잘못된 구현

#### H1. 프론트엔드 Reservation 상태 enum 불일치
- **위치**: `frontend/lib/api/reservation.ts` (line 9)
- **현상**: FE 정의 `'PENDING' | 'PAID' | 'CANCELLED'`
- **실제 BE**: `PENDING`, `CONFIRMED`, `CANCELLED` (`ReservationStatus.java`)
- **결과**: 결제 완료 후 BE가 `CONFIRMED`를 반환해도 FE에서 인식 불가

#### H2. createReservation API 요청 필드 누락
- **위치**: `frontend/lib/api/reservation.ts` (line 13-22)
- **현상**: `{ matchId, userId }`만 전송
- **실제 BE 요구**: `{ userId, matchId, seatIds }` (`CreateReservationRequest.java`)
- **결과**: 호출하더라도 `seatIds` 누락으로 예약 생성 실패

#### H3. PaymentService ↔ TicketingService 로직 중복
- **위치**: `PaymentService.confirmPayment()` vs `TicketingService.confirmPaymentAndFinalizeReservation()`
- **현상**: 두 클래스가 동일한 결제 확인 + 예약 확정 + 좌석 OCCUPIED 로직을 각각 구현
- **문제**: `PaymentController`는 `TicketingService`를 사용하므로 `PaymentService.confirmPayment()`는 데드코드

#### H4. confirmSeats 내 하드코딩된 userId == 1 우회 조건
- **위치**: `AllocationService.confirmSeats()` (line 152)
- **현상**: `(userId == 1 && allocation.getUserId() == null)` 조건으로 검증 우회
- **문제**: 개발/테스트용 코드가 프로덕션에 남아있음

#### H5. 만료 좌석 능동적 회수 없음
- **위치**: `AllocationService.allocateSeat()` (line 82-90)
- **현상**: 만료된 HOLD 좌석은 다른 유저가 선점 시도할 때만 회수됨
- **문제**: 아무도 해당 좌석을 시도하지 않으면 좀비 HOLD 상태 영구 지속
- **영향**: SSE로 다른 유저에게 해당 좌석이 계속 "Reserved"로 표시됨

### 🟡 MEDIUM — 미완성 또는 부정확한 구현

#### M1. 티켓 페이지 API 연동 없음
- **위치**: `frontend/app/tickets/page.tsx` (전체)
- **현상**: 하드코딩된 "No tickets yet" 메시지만 표시
- **문제**: CONFIRMED 상태의 예약을 조회하는 API 호출 없음

#### M2. 가격 체계 부재
- **위치**: Match 도메인 전체, `frontend/app/matches/[matchId]/components/held-seat-item.tsx` (line 27)
- **현상**: Match 도메인에 가격 필드 없음, FE에서 170,000원 하드코딩
- **문제**: `PaymentService.requestPayment()`에 `amount`를 전달해야 하나 실제 가격 소스 없음

#### M3. 좌석 범례 용어 혼란
- **위치**: `frontend/app/matches/[matchId]/components/seat-grid.tsx` (line 205-229)
- **현상**: 범례에서 다른 유저의 HOLD = "Reserved", OCCUPIED = "Sold Out"
- **문제**: 일반적으로 "Reserved"는 예약 완료, "Sold Out"은 재고 소진 의미
  - 현재: HOLD(임시 선점) = "Reserved" 표시 → 사용자 혼란 가능

---

## 12. 프로젝트 목표와 MVP 정의

### 프로젝트 목적
- **유형**: 포트폴리오 / 취업용 프로젝트
- **핵심 문제의식**: 야구 좌석 예매 시 동시 접속자 폭주로 인한 "좌석 선점 실패 경험"을 최소화하는 시스템 설계

### 보여주고 싶은 핵심 역량 (우선순위 순)
1. **문제 해결 과정**: 동시성 문제를 비관적 락으로 해결, SSE 실시간 상태 공유로 선점 실패 최소화
2. **성능 최적화**: 4가지 전략 비교 테스트 → 최적 전략(`denormalized + collapsing`) 채택 → TPS 573 달성
3. **아키텍처 설계력**: 헥사고날 아키텍처, 도메인별 독립 패키지, 크로스 도메인 오케스트레이션
4. **테스트**: BDD Cucumber, 동시성 테스트(CountDownLatch), K6 부하 테스트
5. **인프라**: Docker Compose, CI/CD, Prometheus + Grafana 모니터링

### MVP In-scope (반드시 완성해야 하는 것)
- 회원가입/로그인
- 경기 목록 조회 + 관리자 경기 관리
- SSE 실시간 좌석 현황
- 좌석 선점/해제 (비관적 락 동시성 제어)
- 예약 생성 → 결제(Mock) → 티켓 확인 **(E2E 사용자 여정 완결)**
- 만료 좌석 능동적 회수
- 부하 테스트 결과 (성능 최적화 스토리)

### MVP Out-of-scope (의도적으로 하지 않는 것)
- PG 실연동 (Mock으로 도메인 오케스트레이션 검증 가능)
- 예약 취소/환불 (데모에서 필수 아님)
- 알림 시스템 (부가 기능)
- 대기열 시스템 (Phase 3)
- 시즌권/멤버십, 포인트/쿠폰 (Phase 3)

### Feature Roadmap 현재 위치
- 현재: **Phase 1 (MVP 완성) 진행 중** — 기본 사용자 여정 완결 단계
- 상세: [`docs/project/feature-roadmap.md`](./feature-roadmap.md) 참조

---

## 13. 의도적 제외 사항과 근거

### 인증/인가 (Spring Security / JWT)
- **현재 상태**: userId를 요청 파라미터로 전달
- **제외 근거**: 이 프로젝트의 핵심 역량은 동시성 제어, 실시간 SSE, 성능 최적화. 인증/인가는 보편적 기능으로 차별화 요소가 아님
- **향후 계획**: P1 우선순위로 JWT 기반 인증 구현 예정
- **면접 대비**: "핵심 역량(동시성 제어, 성능 최적화)에 집중하기 위해 의도적으로 후순위로 둠. JWT 구현 자체는 기술적 난이도가 높지 않으며, 프로젝트의 차별점은 인증이 아닌 좌석 배정 동시성 제어에 있음"

### PG 실연동 (토스페이먼츠)
- **현재 상태**: `TossPaymentGatewayAdapter` Mock 어댑터로 시뮬레이션
- **제외 근거**: Mock 어댑터만으로도 결제→예약 확정→좌석 OCCUPIED 오케스트레이션 검증 가능. PG 연동은 외부 API 호출일 뿐 도메인 로직이 아님
- **면접 대비**: "헥사고날 아키텍처의 Port/Adapter 패턴 덕분에 Mock ↔ 실 PG 전환이 Adapter 교체만으로 가능. 아키텍처의 이점을 보여주는 사례"

### 예약 취소/환불
- **현재 상태**: 미구현
- **제외 근거**: MVP 범위에서 "예매 → 확인"의 정방향 흐름이 우선. 취소는 역방향 흐름으로 데모 시나리오에서 필수가 아님
- **향후 계획**: P2 우선순위
- **면접 대비**: "정방향 흐름(예약→결제→확정)의 안정성을 먼저 확보한 후, 역방향 흐름(취소→환불→좌석 복원)을 추가하는 순서로 개발"

### 비밀번호 해싱
- **현재 상태**: 평문 비교
- **제외 근거**: 포트폴리오 데모 환경에서 실제 보안 위험이 없음
- **향후 계획**: P2 — BCrypt 적용 (구현 난이도 낮음)
- **면접 대비**: "보안 위험은 인지하고 있으며, 프로덕션 배포 시 BCrypt + Salt 적용 예정"

---

## 14. 알려진 기술 리스크

### 1. 만료 좌석 반응적 회수의 한계
- **리스크**: 만료된 HOLD 좌석이 다른 사용자가 선점 시도할 때만 회수됨. 시도하지 않으면 좀비 HOLD 상태 영구 지속
- **프로젝트 목표와의 모순**: "좌석 선점 실패 최소화"가 핵심인데, 만료된 좌석이 HOLD로 보이면 오히려 사용 가능한 좌석이 줄어드는 효과
- **영향**: SSE로 HOLD 상태가 브로드캐스트되어 다른 사용자가 해당 좌석 선택 자체를 회피
- **대응 계획**: P0 — 능동적 회수 배치잡(Scheduled) 구현 + SSE 상태 즉시 반영

### 2. 인증 부재로 인한 API 보안 한계
- **리스크**: userId를 파라미터로 전달하므로 다른 사용자의 좌석 선점/해제가 기술적으로 가능
- **영향 범위**: 모든 사용자 식별 기반 API
- **대응 계획**: P1 — JWT 기반 인증 구현

### 3. 성능 목표 부분 미달
- **리스크**: 채택 전략(denormalized + collapsing)의 p95가 평균 1,095ms로 임계값(1,000ms) 소폭 초과
- **원인 추정**: cold start, GC 영향 (중앙값 기준 623ms로는 통과)
- **대응 계획**: JVM 워밍업 최적화, 커넥션 풀 튜닝 검토

### 4. E2E 사용자 여정 단절
- **리스크**: 좌석 확정 후 예약 생성 API 미호출, 결제 페이지 미존재, 티켓 페이지 미연동
- **영향**: 포트폴리오 데모에서 미완성 인상
- **대응 계획**: P0 — Section 11의 CRITICAL 이슈(C1, C2, C3) 해결
