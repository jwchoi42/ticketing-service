# CD 파이프라인 구축 작업 계획

본 문서는 `cicd-pipeline-plan.md`에서 정의된 작업 중 **CD (Continuous Deployment)** 부분의 상세 실행 계획을 기록합니다.

---

## 1. 개요

- **목표**: GitHub Actions를 통해 빌드된 Docker 이미지를 Docker Hub에 푸시하고, AWS EC2 운영 서버에 자동 배포
- **도구**: GitHub Actions, Docker, Docker Compose, SSH
- **대상 서버**: AWS EC2 (Ubuntu 22.04 LTS 권장)
- **도메인**: `wise-hero.cloud`

---

## 2. 작업 체크리스트

### 2.1 서버 측 사전 준비 (수동)
- [ ] AWS EC2 인스턴스 생성 및 보안 그룹 설정 (22, 80, 443 포트 오픈)
- [ ] EC2 내 Docker 및 Docker Compose 설치
- [ ] 도메인 DNS 연결 (`wise-hero.cloud` -> EC2 퍼블릭 IP)
- [ ] GitHub Repository Secrets 설정
    - `EC2_HOST`: EC2 퍼블릭 IP 또는 도메인
    - `EC2_USERNAME`: ssh 사용자 (예: `ubuntu`)
    - `EC2_SSH_KEY`: `.pem` 키 내용

### 2.2 운영 환경 설정 파일 생성
- [ ] `docker-compose.prod.yml` 생성 (전체 서비스 구성)
- [ ] `.env.prod` 가이드 생성 (운영 환경 변수 템플릿)

### 2.3 CD 워크플로우 구축 (`.github/workflows/continuous-deployment.yaml`)
- [ ] **Docker Hub Push Job**
    - [ ] 백엔드 이미지 빌드 및 푸시 (`jwchoi42/ticketing-backend:latest`)
    - [ ] 프론트엔드 이미지 빌드 및 푸시 (`jwchoi42/ticketing-frontend:latest`)
- [ ] **EC2 Deploy Job**
    - [ ] SSH 접속 및 이미지 Pull
    - [ ] `docker-compose.prod.yml`을 이용한 컨테이너 재시작
    - [ ] 미사용 이미지 정리 (Prune)

### 2.4 인프라 고도화 (선택 사항)
- [ ] Nginx HTTPS 설정 (Certbot / Let's Encrypt)
- [ ] 서비스 상태 모니터링 및 알림 연동

---

## 3. 진행 상황 현황

| 단계 | 작업 내용 | 상태 | 비고 |
|:---:|:---|:---:|:---|
| 1 | 서버 사전 준비 | 🏃 진행 중 | |
| 2 | 운영 설정 파일 생성 | 📅 대기 | |
| 3 | CD 워크플로우 구성 | 📅 대기 | |
| 4 | 최종 배포 검증 | 📅 대기 | |

---

## 4. 상세 내용

### 4.1 Docker Image Tagging 전략
- 현재는 단순하게 `:latest` 태그를 사용하지만, 추후 버전 관리를 위해 `${{ github.sha }}` 또는 Git Tag를 사용하는 방식으로 개선 가능합니다.

### 4.2 배포 무중단 전략 (향후 과제)
- 현재는 `docker-compose` 재시작 시 짧은 다운타임이 발생할 수 있습니다.
- 향후 Nginx의 블루/그린 배포 혹은 롤링 업데이트 방식으로 고도화가 필요할 수 있습니다.
