# ADR-0002. Fit Score의 최종 계산은 LLM이 아니라 backend가 한다

- 상태: Accepted
- 날짜: 2026-09-03

## 배경

Fit Analysis는 채용공고 요구사항과 사용자의 Career Evidence를 비교해 0~100의 점수를 낸다.

가장 쉬운 구현은 공고와 Evidence를 통째로 프롬프트에 넣고 이렇게 묻는 것이다.

```text
이 지원자의 적합도를 100점 만점으로 평가하세요.
→ "82점입니다."
```

이 방식에는 세 가지 문제가 있다.

1. **재현되지 않는다.** 같은 입력에 82점, 76점, 85점이 나온다. 사용자가 어제 본 점수와 오늘 본 점수가 다르면 제품을 신뢰하지 않는다.
2. **점수를 설명할 수 없다.** 82가 어디서 왔는지 분해할 수 없으므로, 사용자가 "무엇을 보완해야 하나"에 답할 수 없다. CareerOS는 점수를 보여주는 제품이 아니라 **다음 행동을 정하는 제품**이다.
3. **점수 정책을 바꿀 수 없다.** 가중치를 조정하려면 프롬프트를 고쳐야 하고, 고쳐도 모델이 따를지 알 수 없다. 과거 점수와 비교도 불가능하다.

## 결정

책임을 나눈다.

**LLM이 하는 것 — 관련성 평가만**

```text
Java Requirement       ↔ CE-00021, CE-00034   relevance 0.95
Redis Requirement      ↔ CE-00007             relevance 0.88
Kubernetes Requirement ↔ (없음)                relevance 0.00
```

**backend가 하는 것 — 합산**

```java
score = requiredSkillMatch * 0.35
      + experienceMatch    * 0.25
      + projectEvidence    * 0.15
      + domainMatch        * 0.10
      + careerLevel        * 0.10
      + preferredMatch     * 0.05;
```

구체적으로:

- 가중치는 **Java 상수**다. 프롬프트 문자열에 넣지 않는다.
- LLM에게 요청하는 응답 스키마에 `totalScore` 필드를 **두지 않는다.** 물어보지 않으면 만들어낼 수 없다.
- LLM 응답 스키마와 API 응답 스키마를 분리한다.
  `schemas/fit-analysis.llm.schema.json`(relevance + evidenceRefs + explanation)과
  `schemas/fit-analysis.schema.json`(여기에 backend가 score/weight/totalScore를 채움).

## 이유

이렇게 하면 **같은 입력에 대해 점수 계산 규칙 자체는 deterministic해진다.**

LLM의 비결정성은 `relevance` 값에만 남고, 그 값이 총점에 미치는 영향은 명시적인 가중치로 고정된다.
`relevance`가 0.95에서 0.91로 흔들려도 총점은 예측 가능한 범위에서만 움직인다.

부수 효과로 얻는 것:

- 가중치 튜닝이 코드 변경 한 줄이 되고, 과거 분석을 새 가중치로 재계산할 수 있다.
- 점수 분해가 그대로 사용자 화면이 된다. (`Required Skill 30/35`)
- 가중치 로직을 단위 테스트할 수 있다. LLM 없이.

## 결과

이 결정이 코드에 강제하는 것:

1. 점수만 반환하는 API를 만들지 않는다. 항목별 점수 + 연결된 `evidenceRefs`를 함께 반환한다.

   ```java
   // 금지
   record FitAnalysisResponse(int score, String reason) {}
   ```

2. LLM 응답에 `totalScore`가 들어오면 **파싱 단계에서 거부한다.** 무시하는 게 아니라 스키마 위반으로 처리한다.
3. 가중치 상수를 프롬프트 템플릿에 노출하지 않는다.
4. `relevance` 평가에 사용한 모델·프롬프트 버전을 결과에 기록한다(`modelVersion`). 재현 검증을 위해서다.

## 대안 검토

- **LLM 총점 + backend 검증**: 총점이 항목 합과 다르면 거부. → 거부율이 높아 재시도 비용만 늘고, 얻는 게 없다.
- **전부 규칙 기반(LLM 없음)**: 문자열 매칭으로는 "SSE 지연 개선 경험"이 "실시간 시스템 요구"에 대응한다는 판단을 못 한다. 관련성 평가는 LLM이 잘하는 영역이 맞다.

선택한 방식은 **LLM은 판단, 시스템은 계산**이라는 경계를 명확히 한 것이다.
