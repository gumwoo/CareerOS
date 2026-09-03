# Fit Analysis

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

## Application Recommendation

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

## Fit Score 설계

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
