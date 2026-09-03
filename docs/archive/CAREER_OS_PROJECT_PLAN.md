# CareerOS — AI Career Operating System for Developers

> 공고 탐색부터 지원 판단, 경험 선택, 지원 관리, 코딩테스트, 면접, 결과 분석까지 하나의 맥락으로 운영하는 개발자 취업 AX 서비스

---

## 1. 프로젝트 개요

### 1.1 프로젝트명

**CareerOS**

부제:

**AI Career Operating System for Developers**

한 줄 소개:

> 개발자의 취업 활동 전체를 하나의 흐름으로 연결하고, 개인의 경력 데이터와 지원 결과를 기반으로 다음 행동까지 제안하는 AI 취업 운영 서비스

---

## 2. 문제 정의

현재 개발자 취업 준비는 여러 서비스에 흩어져 있다.

- 채용 공고는 원티드, 사람인, 잡코리아, 링크드인 등에서 찾는다.
- 이력서와 포트폴리오는 Notion, PDF, GitHub 등에 흩어져 있다.
- 자기소개서는 ChatGPT 같은 생성형 AI를 사용한다.
- 코딩테스트 준비는 백준, 프로그래머스, LeetCode 등에서 따로 진행한다.
- 면접 준비는 개인 메모나 블로그, 유튜브에 의존한다.
- 실제 지원 현황은 Excel, Notion, 메모장 등으로 관리한다.
- 탈락/합격 결과가 다음 지원 전략에 제대로 반영되지 않는다.

문제는 단순히 도구가 많다는 것이 아니다.

취업 과정에서 발생하는 모든 정보가 서로 연결되지 않기 때문에 사용자는 매번 다시 판단해야 한다.

예:

- 이 공고에 지원해야 하는가?
- 내 경험 중 무엇을 강조해야 하는가?
- 어떤 경험은 오히려 빼는 것이 좋은가?
- 서류 탈락 원인은 무엇일 가능성이 높은가?
- 코딩테스트까지 3일 남았을 때 무엇을 우선 공부해야 하는가?
- 면접에서는 어떤 프로젝트 질문이 나올 가능성이 높은가?
- 내가 실제로 합격률이 높은 직무 유형은 무엇인가?

CareerOS는 이러한 문제를 해결한다.

---

## 3. 제품 비전

CareerOS의 목표는 단순한 AI 자소서 생성기가 아니다.

사용자가 취업 목표를 설정하면 시스템이 전체 지원 과정을 지속적으로 관리하는 **개인 AI 커리어 운영체제**가 되는 것이 목표다.

```text
사용자 Career Profile
        ↓
채용공고 수집 / 입력
        ↓
공고 분석
        ↓
지원 적합도 판단
        ↓
지원 전략 생성
        ↓
이력서 / 자기소개서 구성
        ↓
지원 Pipeline 관리
        ↓
코딩테스트 준비
        ↓
면접 준비
        ↓
합격 / 탈락 결과 입력
        ↓
개인 전략 업데이트
        ↓
다음 지원 추천
```

---

## 4. 핵심 타깃 사용자

### Primary Target

**대한민국 신입 ~ 5년 차 개발자**

우선 지원 직군:

- Backend Developer
- Server Developer
- Platform Engineer
- Infrastructure Engineer
- DevOps Engineer
- Frontend Developer

초기 MVP에서는 개발 직군에 집중한다.

### 왜 개발자인가?

개발자는 다른 직군 대비 평가에 활용할 수 있는 구조화 가능한 데이터가 많다.

- GitHub
- 프로젝트
- 기술 스택
- 성능 개선 경험
- 트러블슈팅
- 코딩테스트 결과
- 기술 면접
- 오픈소스 활동
- 자격증
- 블로그
- 포트폴리오

따라서 CareerOS의 추천 결과를 단순한 LLM 추측이 아니라 **근거 기반 Career Evidence Matching**으로 만들기 용이하다.

---

## 5. 핵심 가치 제안

### 기존 서비스

