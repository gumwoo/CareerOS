#!/usr/bin/env python3
"""계약(schemas/) 자체를 검사한다.

이 스크립트가 막는 것은 코드 버그가 아니라 **계약이 조용히 무너지는 것**이다.
여기 있는 검사는 전부 실제로 한 번 발생했거나 발생 직전까지 갔던 실패에서 나왔다.
새 실패 유형을 발견하면 주의사항으로 남기지 말고 여기에 검사를 추가한다.

    python tools/check_contracts.py
"""
from __future__ import annotations

import json
import pathlib
import re
import sys

# 콘솔 코드페이지와 무관하게 출력한다 (Windows cp949 대응)
sys.stdout.reconfigure(encoding="utf-8", errors="replace")

ROOT = pathlib.Path(__file__).resolve().parent.parent
SCHEMAS = ROOT / "schemas"
DOCS = ROOT / "docs"

failures: list[str] = []


def fail(check: str, detail: str) -> None:
    failures.append(f"[{check}] {detail}")


def load(name: str) -> dict:
    return json.loads((SCHEMAS / name).read_text(encoding="utf-8"))


def check_schemas_are_valid_json() -> None:
    """스키마가 파싱되지 않으면 런타임에 검증 자체가 사라진다."""
    for path in sorted(SCHEMAS.glob("*.json")):
        try:
            schema = json.loads(path.read_text(encoding="utf-8"))
        except json.JSONDecodeError as e:
            fail("schema-parse", f"{path.name}: {e}")
            continue
        if "$schema" not in schema:
            fail("schema-parse", f"{path.name}: $schema 선언이 없다")
        if "required" not in schema:
            fail("schema-parse", f"{path.name}: required가 없다")


def check_additional_properties_closed() -> None:
    """additionalProperties가 열려 있으면 모델이 임의 필드를 덧붙일 수 있다."""
    for path in sorted(SCHEMAS.glob("*.json")):
        schema = json.loads(path.read_text(encoding="utf-8"))

        def walk(node, trail: str) -> None:
            if not isinstance(node, dict):
                return
            if node.get("type") == "object" and "properties" in node:
                if node.get("additionalProperties") is not False:
                    fail("open-object", f"{path.name}{trail}: additionalProperties: false 가 없다")
            for key, value in node.items():
                if isinstance(value, dict):
                    walk(value, f"{trail}/{key}")
                elif isinstance(value, list):
                    for i, item in enumerate(value):
                        walk(item, f"{trail}/{key}[{i}]")

        walk(schema, "")


def check_llm_schema_has_no_score_fields() -> None:
    """ADR-0002. LLM에게 점수를 물어보지 않는다. 물어보지 않으면 만들어낼 수 없다.

    이 검사가 없으면 fit-analysis.schema.json 을 그대로 Structured Output에 쓰는
    실수가 조용히 통과한다. 실제로 한 번 그 상태였다.
    """
    llm_path = SCHEMAS / "fit-analysis.llm.schema.json"
    if not llm_path.exists():
        fail("adr-0002", "fit-analysis.llm.schema.json 이 없다. LLM 요청 스키마를 분리해야 한다")
        return

    # description 문구가 아니라 실제 속성 이름을 본다.
    # (설명에 "totalScore를 두지 않는다"라고 쓰는 것은 위반이 아니다)
    banned = {"totalScore", "weight", "score"}
    llm_schema = json.loads(llm_path.read_text(encoding="utf-8"))

    def property_names(node) -> set[str]:
        names: set[str] = set()
        if isinstance(node, dict):
            if isinstance(node.get("properties"), dict):
                names |= set(node["properties"])
                for child in node["properties"].values():
                    names |= property_names(child)
            for key, value in node.items():
                if key != "properties":
                    names |= property_names(value)
        elif isinstance(node, list):
            for item in node:
                names |= property_names(item)
        return names

    found = property_names(llm_schema) & banned
    if found:
        fail("adr-0002", f"LLM 요청 스키마에 점수 필드가 있다: {sorted(found)}. "
                         "backend가 계산하는 값을 LLM에게 물어보면 안 된다")

    backend_schema = load("fit-analysis.schema.json")
    for expected in ("totalScore", "components"):
        if expected not in backend_schema["properties"]:
            fail("adr-0002", f"API 응답 스키마에 {expected} 가 없다")


def check_evidence_required_matches_doc() -> None:
    """스키마와 정본 문서가 갈라지면 어느 쪽이 진짜인지 알 수 없게 된다."""
    schema = load("career-evidence.schema.json")
    required = set(schema["required"])

    properties = set(schema["properties"]) - {"id"}
    missing = properties - required
    if missing:
        fail(
            "evidence-required",
            "모든 필드는 required여야 한다('검사 안 함'과 '근거 없음'을 구별하기 위해). "
            f"빠진 것: {sorted(missing)}",
        )

    # 중첩 객체에도 같은 원칙이 적용된다. 여기가 뚫려 있으면
    # context.role 이나 metrics.unit 을 조용히 생략할 수 있다.
    def check_nested(node, trail: str) -> None:
        if not isinstance(node, dict):
            return
        if node.get("type") == "object" and isinstance(node.get("properties"), dict):
            declared = set(node.get("required", []))
            for name in node["properties"]:
                if name not in declared:
                    fail("evidence-required", f"{trail}.{name} 이 required 가 아니다 (생략 허용됨)")
        for key, value in node.items():
            if key == "properties" and isinstance(value, dict):
                for name, child in value.items():
                    check_nested(child, f"{trail}.{name}")
            elif isinstance(value, dict):
                check_nested(value, trail)

    for name, node in schema["properties"].items():
        check_nested(node, name)

    doc = (DOCS / "01-domain" / "career-evidence.md").read_text(encoding="utf-8")
    example = doc.split("```yaml", 1)[1].split("```", 1)[0]
    for field in sorted(required):
        if not re.search(rf"^{re.escape(field)}:", example, re.MULTILINE):
            fail("evidence-required", f"정본 문서 예시에 필수 필드 {field} 가 없다")


