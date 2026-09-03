# Schemas

Agent 출력과 애플리케이션 경계에서 **실제로 검증에 사용하는 계약(contract)** 이다.
문서가 아니라 실행 가능한 파일이므로 `docs/` 바깥에 둔다.

| 파일 | 정본 문서 | 쓰이는 곳 |
| --- | --- | --- |
| [career-evidence.schema.json](career-evidence.schema.json) | [docs/01-domain/career-evidence.md](../docs/01-domain/career-evidence.md) | Evidence 추출 결과 검증 → DB 저장 전 |
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
