# 시스템 아키텍처

## 기술 아키텍처

초기 추천 구조:

```text
                 Web / Mobile
                      │
                      ▼
                 API Gateway
                      │
                      ▼
                 Spring Boot
                      │
        ┌─────────────┼─────────────┐
        │             │             │
        ▼             ▼             ▼
      User          Career        Application
     Service        Service        Service
        │             │             │
        └─────────────┼─────────────┘
                      │
                      ▼
                  PostgreSQL

                 AI Orchestrator
                      │
        ┌─────────────┼─────────────┐
        ▼             ▼             ▼
    Job Agent      Fit Agent    Strategy Agent
        │             │             │
        └─────────────┼─────────────┘
                      ▼
                  LLM Provider

                   Vector DB
                      │
                      ▼
                Career Evidence
```

---
