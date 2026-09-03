# Career Evidence

CareerOS의 가장 중요한 데이터 구조이며, **사용자 경력에 대한 유일한 사실 출처(source of truth)** 다.
근거는 [ADR-0003](../adr/0003-career-evidence-is-source-of-truth.md).

사용자의 경력을 단순 텍스트로 저장하지 않고, 재사용 가능한 **Career Evidence** 단위로 구조화한다.
이 구조의 목적은 하나의 경험을 여러 공고와 문항에 반복적으로 활용할 수 있게 만드는 것이다.

이 문서가 스키마 정본이며, 실행 가능한 형태는 [`schemas/career-evidence.schema.json`](../../schemas/career-evidence.schema.json)이다.
**두 파일은 함께 변경한다.** 한쪽만 바꾸면 안 된다.

## 예시

```yaml
title: SSE 다중 연결 지연 개선

category:
  - Performance
  - Backend
  - Java
  - Troubleshooting

context:
  project: ESS 성능평가 시스템
  role: null
  period: null
  teamSize: null

problem:
  다중 SSE 연결 환경에서 화면 데이터 갱신이 최대 14초까지 지연됨

analysis:
  JFR을 사용해 실행 흐름과 Lock 경합 분석

rootCause:
  SseEmitter.send() 구간에서 Java Monitor Lock contention 발생

action:
  연결별 전송 구조 직렬화 개선

result:
  1000개 연결 환경에서 정체 제거
  JavaMonitorEnter 341 → 0

metrics:
  - name: JavaMonitorEnter
    before: "341"
    after: "0"
    unit: null
  - name: 데이터 갱신 지연
    before: "14"
    after: "0"
    unit: s

skills:
  - Java
  - Spring
  - SSE
  - JFR
  - Performance Analysis

usableFor:
  - 성능 최적화
  - 장애 분석
  - 실시간 시스템
  - 문제 해결

source:
  type: USER_INPUT
  originId: 550e8400-e29b-41d4-a716-446655440000
  excerpt: |
    다중 SSE 연결에서 최대 14초 지연이 발생했고, JFR로 분석해서
    SseEmitter.send()의 락 경합을 찾아 전송 구조를 직렬화했습니다.
  capturedAt: 2026-09-03T10:22:00Z
```

## 필드 정의

**모든 필드가 required다.** 근거가 없는 필드는 생략이 아니라 `null` 또는 빈 배열로 명시한다.
"검사하지 않음"과 "검사했으나 근거 없음"을 구별하기 위해서다.

| 필드 | 없을 때 | 설명 |
| --- | --- | --- |
| `title` | — | 한 줄. 결과가 아니라 **문제를 식별하는 이름**. |
| `category` | — | 분류 태그. Performance / Backend / Frontend / Infra / Troubleshooting / Collaboration 등. 최소 1개. |
| `context.project` | — | 어떤 프로젝트·조직에서 있었던 일인가. **"어디서"** 에 답한다. |
| `context.role` | `null` | 그 안에서 맡은 역할. |
| `context.period` | `null` | 기간. |
| `context.teamSize` | `null` | 팀 규모. |
| `problem` | — | 관찰된 현상. 가능하면 수치를 포함한다. |
| `analysis` | `null` | 어떻게 조사했는가 (도구·방법). |
| `rootCause` | `null` | 실제 원인. **원문이 원인을 밝히지 않았다면 반드시 `null`.** 추측 금지. |
| `action` | — | 무엇을 바꿨는가. |
| `result` | — | 결과 서술. |
| `metrics` | `[]` | 정량 성과를 `name / before / after / unit`으로 분해. 원문에 수치가 없으면 빈 배열. **추정값 금지.** 단위가 없으면 `unit: null`. |
| `skills` | — | 원문에서 **실제로 확인된** 기술만. 문맥 추측 금지. 최소 1개. |
| `usableFor` | `[]` | 이 경험이 답할 수 있는 요구사항 유형. Fit Analysis의 매칭 힌트. |
| `source` | — | 이 Evidence가 **어디서 왔는지**. 아래 참조. |

