# Frontend Integration Technical Specification

본 문서는 프론트엔드(React) 개발을 위해 필요한 백엔드 기술 스택 및 API 명세를 정리한 가이드라인입니다.

## 1. 기술 스택 (Tech Stack)

본 프로젝트의 상세 기술 스택은 [tech-stack.md](./tech-stack.md) 문서를 참고하십시오.
- **주요 변경 사항**: Framework (Next.js), Styling (Tailwind CSS + Shadcn/UI), State Management (Zustand + React Query)

---

## 2. 공통 응답 구조 (Common Response Format)

모든 API 응답은 아래의 형식을 따릅니다.

```json
{
    "status": 200,      // HTTP 상태 코드 (성공 시 보통 200, 일부 201)
    "data": { ... }     // 실제 응답 데이터 객체
}
```

---

## 3. API 명세 (API Specification)

### 3.1. 사용자 (User) - `/api/users`
- **회원 가입**: `POST /sign-up`
    - Body: `{ "email": "...", "password": "..." }`
- **로그인**: `POST /log-in`
    - Body: `{ "email": "...", "password": "..." }`
    - Response: 성공 시 사용자 기본 정보(Role 포함)를 반환합니다.

### 3.2. 경기 (Match) - `/api/matches`
- **경기 목록 조회**: `GET /`
    - 예매 가능한 경기(팀 정보, 일시, 장소 등) 목록을 반환합니다.

### 3.3. 사이트 구조 (Site Hierarchy) - `/api/site`
- 경기(match)와 무관하게 경기장의 물리적 구조를 정의합니다.
1. **권역(Area) 목록 조회**: `GET /api/site/areas` (예: 내야, 외야)
2. **진영(Section) 목록 조회**: `GET /api/site/areas/{areaId}/sections` (예: 연고측, 원정측)
3. **구역(Block) 목록 조회**: `GET /api/site/sections/{sectionId}/blocks` (예: 101블록 ~ 125블록)
4. **구역 내 좌석 목록 조회**: `GET /api/site/blocks/{blockId}/seats` (좌석 번호 등 기본 메타데이터)

### 3.4. 실시간 좌석 상태 (Status Stream) - SSE
- **연결 엔드포인트**: `GET /api/matches/{matchId}/blocks/{blockId}/seats/events`
- **Event: `snapshot`** (연결 직후 전송)
    - 해당 구역의 모든 좌석에 대한 현재 상태 리스트를 전송합니다.
    ```json
    {
      "status": 200,
      "data": {
        "seats": [
          { "id": 1, "status": "AVAILABLE" },
          { "id": 2, "status": "HOLD" }
        ]
      }
    }
    ```
- **Event: `changes`** (변경 사항 발생 시 전송)
    - 상태가 변경된 좌석들만 리스트 형태로 전송합니다.
    ```json
    {
      "status": 200,
      "data": {
        "changes": [
          { "seatId": 1, "status": "HOLD" }
        ]
      }
    }
    ```

### 3.5. 좌석 점유 및 선택 (Allocation)
- **좌석 점유(Hold)**: `POST /api/matches/{matchId}/allocation/seats/{seatId}/hold`
    - Body: `{ "userId": 1 }`
- **좌석 반환(Release)**: `POST /api/matches/{matchId}/allocation/seats/{seatId}/release`
    - Body: `{ "userId": 1 }`
- **선점 완료(Complete)**: `POST /api/matches/{matchId}/allocation/seats/complete`
    - Body: `{ "userId": 1, "seatIds": [1, 2] }`

### 3.6. 예약 및 결제 (Reservation & Payment)
- **예약 생성**: `POST /api/reservations`
- **결제 요청**: `POST /api/payments/request`
- **결제 승인**: `POST /api/payments/confirm`
    
### 3.7. 관리자 (Admin) - `/api/admin`
- **경기 생성**: `POST /api/admin/matches`
    - Body: `{ "matchDate": "...", "homeTeam": "...", "awayTeam": "...", "stadiumName": "..." }`
