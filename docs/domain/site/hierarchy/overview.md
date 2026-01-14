---
trigger: model_decision
description: site-hierarchy
---

자리(Site) 구조 및 구현 규칙
===

1. 계층 구조 (Hierarchy)
---
자리는 다음과 같은 계층 구조로 구성되며, 경기(Match)별로 독립적인 구조를 가진다.
- **영역(Area)**: 가장 상위 계층. 
    - DB 저장 시: INFIELD, OUTFIELD
    - 사용자 노출/Feature 시: 내야, 외야
- **진영(Section)**: 영역의 하위 계층.
    - 내야 영역: DB(HOME, AWAY), Feature(연고, 원정)
    - 외야 영역: DB(LEFT, RIGHT), Feature(좌측, 우측)
- **구간(Block)**: 진영의 하위 계층. 임의의 개수만큼 존재할 수 있다.
    - **네이밍 형식**: {AREA_DB}-{SECTION_DB}-{NUMBER} (예: INFIELD-HOME-1, OUTFIELD-LEFT-10)
    - Feature 시: 내야-연고-1 형식으로 지칭
- **좌석(Seat)**: 구간의 하위 계층. 임의의 개수만큼 존재할 수 있다.

2. 구현 규칙 (Implementation Rules)
---
- **Stadium 용어 배제**: "Stadium"은 매치의 속성일 뿐, site 도메인에서는 독립적인 엔티티나 서비스 명칭으로 사용하지 않는다.
- **개별 UseCase 및 Port**: StadiumUseCase나 StadiumPort와 같은 통합 인터페이스 대신, 각 계층별로 독립적인 UseCase와 Port를 작성한다.
    - 예: LoadAreaPort, LoadSectionPort, GetAreasUseCase, GetSectionsUseCase 등.
- **Package Flow**: dev.ticketing.core.site 패키지 내에서 각 계층의 관심사를 독립적으로 관리한다.

3. 도메인 패키지 구조 (Domain Package Structure)
---
site.domain 패키지는 데이터의 성격에 따라 다음과 같이 하위 패키지로 분리하여 관리한다.
- **hierarchy**: 자리의 물리적인 계층 구조(Area -> Section -> Block -> Seat)를 정의한다.
- **allocation**: 좌석의 실시간 상태(AllocationStatus) 및 특정 시점의 좌석 정보(Allocation) 등 점유/배정과 관련된 모델을 정의한다.
