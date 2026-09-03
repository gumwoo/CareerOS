# CareerOS

개발자의 취업 활동 전체를 하나의 흐름으로 연결하는 AI Career OS.

## Product invariants

되돌리면 제품이 무너지는 제약. 취향이 아니다.

- **Career Evidence가 사용자 경력의 source of truth다.** 이력서 원문·GitHub는 입력이지 사실이 아니다.
- **Evidence Reference 없는 경력/성과 문장 생성 금지.** 근거가 없으면 "근거 부족"으로 반환한다.
- **Fit Score 최종 계산은 backend가 한다.** LLM은 항목별 relevance와 explanation만 담당한다.
- **모든 추천에 근거를 붙인다.** 점수만 반환하는 API를 만들지 않는다.
- **"낮음"과 "근거 부족"을 구분한다.** 전자는 실력 보완, 후자는 데이터 입력을 요구한다.

이 규칙들은 지침일 뿐 런타임 보장이 아니다. 실제 강제는 도메인 타입과 DB 제약으로 한다.
→ @docs/adr/0004-generated-claims-require-evidence-reference.md

## Required references

- Product principles: @docs/00-overview/principles.md
- Hallucination policy: @docs/03-architecture/hallucination-policy.md
- Career Evidence model: @docs/01-domain/career-evidence.md
- Glossary (naming): @docs/01-domain/glossary.md
- Harness / invariants: [docs/03-architecture/harness.md](docs/03-architecture/harness.md)

기능 구현 전에는 해당 문서를 먼저 읽는다. 문서 목록은 [docs/README.md](docs/README.md).
아키텍처 결정의 근거는 [docs/adr/](docs/adr/)에 있다. 어긋나는 구현을 만들지 않는다.

## Repository

```text
backend/     Spring Boot 4.1 + Gradle (Groovy DSL), Java 17
frontend/    Next.js 16 + TypeScript + Tailwind (App Router, src/)
docs/        제품·도메인·아키텍처 문서 (정본)
schemas/     Agent 출력 검증용 JSON Schema (실행 가능한 계약)
.claude/     하네스
```

`frontend/`에는 Next.js가 생성·갱신하는 `AGENTS.md`가 있다(`frontend/CLAUDE.md`가 이를 import).
프론트 작업 시 그쪽 지침도 함께 적용된다.

## Conventions

- 도메인 용어는 glossary 표기를 따른다. 새 용어는 glossary에 먼저 추가한다.
- DB 테이블은 snake_case 복수형, 상태값은 UPPER_SNAKE_CASE.
- LLM 호출은 Structured Output을 기본으로 하고, 응답은 `schemas/`로 검증한 뒤 도메인 객체로 변환한다.
  모델 응답을 그대로 DB에 저장하지 않는다.
- `schemas/`와 `docs/`의 정본 정의는 함께 변경한다.
- **실패를 발견하면 주의사항으로 남기지 않는다.** 불변식으로 적고, 가드로 강제하고,
  가드가 실제로 잡는지 메타테스트로 확인하고, CI에 건다.
  절차는 [harness.md](docs/03-architecture/harness.md).

## 브랜치 / 커밋 / PR

- **기본 브랜치에 직접 커밋하지 않는다.** 브랜치를 파서 PR로 올린다.
- 브랜치 이름은 `<type>/<kebab-case-요약>`. type은 `feat` / `fix` / `docs` / `chore` / `imp`.

  ```text
  feat/evidence-numeric-guard
  fix/excerpt-length-bypass
  docs/adr006-job-parse
  ```

- 커밋 제목은 Conventional Commits + 한국어 평서문.
  "무엇을 했다"가 아니라 **무엇이 문제였고 어떻게 바로잡는지**를 담는다.

  ```text
  fix(evidence): excerpt 한 글자가 원문 대조를 통과하던 구멍을 막는다
  docs(adr): ADR-004의 카디널리티 설명이 Java·DB 예시와 어긋난다
  ```

- 커밋 본문에는 **확인에 쓴 명령과 그 출력**을 남긴다. 수치를 주장하면 근거를 같이 쓴다.
- 커밋 메시지·PR 본문에 `Co-Authored-By: Claude ...` 트레일러나 "Generated with Claude Code"
  문구를 **넣지 않는다.**
- PR 본문은 [`.github/pull_request_template.md`](.github/pull_request_template.md)의
  **7개 섹션을 전부** 채운다. 해당 없으면 "해당 없음"으로 명시하고 섹션을 삭제하지 않는다.
  파일 나열이 아니라 동작·의도 중심으로 쓴다.
- `gh pr create --body-file`로 생성한다. 빈 본문 금지.
- 브랜치에 커밋을 더 얹으면 **PR 본문도 함께 갱신**해 실제 내용과 일치시킨다.

## Validation

```bash
# 계약 (cwd: 저장소 루트)
python tools/check_contracts.py       # schemas/ 와 정본 문서의 정합성
python tools/meta_test_contracts.py   # 검사가 실제로 위반을 잡는지

# backend  (cwd: backend/)
./gradlew test              # 단위 테스트. Docker 불필요.
./gradlew integrationTest   # Testcontainers. Docker 필요.

# frontend (cwd: frontend/)
pnpm lint && pnpm build
```

테스트는 두 계층으로 나뉜다. `src/test`는 Docker 없이 돌아야 하고,
DB·컨테이너가 필요한 것은 `src/integrationTest`에 둔다.
Fit Score 계산이나 excerpt 대조 같은 핵심 로직은 DB 없이 검증할 수 있어야 한다.

로컬 개발용 DB는 `docker compose up -d postgres`.
테스트 컨테이너와 docker-compose는 **같은 이미지**(`pgvector/pgvector:pg16`)를 쓴다.
스키마는 **Flyway가 소유한다.** `ddl-auto: validate`이므로 Hibernate가 테이블을 만들지 않는다.
엔티티를 바꾸면 `backend/src/main/resources/db/migration/`에 마이그레이션을 추가한다.

> 이 환경에서 `pnpm`이 PATH에 없으면 `corepack pnpm ...`으로 실행한다.
> 전역 설치는 관리자 권한 셸에서 `corepack enable pnpm`.
