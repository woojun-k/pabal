# Pabal - 메시징 서비스

## 프로젝트 개요

'Pabal' 프로젝트는 DDD(도메인 주도 설계), CQRS(명령 쿼리 책임 분리) 및 클린 아키텍처 원칙을 엄격하게 준수하여 현대적인 메시징 서비스의 핵심 기능을 구현하는 것을 목표로 하는 백엔드 시스템입니다. 견고하고 확장 가능하며 유지 보수성이 뛰어난 서비스를 제공하기 위해 설계되었습니다.

이 모듈은 다중 테넌시(multi-tenancy)를 지원하도록 설계되었으며, 효율적인 데이터 처리 및 일관성 보장을 위한 고급 패턴을 통합합니다.

## 아키텍처

### 핵심 원칙

*   **도메인 주도 설계 (DDD):** 비즈니스 도메인의 복잡성을 모델링하고 코드에 반영하는 데 중점을 둡니다. 도메인 계층은 핵심 비즈니스 로직과 규칙을 캡슐화하며, 엔티티, 값 객체, 도메인 이벤트 등을 통해 도메인 모델을 풍부하게 표현합니다.
*   **명령 쿼리 책임 분리 (CQRS):** 데이터 읽기(쿼리) 및 쓰기(명령) 작업을 위한 모델을 분리하여 시스템의 확장성, 성능 및 유연성을 최적화합니다. 이는 각 작업에 최적화된 데이터 모델과 패턴을 사용하여 높은 처리량과 낮은 지연 시간을 달성할 수 있도록 합니다.
*   **계층형 아키텍처:** 애플리케이션은 명확하게 구분된 계층(API, Application, Domain, Infrastructure)으로 구성되어 관심사 분리(Separation of Concerns)를 촉진하고 각 계층의 독립성을 보장합니다. 이를 통해 변경의 파급 효과를 최소화하고 유지 보수성을 향상시킵니다.
*   **헥사고날 아키텍처 (Ports & Adapters):** 애플리케이션의 핵심 비즈니스 로직(도메인 및 애플리케이션 계층)이 외부 기술 및 인프라(데이터베이스, 메시징 시스템, UI 등)로부터 독립적으로 유지되도록 설계되었습니다. 포트(인터페이스)는 애플리케이션의 경계를 정의하고, 어댑터는 특정 기술 구현을 통해 이 포트를 외부와 연결합니다.

### 모듈 구조

```
.
└── src/
    └── main/
        └── java/
            └── pabal/
                ├── common/        # 공통 유틸리티, 예외 처리, API 응답 형식, 영속성 엔티티 등 재사용 가능한 구성 요소
                └── messenger/     # 메시징 서비스의 핵심 비즈니스 로직 및 기능 구현
                    ├── api/       # HTTP 및 WebSocket을 통한 외부 인터페이스 정의 (Controller, Request/Response DTO)
                    ├── application/ # 애플리케이션 유스케이스 구현 (Command/Query Handler, Event Listener, Application Service)
                    ├── domain/      # 핵심 도메인 모델 정의 (Entity, Value Object, Event, Repository Interface, Exception)
                    └── infrastructure/ # 도메인 및 애플리케이션 계층의 아웃바운드 포트 구현 (Persistence, Event Publisher, Realtime Adapter)
```

## 기술 스택

*   **언어:** Java 25 (LTS)
*   **프레임워크:** Spring 4.0.2
*   **Persistence:** Spring Data JPA, Hibernate
*   **데이터베이스:** PostgreSQL (예정), H2 Database (개발/테스트용)
*   **빌드 도구:** Gradle 9.3.0
*   **유틸리티:** Lombok
*   **메시징:** WebSocket / STOMP (예정)
*   **캐싱:** Redis (예정)
