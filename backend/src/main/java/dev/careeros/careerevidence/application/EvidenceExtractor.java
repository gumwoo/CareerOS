package dev.careeros.careerevidence.application;

import dev.careeros.careerevidence.domain.SourceInput;

/**
 * 원문에서 Career Evidence 초안을 추출하는 포트.
 *
 * <p>반환값은 도메인 객체가 아니라 <b>JSON 문자열</b>이다.
 * 추출 결과는 도메인으로 들어오기 전에 schemas/career-evidence.schema.json 으로
 * 반드시 검증되어야 하고, 이미 객체로 변환된 뒤에는 그 검증이 의미를 잃기 때문이다.
 */
public interface EvidenceExtractor {

    String extractDraftJson(SourceInput sourceInput);
}