- **경기 수정**: `PUT /api/admin/matches/{matchId}`
- **경기 삭제**: `DELETE /api/admin/matches/{matchId}`
- **경기 오픈 (예매 시작)**: `POST /api/admin/matches/{matchId}/open`
    - 경기 상태를 `DRAFT`에서 `OPEN`으로 변경하며, 해당 경기의 좌석 정보를 생성합니다.

---

## 4. 프론트엔드 구현 상세 가이드라인 (Confirmed)

1. **상태 관리 및 인증**:
   - **유저 인증**: `userId` 기반의 간소화된 인증을 사용하며, `localStorage`에 저장하여 유지합니다.
   - **Client State**: 전역 UI 상태(예: 선택된 좌석 정보)는 **Zustand**를 사용하여 관리합니다.
   - **Server State**: API 데이터 캐싱 및 동기화는 **React Query**를 사용합니다.
   - 모든 API 요청 시 `userId`를 포함하여 전송합니다.

2. **SSE 연결 관리**:
   - 사용자가 '구역(Block)' 선택 시 SSE 연결을 시작하고, 구역 이탈 시 연결을 종료합니다.
   - 구역 변경 시 이전 연결을 정리(Clean-up)하고 새 연결을 수립합니다.
   - 네트워크 오류 등으로 연결 단절 시 **자동으로 3회 재연결**을 시도하며, 최종 실패 시 사용자에게 새로고침을 안내합니다.

3. **좌석도 렌더링 및 UI**:
   - **네비게이션**: 좌측 사이드바에서 `권역(Area) -> 진영(Section)`을 선택합니다.
   - **구역(Block) 이동**: 메인 좌석도 상단의 좌/우 화살표를 사용하여 같은 진영 내의 `구역(Block)`을 이동합니다.
   - **방식**: **Tailwind CSS Grid**를 사용하여 좌석을 배치합니다.
   - **Shadcn/UI**: 버튼, 다이얼로그, 토스트 등의 공통 컴포넌트는 Shadcn/UI 라이브러리를 활용합니다.
   - **낙관적 업데이트 (Optimistic UI)**: 좌석 클릭 시 즉시 UI 상태를 `HOLD`로 변경하고 API 결과에 따라 실패 시 롤백합니다.
   - **좌석 선택 취소**: 이미 내가 선택한 좌석을 다시 클릭하면 즉시 `Release` API를 호출하여 선점을 해제합니다.
   - **색상 구성**:
     - `AVAILABLE`: 연한 회색 (Light Gray)
     - `HOLD`: 주황색 (Orange)
     - `OCCUPIED`: 파란색 (Blue)
   - **기타**: 내가 선택한 좌석은 체크 아이콘으로 별도 표시하며, 상태 변경 시 CSS transition을 통해 부드럽게 전환합니다.

4. **사용자 플로우 및 상태 유지**:
   - **뒤로가기 시 유지**: 사용자가 구역(Block)을 나갔다가 다시 돌아오더라도, 이미 선점(`HOLD`)한 좌석 정보는 Zustand Store 또는 `localStorage`를 통해 유지되어 표시됩니다.
   - **타이머 미적용**: 현재 버전에서는 선점 제한 시간(타이머) 기능을 구현하지 않습니다.

5. **환경 및 반응형 디자인 (Mobile-First & App-Like)**:
   - **모바일 최적화**: 모바일 앱과 유사한 사용자 경험(UX)을 최우선으로 고려합니다. (Mobile-First)
   - **레이아웃 구조**:
     - **Bottom Navigation**: 주요 메뉴(홈, 예매, 마이페이지 등)는 하단 탭 내비게이션으로 구성합니다.
     - **Seat Selection**: 사이드바 대신 **Bottom Sheet (Drawer)** 또는 **단계별 선택(Step Wizard)** 방식을 사용하여 모바일 화면 공간을 확보합니다.
   - **터치 타겟**: 모든 인터랙티브 요소는 모바일 터치 환경에 적합한 크기(최소 44px)를 확보합니다.
   - **반응형**: 데스크톱에서도 모바일 뷰 형태(중앙 정렬 앱 컨테이너)를 유지하거나, 적절히 확장하여 보여줍니다.

