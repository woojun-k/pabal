# Pull Request Guide

> 상위 문서: [문서 홈](../README.md)  
> 관련 문서: [문서 작성 가이드](docs-guide.md), [ADR 목록](../adr/README.md)

## 목적

이 문서는 Project Pabal에서 Pull Request를 생성하고 리뷰할 때 따르는 기준을 정의한다.

PR은 단순히 코드를 병합하기 위한 절차가 아니라, 변경 의도와 영향 범위를 팀이 함께 검증하는 단위다.

---

## 기본 원칙

Project Pabal의 PR은 다음 원칙을 따른다.

1. 하나의 PR은 하나의 명확한 목적을 가진다.
2. 코드 변경과 문서 변경은 가능한 한 같은 PR에서 처리한다.
3. 구조적 결정은 ADR로 남긴다.
4. 리뷰어가 변경 의도를 빠르게 이해할 수 있도록 설명을 남긴다.
5. 테스트, 문서, 마이그레이션 영향 여부를 PR에서 명확히 표시한다.

---

## 브랜치 전략

기본 브랜치는 `main`이다.

작업 브랜치는 변경 목적에 따라 다음 형식을 사용한다.

```text
feature/{topic}
fix/{topic}
refactor/{topic}
docs/{topic}
test/{topic}
chore/{topic}
```

예시:

```text
feature/message-send-command
fix/chat-room-authorization
refactor/message-persistence-mapping
docs/add-pr-guide
test/message-command-handler
chore/update-gradle-config
```

### 브랜치 타입 기준

| 타입 | 사용 기준 |
|---|---|
| `feature` | 새로운 기능 추가 |
| `fix` | 버그 수정 |
| `refactor` | 동작 변경 없는 구조 개선 |
| `docs` | 문서만 변경 |
| `test` | 테스트 추가 또는 수정 |
| `chore` | 빌드, 설정, 의존성, CI 등 보조 작업 |

---

## PR 크기 기준

PR은 리뷰 가능한 크기로 유지한다.

권장 기준:

- 가능하면 하나의 주요 관심사만 포함한다.
- 대규모 구조 변경은 단계별 PR로 나눈다.
- 기능 구현과 대규모 리팩토링은 가능하면 분리한다.
- DB 스키마 변경, 보안 정책 변경, 아키텍처 변경은 PR 설명에 별도로 강조한다.

피해야 할 PR:

```text
- 기능 추가 + 대규모 리팩토링 + 문서 재작성 + 테스트 구조 변경이 한 번에 들어간 PR
- 변경 이유가 설명되지 않은 대량 파일 수정 PR
- 리뷰어가 실행/검증 방법을 알 수 없는 PR
```

---

## PR 제목 규칙

PR 제목은 변경 유형과 핵심 내용을 드러내야 한다.

권장 형식:

```text
[{type}] 변경 내용 요약
```

예시:

```text
[feature] 메시지 전송 Command 처리 추가
[fix] 채팅방 참가자 인가 검증 누락 수정
[refactor] Message persistence mapping 구조 분리
[docs] PR 가이드 추가
[test] MessageCommandHandler 단위 테스트 추가
[chore] Gradle 멀티모듈 설정 정리
```

---

## PR 설명 템플릿

PR 본문은 다음 템플릿을 사용한다.

```md
## 변경 요약

<!-- 한 줄로 요약한다. 커밋 메시지처럼 무엇을 했는지 서술한다. -->

-

## 변경 이유

<!-- 왜 이 변경이 필요했는지, 어떤 문제나 요구사항에서 비롯됐는지 서술한다. -->
<!-- 대안을 검토했다면 왜 이 방식을 선택했는지도 간략히 적는다. -->

-

## 주요 변경 사항

-

## 영향 범위

- API:
- Domain:
- Application:
- Infrastructure:
- Security:
- Realtime:
- Database:
- Docs:
- Breaking Change: <!-- 없음 / 있음 (내용: ) -->

## 테스트

- [ ] 단위 테스트 추가/수정
- [ ] 통합 테스트 추가/수정
- [ ] 로컬 실행 확인
- [ ] 기존 테스트 통과
- [ ] 테스트 불필요 또는 불가 (사유: )

## 문서 반영

- [ ] 관련 docs 수정
- [ ] ADR 검토 완료 — 필요 없음
- [ ] ADR 검토 완료 — 추가/수정 완료

## 스크린샷 / 참고 자료

<!-- UI 변경, 실행 결과, 성능 비교, 관련 이슈/티켓 링크 등 -->
<!-- 해당 없으면 `해당 없음`으로 표시한다. -->

-

## 리뷰 포인트

<!-- 리뷰어가 집중해줬으면 하는 부분, 불확실한 설계 결정, 피드백이 필요한 트레이드오프 등을 적는다. -->

-
```

