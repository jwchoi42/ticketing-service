---
trigger: model_decision
---

# CI/CD 파이프라인 구성 계획

본 문서는 티켓팅 서비스의 CI/CD 파이프라인을 구성하기 위한 작업 계획입니다.

## 1. 개요

### 1.1 목표
- GitHub Actions를 활용하여 dev/main 브랜치에 push 또는 PR 시 빌드와 테스트 자동화 (CI)
- Docker 이미지를 빌드하여 Docker Hub에 푸시하고, EC2에서 실행 (CD)

### 1.2 배포 환경 정보
| 항목 | 값 |
|------|-----|
| 도메인 | wise-hero.cloud |
| Docker Hub ID | jwchoi42 |
| 클라우드 | AWS EC2 |
| DB 운영 방식 | EC2에서 Docker로 운영 (추후 RDS 마이그레이션 가능) |
| 프론트엔드 배포 | EC2에서 Nginx로 서빙 |

---

## 2. 현재 프로젝트 상태

### 2.1 프로젝트 구조 (모노레포)
```
ticketing-service/
├── frontend/                    # React + Vite + TypeScript
│   ├── src/
│   ├── package.json
│   └── vite.config.ts
├── src/main/java/              # Spring Boot 백엔드
├── build.gradle
├── Dockerfile                  # 백엔드 Dockerfile (존재함)
├── docker-compose.yaml         # 개발용 (존재함)
└── .github/workflows/          # ❌ 없음 (생성 필요)
```

### 2.2 기술 스택
| 구성요소 | 기술 스택 | Docker 파일 상태 |
|---------|----------|-----------------|
| 백엔드 | Spring Boot 3.5 + Java 17 | ✅ 존재 |
| 프론트엔드 | React 19 + Vite 7.2 + TypeScript | ❌ 생성 필요 |
| DB | PostgreSQL 17 + Redis 7 | docker-compose에 정의됨 |
| CI/CD | GitHub Actions | ❌ 생성 필요 |

---

## 3. 목표 아키텍처

### 3.1 CI/CD 파이프라인 흐름
```
┌─────────────────────────────────────────────────────────────────┐
│                        GitHub Actions (CI)                       │
├─────────────────────────────────────────────────────────────────┤
│  PR/Push to dev/main                                             │
│       ↓                                                          │
│  ┌─────────────────┐    ┌─────────────────┐                     │
│  │ Backend Job     │    │ Frontend Job    │   ← 병렬 실행       │
│  │ - Build         │    │ - Build         │                     │
│  │ - Test          │    │ - Lint          │                     │
│  │ - Docker Build  │    │ - Docker Build  │                     │
│  │ - Push to Hub   │    │ - Push to Hub   │                     │
│  └────────┬────────┘    └────────┬────────┘                     │
└───────────┼──────────────────────┼──────────────────────────────┘
            │                      │
            ▼                      ▼
┌─────────────────────────────────────────────────────────────────┐
│                         Docker Hub                               │
│  ┌─────────────────────┐    ┌─────────────────────┐             │
│  │ jwchoi42/backend    │    │ jwchoi42/frontend   │             │
│  └─────────────────────┘    └─────────────────────┘             │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                          EC2 (CD)                                │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                    docker-compose                         │   │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐  │   │
│  │  │ Backend  │  │ Frontend │  │PostgreSQL│  │  Redis   │  │   │
│  │  │  :8080   │  │  :80/443 │  │  :5432   │  │  :6379   │  │   │
│  │  └──────────┘  └──────────┘  └──────────┘  └──────────┘  │   │
│  └──────────────────────────────────────────────────────────┘   │
│                              │                                   │
│                         도메인 연결                              │
│                   wise-hero.cloud                                │
└─────────────────────────────────────────────────────────────────┘
```