```text
"공고를 추천해드립니다."
"자소서를 작성해드립니다."
"면접 질문을 생성해드립니다."
```

### CareerOS

```text
"당신이 이번 주 어떤 회사에 지원하고,
어떤 경험을 강조하고,
무엇을 준비해야 하는지 관리합니다."
```

핵심 차별점은 생성이 아니라 **운영(Operating)** 이다.

---

# 6. 핵심 개념 — Career Evidence

CareerOS의 가장 중요한 데이터 구조다.

사용자의 경력을 단순 텍스트로 저장하지 않고, 재사용 가능한 **Career Evidence** 단위로 구조화한다.

예:

```yaml
title: SSE 다중 연결 지연 개선

category:
  - Performance
  - Backend
  - Java
  - Troubleshooting

context:
  project: ESS 성능평가 시스템

problem:
  다중 SSE 연결 환경에서 화면 데이터 갱신이 최대 14초까지 지연됨

analysis:
  JFR을 사용해 실행 흐름과 Lock 경합 분석

rootCause:
  SseEmitter.send() 구간에서 Java Monitor Lock contention 발생

action:
  연결별 전송 구조 직렬화 개선

result:
  1000개 연결 환경에서 정체 제거
  JavaMonitorEnter 341 → 0

skills:
  - Java
  - Spring
  - SSE
  - JFR
  - Performance Analysis

usableFor:
  - 성능 최적화
  - 장애 분석
  - 실시간 시스템
  - 문제 해결
```

이 구조의 목적은 하나의 경험을 여러 공고와 문항에 반복적으로 활용할 수 있게 만드는 것이다.

---

# 7. 주요 기능

## 7.1 Career Bank

사용자의 모든 취업 자산을 저장하는 공간.

저장 대상:

- 기본 프로필
- 경력
- 프로젝트
- 기술 스택
- 자격증
- 수상
- 교육
- GitHub
- 블로그
- 포트폴리오
- 기존 이력서
- 기존 자기소개서
- 면접 질문
- 코딩테스트 기록
- 지원 결과

Career Bank 내부에서는 AI가 사용자 경험을 Career Evidence 단위로 분해한다.

---

## 7.2 Job Import

초기 MVP에서는 채용 플랫폼 전체 크롤링을 목표로 하지 않는다.

사용자가 채용공고 URL 또는 텍스트를 입력하면 공고를 등록한다.

지원 방식:

```text
URL 붙여넣기
텍스트 붙여넣기
PDF 업로드
브라우저 Extension (후속)
```

저장 정보:

- 회사
- 포지션
- 경력 조건
- 필수 역량
- 우대사항
- 업무 내용
- 기술 스택
- 전형 절차
- 마감일
- 근무지
- 채용공고 원문

---

## 7.3 Job Analyzer

채용공고를 구조화한다.

예:

```json
{
  "role": "Backend Developer",
  "requiredSkills": [
    "Java",
    "Spring",
    "RDBMS"
  ],
  "preferredSkills": [
    "Kubernetes",
    "Redis",
    "Kafka"
  ],
  "responsibilities": [
    "대규모 트래픽 서비스 개발",
    "시스템 성능 개선"
  ],
  "seniority": "Junior"
}
```

---

## 7.4 Fit Analysis

사용자의 Career Evidence와 공고를 비교한다.

단순 점수만 제공하지 않는다.

예:

```text
지원 적합도: 82 / 100

강점

Java / Spring                 매우 높음
DB 성능 분석                  높음
Redis                         높음
실시간 시스템                매우 높음
문제 분석                    매우 높음

보완 필요

Kubernetes 운영              보통
대규모 조직 협업             근거 부족
DDD                           근거 부족
```

각 판단에는 반드시 근거가 연결된다.

예:

```text
공고 요구:
"성능 문제를 측정하고 개선한 경험"

관련 Career Evidence:
ESS SSE 지연 개선

관련도:
94%

근거:
JFR 분석 → Lock contention 발견 → 1000 connection 검증
```

