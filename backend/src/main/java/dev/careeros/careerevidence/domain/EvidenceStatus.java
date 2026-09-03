package dev.careeros.careerevidence.domain;

/**
 * LLM이 추출한 결과는 곧바로 사실이 되지 않는다. 사용자가 확인해야 CONFIRMED가 된다.
 * 근거: ADR-0003 (Career Evidence를 사용자 경력의 유일한 사실 출처로 삼는다)
 */
public enum EvidenceStatus {
    DRAFT,
    CONFIRMED
}
