# 검증 하네스

이 프로젝트에서 하네스의 목적은 하나다.

> **한 번 발견한 실패를 사람의 주의사항으로 끝내지 않고, 다시 통과할 수 없는 규칙으로 승격한다.**

`CLAUDE.md`나 `principles.md`에 "거짓말하지 마라"라고 적는 것은 지침이지 보장이 아니다.
지침은 지켜지지 않을 수 있고, 지켜지지 않았을 때 아무 일도 일어나지 않는다.

## 승격 절차

실패를 발견하면 다음 순서로 옮긴다. 마지막 칸까지 가지 않으면 승격이 끝난 것이 아니다.

```text
Incident        실제로 일어난 실패
   ↓
Invariant       그 실패가 어긴 규칙을 한 문장으로
   ↓
Guard           규칙을 강제하는 코드 (스키마 / 도메인 / DB / 스크립트)
   ↓
Meta Test       가드가 실제로 그 실패를 잡는지 확인 (일부러 깨뜨려 본다)
   ↓
CI Gate         가드가 실패하면 merge 불가
```

### 무엇을 승격할 것인가

모든 실패를 규칙으로 만들면 오탐과 유지비가 하네스를 갉아먹는다.
아래 네 가지를 따져서 판단하고, **승격하지 않기로 했다면 그 이유를 남긴다.**

| 기준 | 질문 |
| --- | --- |
| 반복 클래스 | 이번 한 번이 아니라 **같은 종류가 또 나올** 실패인가 |
| 무증상 | 잡지 못하면 **조용히 통과**하는가. 어차피 시끄럽게 터진다면 가드는 덜 급하다 |
| 오탐 | 정상적인 변경을 잘못 막을 가능성이 낮은가 |
| 비용 | 가드와 메타테스트를 유지하는 값이 얻는 것보다 싼가 |

예를 들어 "문서에 하드코딩된 개수가 낡는 것"은 무증상이지만 오탐과 유지비가 커서
승격하지 않을 수 있다. 그 판단 자체를 커밋 본문이나 문서에 남겨야
다음 사람이 같은 고민을 처음부터 다시 하지 않는다.

### 예 — 수치 변형

```text
Incident    원문에는 "341 → 0"인데 추출 결과는 "1400 → 0"이었다.
            DRAFT로 저장되고, 사용자는 자기 경력이라 "그랬던 것 같다"고 확인한다.
            CONFIRMED가 되면 사실이 되고 evidenceRef가 붙은 채 이력서에 들어간다.
            추적 체인은 형식적으로 완전한데 끝점의 숫자가 원문에 없다.
   ↓
Invariant   Evidence의 모든 정량 수치는 원문에 존재해야 한다.
   ↓
Guard       NumericFacts + EvidenceDraftValidator.verifyNumbersComeFromSource()
   ↓
Meta Test   EvidenceDraftValidatorTest.rejectsFabricatedMetrics()
   ↓
CI          .github/workflows/backend.yml
```

### 예 — 실행 비트 유실

```text
Incident    PR #1 의 backend job 두 개가 7초 만에 실패했다.
            ./gradlew: Permission denied (exit 126)
            Windows에서 zip을 풀어 커밋해 실행 비트가 100644로 들어갔다.
            Windows 로컬에서는 정상 동작해서 CI에 올라가기 전까지 드러나지 않았다.
   ↓
Invariant   실행되어야 하는 스크립트는 실행 비트를 유지한다.
   ↓
Guard       check_contracts.check_executables_keep_exec_bit()
   ↓
Meta Test   meta_test_contracts.FUNCTION_CASES — 모드를 100644로 위조해 확인
   ↓
CI          .github/workflows/contracts.yml
```

승격 기준으로 따지면: Windows에서 파일을 새로 만들 때마다 재발하는 **반복 클래스**이고,
로컬에서는 **무증상**이며, 파일명 패턴이 명확해 **오탐이 없고**, 검사가 몇 줄이라 **싸다**.
네 가지를 모두 만족한다.

## 현재 불변식

