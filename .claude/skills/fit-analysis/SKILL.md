---
name: fit-analysis
description: 구조화된 채용공고와 사용자의 Career Evidence를 비교해 Fit Score, 강점/약점, 근거를 산출할 때 사용한다. "이 공고 적합도", "지원할까", "매칭해줘", "Fit Score" 같은 요청 및 Fit 관련 코드 구현 시 사용.
---

# Fit Analysis

공고 요구사항과 Career Evidence를 매칭한다.
산식 정본은 `docs/02-features/fit-analysis.md`.

## 절대 규칙 — 점수 계산 책임

**총점을 LLM이 만들지 않는다.** LLM은 항목별 관련성만 평가하고, 합산은 시스템이 한다.

이 스킬을 대화에서 사용할 때도 동일하게, 항목별 평가를 먼저 표로 만든 뒤 가중치를 적용해 산술 계산한다.
"대략 82점 정도" 같은 직관적 총점을 쓰지 않는다.

| 항목 | 가중치 |
| --- | --- |
| Required Skill Match | 35 |
| Experience Match | 25 |
| Project Evidence | 15 |
| Domain Match | 10 |
| Career Level | 10 |
| Preferred Qualification | 5 |

## 절대 규칙 — 근거

- 모든 강점/약점 판단에 **Career Evidence를 연결한다.** 연결할 Evidence가 없으면 강점으로 쓰지 않는다.
- 연결할 근거가 없는 요구사항은 "낮음"이 아니라 **"근거 부족"** 으로 구분해 표기한다.
  (실제로 못하는 것과, 데이터가 없는 것은 다르다. 사용자에게 다른 액션을 요구한다.)
- Evidence에 없는 능력을 문맥으로 추정해 점수를 주지 않는다.

## 출력 형식

```text
Fit Score

Required Skill        30 / 35
Experience            21 / 25
Evidence              13 / 15
Domain                 7 / 10
Career Level           8 / 10
Preferred              3 / 5
─────────────────────────────
TOTAL                 82 / 100

강점
  Java / Spring        ← CE-00021, CE-00034
  성능 분석            ← CE-00021 (JFR 분석 → Lock contention → 1000 conn 검증)

근거 부족
  Kubernetes 운영      ← 관련 Evidence 없음
  대규모 조직 협업     ← 관련 Evidence 없음

지원 추천: YES
  이유 1. 필수 역량 5개 중 4개 충족
  이유 2. 성능 분석 경험이 공고 요구와 직접 대응
위험
  1. Kubernetes 운영 경험 제한적
```

## 구현 시 주의

- 가중치는 Java 상수로 둔다. 프롬프트 문자열에 넣지 않는다.
- 점수만 반환하는 API를 만들지 않는다. 항목별 점수 + Evidence 참조를 함께 반환한다.
- LLM 응답은 항목별 `relevance`(0~1)와 `evidenceRefs`까지만. `totalScore` 필드를 스키마에 두지 않는다.
