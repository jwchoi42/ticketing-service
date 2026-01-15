# Frontend Implementation Task Plan

본 문서는 `docs/frontend/spec.md`를 바탕으로 한 프론트엔드 구현 계획 및 진행 상황을 기록합니다.

## 1. 기술 스택 (Tech Stack)
- **Framework**: Vite + React (CSR)
- **Language**: TypeScript
- **State Management**: React Hooks (`useState`, `useContext`)
- **API Client**: Axios
- **Styling**: Vanilla CSS (CSS Modules or Global CSS)
- **Real-time**: SSE (Server-Sent Events)

## 2. 구현 단계 및 진행 상황

### Phase 1: 기반 설정 (Initialization) - [x]
- [x] Vite 프로젝트 초기화 (React + TS)
- [x] 기본 폴더 구조 설정 (`src/api`, `src/components`, `src/hooks`, `src/pages`, `src/styles`)
- [x] 전역 상수 및 환경 변수 설정 (`.env`)
- [x] Axios 인스턴스 설정 및 공통 응답 처리

### Phase 2: 코어 인프라 및 인증 (Core Infrastructure & Auth) - [x]
- [x] `AuthContext` 구현 (localStorage 기반 `userId` 관리)
- [x] 로그인 (`/log-in`) 및 회원가입 (`/sign-up`) 페이지 구현
- [x] API 연동 레이어 (Matches, Site Hierarchy) 구현

### Phase 3: 주요 페이지 및 컴포넌트 (UI & Components) - [x]
- [x] 경기 목록 페이지 (`MatchListPage`)
- [x] 경기별 좌석 선택 페이지 (`SeatSelectionPage`)
    - [x] 권역(Area) / 진영(Section) / 구역(Block) 네비게이션
    - [x] 좌석 선택 레이아웃 (CSS Grid)
- [x] 공통 UI 컴포넌트 (Button, Modal, Toast, StatusIndicator)

### Phase 4: 실시간 상태 연동 (Real-time Integration) - [x]
- [x] `useSSE` 커스텀 훅 구현 (자동 재연결, 스냅샷 처리, 변경사항 반영)
- [x] 좌석 선점(Hold/Release) 기능 및 낙관적 업데이트 적용
- [x] 실시간 상태 변경 자동 반영 UI 검증

### Phase 5: 예약 및 결제 (Reservation & Payment) - [x]
- [x] 예약 생성 연동
- [x] 결제 프로세스 (요청 -> 승인) UI 구현
- [x] 최종 예약 확인 및 마이프론트엔드 완료 처리

### Phase 6: 폴리싱 및 배포 (Polish & Deployment) - [x]
- [x] 전반적인 UI/UX 개선 (트랜지션, 애니메이션)
- [x] 에러 핸들링 보완 (네트워크 오류, 동시성 충돌)
- [x] Netlify 배포 설정 (`_redirects`, `netlify.toml`)

---

## 3. 진행 일지

### 2026-01-13
- `frontend-task-plan.md` 작성 및 작업 시작.
- Vite 프로젝트 초기화 (React + TS, Axios, React Router).
- `.env` 및 Axios 공통 응답 처리 설정.
- `AuthContext` 구현 및 `LoginPage` 개발.
- 경기 목록 및 좌석 계층 구조 API 연동.
- `MatchListPage`, `SeatSelectionPage` (레이아웃 및 네비게이션) 구현.
- Phase 1, 2 완료 및 Phase 3 상당 부분 진행.
- `useSSE` 훅 구현 및 실시간 좌석 상태 연동 완료.
- 좌석 선점(Hold/Release) 및 낙관적 업데이트 적용.
- 예약 생성 및 결제 프로세스 구현 완료.
- `ToastContext` 및 실시간 연결 상태 인디케이터 추가.
- `Header` 컴포넌트 추가 및 전반적인 UI 폴리싱.
- Netlify 배포 설정 완료.
- **모든 Phase 구현 완료.**
