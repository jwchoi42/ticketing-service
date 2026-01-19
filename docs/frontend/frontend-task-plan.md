# Frontend Implementation Task Plan (Next.js Migration)

본 문서는 `docs/frontend/spec.md` 및 `docs/frontend/tech-stack.md`를 바탕으로 한 Next.js 기반 프론트엔드 재구축 계획을 기록합니다.

## 1. 기술 스택 (Tech Stack)
- **Framework**: Next.js (App Router)
- **Language**: TypeScript
- **Styling**: Tailwind CSS + Shadcn/UI
- **State Management**:
    - Client: Zustand
    - Server: React Query
- **Networking**: Axios
- **Real-time**: SSE (Server-Sent Events)

## 2. 구현 단계 및 진행 상황

### Phase 1: 프로젝트 초기화 (Initialization) - [ ]
- [ ] Next.js 프로젝트 생성 (`npx create-next-app`)
- [ ] Tailwind CSS 및 Shadcn/UI 초기 설정
- [ ] 프로젝트 구조 정리 (`app/`, `components/`, `lib/`, `store/`, `hooks/`)
- [ ] 환경 변수 설정 (`.env.local`)

### Phase 2: 핵심 아키텍처 (Core Architecture) - [ ]
- [ ] Axios 인스턴스 설정 (Interceptors)
- [ ] React Query Provider 설정
- [ ] Zustand Store 초기 구성
- [ ] `AuthContext` 또는 User Store 구현 (localStorage 연동)

### Phase 3: 기본 UI 구현 (UI Foundation) - [ ]
- [ ] 레이아웃 구성 (`layout.tsx`, `page.tsx`)
- [ ] 공통 컴포넌트 추가 (Button, Input, Card, Dialog via Shadcn)
- [ ] 로그인 (`/login`) 및 회원가입 (`/signup`) 페이지

### Phase 4: 주요 기능 구현 (Feature Implementation) - [ ]
- [ ] 경기 목록 페이지 (`/matches`)
- [ ] 경기별 좌석 선택 페이지 (`/matches/[id]`)
    - [ ] 권역/진영/구역 네비게이션
    - [ ] 좌석 배치도 (Tailwind Grid) 구현

### Phase 5: 실시간 연동 (Real-time Integration) - [ ]
- [ ] `useSSE` 훅 재구현 (Next.js 환경 적응)
- [ ] 실시간 좌석 상태 반영 및 최적화
- [ ] 낙관적 업데이트 로직 적용

### Phase 6: 마무리 (Polish & Verify) - [ ]
- [ ] 에러 핸들링 및 Toast 메시지 연동
- [ ] 최종 사용자 플로우 검증 (Manual Test)

---

## 3. 진행 일지

### 2026-01-19
- Next.js 마이그레이션을 위한 `frontend-task-plan.md` 재작성.
- 기존 진행 상황 초기화.
