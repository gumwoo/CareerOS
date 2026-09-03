# Job Import / Analyzer

초기 MVP에서는 채용 플랫폼 전체 크롤링을 목표로 하지 않는다.

사용자가 채용공고 URL 또는 텍스트를 입력하면 공고를 등록한다.

지원 방식:

```text
URL 붙여넣기
텍스트 붙여넣기
PDF 업로드
브라우저 Extension (후속)
```

저장 정보:

- 회사
- 포지션
- 경력 조건
- 필수 역량
- 우대사항
- 업무 내용
- 기술 스택
- 전형 절차
- 마감일
- 근무지
- 채용공고 원문

---

## Job Analyzer

채용공고를 구조화한다.

예:

```json
{
  "role": "Backend Developer",
  "requiredSkills": [
    "Java",
    "Spring",
    "RDBMS"
  ],
  "preferredSkills": [
    "Kubernetes",
    "Redis",
    "Kafka"
  ],
  "responsibilities": [
    "대규모 트래픽 서비스 개발",
    "시스템 성능 개선"
  ],
  "seniority": "Junior"
}
```

---
