# Infrastructure Code Review Report

현재 Terraform 및 배포 스크립트 코드를 검토한 결과, 전반적으로 잘 구성되어 있습니다. 모듈화, 태그 일관성, 보안 설정(SSH 제거, SSM 사용) 등 **실무 Best Practice**를 상당 부분 따르고 있습니다.

아래는 개선하면 더 좋을 점들과, 실무와 조금 다른 부분들을 정리한 것입니다.

---

## 잘 된 점 (Good Practices)

| 항목 | 설명 |
|---|---|
| **모듈화** | VPC, EC2, IAM, Security Group, CodeDeploy를 각각 독립 모듈로 분리하여 재사용성과 관리 용이성이 높음 |
| **SSM 기반 접근** | SSH 키 없이 AWS SSM Session Manager로 서버 접속하도록 설계, 보안상 매우 권장되는 방식 |
| **비밀번호 관리** | SSM Parameter Store를 활용하여 코드에 비밀번호를 노출하지 않음 - 실무 정석 |
| **태그 일관성** | 모든 리소스에 `ManagedBy = Terraform` 태그를 붙여 추적 및 비용 관리 용이 |
| **Auto Rollback** | CodeDeploy에 배포 실패 시 자동 롤백 설정이 되어 있음 |
| **EIP 사용** | 고정 IP를 사용하여 서버 재시작 후에도 IP가 유지됨 |

---

## 개선 권장 사항 (Recommendations)

### 1. VPC: 가용 영역(AZ) 하드코딩 제거

> [!WARNING]
> **현재 코드 ([vpc/main.tf](file:///c:/private-lesson/ticketing-service/infra/terraform/aws/modules/vpc/main.tf#L22))**
> ```hcl
> availability_zone = "${var.aws_region}a"  # "ap-northeast-2a"로 고정됨
> ```

**문제점:** 특정 AZ가 장애를 겪으면 서비스 전체가 영향을 받습니다. 실무에서는 여러 AZ에 서브넷을 분산시킵니다.

**권장 수정:** AZ를 변수로 받거나, `data` 소스로 동적으로 가져오기
```hcl
data "aws_availability_zones" "available" {
  state = "available"
}
# 그 후 [0], [1] 등으로 접근
```

---

### 2. IAM: SSM 권한의 리전/계정 하드코딩

> [!IMPORTANT]
> **현재 코드 ([iam/main.tf](file:///c:/private-lesson/ticketing-service/infra/terraform/aws/modules/iam/main.tf#L104))**
> ```hcl
> Resource = "arn:aws:ssm:ap-northeast-2:*:parameter/ticketing/prod/*"
> ```

**문제점:** 리전이 `ap-northeast-2`로 고정되어 있어, 다른 환경(dev, staging) 또는 다른 리전에서 재사용하기 어렵습니다.

**권장 수정:** `data.aws_region` 및 `data.aws_caller_identity`를 활용
```hcl
data "aws_region" "current" {}
data "aws_caller_identity" "current" {}

# Policy에서 사용:
Resource = "arn:aws:ssm:${data.aws_region.current.name}:${data.aws_caller_identity.current.account_id}:parameter/ticketing/${var.env}/*"
```

---

### 3. EC2: `user_data` 변경 감지 문제

> [!NOTE]
> **현재 코드 ([ec2/main.tf](file:///c:/private-lesson/ticketing-service/infra/terraform/aws/modules/ec2/main.tf#L37))**
> ```hcl
> user_data = file("${path.module}/scripts/init.sh")
> ```

**문제점:** `init.sh` 내용이 바뀌어도 Terraform이 이를 감지하여 EC2를 재생성하는데, 이게 의도치 않은 경우가 많습니다.

**권장 수정:** `user_data_replace_on_change = false` 추가 또는, 실무에서는 **Golden AMI**(미리 Docker 등이 설치된 이미지)를 사용하여 `user_data`를 최소화합니다.
```hcl
user_data_replace_on_change = false  # init.sh 변경 시 EC2 재생성 방지
```

---

### 4. 배포 스크립트: 에러 핸들링 및 로깅 강화

> **현재 코드 ([deploy.sh](file:///c:/private-lesson/ticketing-service/infra/aws/scripts/deploy.sh))**

**문제점:**
1. SSM 조회 실패 시 빈 문자열로 로그인을 시도하여 에러 원인 파악이 어려움
2. 배포 후 컨테이너가 정상적으로 떴는지 확인(Health Check)이 없음

**권장 수정:**
```bash
# SSM 호출 후 값 검증 추가
if [ -z "$DB_PASSWORD" ]; then
  echo "ERROR: Failed to fetch DB_PASSWORD from SSM"
  exit 1
fi

# 배포 완료 후 헬스체크 추가
echo "Waiting for backend to be healthy..."
sleep 10
curl --fail http://localhost/actuator/health || echo "Warning: Health check failed"
```

---

### 5. 전체적으로 `tfstate` 관리

> **현재 상태:** `terraform.tfstate` 파일이 로컬에 저장됨

**문제점 (실무에서는 치명적):** 로컬 tfstate는 팀 협업 시 충돌 위험이 있고, 실수로 삭제하면 인프라 추적이 불가능해집니다.

**권장 수정:** S3 + DynamoDB를 사용한 원격 상태 백엔드 설정

```hcl
# provider.tf 또는 backend.tf에 추가
terraform {
  backend "s3" {
    bucket         = "your-terraform-state-bucket"
    key            = "ticketing/prod/terraform.tfstate"
    region         = "ap-northeast-2"
    dynamodb_table = "terraform-locks"
    encrypt        = true
  }
}
```

> [!CAUTION]
> 이 설정은 개인 프로젝트에서는 필수가 아니지만, **협업 환경이나 취업 포트폴리오에서 언급하면 큰 플러스 요인**입니다.

---

### 6. 정리: 우선순위별 개선 사항

| 우선순위 | 항목 | 난이도 | 영향도 |
|:---:|---|:---:|:---:|
| ⭐⭐⭐ | `deploy.sh` 에러 핸들링 추가 | 낮음 | 높음 |
| ⭐⭐⭐ | IAM 리전/계정 하드코딩 제거 | 낮음 | 중간 |
| ⭐⭐ | VPC AZ 동적화 | 중간 | 중간 |
| ⭐⭐ | S3 원격 상태 백엔드 (팀 협업 시) | 중간 | 높음 |
| ⭐ | `user_data_replace_on_change` 설정 | 낮음 | 낮음 |

---

## 결론

현재 코드는 **개인 프로젝트 및 학습용으로 매우 훌륭한 수준**입니다. 위에 언급된 개선 사항들은 "잘못된 것"이라기보다는, "대규모 팀 환경에서 더 안정적으로 운영하기 위한 추가 조치"에 가깝습니다.

특히 SSM을 통한 비밀번호 관리와 SSH 미사용 정책은 실무에서도 권장되는 패턴으로, 포트폴리오로 충분히 어필할 수 있는 구성입니다.
