# 용어 정의 (Glossary)

코드·API·DB·문서에서 **동일한 개념에 동일한 이름**을 쓰기 위한 기준 문서.
새 용어를 도입하기 전에 여기에 먼저 추가한다.

| 용어 | 코드 표기 | 정의 |
| --- | --- | --- |
| Career Profile | `CareerProfile` | 사용자의 경력 전반을 담는 루트. 경력/프로젝트/스킬/학력/자격/수상을 포함한다. |
| Career Evidence | `CareerEvidence` | 하나의 경험을 `problem / analysis / rootCause / action / result` 구조로 분해한 재사용 단위. 문서 생성과 Fit 판단의 **유일한 사실 출처**. |
| Evidence Reference | `evidenceRef` | 생성된 문장이 어떤 Career Evidence에 근거했는지 가리키는 참조. 생성 문장에는 반드시 하나 이상 붙는다. |
| Career Bank | — | 사용자의 취업 자산 전체를 저장하는 공간. Career Profile + Career Evidence의 상위 개념. |
| Job Posting | `JobPosting` | 사용자가 등록한 채용공고 원문 및 메타데이터. |
| Requirement | `JobRequirement` | 공고에서 구조화해 추출한 필수/우대 요구사항 단위. |
| Fit Score | `FitScore` | 공고 요구사항과 Career Evidence의 정합도 점수(0~100). 항목별 관련성은 LLM이 평가하고, **최종 합산은 시스템이 수행한다.** |
| Fit Analysis | — | Fit Score + 강점/약점 + 근거(Evidence 연결)를 산출하는 과정. |
| Application | `Application` | 하나의 공고에 대한 지원 건. Workspace의 실체. |
| Application Workspace | — | 하나의 공고를 중심으로 문서·일정·할 일·결과를 모아 보는 화면 단위. `Application`의 UI 표현. |
| Application Pipeline | `ApplicationStage` | 지원 상태 전이. `SAVED → ANALYZING → PREPARING → APPLIED → DOCUMENT_PASS → CODING_TEST → INTERVIEW → FINAL → OFFER` 및 각 단계의 REJECTED 상태. |
| Career Insight | `CareerInsight` | 누적된 지원 결과에서 도출한 개인별 시장 반응 분석. |
| Career Feedback Loop | — | 지원 결과를 Career Insight로 바꾸고 다음 추천에 반영하는 순환 구조. |
| Agent | — | 서비스 내부의 역할별 AI 실행 단위(Scout / Fit / Career / Strategy / Document / Coding / Interview). 사용자에게 직접 노출되지 않는다. |

## 표기 규칙

- 문서·코드 모두 위 영문 표기를 그대로 쓴다. 한글 번역어를 새로 만들지 않는다.
- DB 테이블은 snake_case 복수형 (`career_evidences`, `job_postings`).
- 상태값은 UPPER_SNAKE_CASE.
