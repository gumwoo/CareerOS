# Application Workspace / Pipeline

하나의 채용공고가 하나의 Workspace가 된다.

예:

```text
Toss Backend Developer

────────────────────

공고 분석              완료
이력서                 완료
자기소개서             작성 중
코딩테스트             예정
면접                   대기
최종 결과              -

────────────────────

현재 할 일

□ 자기소개서 2번 문항 완료
□ Redis 장애 대응 정리
□ 이번 주 BFS/DFS 5문제 풀이
```

---

## Application Pipeline

지원 상태를 하나의 Pipeline으로 관리한다.

상태 예:

```text
SAVED
   ↓
ANALYZING
   ↓
PREPARING
   ↓
APPLIED
   ↓
DOCUMENT_PASS
   ↓
CODING_TEST
   ↓
INTERVIEW
   ↓
FINAL
   ↓
OFFER
```

탈락 상태:

```text
DOCUMENT_REJECTED
CODING_TEST_REJECTED
INTERVIEW_REJECTED
FINAL_REJECTED
```

---