def check_evidence_source_is_required() -> None:
    """ADR-0003. 출처 없는 Evidence는 출처를 알 수 없는 주장이다."""
    schema = load("career-evidence.schema.json")
    if "source" not in schema["required"]:
        fail("adr-0003", "career-evidence.schema.json 의 required 에 source 가 없다")
        return

    source = schema["properties"]["source"]
    for field in ("type", "originId", "excerpt", "capturedAt"):
        if field not in source["required"]:
            fail("adr-0003", f"source.{field} 가 required 가 아니다")

    # excerpt 한 글자짜리는 "근거가 된 원문 구절"이라고 부를 수 없다.
    # 길이 하한이 없으면 contains() 대조를 형식적으로 통과시킬 수 있다.
    excerpt = source["properties"]["excerpt"]
    if excerpt.get("minLength", 0) < 20:
        fail("adr-0003", "source.excerpt 에 실질적인 minLength 하한이 없다 "
                         f"(현재 {excerpt.get('minLength', 0)})")


def check_metrics_are_separated_from_result() -> None:
    """수치가 result 문자열에 묻혀 있으면 변형 여부를 기계적으로 확인할 수 없다."""
    schema = load("career-evidence.schema.json")
    metrics = schema["properties"].get("metrics")
    if metrics is None:
        fail("numeric-guard", "metrics 필드가 없다. 수치를 result 문자열에 두면 검증할 수 없다")
        return
    for field in ("name", "before", "after"):
        if field not in metrics["items"]["required"]:
            fail("numeric-guard", f"metrics.items.{field} 가 required 가 아니다")


def check_judgements_require_evidence() -> None:
    """근거 없는 강점은 강점이 아니다. evidenceRefs 는 비어 있을 수 없다."""
    for name in ("fit-analysis.schema.json", "fit-analysis.llm.schema.json"):
        schema = load(name)
        judgement = schema.get("$defs", {}).get("judgement")
        if judgement is None:
            fail("evidence-backed", f"{name}: $defs.judgement 가 없다")
            continue
        refs = judgement["properties"]["evidenceRefs"]
        if refs.get("minItems") != 1:
            fail("evidence-backed", f"{name}: judgement.evidenceRefs 에 minItems: 1 이 없다")
        if "evidenceRefs" not in judgement["required"]:
            fail("evidence-backed", f"{name}: judgement.evidenceRefs 가 required 가 아니다")


def check_weakness_and_no_evidence_are_separate() -> None:
    """'역량이 낮다'와 '판단할 근거가 없다'는 사용자에게 다른 행동을 요구한다."""
    for name in ("fit-analysis.schema.json", "fit-analysis.llm.schema.json"):
        properties = load(name)["properties"]
        if "insufficientEvidence" not in properties:
            fail("evidence-vs-weakness", f"{name}: insufficientEvidence 필드가 없다")


def check_job_posting_keeps_required_and_preferred_apart() -> None:
    """공고의 우대사항을 필수요건으로 승격시키면 Fit 판단 전체가 왜곡된다."""
    properties = load("job-posting.schema.json")["properties"]
    for field in ("requiredSkills", "preferredSkills"):
        if field not in properties:
            fail("job-required-preferred", f"job-posting.schema.json 에 {field} 가 없다")
    if "rawContent" not in load("job-posting.schema.json")["required"]:
        fail("job-required-preferred", "rawContent 가 required 가 아니다. 원문 없이는 검증할 수 없다")


def check_doc_links_resolve() -> None:
    targets = list(DOCS.rglob("*.md")) + list(SCHEMAS.glob("*.md")) + [ROOT / "CLAUDE.md"]
    targets += list((ROOT / ".claude").rglob("*.md"))
    for path in targets:
        if not path.exists():
            continue
        text = path.read_text(encoding="utf-8")
        for match in re.finditer(r"\]\(([^)#:]+\.(?:md|json))\)", text):
            if not (path.parent / match.group(1)).exists():
                fail("dead-link", f"{path.relative_to(ROOT)} -> {match.group(1)}")
        for match in re.finditer(r"@(docs/[^\s]+\.md)", text):
            if not (ROOT / match.group(1)).exists():
                fail("dead-link", f"{path.relative_to(ROOT)} @import -> {match.group(1)}")


CHECKS = [
    check_schemas_are_valid_json,
    check_additional_properties_closed,
    check_llm_schema_has_no_score_fields,
    check_evidence_required_matches_doc,
    check_evidence_source_is_required,
    check_metrics_are_separated_from_result,
    check_judgements_require_evidence,
    check_weakness_and_no_evidence_are_separate,
    check_job_posting_keeps_required_and_preferred_apart,
    check_doc_links_resolve,
]


def main() -> int:
    for check in CHECKS:
        check()

    if failures:
        print(f"계약 검사 실패 {len(failures)}건\n")
        for line in failures:
            print(f"  {line}")
        return 1

    print(f"계약 검사 {len(CHECKS)}종 통과")
    return 0


if __name__ == "__main__":
    sys.exit(main())