| # | 불변식 | 강제 위치 | 상태 |
| --- | --- | --- | --- |
| 1 | Evidence는 출처(`source`) 없이 존재할 수 없다 | 스키마 + 도메인 생성자 + DB NOT NULL | 적용됨 |
| 2 | `source.excerpt`는 원문에서 그대로 잘라낸 구절이다 | `CareerEvidence.buildVerifiedAgainst()` | 적용됨 |
| 3 | 근거로 인정되려면 excerpt가 충분히 길어야 한다 | `SourceInput.MIN_EXCERPT_LENGTH` + 스키마 + DB CHECK | 적용됨 |
| 4 | Evidence의 정량 수치는 원문에 존재해야 한다 | `NumericFacts` | 적용됨 |
| 5 | 추출 결과는 사용자 확인 전까지 사실이 아니다 | `EvidenceStatus.DRAFT` 하드코딩 | 적용됨 |
| 6 | 출처(`type`/`originId`/`url`/`capturedAt`)는 시스템이 소유한다 | `career-evidence.llm.schema.json` (모델이 말할 수 없음) | 적용됨 |
| 7 | 필드 생략과 `null` 명시는 다르다 | 스키마 `required` 전면 적용 | 적용됨 |
| 8 | 모델이 계약에 없는 필드를 덧붙일 수 없다 | `additionalProperties: false` | 적용됨 |
| 9 | LLM은 최종 Fit Score를 계산하지 않는다 | `fit-analysis.llm.schema.json` | 계약만 (구현 전) |
| 10 | 강점은 Evidence 없이 성립하지 않는다 | `judgement.evidenceRefs` `minItems: 1` | 계약만 (구현 전) |
| 11 | "근거 부족"과 "역량 낮음"을 구분한다 | `insufficientEvidence` 별도 필드 | 계약만 (구현 전) |
| 12 | 공고의 우대사항을 필수요건으로 옮길 수 없다 | `requiredSkills` / `preferredSkills` 분리 | 계약만 (구현 전) |
| 13 | 생성 문장은 Evidence Reference를 정확히 1개 가진다 | — | **미구현** (ADR-0004) |
| 14 | 실행되어야 하는 스크립트는 실행 비트를 유지한다 | `check_executables_keep_exec_bit` | 적용됨 |
| 15 | 스텁 추출기는 기본 프로파일에서 등록되지 않는다 | `@Profile("stub")` + `StubExtractorProfileTest` | 적용됨 |
| 16 | 필수 개수 제약으로 모델에게 지어낼 압력을 주지 않는다 | `skills`/`category` `minItems` 제거 | 적용됨 |
| 17 | 조립 결과도 계약을 만족해야 한다 | `validateAssembled()` | 적용됨 |
| 18 | LLM 계약 = Structured Output 계약 = 추출기 반환 계약 | `career-evidence.llm.schema.json` 루트 object + `evidences` | 적용됨 |
| 19 | 모델에게 넘기는 스키마는 저장소의 계약 파일이다 | `AnthropicEvidenceExtractor` (POJO 파생 금지) | 적용됨 |
| 20 | 요청용 스키마 축소가 검증 규칙을 느슨하게 만들지 않는다 | `StructuredOutputSchema` + `StructuredOutputSchemaTest` | 적용됨 |
| 21 | 프롬프트에는 그 SourceInput의 원문만 들어간다 | `EvidencePromptBuilderTest` | 적용됨 |

9~12는 계약에는 있으나 강제할 코드가 아직 없다. 해당 기능을 구현할 때
가드와 메타테스트를 **같은 커밋에서** 추가한다.

## 구성

```text
schemas/                     계약. 런타임 검증에 실제로 쓰인다.
  career-evidence.llm.schema.json  LLM 요청용 — { evidences: [...] }, 출처 없음
  career-evidence.schema.json      완성된 Evidence — backend가 출처를 채움
  job-posting.schema.json
  fit-analysis.llm.schema.json    LLM 요청용 — 점수 필드 없음
  fit-analysis.schema.json        API 응답용 — backend가 점수를 채움

tools/
  check_contracts.py         계약 자체를 검사
  meta_test_contracts.py     검사가 실제로 위반을 잡는지 검사

backend/src/test/            단위 테스트 (Docker 불필요)
backend/src/integrationTest/ Testcontainers (Docker 필요)

.github/workflows/           변경 영역별 CI
  contracts.yml              schemas/ docs/ tools/
  backend.yml                backend/ schemas/
  frontend.yml               frontend/
```

## 규칙을 추가할 때

새 불변식을 만들면 **같은 커밋에서** 다음을 함께 넣는다. 하나라도 빠지면 승격이 끝난 게 아니다.

1. 가드 (스키마 / 도메인 / DB / 스크립트)
2. 그 가드를 **깨뜨리는** 테스트 — 계약 규칙이면 `tools/meta_test_contracts.py`의 `CASES`에,
   런타임 규칙이면 해당 테스트 클래스에
3. `harness.md`의 불변식 표 갱신
4. CI에서 도는 경로에 포함되는지 확인

## 실행

```bash
python tools/check_contracts.py        # 계약 검사
python tools/meta_test_contracts.py    # 하네스 자체 검사

cd backend && ./gradlew test            # 단위
cd backend && ./gradlew integrationTest # 통합 (Docker)
```

## 왜 메타테스트가 필요한가

`check_contracts.py`가 통과하는 것만으로는 아무것도 증명되지 않는다.
검사가 아무 일도 하지 않아도 통과하기 때문이다.

그래서 `meta_test_contracts.py`는 계약을 **일부러 망가뜨린 뒤** 검사가 실패하는지 본다.
여기서 "검사가 놓침"이 나오면 계약이 깨진 게 아니라 **하네스가 깨진 것**이다.

## 아직 없는 것

- **AI Evaluation** — 추출기는 붙었지만 골든 데이터셋이 아직 없다.
  지금 가드가 잡아본 것은 스텁의 가짜 오류뿐이고, 실제 모델이 어떤 방식으로 틀리는지는
  측정된 바 없다. `tests/ai-eval/`에 케이스를 쌓고 프롬프트·모델을 바꿀 때마다 재실행한다.
  모델을 Sonnet 5에서 Haiku 4.5로 내릴지도 여기서 **측정해서** 정한다.
- **E2E** — 화면이 없다.
- **아키텍처 규칙 가드** — 계층 간 의존 방향을 강제하는 검사(ArchUnit 등).
  패키지가 몇 개 안 되는 지금은 이르다.
