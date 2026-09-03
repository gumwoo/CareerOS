---
name: job-parse
description: 채용공고 원문(URL 본문, 붙여넣은 텍스트, PDF 추출 텍스트)을 CareerOS의 JobPosting / JobRequirement 구조로 변환할 때 사용한다. "공고 파싱", "이 공고 구조화해줘", "요구사항 뽑아줘" 같은 요청에 사용.
---

# 채용공고 구조화

공고 원문을 Fit Analysis가 소비할 수 있는 형태로 변환한다.
필드 정의는 `docs/02-features/job-import-analyzer.md`를 따르고, 출력은 `schemas/job-posting.schema.json`을 통과해야 한다.

## 절대 규칙

- **원문에 없는 요구사항을 추가하지 않는다.** "Backend니까 Docker는 당연히 필요" 같은 보완을 하지 않는다.
- 필수(required)와 우대(preferred)를 임의로 옮기지 않는다. 원문의 구분을 그대로 유지한다.
- 원문이 모호하면 추측하지 말고 `null` + 사용자 확인.
- 원문 전체를 `rawContent`로 보존한다. 요약본으로 대체하지 않는다.

## 출력 구조

```json
{
  "company": "",
  "position": "",
  "role": "Backend Developer",
  "seniority": "Junior | Mid | Senior | null",
  "experienceRequirement": "",
  "requiredSkills": [],
  "preferredSkills": [],
  "responsibilities": [],
  "recruitmentProcess": [],
  "deadline": null,
  "location": null,
  "rawContent": ""
}
```

## 정규화

- 기술명은 표준 표기로 통일한다: `spring boot` → `Spring Boot`, `k8s` → `Kubernetes`, `postgres` → `PostgreSQL`.
- 한 항목에 기술이 여러 개면 분리한다. `"Java/Spring 경험"` → `["Java", "Spring"]`.
- 기술이 아닌 요구(예: "대규모 트래픽 서비스 개발 경험")는 `skills`가 아니라 `responsibilities`로 보낸다.
- 마감일은 ISO 8601. "상시채용"은 `null` + `deadlineNote`.

## 절차

1. 원문에서 섹션 경계(자격요건/우대사항/주요업무/전형절차)를 찾는다.
2. 각 항목을 위 구조에 매핑한다.
3. 기술명을 정규화한다.
4. 매핑하지 못한 원문 구절을 목록으로 보고한다. **버리지 않는다.**
