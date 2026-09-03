# ADR-0005. Vector Search는 PostgreSQL + pgvector로 시작한다

- 상태: Accepted
- 날짜: 2026-09-03

## 배경

Fit Analysis는 공고 요구사항과 Career Evidence의 의미적 유사도를 계산해야 한다.
문자열 매칭으로는 "SSE 지연 개선 경험"이 "실시간 시스템 요구"에 대응한다는 판단을 못 한다.

선택지는 전용 Vector DB(Qdrant, Weaviate, Pinecone)와 PostgreSQL + pgvector다.

## 결정

**PostgreSQL + pgvector로 시작한다.** 전용 Vector DB는 도입하지 않는다.

## 이유

**첫째, 데이터가 작다.**

MVP 사용자 한 명의 Career Evidence는 수십 건 수준이다. 수천 명이 모여도 수십만 벡터다.
pgvector가 문제가 되는 규모가 아니다.

**둘째, 벡터 검색이 항상 관계형 필터와 함께 온다.**

```sql
select * from career_evidences
where user_id = ?
  and status = 'CONFIRMED'
order by embedding <=> ?
limit 10;
```

Fit Analysis는 **특정 사용자의 확정된 Evidence 중에서만** 검색한다. 전역 유사도 검색이 아니다.
전용 Vector DB를 쓰면 이 필터를 메타데이터 필터로 흉내 내거나
두 저장소를 조인해야 하고, 그 순간 정합성 문제가 생긴다.

**셋째, 운영 요소를 하나 줄인다.**

Phase 0 단계에서 백업·마이그레이션·모니터링 대상을 늘릴 이유가 없다.

**넷째, 되돌리기 쉽다.**

이 결정은 [ADR-0003](0003-career-evidence-is-source-of-truth.md)이나
[ADR-0004](0004-generated-claims-require-evidence-reference.md)와 성격이 다르다.
저 둘은 뒤집으면 제품 정체성이 바뀌지만, 이건 **검색 구현을 교체하는 일**이다.
Evidence 원본이 PostgreSQL에 있는 한 임베딩은 언제든 재생성할 수 있다.

## 결과

1. 벡터 검색을 `EvidenceSearchPort` 인터페이스 뒤에 둔다.
   구현체는 `PgVectorEvidenceSearch`. 서비스 레이어가 pgvector를 직접 알지 않는다.
2. 임베딩은 파생 데이터로 취급한다. 모델을 바꾸면 전체 재생성이 가능해야 한다.
   `career_evidences.embedding_model`, `embedded_at` 컬럼을 둔다.
3. 유사도 점수를 Fit Score에 직접 넣지 않는다.
   벡터 검색은 **후보 Evidence를 좁히는 용도**이고, 관련성 판단은 LLM이, 점수 계산은 backend가 한다.
   → [ADR-0002](0002-fit-score-computed-by-system-not-llm.md)

## 전환 조건

아래에 해당하면 재검토한다. 그전에는 옮기지 않는다.

- 벡터 수가 수백만 건을 넘고 `ivfflat`/`hnsw` 튜닝으로 지연 목표를 못 맞출 때
- 벡터 검색 부하가 트랜잭션 워크로드에 영향을 줄 때
- 사용자 간 전역 검색(예: "비슷한 경력의 사용자") 기능이 실제로 필요해질 때