### 3.2 EC2 내부 구조
```
┌─────────────────── EC2 ───────────────────┐
│                                            │
│   ┌─────────────────────────────────────┐  │
│   │          Nginx (프론트엔드)          │  │  ← wise-hero.cloud:80/443
│   │   - React 정적 파일 서빙             │  │
│   │   - /api/* → Backend로 프록시        │  │
│   └──────────────┬──────────────────────┘  │
│                  │                          │
│   ┌──────────────▼──────────────────────┐  │
│   │         Backend (Spring Boot)        │  │  ← :8080 (내부)
│   └──────────────┬──────────────────────┘  │
│                  │                          │
│   ┌──────────────▼────┐  ┌──────────────┐  │
│   │    PostgreSQL     │  │    Redis     │  │
│   │      :5432        │  │    :6379     │  │
│   └───────────────────┘  └──────────────┘  │
└────────────────────────────────────────────┘
```

---

## 4. 생성해야 할 파일 목록

| 순서 | 파일 경로 | 용도 |
|-----|----------|------|
| 1 | `frontend/Dockerfile` | 프론트엔드 Docker 이미지 빌드 |
| 2 | `frontend/nginx.conf` | Nginx 설정 (정적 파일 서빙 + API 리버스 프록시) |
| 3 | `.github/workflows/ci.yml` | PR/Push 시 빌드 및 테스트 자동화 |
| 4 | `.github/workflows/cd.yml` | main 브랜치 푸시 시 Docker Hub → EC2 배포 |
| 5 | `docker-compose.prod.yml` | EC2 운영 환경용 compose 파일 |
| 6 | `.env.example` | 환경 변수 템플릿 |

---

## 5. 세부 작업 단계

### 1단계: 사전 준비 (수동 작업)

#### 1-1. Docker Hub 계정 설정
1. https://hub.docker.com 접속 및 로그인 (ID: jwchoi42)
2. Settings → Personal Access Tokens에서 토큰 생성
3. 토큰 저장 (GitHub Secrets에 사용)

#### 1-2. GitHub Secrets 설정
리포지토리 → Settings → Secrets and variables → Actions에 추가:

| Secret 이름 | 값 | 설명 |
|------------|-----|------|
| `DOCKERHUB_USERNAME` | jwchoi42 | Docker Hub 사용자명 |
| `DOCKERHUB_TOKEN` | (생성한 토큰) | Docker Hub 액세스 토큰 |
| `EC2_HOST` | (EC2 퍼블릭 IP) | EC2 접속 주소 |
| `EC2_SSH_KEY` | (pem 파일 내용) | EC2 SSH 프라이빗 키 |
| `EC2_USERNAME` | ubuntu | EC2 접속 사용자 |

#### 1-3. EC2 인스턴스 준비
1. AWS EC2 인스턴스 생성 (Ubuntu 22.04 LTS 권장)
2. 보안 그룹에서 포트 오픈: 22(SSH), 80(HTTP), 443(HTTPS)
3. Docker 및 Docker Compose 설치
4. 도메인(wise-hero.cloud) DNS 설정 → EC2 퍼블릭 IP 연결

---

### 2단계: 프론트엔드 Docker 설정 생성

#### 2-1. frontend/Dockerfile 생성
- Node.js 베이스 이미지로 빌드
- Nginx 이미지로 정적 파일 서빙
- 멀티스테이지 빌드 적용

#### 2-2. frontend/nginx.conf 생성
- React SPA를 위한 fallback 설정 (모든 경로 → index.html)
- `/api/*` 요청을 백엔드로 프록시
- gzip 압축 설정

---

### 3단계: GitHub Actions CI 워크플로우 생성

#### 3-1. .github/workflows/ci.yml 생성
**트리거**: dev/main 브랜치에 push 또는 PR

**Backend Job**:
1. Java 17 설정
2. Gradle 캐시 설정
3. `./gradlew build` 실행 (테스트 포함)
4. 테스트 결과 리포트 업로드

**Frontend Job**:
1. Node.js 설정
2. npm 캐시 설정
3. `npm ci` 의존성 설치
4. `npm run lint` 린트 검사
5. `npm run build` 빌드

---

