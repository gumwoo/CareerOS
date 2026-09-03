package dev.careeros.careerevidence.application;

import dev.careeros.careerevidence.domain.SourceInput;

/**
 * 원문에서 Career Evidence 초안을 추출하는 포트.
 *
 * <p>반환값은 도메인 객체가 아니라 <b>JSON 문자열</b>이다.
 * 추출 결과는 도메인으로 들어오기 전에 schemas/career-evidence.schema.json 으로
 * 반드시 검증되어야 하고, 이미 객체로 변환된 뒤에는 그 검증이 의미를 잃기 때문이다.
 *
 * <p>반환 형식은 <b>Evidence 초안의 JSON 배열</b>이다. 이력서나 프로젝트 설명 하나에
 * 성능 개선·장애 분석·협업처럼 독립적인 경험이 여러 개 들어 있는 것이 정상이고,
 * 그것을 하나로 뭉치면 Fit Analysis 에서 재사용할 수 없다.
 * (career-evidence 스킬도 "독립적인 경험이 여러 개면 분리한다"를 전제한다)
 *
 * <p>추출할 경험이 없으면 <b>빈 배열</b>을 반환한다. 억지로 하나를 만들지 않는다.
 */
public interface EvidenceExtractor {

    String extractDraftsJson(SourceInput sourceInput);
}
