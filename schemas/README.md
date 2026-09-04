# Schemas

Agent 출력과 애플리케이션 경계에서 **실제로 검증에 사용하는 계약(contract)** 이다.
문서가 아니라 실행 가능한 파일이므로 `docs/` 바깥에 둔다.

| 파일 | 정본 문서 | 쓰이는 곳 |
| --- | --- | --- |
| [career-evidence.llm.schema.json](career-evidence.llm.schema.json) | [docs/01-domain/career-evidence.md](../docs/01-domain/career-evidence.md) | **LLM 요청 스키마.** `{ evidences: [...] }`. 출처(`source`) 없음 |
| [career-evidence.schema.json](career-evidence.schema.json) | [docs/01-domain/career-evidence.md](../docs/01-domain/career-evidence.md) | **완성된 Evidence.** backend가 출처를 주입한 뒤 재검증 |
| [job-posting.schema.json](job-posting.schema.json) | [docs/02-features/job-import-analyzer.md](../docs/02-features/job-import-analyzer.md) | 공고 파싱 결과 검증 |
| [fit-analysis.llm.schema.json](fit-analysis.llm.schema.json) | [ADR-0002](../docs/adr/0002-fit-score-computed-by-system-not-llm.md) | **LLM 요청 스키마.** relevance 평가만. 점수 필드 없음 |
| [fit-analysis.schema.json](fit-analysis.schema.json) | [docs/02-features/fit-analysis.md](../docs/02-features/fit-analysis.md) | **API 응답 스키마.** backend가 점수를 채운 최종 결과 |

## 검증 지점

```text
자연어 입력
    ↓
LLM (Structured Output)
    ↓
JSON Schema Validation   ← 여기서 막는다
    ↓
도메인 객체 변환
    ↓
PostgreSQL
```

모델 응답을 검증 없이 DB에 넣지 않는다.

## 규칙

1. **문서와 스키마는 함께 바꾼다.** 한쪽만 바꾸면 정본이 둘로 갈라진다.
2. `additionalProperties: false`를 유지한다. 모델이 임의 필드를 덧붙이는 것을 막는다.
   **LLM 요청 스키마는 루트가 object여야 한다.** Structured Output이 배열 루트를 받지 않는다.
   목록을 낼 때는 `{ evidences: [...] }`처럼 감싼다.
3. **"모르는 값"은 `null`을 허용하되 required에서 빼지 않는다.**
   필드 자체를 생략하는 것과 `null`로 명시하는 것은 다르다. 후자만 "확인했고 없었다"를 뜻한다.
4. **`fit-analysis.schema.json`을 LLM Structured Output에 그대로 쓰지 않는다.**
   이 파일은 `totalScore` / `weight` / `score`를 포함하며, 이 셋은 backend가 채우는 필드다.
   LLM에게 보낼 때는 반드시 `fit-analysis.llm.schema.json`을 쓴다.

   ```text
   LLM 요청 (fit-analysis.llm.schema.json)
     relevance, evidenceRefs, explanation
             ↓
   backend 합산
             ↓
   API 응답 (fit-analysis.schema.json)
     relevance, evidenceRefs, explanation, weight, score, totalScore
   ```

   → 근거: [ADR-0002](../docs/adr/0002-fit-score-computed-by-system-not-llm.md)

5. **모델에게 시스템이 아는 값을 물어보지 않는다.**
   Evidence의 출처(`type`/`originId`/`url`/`capturedAt`)는 `SourceInput`에 이미 있다.
   물어보면 저장할 때 버리게 되고, **버릴 값의 형식이 틀렸다는 이유로 멀쩡한 추출 전체가
   거부될 수 있다.** 모델이 출처에 대해 말하는 값은 `sourceExcerpt` 하나뿐이다.

   ```text
   LLM 요청 (career-evidence.llm.schema.json)
     { evidences: [ ... + sourceExcerpt ] }
             ↓
   원문 대조 (excerpt / 수치)
             ↓
   backend 가 SourceInput 에서 source 주입
             ↓
   완성 검증 (career-evidence.schema.json)
     ... + source{type,originId,excerpt,url,capturedAt}
   ```

   조립 결과를 다시 검증하는 이유는 그러지 않으면 완성 스키마가 런타임에서
   아무도 쓰지 않는 계약이 되기 때문이다. 조립 로직의 버그도 여기서 잡힌다.
