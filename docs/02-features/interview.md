# Interview Manager

지원 회사와 공고, Career Evidence를 기반으로 예상 질문을 생성한다.

예:

```text
FlowTicket

예상 질문

1. Redis Lua를 사용한 이유는 무엇인가?
2. 분산락 대신 Lua를 선택한 이유는?
3. Redis 장애 시 대기열은 어떻게 되는가?
4. 600 RPS 부하 테스트에서 병목은 무엇이었는가?
```

사용자의 답변 기록을 저장하여 반복 학습한다.

```text
질문
↓
사용자 답변
↓
AI 피드백
↓
개선 답변
↓
최종 답변 저장
```

---
