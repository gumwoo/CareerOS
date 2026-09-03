# Hallucination 방지 정책

## Hallucination 방지 전략

CareerOS에서는 신뢰성이 중요하다.

원칙:

1. Career Evidence에 존재하지 않는 경험 생성 금지
2. 숫자 성과 자동 생성 금지
3. 사용자가 입력하지 않은 기술 사용 경험 생성 금지
4. 생성된 문장마다 Evidence Reference 저장
5. 사용자가 최종 확인 후 문서 사용

예:

```text
Generated Sentence

"JFR을 활용해 SSE 지연 원인을 분석하고
1000개의 동시 연결 환경에서 병목을 제거했습니다."

Evidence:
CE-00021

Source:
ESS Project
```

---