불필요한 항목은 삭제하지 말고 `해당 없음`으로 표시한다.

예시:

```text
Database: 해당 없음
Realtime: 해당 없음
```

---

## 문서 변경 기준

다음 변경이 포함되면 관련 `docs/` 문서도 함께 수정한다.

| 변경 유형 | 수정 대상 |
|---|---|
| 패키지/모듈 구조 변경 | `docs/architecture/package-structure-and-layers.md` |
| 요청 처리 흐름 변경 | `docs/architecture/runtime-flow.md` |
| Persistence 변환 구조 변경 | `docs/architecture/persistence-boundary-and-mapping.md` |
| DB 스키마/제약 변경 | `docs/architecture/database-schema-and-constraints.md` |
| 이벤트 발행/트랜잭션 경계 변경 | `docs/architecture/event-and-transaction-boundary.md` |
| 도메인 규칙 변경 | `docs/domain/messenger-domain-model.md` |
| Command/Query 변경 | `docs/use-cases/command-query-catalog.md` |
| HTTP API 변경 | `docs/use-cases/http-api-and-error-mapping.md` |
| 에러 코드 변경 | `docs/use-cases/error-code-exception-mapping.md` |
| Realtime 이벤트 변경 | `docs/realtime/event-schema.md` |
| STOMP/WebSocket 변경 | `docs/realtime/stomp-guide.md`, `docs/realtime/websocket-configuration.md` |
| JWT Claim 변경 | `docs/security/jwt-claim-design.md` |
| 인가/멀티테넌시 변경 | `docs/security/authorization-and-multitenancy.md` |
| 테스트 전략 변경 | `docs/testing/testing-strategy.md`, `docs/testing/test-case-catalog.md` |

---

## ADR 작성 기준

다음과 같은 변경은 ADR 추가 또는 수정을 검토한다.

- 모듈 경계 변경
- 도메인 모델링 방향 변경
- Persistence 전략 변경
- 이벤트 발행 시점 변경
- 인증/인가 모델 변경
- 멀티테넌시 격리 방식 변경
- Realtime 프로토콜 또는 메시지 계약 변경
- 주요 기술 스택 변경
- 장기적으로 되돌리기 어려운 구조적 결정

ADR은 다음 위치에 작성한다.

```text
docs/adr/
```

새 ADR은 순번을 증가시켜 작성한다.

```text
docs/adr/0009-adopt-postgresql-rls-for-tenant-isolation.md
```

기존 결정을 바꾸는 경우 기존 ADR을 조용히 덮어쓰지 않는다.

대신:

1. 새 ADR을 작성한다.
2. 기존 ADR의 상태를 `Superseded`로 변경한다.
3. 두 ADR을 서로 링크한다.
4. 관련 architecture/security/realtime/use-case 문서를 함께 갱신한다.

---

## 리뷰어 체크리스트

리뷰어는 다음 항목을 확인한다.

### 기능/동작

- [ ] 변경 의도가 PR 설명과 일치하는가?
- [ ] 요구사항을 과도하게 확장하지 않았는가?
- [ ] 예외 케이스가 고려되었는가?
- [ ] 기존 기능을 깨뜨리지 않는가?

### 아키텍처

- [ ] API/Application/Domain/Infrastructure 경계가 유지되는가?
- [ ] Domain이 Infrastructure 또는 Persistence 세부 구현에 의존하지 않는가?
- [ ] Application이 Infrastructure 구현체에 직접 의존하지 않는가?
- [ ] 멀티모듈 전환 방향과 충돌하지 않는가?