---

## 7.5 Application Recommendation

Fit Analysis 결과를 바탕으로 지원 여부를 추천한다.

결과 예:

```text
지원 추천: YES

추천 이유

1. 핵심 필수 역량 5개 중 4개 충족
2. 성능 분석 경험이 공고 요구와 높은 연관성
3. Redis / 실시간 처리 경험 보유
4. 경력 조건 충족

위험 요소

1. Kubernetes 운영 경험이 제한적
2. 대규모 조직 협업 사례가 부족함
```

---

## 7.6 Application Strategy

지원하기로 결정한 공고에 대해 전략을 생성한다.

예:

```text
이번 지원에서 강조할 경험

1. ESS SSE 성능 개선
2. FlowTicket 600 RPS 부하 테스트
3. Redis Lua 기반 동시성 제어

가급적 제외

- 단순 CRUD 기능 개발
- 짧은 교육 프로젝트 나열

보완해야 할 항목

- Kubernetes 질문 대비
- Redis 장애 대응 정리
- Kafka Outbox 패턴 복습
```

---

# 8. Application Workspace

하나의 채용공고가 하나의 Workspace가 된다.

예:

```text
Toss Backend Developer

────────────────────

공고 분석              완료
이력서                 완료
자기소개서             작성 중
코딩테스트             예정
면접                   대기
최종 결과              -

────────────────────

현재 할 일

□ 자기소개서 2번 문항 완료
□ Redis 장애 대응 정리
□ 이번 주 BFS/DFS 5문제 풀이
```

---

# 9. Application Pipeline

지원 상태를 하나의 Pipeline으로 관리한다.

상태 예:

```text
SAVED
   ↓
ANALYZING
   ↓
PREPARING
   ↓
APPLIED
   ↓
DOCUMENT_PASS
   ↓
CODING_TEST
   ↓
INTERVIEW
   ↓
FINAL
   ↓
OFFER
```

탈락 상태:

```text
DOCUMENT_REJECTED
CODING_TEST_REJECTED
INTERVIEW_REJECTED
FINAL_REJECTED
```

---

# 10. Document Agent

문서를 새로 창작하는 기능이 아니라 Career Evidence를 기반으로 조합하는 기능이다.

예:

```text
공고:
성능 개선 경험 요구

↓

Career Evidence 검색

↓

후보

1. SSE 14초 지연 개선
2. PQM 조회 1.22s → 426ms
3. EIS 조회 601ms → 44.82ms

↓

공고와 가장 관련 높은 경험 선택

↓

이력서 / 자기소개서 문장 구성
```

목표:

**Hallucination 최소화**

AI는 사용자가 제공한 Career Evidence 바깥의 성과를 생성하지 않는다.

---

# 11. Coding Test Manager

지원 중인 회사의 전형 일정과 사용자의 풀이 기록을 연결한다.

예:

```text
SKT Coding Test

D-3

최근 풀이

DFS/BFS        8
Implementation 4
Hash           3
DP             0

추천

오늘:
DFS/BFS 복습 3문제
구현 2문제

내일:
Hash 2문제
실전 모의 1회
```

향후 기능:

- 백준 연동
- 프로그래머스 연동
- 문제 유형 자동 분류
- 오답 기록
- 지원 회사별 준비 전략

---

# 12. Interview Manager

지원 회사와 공고, Career Evidence를 기반으로 예상 질문을 생성한다.

예:

```text
FlowTicket

예상 질문

1. Redis Lua를 사용한 이유는 무엇인가?
2. 분산락 대신 Lua를 선택한 이유는?
3. Redis 장애 시 대기열은 어떻게 되는가?
4. 600 RPS 부하 테스트에서 병목은 무엇이었는가?
```

사용자의 답변 기록을 저장하여 반복 학습한다.

```text
질문
↓
사용자 답변
↓
AI 피드백
↓
개선 답변
↓
최종 답변 저장
```

---

# 13. Career Feedback Loop

