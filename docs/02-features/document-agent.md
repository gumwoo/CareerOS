# Document Agent

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
