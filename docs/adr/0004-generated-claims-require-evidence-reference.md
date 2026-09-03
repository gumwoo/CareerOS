# ADR-0004. 생성된 모든 경력 주장은 Evidence Reference를 필수로 가진다

- 상태: Accepted
- 날짜: 2026-09-03

## 배경

Document Agent는 이력서·자기소개서 문장을 생성한다. 예:

> "JFR을 활용해 SSE 지연 원인을 분석하고 1000개의 동시 연결 환경에서 병목을 제거했습니다."

이 문장이 사용자의 실제 경험에서 나온 것인지, 모델이 그럴듯하게 지어낸 것인지
**결과물만 봐서는 구분할 수 없다.** 둘 다 자연스러운 한국어 문장이기 때문이다.

취업 문서에서 이건 단순한 품질 문제가 아니다.
사용자가 면접에서 답변하지 못하는 문장이 이력서에 들어가면 제품이 사용자를 해친다.

`docs/00-overview/principles.md`와 `CLAUDE.md`에 "Evidence 밖 사실 생성 금지"라고 적어두는 것으로는
부족하다. 그것들은 개발 중 지침이지 **런타임 보장이 아니다.**
지침은 지켜지지 않을 수 있고, 지켜지지 않았을 때 아무 일도 일어나지 않는다.

## 결정

Evidence Reference를 **타입 시스템과 DB 제약으로 강제한다.**

도메인:

```java
public record GeneratedClaim(
    String text,
    EvidenceRef evidenceRef,
    ClaimSource source
) {
    public GeneratedClaim {
        if (evidenceRef == null) {
            throw new InvalidGeneratedClaimException(
                "Generated claim requires evidenceRef: " + text
            );
        }
    }
}
```

DB:

```sql
create table generated_document_sentences (
    id                 uuid primary key,
    document_id        uuid not null references documents(id),
    text               text not null,
    career_evidence_id uuid not null references career_evidences(id),
    created_at         timestamptz not null default now()
);
```

`career_evidence_id`는 **NOT NULL + FK**다. nullable로 두지 않는다.

체인 전체:

```text
CareerEvidence
      ↓
EvidenceReference
      ↓
GeneratedClaim
      ↓
ResumeSentence
```

## 이유

**"불가능하게 만드는 것"과 "하지 말라고 하는 것"은 다르다.**

`evidenceRef`를 nullable로 두면 언젠가 반드시 null이 들어온다.
급한 기능을 붙이다가, 마이그레이션 중에, 혹은 다른 에이전트가 "일단 되게" 만들면서.
그리고 그때 아무도 모른다.

NOT NULL로 두면 근거 없는 문장은 **저장 자체가 실패한다.**
실패는 시끄럽고, 시끄러운 실패는 고쳐진다.

이것이 `principles.md`의 원칙 3·4번(모든 추천에 근거 / 사용자의 실제 경험을 벗어나지 않는다)을
문서에서 코드로 옮기는 방법이다.

## 결과

이 결정이 코드에 강제하는 것:

1. **생성 문장은 항상 Evidence 단위로 쪼개서 만든다.**
   여러 Evidence를 뭉뚱그린 한 문단을 통째로 생성하지 않는다.
   한 문장 = 하나 이상의 Evidence Reference.

2. LLM 응답 스키마에서 `evidenceRef`를 optional로 두지 않는다.
   근거를 못 찾으면 문장을 생성하지 않고 **"근거 부족"으로 반환한다.**

3. 사용자에게 노출되는 생성 문장에는 근거를 함께 표시한다.
   내부적으로만 저장하고 화면에서 숨기면 사용자가 검증할 수 없다.

4. 문장을 수동 편집하면 Reference 유효성을 다시 확인한다.
   사용자가 직접 고친 문장은 `USER_EDITED`로 표시하고 AI 생성물과 구분한다.

## 적용 범위

경력에 대한 **사실 주장**에만 적용한다.

- 적용됨: "1000개 연결 환경에서 병목을 제거했습니다" → Evidence 필요
- 적용 안 됨: "안녕하십니까. 지원자 ○○○입니다" → 사실 주장이 아님

연결어·인사말·구조적 문장은 `ClaimSource.STRUCTURAL`로 분류해 예외 처리한다.
단, 이 예외를 넓게 쓰면 결정이 무력화되므로 **분류 자체를 LLM에게 맡기지 않는다.**
템플릿에서 정해진 자리에만 STRUCTURAL 문장이 들어갈 수 있다.

## 관련

- [ADR-0003](0003-career-evidence-is-source-of-truth.md) — Evidence가 사실 출처인 이유
- [Hallucination 방지 정책](../03-architecture/hallucination-policy.md)