CareerOS에서 가장 중요한 장기 기능.

지원 결과가 누적되면 사용자의 실제 시장 반응을 분석한다.

예:

```text
최근 지원 15건

Backend Developer
지원 8
서류 합격 6

Platform Engineer
지원 4
서류 합격 3

AI Engineer
지원 3
서류 합격 0
```

분석:

```text
현재 프로필에서는

Backend / Platform 직무에서
서류 반응이 높습니다.

AI Engineer 포지션은
관련 프로젝트 근거가 부족해
합격 가능성이 낮게 관찰됩니다.
```

이를 다음 공고 추천에 반영한다.

---

# 14. Personal Career Graph

장기적으로 사용자의 경력을 Graph 형태로 표현한다.

예:

```text
User
 ├─ Skill
 │   ├─ Java
 │   ├─ Spring
 │   ├─ Redis
 │   └─ PostgreSQL
 │
 ├─ Project
 │   ├─ ESS
 │   └─ FlowTicket
 │
 ├─ Evidence
 │   ├─ SSE Performance
 │   ├─ Redis Lua
 │   └─ DB Optimization
 │
 ├─ Application
 │   ├─ Toss
 │   ├─ SKT
 │   └─ Naver
 │
 └─ Result
     ├─ Pass
     └─ Reject
```

이를 기반으로 개인화된 전략을 만든다.

---

# 15. 내부 AX 구조

사용자는 여러 Agent를 직접 관리하지 않는다.

서비스 내부에서 역할별 Agent가 동작한다.

```text
                  Career Memory
                       │
            ┌──────────┼──────────┐
            │          │          │
            ▼          ▼          ▼
        Scout       Fit        Career
        Agent       Agent       Agent
            │          │          │
            └──────────┼──────────┘
                       ▼
                  Strategy Agent
                       │
          ┌────────────┼────────────┐
          │            │            │
          ▼            ▼            ▼
      Document       Coding      Interview
       Agent         Agent        Agent
          │            │            │
          └────────────┼────────────┘
                       ▼
                   User Action
                       │
                       ▼
                      Result
                       │
                       ▼
                 Strategy Update
```

---

# 16. Agent 역할

## Scout Agent

역할:

- 채용공고 분석
- 사용자 조건과 맞는 공고 후보 선정
- 마감 임박 공고 탐지

---

## Fit Agent

역할:

- Job Requirement ↔ Career Evidence Matching
- Fit Score 계산
- 강점 / 약점 분석
- 근거 제공

---

## Career Agent

역할:

- 현재 경력 수준 판단
- 지원 가능한 직무 범위 분석
- 부족한 역량 분석

---

## Strategy Agent

역할:

- 지원 여부 판단
- 강조할 경험 선택
- 제외할 경험 선택
- 준비 전략 생성

---

## Document Agent

역할:

- 이력서 구성
- 자기소개서 구성
- Career Evidence 기반 문장 생성

---

## Coding Agent

역할:

- 코딩테스트 일정 관리
- 학습 계획 생성
- 취약 유형 분석

---

## Interview Agent

역할:

- 예상 면접 질문 생성
- 사용자 답변 평가
- 프로젝트별 질문 관리

---

# 17. MVP 범위

초기 버전에서는 기능을 과하게 확장하지 않는다.

## MVP 1

### Career Bank

- 프로필 등록
- 경력 등록
- 프로젝트 등록
- Career Evidence 생성

### Job

- 공고 URL 입력
- 공고 텍스트 입력
- 공고 구조화

### Fit Analysis

- 공고 ↔ Career Evidence 비교
- Fit Score
- 강점 / 약점
- 지원 추천

### Application Workspace

- 지원 상태 관리
- 일정 관리
- 할 일 관리

### Strategy

- 강조 경험 추천
- 부족 역량 분석
- 지원 전략

---

# 18. MVP에서 제외할 기능

초기에는 다음 기능을 만들지 않는다.