### 보안/멀티테넌시

- [ ] tenantId/userId를 클라이언트 입력에 의존하지 않는가?
- [ ] JWT Principal 또는 인증 컨텍스트 기반으로 scope를 판단하는가?
- [ ] 권한 검증이 필요한 경계에서 누락되지 않았는가?
- [ ] Repository 조회 조건에 tenant scope가 반영되는가?

### 데이터/Persistence

- [ ] Domain Model과 JPA Entity 경계가 유지되는가?
- [ ] State/Persisted Wrapper 사용 의도가 명확한가?
- [ ] DB 제약조건과 애플리케이션 검증이 충돌하지 않는가?
- [ ] 마이그레이션 영향이 설명되었는가?

### Realtime

- [ ] Inbound command와 outbound event 책임이 분리되어 있는가?
- [ ] 트랜잭션 commit 이전에 외부 이벤트를 확정 발행하지 않는가?
- [ ] STOMP destination과 payload contract가 문서와 일치하는가?
- [ ] subscription authorization이 고려되었는가?

### 테스트

- [ ] 변경된 도메인 규칙에 대한 테스트가 있는가?
- [ ] application use-case 테스트가 필요한 경우 추가되었는가?
- [ ] API contract 변경 시 controller/API 테스트가 있는가?
- [ ] infrastructure 변경 시 통합 테스트 필요성이 검토되었는가?

### 문서

- [ ] 관련 `docs/` 문서가 수정되었는가?
- [ ] ADR이 필요한 결정인지 검토되었는가?
- [ ] 문서 링크가 깨지지 않는가?
- [ ] 현재 상태와 계획 상태가 명확히 구분되어 있는가?

---

## 작성자 체크리스트

PR 작성자는 제출 전 다음 항목을 확인한다.

```md
- [ ] PR 제목이 변경 내용을 명확히 설명한다.
- [ ] PR 본문에 변경 이유와 영향 범위를 작성했다.
- [ ] 하나의 PR에 너무 많은 관심사를 넣지 않았다.
- [ ] 로컬에서 필요한 테스트를 실행했다.
- [ ] 문서 변경 필요 여부를 확인했다.
- [ ] ADR 필요 여부를 확인했다.
- [ ] Breaking Change 여부를 확인하고 영향 범위에 명시했다.
- [ ] 보안/멀티테넌시 영향 여부를 확인했다.
- [ ] DB 마이그레이션 영향 여부를 확인했다.
- [ ] 리뷰어가 집중해서 봐야 할 부분을 적었다.
```

---

## Merge 기준

PR은 다음 조건을 만족한 뒤 병합한다.

- 필수 리뷰어 승인
- CI 통과
- 충돌 없음
- 필요한 문서 반영 완료
- 필요한 ADR 반영 완료
- 리뷰 코멘트 해결 또는 합의 완료

권장 merge 방식:

```text
main 브랜치: squash merge 또는 rebase merge
작업 브랜치 간 통합: 필요 시 merge commit 허용
```

`main`에는 의미 있는 단위의 변경 이력이 남아야 한다.

---

## PR 예시

### 좋은 예시

```text
[feature] 메시지 전송 Command 처리 추가
```

포함 내용:

```text
- Send message command handler 추가
- Message domain behavior 사용
- Repository port 호출
- 단위 테스트 추가
- command-query catalog 문서 갱신
```

### 나쁜 예시

```text
메시지 기능 수정
```

문제:

```text
- 변경 범위가 불명확함
- 기능 추가인지 버그 수정인지 알 수 없음
- 리뷰 포인트가 없음
- 문서/테스트 영향이 보이지 않음
```

---

## 최소 운영 규칙

팀 상황상 모든 규칙을 엄격히 적용하기 어렵다면 최소한 아래 기준은 지킨다.

1. PR 제목은 명확하게 작성한다.
2. PR 본문에 변경 이유를 적는다.
3. 테스트 여부를 표시한다.
4. 구조/보안/도메인/API 변경 시 관련 docs를 수정한다.
5. 장기 결정은 ADR로 남긴다.