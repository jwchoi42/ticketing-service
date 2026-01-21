# CI 파이프라인 구축 작업 계획

본 문서는 `cicd-pipeline-plan.md`에서 정의된 작업 중 **CI (Continuous Integration)** 부분의 상세 실행 계획을 기록합니다.

---

## 1. 개요

- **목표**: 백엔드(Spring Boot) 및 프론트엔드(React)의 빌드, 테스트, 코드 품질 검사를 자동화
- **도구**: GitHub Actions
- **트리거**: `main`, `dev` 브랜치에 대한 Push 및 PR

---

## 2. 작업 체크리스트

### 2.1 사전 준비
- [x] Docker Hub 계정 토큰 발급 (ID: jwchoi42)
- [x] GitHub Secrets 설정 (`DOCKERHUB_USERNAME`, `DOCKERHUB_TOKEN`)
- [x] 로컬 빌드 검증 (백엔드: `./gradlew clean build`, 프론트엔드: `npm run build`)

### 2.2 프론트엔드 환경 설정
- [x] `frontend/Dockerfile` 생성 (Multi-stage build)
- [x] `frontend/nginx.conf` 생성 (SPA fallback 및 API proxy 설정)

### 2.3 CI 워크플로우 구축 (`.github/workflows/continuous-integration.yaml`)
- [x] 기본 트리거 정의 및 전역 환경 변수 설정
- [x] **Backend Job**
    - [x] Java 17 (Eclipse Temurin) 설정
    - [x] Gradle 빌드 및 의존성 캐싱
    - [x] 전체 테스트 실행 (JUnit + Cucumber)
    - [x] 테스트 결과 리포트 업로드
- [x] **Frontend Job** (병렬 실행)
    - [x] Node.js 22 설정
    - [x] npm 의존성 캐싱 (`npm ci`)
    - [x] ESLint 린트 검사 (`npm run lint`)
    - [x] 프로덕션 빌드 검사 (`npm run build`)

### 2.4 최종 검증
- [ ] PR 생성 시 워크플로우 정상 트리거 확인
- [ ] 각 Job의 성공/실패 여부 및 로그 확인
- [ ] 빌드 시간 최적화 (캐싱 동작 확인)

---

## 3. 진행 상황 현황

| 단계 | 작업 내용 | 상태 | 비고 |
|:---:|:---|:---:|:---|
| 1 | 사전 준비 | ✅ 완료 | |
| 2 | 프론트엔드 설정 | ✅ 완료 | |
| 3 | CI 워크플로우 구성 | ✅ 완료 | |
| 4 | 배포 전 최종 검증 | 🏃 진행 중 | GitHub Actions 실행 대기 |

---

## 4. 상세 내용

### 4.1 Backend CI 상세
- **Testcontainers**: 백엔드 테스트 실행 시 Docker가 필요하므로, GitHub Actions 라이너 환경에서 Docker가 가용함을 활용.
- **Gradle Wrapper**: `./gradlew`를 사용하여 버전 일관성 유지.

### 4.2 Frontend CI 상세
- **Node Version**: LTS인 22 버전 사용.
- **Lint**: Husky 등을 통한 pre-commit이 있더라도 CI에서 강제하여 코드 품질 유지.