- 전체 채용 플랫폼 크롤링
- 자동 지원
- 자동 이메일 발송
- 기업 HR 기능
- 모든 직군 지원
- 자체 코딩테스트 플랫폼
- 자동 면접 영상 분석
- 완전 자동 자기소개서 생성

핵심은 기능 개수가 아니라 **Career Memory + Application Workflow**다.

---

# 19. 사용자 Flow

```text
회원가입
   ↓
Career Profile 작성
   ↓
이력서 / 프로젝트 등록
   ↓
Career Evidence 생성
   ↓
공고 URL 입력
   ↓
공고 분석
   ↓
Fit Analysis
   ↓
지원 여부 결정
   ↓
Application Workspace 생성
   ↓
지원 전략
   ↓
이력서 / 자기소개서 준비
   ↓
지원
   ↓
코딩테스트 / 면접
   ↓
결과 기록
   ↓
개인 전략 업데이트
```

---

# 20. 주요 화면

## Dashboard

```text
이번 주 Career Dashboard

지원 중                    7
서류 결과 대기             3
코딩테스트 예정            1
면접 예정                  1

────────────────────────

추천 공고

Toss Backend Platform      87
Naver Backend              83
Kakao Server               79

────────────────────────

오늘 해야 할 일

1. Toss 지원서 제출
2. SKT BFS 3문제
3. Redis 장애 대응 복습

────────────────────────

Career Insight

Backend 지원
서류 합격률 68%

Platform 지원
서류 합격률 75%

AI Engineer
서류 합격률 0%
```

---

## Job Detail

```text
Toss Backend Developer

Fit Score
82 / 100

강점

Java/Spring          ██████████
Redis                █████████
Performance          ██████████
Kubernetes           █████

추천 Evidence

1. SSE 성능 개선
2. Redis Lua 동시성
3. 600 RPS 부하 테스트
```

---

## Application Workspace

```text
Toss Backend

공고 분석             ✓
이력서                ✓
자기소개서            진행 중
코딩테스트            예정
면접                  대기

Next Action

자기소개서 2번 문항 완료
```

---

# 21. 기술 아키텍처

초기 추천 구조:

```text
                 Web / Mobile
                      │
                      ▼
                 API Gateway
                      │
                      ▼
                 Spring Boot
                      │
        ┌─────────────┼─────────────┐
        │             │             │
        ▼             ▼             ▼
      User          Career        Application
     Service        Service        Service
        │             │             │
        └─────────────┼─────────────┘
                      │
                      ▼
                  PostgreSQL


                 AI Orchestrator
                      │
        ┌─────────────┼─────────────┐
        ▼             ▼             ▼
    Job Agent      Fit Agent    Strategy Agent
        │             │             │
        └─────────────┼─────────────┘
                      ▼
                  LLM Provider


                   Vector DB
                      │
                      ▼
                Career Evidence
```

---

# 22. 추천 기술 스택

## Backend

- Java 17+
- Spring Boot
- Spring Security
- Spring Data JPA 또는 MyBatis
- PostgreSQL
- Redis

## AI

- OpenAI / Anthropic API
- Embedding
- Vector Search
- Structured Output
- Tool Calling

## Vector Search

초기:

- PostgreSQL + pgvector

향후:

- Qdrant
- Weaviate
- Pinecone

## Frontend

- React
- Next.js
- TypeScript

## Infra

초기:

- Docker
- GitHub Actions
- AWS

향후:

- Kubernetes
- OpenTelemetry
- Prometheus
- Grafana

---

# 23. 핵심 도메인 모델

```text
User

CareerProfile
 ├─ Experience
 ├─ Project
 ├─ Skill
 ├─ Education
 ├─ Certificate
 └─ Award

CareerEvidence

JobPosting
 ├─ Requirement
 ├─ PreferredQualification
 └─ RecruitmentProcess

JobFit

Application
 ├─ ApplicationStage
 ├─ Document
 ├─ CodingTest
 ├─ Interview
 └─ Result

CareerInsight
```

---

# 24. 초기 DB 설계 예시