`result`와 별개로 `metrics`를 두는 이유는 검증 때문이다.
수치가 `result` 문자열 안에 묻혀 있으면 나중에 값이 변형됐는지(`341 → 0`이 "대폭 감소"로 바뀌는 것)
기계적으로 확인할 수 없다.

`null`과 빈 배열은 **"확인했고 근거가 없었다"** 를 뜻한다.
채우지 못한 필드는 사용자에게 질문한다. 조용히 넘어가지 않는다.

## `source` — 출처 추적

`context.project`가 **"어떤 프로젝트에서 있었던 일인가"** 라면,
`source`는 **"이 Evidence 레코드가 어떤 입력으로부터 만들어졌는가"** 다. 둘은 다르다.

`source`가 필수인 이유는 추적 체인 때문이다.

```text
GeneratedClaim.evidenceRef
        ↓
CareerEvidence
        ↓
CareerEvidence.source
        ↓
사용자가 실제로 입력한 원문
```

이 체인이 끊기지 않아야 "이 이력서 문장은 사용자가 실제로 쓴 어떤 말에서 나왔는가"에
끝까지 답할 수 있다. `source` 없이 존재하는 Evidence는 출처를 알 수 없는 주장이므로 허용하지 않는다.

| 필드 | 필수 | 설명 |
| --- | --- | --- |
| `source.type` | ✔ | `USER_INPUT` / `RESUME_UPLOAD` / `PROJECT_ENTRY` / `EXTERNAL_URL` |
| `source.originId` | ✔ | 원본 레코드(업로드 문서, 프로젝트, 사용자 입력 세션)의 식별자. |
| `source.excerpt` | ✔ | 이 Evidence의 근거가 된 **원문 구절 그대로.** 요약·윤문하지 않는다. 최소 20자 — 한두 글자짜리는 "근거가 된 구절"이 아니다. |
| `source.url` | ✔ | `type`이 `EXTERNAL_URL`일 때의 원본 주소. 아니면 `null`. |
| `source.capturedAt` | ✔ | 원문을 수집한 시각 (ISO 8601). |

`source.excerpt`를 요약본으로 대체하면 안 된다. 나중에 "이 수치가 어디서 나왔나"를 검증할 때
원문 그대로가 아니면 검증이 성립하지 않는다.

## 상태 전이

Evidence는 만들어지자마자 사실이 되지 않는다.

```text
추출 -> DRAFT -> (사용자 확인) -> CONFIRMED
```

| 상태 | 의미 |
| --- | --- |
| `DRAFT` | 추출과 검증은 통과했으나 사용자가 아직 확인하지 않았다. **Fit Analysis 등은 이 상태를 읽지 않는다.** |
| `CONFIRMED` | 사용자가 원문과 대조해 확인했다. 이 시점부터 사실로 사용된다. |

`status`는 스키마(`career-evidence.schema.json`)에 **없다.** 추출기가 정하는 값이 아니라
시스템이 부여하는 값이기 때문이다. 누락이 아니므로 추가하지 말 것.

근거: [ADR-0003](../adr/0003-career-evidence-is-source-of-truth.md)

## 생성 규칙

Evidence를 만들 때 지켜야 할 것은 [Hallucination 방지 정책](../03-architecture/hallucination-policy.md)에 있다.
요약하면:

1. 원문에 없는 수치·기술·원인을 만들지 않는다.
2. 하나의 서술에 독립적인 경험이 여러 개면 분리한다.
3. 채우지 못한 필드는 사용자 질문으로 되돌린다.
4. 저장 전 `career-evidence.schema.json`으로 검증한다.
5. `source.type` / `source.capturedAt` / `source.url`은 **시스템이 채운다.**
   추출기에게 물어볼 이유가 없는 값이고, 물어보면 실제 입력 경로와 어긋날 수 있다.
6. `metrics`의 수치는 원문에 실제로 존재해야 한다. 없는 수치는 저장 단계에서 거부된다.
