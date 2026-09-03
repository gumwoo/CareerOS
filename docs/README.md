# CareerOS Docs

> 개발자의 취업 활동 전체를 하나의 흐름으로 연결하고, 개인의 경력 데이터와 지원 결과를 기반으로 다음 행동까지 제안하는 AI 취업 운영 서비스

원본 기획서는 [archive/CAREER_OS_PROJECT_PLAN.md](archive/CAREER_OS_PROJECT_PLAN.md)에 그대로 보존되어 있다.
아래 문서들은 그 내용을 변경 빈도에 따라 분리한 것이며, **이후 수정은 아래 문서에서만 한다.**

## 00. Overview — 거의 변하지 않는 것

| 문서 | 내용 |
| --- | --- |
| [problem.md](00-overview/problem.md) | 문제 정의 |
| [vision.md](00-overview/vision.md) | 제품 비전 · 핵심 가치 제안 · 장기 비전 |
| [target-users.md](00-overview/target-users.md) | 핵심 타깃 사용자 |
| [principles.md](00-overview/principles.md) | **프로젝트 핵심 원칙 8개** — 모든 설계 판단의 기준 |
| [brand-and-copy.md](00-overview/brand-and-copy.md) | 소개 문구 · 슬로건 · Repository 설명 |

## 01. Domain — 설계의 정본

| 문서 | 내용 |
| --- | --- |
| [career-evidence.md](01-domain/career-evidence.md) | **Career Evidence 구조** — 제품에서 가장 중요한 데이터 구조 |
| [domain-model.md](01-domain/domain-model.md) | 핵심 도메인 모델 |
| [glossary.md](01-domain/glossary.md) | 용어 정의 (코드 네이밍의 기준) |

## ADR — 되돌리면 제품이 바뀌는 결정

| ADR | 결정 |
| --- | --- |
| [0001](adr/0001-record-architecture-decisions.md) | 아키텍처 결정을 ADR로 기록한다 |
| [0002](adr/0002-fit-score-computed-by-system-not-llm.md) | Fit Score 최종 계산은 LLM이 아니라 backend가 한다 |
| [0003](adr/0003-career-evidence-is-source-of-truth.md) | Career Evidence를 사용자 경력의 유일한 사실 출처로 삼는다 |
| [0004](adr/0004-generated-claims-require-evidence-reference.md) | 생성된 경력 주장은 Evidence Reference를 필수로 가진다 |
| [0005](adr/0005-pgvector-first.md) | Vector Search는 PostgreSQL + pgvector로 시작한다 |

## 실행 가능한 계약

문서가 아니라 런타임 검증에 쓰이므로 `docs/` 바깥에 있다. → [schemas/](../schemas/README.md)

## 02. Features

| 문서 | 내용 |
| --- | --- |
| [career-bank.md](02-features/career-bank.md) | Career Bank |
| [job-import-analyzer.md](02-features/job-import-analyzer.md) | Job Import / Job Analyzer |
| [fit-analysis.md](02-features/fit-analysis.md) | Fit Analysis · 지원 추천 · **Fit Score 산식** |
| [application-strategy.md](02-features/application-strategy.md) | 지원 전략 생성 |
| [application-workspace.md](02-features/application-workspace.md) | Application Workspace · Pipeline |
| [document-agent.md](02-features/document-agent.md) | Document Agent |
| [coding-test.md](02-features/coding-test.md) | Coding Test Manager |
| [interview.md](02-features/interview.md) | Interview Manager |
| [feedback-loop.md](02-features/feedback-loop.md) | Career Feedback Loop · Personal Career Graph |

## 03. Architecture

| 문서 | 내용 |
| --- | --- |
| [system-architecture.md](03-architecture/system-architecture.md) | 시스템 아키텍처 |
| [tech-stack.md](03-architecture/tech-stack.md) | 기술 스택 |
| [ai-orchestration.md](03-architecture/ai-orchestration.md) | 내부 AX 구조 · Agent 역할 |
| [hallucination-policy.md](03-architecture/hallucination-policy.md) | **Hallucination 방지 정책** — AI 기능의 하드 제약 |
| [harness.md](03-architecture/harness.md) | **검증 하네스** — 실패를 규칙으로 승격하는 절차와 현재 불변식 목록 |
| [data-model.md](03-architecture/data-model.md) | 초기 DB 설계 |

## 04. Product — 자주 변하는 것

| 문서 | 내용 |
| --- | --- |
| [mvp-scope.md](04-product/mvp-scope.md) | MVP 범위 / 제외 기능 |
| [roadmap.md](04-product/roadmap.md) | 개발 로드맵 (Phase 0~6) |
| [user-flow.md](04-product/user-flow.md) | 사용자 Flow |
| [screens.md](04-product/screens.md) | 주요 화면 |
| [demo-scenario.md](04-product/demo-scenario.md) | 첫 데모 시나리오 · 성공 기준 |
| [kpi.md](04-product/kpi.md) | 핵심 KPI |
| [business-model.md](04-product/business-model.md) | 수익 모델 · Moat · 확장 방향 |
