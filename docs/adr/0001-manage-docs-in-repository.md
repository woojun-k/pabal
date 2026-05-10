# ADR-0001: 공식 협업 문서는 레포의 docs 디렉터리에서 관리한다

## Status

Accepted

## Context

기존 문서는 Obsidian vault 형태로 정리되어 있었다. Obsidian은 개인 사고 정리, 문서 간 탐색, 초안 작성에는 강하지만 팀 협업에서 다음 한계가 있다.

- 코드 변경과 문서 변경을 같은 PR에서 리뷰하기 어렵다.
- 브랜치별 문서 상태를 코드와 정확히 맞추기 어렵다.
- 팀원이 Obsidian 사용법과 vault 구조를 함께 따라야 한다.
- GitHub에서 바로 읽기 어려운 `[[WikiLink]]`가 많아진다.

Pabal은 멀티모듈, DDD, CQRS, realtime, security, persistence 경계가 함께 움직이는 프로젝트다. 따라서 문서가 코드와 분리되면 빠르게 낡을 가능성이 높다.

## Decision

공식 협업 문서는 프로젝트 레포의 `docs/` 디렉터리에서 관리한다.

- Markdown 파일을 기본 형식으로 사용한다.
- 링크는 일반 Markdown 상대 링크를 사용한다.
- 설계 변경 PR에는 관련 문서 변경을 함께 포함한다.
- Obsidian은 개인 초안, 설계 탐색, 회고 용도로 사용할 수 있으나 SSoT는 `docs/`로 둔다.
- 구조적 설계 결정은 `docs/adr/`에 ADR로 남긴다.

## Consequences

### Positive

- 코드와 문서의 버전을 같은 브랜치에서 맞출 수 있다.
- 문서 변경도 PR 리뷰 대상이 된다.
- GitHub에서 별도 도구 없이 바로 읽을 수 있다.
- 이후 MkDocs, Docusaurus, GitHub Pages 등으로 확장하기 쉽다.

### Negative

- Obsidian graph 기반 탐색성은 일부 줄어든다.
- 문서 링크를 GitHub 기준 상대 링크로 유지해야 한다.
- 개인 메모와 공식 문서의 경계를 의식적으로 관리해야 한다.

### Follow-up

- [ ] 문서 변경이 필요한 PR 기준을 `docs/contributing/docs-guide.md`에 유지한다.
- [ ] README에서 주요 문서로 이동하는 진입점을 계속 최신화한다.