```text
users

career_profiles

experiences

projects

skills

career_evidences

job_postings

job_requirements

job_fit_results

applications

application_stages

application_tasks

documents

coding_test_records

interview_questions

interview_answers

application_results

career_insights
```

---

# 25. Fit Score 설계

단순 LLM 점수를 그대로 사용하지 않는다.

예:

```text
Fit Score

Required Skill Match       35%
Experience Match           25%
Domain Match               10%
Project Evidence           15%
Career Level               10%
Preferred Qualification     5%
```

예:

```text
Required Skill        30 / 35
Experience            21 / 25
Domain                 7 / 10
Evidence              13 / 15
Career Level           8 / 10
Preferred              3 / 5

TOTAL
82 / 100
```

LLM은 각 항목의 관련성을 평가하지만 최종 점수 계산은 시스템이 수행한다.

---

# 26. Hallucination 방지 전략

CareerOS에서는 신뢰성이 중요하다.

원칙:

1. Career Evidence에 존재하지 않는 경험 생성 금지
2. 숫자 성과 자동 생성 금지
3. 사용자가 입력하지 않은 기술 사용 경험 생성 금지
4. 생성된 문장마다 Evidence Reference 저장
5. 사용자가 최종 확인 후 문서 사용

예:

```text
Generated Sentence

"JFR을 활용해 SSE 지연 원인을 분석하고
1000개의 동시 연결 환경에서 병목을 제거했습니다."

Evidence:
CE-00021

Source:
ESS Project
```

---

# 27. 수익 모델

## Free

- Career Profile
- 공고 저장
- 월 5회 Fit Analysis
- Application Tracker

## Pro

예:

월 9,900 ~ 19,900원

- 무제한 Fit Analysis
- AI 지원 전략
- Document Agent
- Interview Agent
- Coding Test Planner
- Career Insight

## 향후

- 대학 취업센터 B2B
- 부트캠프
- 개발자 교육기관
- 취업 컨설팅 업체
- Recruiting Partnership

---

# 28. 핵심 KPI

초기:

- Weekly Active Users
- 등록 공고 수
- Application 생성 수
- Fit Analysis 사용률
- Career Evidence 생성 수

제품 가치 검증:

- 지원 전환율
- 서류 합격률
- Interview 전환율
- 사용자 Retention
- 추천 공고 실제 지원율

---

# 29. 경쟁력 / Moat

CareerOS의 장기 경쟁력은 LLM이 아니다.

LLM은 모든 경쟁사가 사용할 수 있다.

진짜 자산은 다음 데이터다.

```text
사용자 Career Evidence
+
채용공고
+
지원 당시 전략
+
사용한 이력서
+
사용한 자기소개서
+
코딩테스트 결과
+
면접 질문
+
서류 결과
+
최종 결과
```

이 데이터가 쌓이면 개인별 취업 전략이 점점 정교해진다.

---

# 30. 제품 확장 방향

## Phase 1

Developer Career OS

## Phase 2

IT 직군 확대

- Data Engineer
- Data Analyst
- Product Manager
- Designer

## Phase 3

전체 직군

## Phase 4

Career Lifecycle

```text
취업
 ↓
이직
 ↓
커리어 성장
 ↓
연봉 협상
 ↓
다음 이직
```

궁극적으로는:

> "취업 도구"가 아니라 "개인의 커리어를 지속적으로 운영하는 AI"

를 목표로 한다.

---

# 31. 개발 로드맵

## Phase 0 — Foundation

- Repository 생성
- Spring Boot
- Next.js
- PostgreSQL
- Authentication
- 기본 CI

## Phase 1 — Career Bank

- Career Profile CRUD
- Project / Experience CRUD
- Career Evidence 생성
- Evidence Search

## Phase 2 — Job Analysis

- Job 등록
- Job Analyzer
- Requirement 구조화

## Phase 3 — Fit Engine

- Evidence Matching
- Fit Score
- 근거 표시
- 지원 추천

