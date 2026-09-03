# ADR-0003. Career Evidence를 사용자 경력의 유일한 사실 출처로 삼는다

- 상태: Accepted
- 날짜: 2026-09-03

## 배경

CareerOS는 "무엇을 강조할지", "어떤 경험이 이 공고에 맞는지"를 판단한다.
그러려면 **사용자의 경력에 대해 무엇을 사실로 인정할 것인가**를 먼저 정해야 한다.

후보는 여럿이다.

- 사용자가 올린 이력서 PDF 원문
- 사용자가 입력한 자유 서술 텍스트
- GitHub 저장소에서 추론한 기술 스택
- LLM이 대화에서 추출해 기억하고 있는 내용
- 구조화된 Career Evidence 레코드

여러 개를 동시에 인정하면 반드시 충돌한다.
이력서에는 "Kubernetes 운영 경험"이라고 써 있는데 Evidence에는 관련 기록이 없을 때,
Fit Analysis는 무엇을 기준으로 판단해야 하는가?

## 결정

**Career Evidence만이 사실이다.**

- Fit Analysis, Document Agent, Interview Agent, Career Insight —
  사용자 경력을 참조하는 모든 기능은 **Career Evidence만 읽는다.**
- 이력서 원문, 자유 서술, GitHub는 **Evidence의 입력**이지 사실 자체가 아니다.
  Evidence로 변환되고 사용자가 확인하기 전까지는 판단 근거가 되지 않는다.
- 그 변환 경로를 추적할 수 있도록 모든 Evidence는 `source` 필드를 필수로 가진다.
  (`type` / `originId` / `excerpt` / `capturedAt` — [career-evidence.md](../01-domain/career-evidence.md))
- LLM의 대화 맥락은 사실 저장소가 아니다. 세션이 끝나면 사라지는 것에 경력을 의존하지 않는다.

## 이유

**첫째, 검증 가능한 단위가 필요하다.**

"Kubernetes를 다룰 줄 안다"는 참/거짓을 판정할 수 없다.
반면 Evidence는 `problem → analysis → rootCause → action → result` 구조를 강제하므로,
근거가 없으면 **채울 수 없다.** 빈칸이 그대로 신호가 된다.

이것이 "낮음"과 "근거 부족"을 구분할 수 있게 하는 기반이다. 둘은 사용자에게 다른 행동을 요구한다.

- 낮음 → 실력을 보완하라
- 근거 부족 → **경험은 있는데 기록하지 않았을 수 있다. 입력하라**

원문 텍스트를 사실로 삼으면 이 구분 자체가 불가능하다.

**둘째, 재사용이 가능해야 한다.**

같은 경험이 공고 A에서는 "성능 최적화 경험"으로, 공고 B에서는 "장애 분석 경험"으로 쓰인다.
원문 문단은 한 번 쓰면 끝이지만 Evidence는 `usableFor`로 여러 요구사항에 매칭된다.

**셋째, 추적 체인의 시작점이 필요하다.**

```text
이력서 문장
    ↓ evidenceRef
CareerEvidence
    ↓ source
사용자가 실제로 입력한 원문
```

"이 문장은 왜 이렇게 썼는가"에 끝까지 답하려면 중간에 구조화된 고정점이 있어야 한다.
Evidence가 그 고정점이다. → [ADR-0004](0004-generated-claims-require-evidence-reference.md)

## 결과

이 결정이 코드에 강제하는 것:

1. 경력 관련 조회는 `career_evidences`를 거친다. 이력서 원문 테이블을 직접 읽어 판단하는 코드를 만들지 않는다.
2. `career_evidences.source_*` 컬럼은 NOT NULL이다. 출처 없는 Evidence는 저장할 수 없다.
3. Evidence 생성은 **사용자 확인 단계를 거친다.** LLM이 추출한 즉시 확정하지 않는다.
   추출 결과는 `DRAFT` 상태로 저장하고, 사용자가 승인해야 `CONFIRMED`가 된다.
4. GitHub 연동 같은 자동 수집 기능을 붙일 때도 동일하다. 자동으로 Evidence를 `CONFIRMED`로 만들지 않는다.

## 대안 검토

- **원문 + Evidence 병행 참조**: 충돌 시 우선순위 규칙이 필요하고, 그 규칙이 결국 "Evidence 우선"으로 수렴한다. 복잡도만 늘어난다.
- **Evidence 없이 임베딩 검색만**: 유사도는 나오지만 "왜 이 경험인가"를 설명할 수 없다. 근거 제시가 제품 요구사항이므로 탈락.
