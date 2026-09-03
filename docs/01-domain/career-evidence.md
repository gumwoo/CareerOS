# Career Evidence

CareerOS의 가장 중요한 데이터 구조다.

사용자의 경력을 단순 텍스트로 저장하지 않고, 재사용 가능한 **Career Evidence** 단위로 구조화한다.

예:

```yaml
title: SSE 다중 연결 지연 개선

category:
  - Performance
  - Backend
  - Java
  - Troubleshooting

context:
  project: ESS 성능평가 시스템

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
```

이 구조의 목적은 하나의 경험을 여러 공고와 문항에 반복적으로 활용할 수 있게 만드는 것이다.

---