## Phase 4 — Application OS

- Pipeline
- Task
- Deadline
- Workspace

## Phase 5 — AI Strategy

- 지원 전략
- Evidence 추천
- Document 지원

## Phase 6 — Feedback Loop

- 지원 결과 저장
- 개인 통계
- Career Insight

---

# 32. 첫 번째 데모 시나리오

데모 사용자가 자신의 Career Profile을 등록한다.

```text
Java Backend Developer
경력 1년

Skills

Java
Spring
PostgreSQL
Redis
Kafka
AWS
```

프로젝트:

```text
ESS
FlowTicket
```

이후 Backend 채용공고를 등록한다.

CareerOS가:

```text
Fit Score: 84

추천 지원: YES

추천 Evidence

1. SSE 지연 개선
2. Redis Lua 동시성 제어
3. EKS 부하 테스트

부족 역량

Kubernetes 운영
DDD
```

를 보여준다.

사용자가 지원하기를 누르면 Application Workspace가 생성된다.

이 흐름이 MVP의 핵심 데모다.

---

# 33. 프로젝트 성공 기준

단순히 AI 기능을 많이 만드는 것이 목표가 아니다.

다음 질문에 YES라고 답할 수 있어야 한다.

> 사용자가 채용공고 하나를 발견했을 때 CareerOS만 열어도
> "지원할지 / 무엇을 강조할지 / 무엇을 준비할지"
> 결정할 수 있는가?

YES라면 제품의 핵심 가치가 구현된 것이다.

---

# 34. README용 짧은 소개

```text
CareerOS is an AI-powered career operating system for developers.

Instead of simply generating resumes or recommending job postings,
CareerOS connects a developer's career evidence, job applications,
coding tests, interviews, and outcomes into a single continuous workflow.

It analyzes job postings, matches them against verified career evidence,
recommends application strategies, tracks recruiting pipelines,
and improves future recommendations based on real application results.
```

---

# 35. GitHub Repository 설명

Repository Name:

```text
careeros
```

GitHub Description:

```text
AI-powered Career Operating System for developers — job fit analysis, career evidence, application strategy, recruiting pipeline, coding tests, interviews, and feedback loops.
```

---

# 36. 한글 소개 문구

```text
개발자의 경력 데이터와 실제 지원 결과를 연결하여
공고 분석 → 지원 판단 → 경험 선택 → 지원 관리 → 코테 → 면접 → 결과 분석까지
하나의 흐름으로 운영하는 AI Career OS
```

---

# 37. 슬로건 후보

### Option 1

**Don't just apply. Operate your career.**

### Option 2

**Your career, continuously optimized.**

### Option 3

**From job discovery to offer. One career system.**

### Option 4

**취업 준비가 아니라, 커리어 운영.**

---

# 38. 프로젝트 핵심 원칙

1. Chatbot-first 제품을 만들지 않는다.
2. AI 생성보다 Workflow를 우선한다.
3. 모든 추천에는 근거를 제공한다.
4. 사용자의 실제 경험을 벗어나지 않는다.
5. 지원 결과가 다음 전략에 반영되어야 한다.
6. 사용자가 오늘 무엇을 해야 하는지 명확히 보여준다.
7. 처음부터 모든 직군을 지원하지 않는다.
8. 개발자 취업 시장에서 먼저 Product-Market Fit을 검증한다.

---

# 39. 장기 비전

CareerOS가 충분한 데이터를 확보하면 사용자는 매번 취업 준비를 처음부터 다시 할 필요가 없다.

CareerOS는 사용자의 과거 경험과 시장 반응을 지속적으로 기억한다.

```text
당신이 무엇을 잘하는지
어떤 회사에서 반응이 좋은지
어떤 경험이 서류에서 효과적이었는지
어떤 면접 질문에 약한지
어떤 기술이 현재 부족한지
```

를 알고 다음 커리어 액션을 제안한다.

최종적인 제품 비전은 다음과 같다.

> **Every developer should have an AI Career Manager.**

