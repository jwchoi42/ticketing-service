# Frontend Integration Technical Specification

본 문서는 프론트엔드(React) 개발을 위해 필요한 백엔드 기술 스택 및 API 명세를 정리한 가이드라인입니다.

## 1. 기술 스택 (Tech Stack)

### Frontend Core (Confirmed)
- **Framework**: Vite + React (CSR)
- **Language**: TypeScript
- **State Management**: React Hooks (`useState`, `useContext`) - 추가 라이브러리 없이 단순 구성
- **API Client**: Axios (응답 인터셉터 처리 및 에러 핸들링 용이)

### Backend Core
- **Language**: Java 17
- **Framework**: Spring Boot 3.5.9
- **Database**: PostgreSQL (사용자, 경기, 예약, 결제, 좌석 데이터 모두 관리)
- **Real-time**: SSE (Server-Sent Events)
    - **동작 원리**: 서버 메모리에서 SSE 연결을 관리하며, DB를 1초 주기로 폴링(Polling)하여 변경된 좌석 상태를 추출하고 클라이언트에 전송합니다. (Redis를 사용하지 않는 구조임에 유의)
- **API Documentation**: [Swagger UI](http://localhost:8080/swagger-ui/index.html)

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
    - Response: 성공 시 사용자 기본 정보를 반환합니다.

### 3.2. 경기 (Match) - `/api/matches`
- **경기 목록 조회**: `GET /`
    - 예매 가능한 경기(팀 정보, 일시, 장소 등) 목록을 반환합니다.

### 3.3. 사이트 구조 (Site Hierarchy) - `/api/matches/{matchId}`
- 경기별로 좌석 구조가 정의되므로 모든 경로는 `matchId` 기반입니다.
1. **권역(Area) 목록 조회**: `GET /areas` (예: 내야, 외야)
2. **진영(Section) 목록 조회**: `GET /areas/{areaId}/sections` (예: 연고측, 원정측)
3. **구역(Block) 목록 조회**: `GET /sections/{sectionId}/blocks` (예: 101블록 ~ 125블록)
4. **구역 내 좌석 목록 조회**: `GET /blocks/{blockId}/seats` (좌석 번호 등 기본 메타데이터)

### 3.4. 실시간 좌석 상태 (Status Stream) - SSE
- **연결 엔드포인트**: `GET /api/matches/{matchId}/statusstream/blocks/{blockId}/seats/stream`
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

---

## 4. 프론트엔드 구현 상세 가이드라인 (Confirmed)

1. **상태 관리 및 인증**:
   - 별도의 인증 토큰(JWT) 없이 `userId` 기반의 간소화된 방식을 사용합니다.
   - 로그인 성공 후 받은 `userId`를 `localStorage`에 저장하여 새로고침 시에도 유지합니다.
   - 모든 API 요청 시 해당 `userId`를 포함하여 전송합니다.

2. **SSE 연결 관리**:
   - 사용자가 '구역(Block)' 선택 시 SSE 연결을 시작하고, 구역 이탈 시 연결을 종료합니다.
   - 구역 변경 시 이전 연결을 정리(Clean-up)하고 새 연결을 수립합니다.
   - 네트워크 오류 등으로 연결 단절 시 **자동으로 3회 재연결**을 시도하며, 최종 실패 시 사용자에게 새로고침을 안내합니다.

3. **좌석도 렌더링 및 UI**:
   - **방식**: HTML/CSS Grid를 사용한 단순 배치 (Button 또는 Div 활용)
   - **낙관적 업데이트 (Optimistic UI)**: 좌석 클릭 시 즉시 UI 상태를 `HOLD`로 변경하고 API 결과에 따라 실패 시 롤백합니다.
   - **좌석 선택 취소**: 이미 내가 선택한 좌석을 다시 클릭하면 즉시 `Release` API를 호출하여 선점을 해제합니다.
   - **색상 구성**:
     - `AVAILABLE`: 연한 회색 (Light Gray)
     - `HOLD`: 주황색 (Orange)
     - `OCCUPIED`: 파란색 (Blue)
   - **기타**: 내가 선택한 좌석은 체크 아이콘으로 별도 표시하며, 상태 변경 시 CSS transition을 통해 부드럽게 전환합니다.

4. **사용자 플로우 및 상태 유지**:
   - **뒤로가기 시 유지**: 사용자가 구역(Block)을 나갔다가 다시 돌아오더라도, 이미 선점(`HOLD`)한 좌석 정보는 `localStorage` 또는 전역 상태를 통해 유지되어 다시 돌아왔을 때 그대로 표시됩니다. 즉, 뒤로가기 시 자동으로 `Release`를 호출하지 않습니다.
   - **타이머 미적용**: 현재 버전에서는 선점 제한 시간(타이머) 기능을 구현하지 않습니다.

5. **환경 및 반응형 디자인**:
   - **데스크톱 전용**: 본 애플리케이션은 백엔드 API 검증 및 테스트를 위한 목적이 크므로, 데스크톱 환경(최소 1280px 이상)에 최적화하여 개발합니다. 모바일 대응(반응형)은 고려하지 않습니다.

6. **데이터 로딩 및 성능**:
   - **전체 로딩 (Total Loading)**: 구역(Block) 진입 시 해당 구역의 모든 좌석(약 100개 내외)을 한 번에 로드합니다. 테스트 환경이므로 지연 로딩은 고려하지 않습니다.
   - **SSE 클린업**: 구역 이동 또는 페이지 이탈 시 반드시 SSE 연결을 명시적으로 종료(Close)하여 브라우저 메모리 누수를 방지합니다.

7. **에러 처리 및 실시간 상태 표시**:
   - **동시성 충돌**: 좌석 선점 실패 시(예: 409 Conflict) Toast 메시지를 통해 사용자에게 알리고, 낙관적으로 업데이트했던 UI를 즉시 원래 상태로 롤백합니다.
   - **연결 상태 인디케이터**: SSE 연결 상태를 사용자가 직관적으로 알 수 있도록 화면 상단 또는 구석에 작은 상태 표시등(초록: 정상, 노랑: 재연결 중, 빨강: 단절)을 구현합니다.

8. **접근성 및 다국어 지원**:
   - **단순화**: 테스트 및 API 검증이 목적이므로 웹 접근성(a11y) 및 색맹 보호 모드 등은 고려하지 않습니다.
   - **한국어 전용**: 다국어(i18n) 지원 없이 한국어 단일 언어로 개발합니다.

9. **테스트 및 배포 전략**:
   - **매뉴얼 테스트**: 자동화된 단위/E2E 테스트 대신, 브라우저를 통한 육안 확인 및 직접 클릭(Manual Verification)을 통해 API 연동을 검증합니다.
   - **로컬 실행**: 기본적으로 로컬 환경(`localhost:5173`) 실행을 전제로 하며, 서버 주소 등은 `.env` 파일을 통해 유연하게 관리합니다.
   - **Netlify 배포 (향후)**: 향후 Netlify를 통해 외부에 배포할 수 있도록 구성합니다. 
     - SPA 특성상 `_redirects` 파일 또는 `netlify.toml` 설정을 통해 모든 경로를 `index.html`로 리다이렉트하는 설정을 포함합니다.
     - 배포 시 API 서버 주소는 Netlify 환경 변수(`VITE_API_URL`)를 통해 관리합니다.

10. **폴링 지연**: 서버가 1초 주기로 DB를 확인하므로, 실제 상태 변경과 SSE 수신 사이에 최대 1초 내외의 지연이 발생할 수 있음을 고려하십시오.

---

## 5. 심층 인터뷰 질문 리스트 (심층 검토용)

아직 논의 중이거나 상세 구현 시 고려해야 할 질문들입니다.

### 1. 아키텍처 & 기술 스택 관련
- Q1: React 프레임워크 선택 (Vite+React+TS 확정)
- Q2: 상태 관리 라이브러리 (React Hooks 확정)
- Q3: API 통신 레이어 (Axios 확정)

### 2. 인증 & 보안
- Q4: 인증 방식의 구체적 구현 (userId 기반 간소화 확정)
- Q5: 인증 상태 관리 (localStorage 확정)

### 3. SSE 실시간 통신
- Q6: SSE 연결 생명주기 관리 (구역별 연결, 3회 재시도 확정)
- Q7: SSE 데이터 동기화 전략 (낙관적 업데이트 확정)
- Q8: SSE 연결 실패 처리 (3회 시도 후 안내 확정)

### 4. UI/UX 디자인
- Q9: 좌석도 렌더링 방식 (CSS Grid 확정)
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
- Q21: 빌드 & 배포 (로컬 실행 및 Netlify 배포 확정)

### 10. 향후 확장성 (고려 사항)
- Q22: Redis 마이그레이션 대비
- Q23: 추가 기능 계획 (좌석 추천, 대기열 등)

