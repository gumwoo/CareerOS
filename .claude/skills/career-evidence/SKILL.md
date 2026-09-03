---
name: career-evidence
description: 사용자의 경험 서술(프로젝트 설명, 이력서 문단, 트러블슈팅 회고 등)을 CareerOS의 Career Evidence 구조로 분해할 때 사용한다. "이 경험 Evidence로 만들어줘", "프로젝트를 Evidence화", "경험 구조화" 같은 요청에 사용.
---

# Career Evidence 생성

원문 서술을 재사용 가능한 Career Evidence 단위로 분해한다.
정본 스키마는 `docs/01-domain/career-evidence.md`이며, 작업 전 반드시 읽는다.
검증용 계약은 `schemas/career-evidence.schema.json`이다. 출력은 이 스키마를 통과해야 한다.

## 절대 규칙

원문에 **없는 것은 만들지 않는다.** 이 스킬의 존재 이유가 그것이다.

- 수치 성과를 추정하거나 반올림해 채우지 않는다. 원문에 없으면 필드를 비운다.
- 사용 기술을 문맥으로 추측해 `skills`에 넣지 않는다. (예: "Spring 프로젝트"라는 이유로 JPA를 추가하지 않는다)
- `rootCause`를 그럴듯하게 지어내지 않는다. 원문이 원인을 밝히지 않았다면 `rootCause: null`로 두고 사용자에게 질문한다.
- 채우지 못한 필드는 **누락 목록으로 사용자에게 보고한다.** 조용히 비워두지 않는다.

## 출력 구조

```yaml
title:        # 한 줄. 결과가 아니라 문제를 식별하는 이름
category:     # Performance / Backend / Frontend / Infra / Troubleshooting / Collaboration 등
context:
  project:    # 어떤 프로젝트에서
problem:      # 무엇이 문제였는가 (관찰된 현상, 가능하면 수치)
analysis:     # 어떻게 조사했는가 (도구·방법)
rootCause:    # 실제 원인. 추측이면 만들지 말 것
action:       # 무엇을 바꿨는가
result:       # 결과. 측정값이 있으면 before → after
skills:       # 원문에서 실제로 확인된 기술만
usableFor:    # 이 경험이 답할 수 있는 요구사항 유형
```

## 절차

1. 원문에서 문제 → 분석 → 원인 → 조치 → 결과 흐름을 찾는다.
2. 하나의 서술에 독립적인 경험이 여러 개면 **분리한다.** 억지로 합치지 않는다.
3. 채우지 못한 필드를 목록화한다.
4. Evidence 초안 + 누락 필드 질문을 함께 제시한다.

## 좋은 분해 / 나쁜 분해

나쁨 — 결과 중심이라 재사용이 안 됨:

```yaml
title: 성능을 개선했다
result: 빨라졌다
```

좋음 — 어떤 요구사항에 매칭될지가 드러남:

```yaml
title: SSE 다중 연결 지연 개선
problem: 다중 SSE 연결 환경에서 화면 갱신이 최대 14초 지연
analysis: JFR로 실행 흐름과 Lock 경합 분석
rootCause: SseEmitter.send() 구간의 Java Monitor Lock contention
action: 연결별 전송 구조 직렬화 개선
result: 1000 연결 환경에서 정체 제거, JavaMonitorEnter 341 → 0
usableFor: [성능 최적화, 장애 분석, 실시간 시스템, 문제 해결]
```