### 4단계: GitHub Actions CD 워크플로우 생성

#### 4-1. .github/workflows/cd.yml 생성
**트리거**: main 브랜치에 push (CI 성공 후)

**Backend Deploy Job**:
1. Docker 빌드: `docker build -t jwchoi42/ticketing-backend:latest .`
2. Docker Hub 로그인
3. Docker Hub에 푸시

**Frontend Deploy Job**:
1. Docker 빌드: `docker build -t jwchoi42/ticketing-frontend:latest ./frontend`
2. Docker Hub 로그인
3. Docker Hub에 푸시

**Deploy to EC2 Job**:
1. SSH로 EC2 접속
2. 최신 이미지 pull
3. docker-compose로 서비스 재시작

---

### 5단계: 운영 환경 Docker Compose 생성

#### 5-1. docker-compose.prod.yml 생성
서비스 구성:
- `frontend`: Nginx + React (포트 80, 443)
- `backend`: Spring Boot (포트 8080, 내부만)
- `database`: PostgreSQL 17
- `redis`: Redis 7

네트워크:
- 내부 네트워크로 서비스 간 통신
- 프론트엔드만 외부 노출

볼륨:
- PostgreSQL 데이터 영속화
- Redis 데이터 영속화 (선택)

---

### 6단계: HTTPS 설정 (선택, 권장)

#### 6-1. Let's Encrypt SSL 인증서 설정
1. Certbot 설치
2. SSL 인증서 발급: `certbot --nginx -d wise-hero.cloud`
3. 자동 갱신 설정

---

## 6. 작업 체크리스트

### 사전 준비 (수동)
- [ ] Docker Hub 계정 로그인 및 토큰 생성
- [ ] GitHub Secrets 설정 (5개)
- [ ] EC2 인스턴스 생성 및 Docker 설치
- [ ] 도메인 DNS 설정 (wise-hero.cloud → EC2 IP)

### 파일 생성 (자동화 가능)
- [ ] `frontend/Dockerfile` 생성
- [ ] `frontend/nginx.conf` 생성
- [ ] `.github/workflows/ci.yml` 생성
- [ ] `.github/workflows/cd.yml` 생성
- [ ] `docker-compose.prod.yml` 생성
- [ ] `.env.example` 생성

### 배포 후 검증
- [ ] CI 파이프라인 동작 확인 (PR 생성 시 빌드/테스트)
- [ ] CD 파이프라인 동작 확인 (main 푸시 시 배포)
- [ ] wise-hero.cloud 접속 확인
- [ ] API 동작 확인 (/api/*)
- [ ] HTTPS 설정 (선택)

---

## 7. 예상 환경 변수

### EC2 .env 파일
```env
# Database
POSTGRES_DB=ticketing_db
POSTGRES_USER=postgres
POSTGRES_PASSWORD=<secure-password>

# Spring Boot
SPRING_DATASOURCE_URL=jdbc:postgresql://database:5432/ticketing_db
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=<secure-password>
SPRING_REDIS_HOST=redis
SPRING_REDIS_PORT=6379

# Application
SPRING_PROFILES_ACTIVE=prod
```

---

## 8. 참고 사항

### 8.1 DB 마이그레이션 고려
현재는 EC2에서 Docker로 PostgreSQL을 운영하지만, 추후 AWS RDS로 마이그레이션 가능:
- 장점: 자동 백업, 고가용성, 관리 편의성
- 단점: 추가 비용 발생

### 8.2 보안 고려사항
- EC2 보안 그룹: 필요한 포트만 오픈
- SSH 접속: 키 기반 인증만 허용
- 환경 변수: GitHub Secrets로 관리, 코드에 하드코딩 금지
- HTTPS: Let's Encrypt로 무료 SSL 인증서 적용 권장

### 8.3 모니터링 (향후)
- Spring Boot Actuator 활용 (/actuator/health)
- 로그 수집: CloudWatch 또는 ELK 스택
- 알림: Slack 또는 Discord 웹훅 연동
