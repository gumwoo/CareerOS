#!/usr/bin/env python3
"""하네스 자체가 고장나지 않았는지 검사한다.

`check_contracts.py`가 **통과하는 것**만으로는 부족하다.
검사가 아무것도 안 하고 있어도 통과하기 때문이다.

그래서 계약을 일부러 망가뜨린 뒤 검사가 실제로 실패하는지 확인한다.
여기서 하나라도 "검사가 놓침"이 나오면 계약이 깨진 게 아니라 **하네스가 깨진 것**이다.

    python tools/meta_test_contracts.py
"""
from __future__ import annotations

import io

import json
import pathlib
import subprocess
import sys

# 콘솔 코드페이지와 무관하게 출력한다 (Windows cp949 대응)
sys.stdout.reconfigure(encoding="utf-8", errors="replace")

ROOT = pathlib.Path(__file__).resolve().parent.parent
CHECKER = ROOT / "tools" / "check_contracts.py"


def remove_required(field: str):
    def mutate(schema: dict) -> None:
        schema["required"].remove(field)
    return mutate


# (설명, 대상 파일, 계약을 망가뜨리는 방법)
# 새 불변식을 추가하면 그것을 깨뜨리는 케이스도 여기에 함께 추가한다.
CASES = [
    (
        "ADR-0002 · LLM 요청 스키마에 totalScore가 생기면",
        "schemas/fit-analysis.llm.schema.json",
        lambda s: s["properties"].update({"totalScore": {"type": "integer"}}),
    ),
    (
        "ADR-0003 · source가 required에서 빠지면",
        "schemas/career-evidence.schema.json",
        remove_required("source"),
    ),
    (
        "ADR-0003 · source.excerpt가 required에서 빠지면",
        "schemas/career-evidence.schema.json",
        lambda s: s["properties"]["source"]["required"].remove("excerpt"),
    ),
    (
        "근거 강제 · 강점이 Evidence 없이 성립하게 되면",
        "schemas/fit-analysis.schema.json",
        lambda s: s["$defs"]["judgement"]["properties"]["evidenceRefs"].pop("minItems"),
    ),
    (
        "계약 폐쇄 · 모델이 임의 필드를 덧붙일 수 있게 되면",
        "schemas/job-posting.schema.json",
        lambda s: s.update({"additionalProperties": True}),
    ),
    (
        "생략 vs null · rootCause를 생략해도 되게 만들면",
        "schemas/career-evidence.schema.json",
        remove_required("rootCause"),
    ),
    (
        "근거 vs 역량 · insufficientEvidence를 없애면",
        "schemas/fit-analysis.schema.json",
        lambda s: s["properties"].pop("insufficientEvidence"),
    ),
    (
        "공고 원문 · rawContent를 required에서 빼면",
        "schemas/job-posting.schema.json",
        remove_required("rawContent"),
    ),
]


def checker_fails() -> bool:
    return subprocess.run(
        [sys.executable, str(CHECKER)], capture_output=True
    ).returncode != 0


def main() -> int:
    if checker_fails():
        print("계약이 이미 깨져 있다. 먼저 check_contracts.py 를 통과시킬 것.")
        return 1

    missed: list[str] = []

    for description, relative_path, mutate in CASES:
        path = ROOT / relative_path
        # 줄바꿈까지 그대로 되돌리려면 바이트로 다뤄야 한다.
        # 텍스트로 읽고 쓰면 Windows에서 LF가 CRLF로 바뀌어 파일이 변경된 상태로 남는다.
        original = path.read_bytes()
        try:
            schema = json.loads(original.decode("utf-8"))
            mutate(schema)
            path.write_bytes(json.dumps(schema, ensure_ascii=False, indent=2).encode("utf-8"))
            caught = checker_fails()
        finally:
            # 검사가 어떻게 끝나든 원본을 되돌린다.
            path.write_bytes(original)

        print(f"  {'OK  ' if caught else 'MISS'}  {description}")
        if not caught:
            missed.append(description)

    if checker_fails():
        print("\n원본 복원에 실패했다. git status 로 확인할 것.")
        return 1

    if missed:
        print(f"\n하네스가 {len(missed)}건을 놓쳤다. 계약이 아니라 검사를 고쳐야 한다.")
        return 1

    print(f"\n메타테스트 {len(CASES)}건 통과. 검사가 실제로 위반을 잡는다.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
