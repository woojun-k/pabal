# 문서 작성 가이드

## 목적

이 문서는 `docs/`를 팀 협업용 공식 문서 공간으로 운영하기 위한 기준이다.

코드 구조, 아키텍처, 도메인 규칙, API, 운영 방식, ADR 등 팀원이 따라야 하는 내용은 반드시 `docs/`에 기록한다.

문서 변경은 코드 변경과 동일하게 Pull Request를 통해 리뷰한다.
설계나 구조 변경이 발생한 경우 관련 문서도 같은 PR에서 함께 수정하는 것을 원칙으로 한다.

## 기본 원칙

- 문서는 코드와 같은 변경 단위로 관리한다.
- 코드 구조, API, DB schema, 보안 정책, realtime contract가 바뀌면 관련 문서를 함께 수정한다.
- 설계 결정은 결과만 적지 말고 맥락과 trade-off를 함께 남긴다.
- 확정되지 않은 아이디어는 `Status: Draft` 또는 `Status: Proposed`로 표시한다.
- 개인 메모성 문서는 레포에 직접 넣기보다 Obsidian 또는 별도 초안에서 다듬은 뒤 반영한다.

## 문서 위치 기준

| 문서 성격 | 위치 |
| --- | --- |
| 전체 개요와 읽기 순서 | `docs/README.md` |
| 아키텍처, 모듈, 런타임, 운영 | `docs/architecture/` |
| 도메인 모델과 규칙 | `docs/domain/` |
| 유스케이스, API, 에러 매핑 | `docs/use-cases/` |
| WebSocket, STOMP, realtime contract | `docs/realtime/` |
| JWT, principal, authorization, multitenancy | `docs/security/` |
| 테스트 전략과 케이스 | `docs/testing/` |
| 설계 결정 기록 | `docs/adr/` |

## PR 체크리스트

문서가 필요한 변경인지 판단할 때 아래를 확인한다.

- [ ] 모듈 의존 방향이 바뀌었는가?
- [ ] 새로운 도메인 규칙 또는 invariant가 생겼는가?
- [ ] API request/response/error shape가 바뀌었는가?
- [ ] DB schema, constraint, migration이 바뀌었는가?
- [ ] realtime event type/payload/destination이 바뀌었는가?
- [ ] JWT claim, principal, authorization boundary가 바뀌었는가?
- [ ] 테스트 기준 또는 운영 설정이 바뀌었는가?
- [ ] ADR이 필요한 수준의 기술 선택인가?

## ADR 작성 기준

ADR은 다음 조건 중 하나라도 만족하면 작성한다.

- 되돌리기 어려운 구조적 결정이다.
- 여러 선택지 사이의 trade-off가 존재한다.
- 팀원이 나중에 “왜 이렇게 했는가?”를 물을 가능성이 높다.
- 코드만 보고 의도를 파악하기 어렵다.

작성 시에는 `docs/adr/template.md`를 복사해서 사용한다.
