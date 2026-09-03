# CareerOS

개발자의 취업 활동 전체를 하나의 흐름으로 연결하는 AI Career OS.
제품 문서는 [docs/README.md](docs/README.md)에서 시작한다.

## 저장소 구조

```text
backend/     Spring Boot (Java 17+) — API, 도메인, Fit Score 계산
frontend/    Next.js (TypeScript) — 웹 UI
docs/        제품·설계 문서 (정본)
.claude/     하네스 (skills, settings)
```

> 현재 Phase 0. `backend/`, `frontend/`는 아직 생성되지 않았다.
> 코드 작업을 시작할 때 이 문서의 구조 설명도 함께 갱신한다.

## 작업 전 반드시 읽을 문서

코드나 프롬프트를 작성하기 전에 아래를 확인한다. 추측으로 채우지 않는다.

| 상황 | 문서 |
| --- | --- |
| 모든 설계 판단 | [docs/00-overview/principles.md](docs/00-overview/principles.md) |
| AI 생성 기능 전반 | [docs/03-architecture/hallucination-policy.md](docs/03-architecture/hallucination-policy.md) |
| 경험 데이터 구조 | [docs/01-domain/career-evidence.md](docs/01-domain/career-evidence.md) |
| 네이밍 | [docs/01-domain/glossary.md](docs/01-domain/glossary.md) |
| Fit 점수 | [docs/02-features/fit-analysis.md](docs/02-features/fit-analysis.md) |

## 하드 제약 (위반 시 구현이 잘못된 것)

이 프로젝트는 AI가 생성한 결과의 **신뢰성**이 제품 가치의 전부다.
아래는 취향이 아니라 제약이다.

1. **Evidence 밖의 사실을 생성하지 않는다.**
   사용자가 입력하지 않은 성과·수치·기술 사용 경험을 문장으로 만들지 않는다.
   근거가 없으면 "근거 부족"으로 표시한다. 그럴듯하게 채우지 않는다.

2. **생성된 모든 문장은 Evidence Reference를 가진다.**
   Reference 없이 사용자에게 노출되는 생성 문장이 있으면 안 된다.
   응답 스키마 설계 시 `evidenceRef`를 optional로 두지 않는다.

3. **Fit Score의 최종 계산은 시스템이 한다.**
   LLM은 항목별 관련성만 평가한다. 총점을 LLM에게 물어보지 않는다.
   가중치(Required 35 / Experience 25 / Domain 10 / Evidence 15 / Level 10 / Preferred 5)는
   Java 코드 안의 상수이며, 프롬프트 안에 두지 않는다.

4. **모든 추천에는 근거를 붙인다.**
   점수만 반환하는 API는 만들지 않는다. 강점/약점 각각에 연결된 Evidence를 함께 반환한다.

5. **Chatbot-first로 만들지 않는다.**
   기능은 Workflow(화면·상태·할 일)로 표현한다. "AI에게 물어보세요" 형태의 UI를 기본값으로 두지 않는다.

## LLM 호출 규칙

- Structured Output(JSON Schema)을 기본으로 한다. 자유 텍스트 파싱에 의존하지 않는다.
- 프롬프트는 코드에 하드코딩하지 않고 버전 관리 가능한 리소스로 분리한다.
- 모델 응답을 그대로 DB에 저장하지 않는다. 도메인 객체로 검증 후 저장한다.
- 재현이 필요한 호출(Fit Analysis, Document 생성)은 입력·출력·모델·프롬프트 버전을 기록한다.

## 코드 작업

- 기존 코드를 읽기 전에 수정하지 않는다.
- 요청 범위를 넘는 리팩터링을 하지 않는다.
- 도메인 용어는 [glossary.md](docs/01-domain/glossary.md) 표기를 따른다. 새 용어는 glossary에 먼저 추가한다.
- DB 테이블은 snake_case 복수형, 상태값은 UPPER_SNAKE_CASE.

## 명령어

Phase 0 진행 중이라 아직 확정되지 않았다. 스캐폴딩 후 아래를 채운다.

```text
backend  test:  (미정)
backend  run:   (미정)
frontend dev:   (미정)
frontend build: (미정)
```

## 커밋

Conventional Commits를 따른다.

```text
feat(fit): Evidence 매칭 점수 계산 추가
fix(job): 공고 파싱 시 마감일 누락 처리
docs: Career Evidence 스키마 정리
chore(harness): skill 추가
```