6. **데이터 로딩 및 성능**:
   - **전체 로딩 (Total Loading)**: React Query의 `useQuery`를 활용하여 구역(Block) 진입 시 데이터를 로드합니다.
   - **SSE 클린업**: `useEffect` cleanup function 등을 통해 컴포넌트 언마운트 시 SSE 연결을 확실히 종료합니다.
   - **모바일 성능**: 리스트 렌더링 시 가상화(Virtualization) 등을 고려하여 스크롤 성능을 최적화합니다.

7. **에러 처리 및 실시간 상태 표시**:
   - **동시성 충돌**: 좌석 선점 실패 시(예: 409 Conflict) Toast 메시지(모바일 하단 또는 상단)를 통해 알립니다.
   - **연결 상태 인디케이터**: 모바일 화면 공간을 고려하여 헤더 영역에 작게 표시하거나, 연결 끊김 시 전체 화면 오버레이 또는 상단 배너로 확실하게 인지시킵니다.

8. **접근성 및 다국어 지원**:
   - **단순화**: 테스트 및 API 검증이 목적이므로 웹 접근성(a11y) 및 색맹 보호 모드 등은 고려하지 않습니다.
   - **한국어 전용**: 다국어(i18n) 지원 없이 한국어 단일 언어로 개발합니다.

9. **테스트 및 배포 전략**:
   - **모바일 테스트**: Chrome DevTools의 모바일 에뮬레이터를 주로 사용하여 검증합니다.
   - **로컬 실행**: `npm run dev`를 통해 로컬 환경에서 실행합니다.

10. **폴링 지연**: 서버가 1초 주기로 DB를 확인하므로, 실제 상태 변경과 SSE 수신 사이에 최대 1초 내외의 지연이 발생할 수 있음을 고려하십시오.

---

## 5. 심층 인터뷰 질문 리스트 (심층 검토용)

아직 논의 중이거나 상세 구현 시 고려해야 할 질문들입니다.

### 1. 아키텍처 & 기술 스택 관련
- Q1: React 프레임워크 선택 (Next.js 확정)
- Q2: 상태 관리 라이브러리 (Client: Zustand, Server: React Query 확정)
- Q3: API 통신 레이어 (Axios 확정)

### 2. 인증 & 보안
- Q4: 인증 방식의 구체적 구현 (userId 기반 간소화 확정)
- Q5: 인증 상태 관리 (localStorage + Zustand persist 확정)

### 3. SSE 실시간 통신
- Q6: SSE 연결 생명주기 관리 (구역별 연결, 3회 재시도 확정)
- Q7: SSE 데이터 동기화 전략 (낙관적 업데이트 확정)
- Q8: SSE 연결 실패 처리 (3회 시도 후 안내 확정)

### 4. UI/UX 디자인
- Q9: 좌석도 렌더링 방식 (Tailwind CSS Grid 확정)
- Q10: 좌석 상태 시각화 (회색/주황/파랑, 체크 아이콘, 애니메이션 확정)
- Q11: 사용자 플로우 (취소/뒤로가기/타이머 확정)
- Q12: 반응형 디자인 (데스크톱 전용 확정)

### 5. 성능 & 최적화
- Q13: 좌석 데이터 로딩 전략 (전체 로딩 확정)
- Q14: 메모리 관리 (SSE 클린업 확정)

### 6. 에러 처리 & 엣지 케이스
- Q15: 동시성 충돌 처리 (Toast & Rollback 확정)
- Q16: 네트워크 불안정 환경 (연결 상태 인디케이터 확정)

### 7. 접근성 & 사용성
- Q17: 웹 접근성 (단순화 확정)
- Q18: 다국어 지원 (한국어 전용 확정)

### 8. 테스트 전략
- Q19: 테스트 범위 (매뉴얼 테스트 확정)

### 9. 배포 & 환경
- Q20: 환경 분리 (로컬 중심 확정)
- Q21: 빌드 & 배포 (로컬 실행 확정, 배포 미고려)

### 10. 향후 확장성 (고려 사항)
- Q22: Redis 마이그레이션 대비
- Q23: 추가 기능 계획 (좌석 추천, 대기열 등)

