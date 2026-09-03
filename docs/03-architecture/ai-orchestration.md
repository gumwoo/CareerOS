# AX 구조 / Agent 역할

## 내부 AX 구조

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

## Agent 역할

### Scout Agent

역할:

- 채용공고 분석
- 사용자 조건과 맞는 공고 후보 선정
- 마감 임박 공고 탐지

---

### Fit Agent

역할:

- Job Requirement ↔ Career Evidence Matching
- Fit Score 계산
- 강점 / 약점 분석
- 근거 제공

---

### Career Agent

역할:

- 현재 경력 수준 판단
- 지원 가능한 직무 범위 분석
- 부족한 역량 분석

---

### Strategy Agent

역할:

- 지원 여부 판단
- 강조할 경험 선택
- 제외할 경험 선택
- 준비 전략 생성

---

### Document Agent

역할:

- 이력서 구성
- 자기소개서 구성
- Career Evidence 기반 문장 생성

---

### Coding Agent

역할:

- 코딩테스트 일정 관리
- 학습 계획 생성
- 취약 유형 분석

---

### Interview Agent

역할:

- 예상 면접 질문 생성
- 사용자 답변 평가
- 프로젝트별 질문 관리

---
