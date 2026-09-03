-- 도메인 불변식을 DB에도 건다.
--
-- CareerEvidence 생성자는 title/problem/action/result 가 blank 가 아님을 강제하는데
-- V1 에는 not null 만 있어 빈 문자열이 들어갈 수 있었다. 애플리케이션을 우회한
-- 마이그레이션·백필·수동 SQL 로 빈 Evidence 가 생기면 Fit Analysis 에서
-- "근거 부족"이 아니라 조용한 매칭 실패로 나타난다.

alter table career_evidences
    add constraint career_evidences_title_not_blank
        check (length(btrim(title)) > 0),
    add constraint career_evidences_problem_not_blank
        check (length(btrim(problem)) > 0),
    add constraint career_evidences_action_not_blank
        check (length(btrim(action)) > 0),
    add constraint career_evidences_result_not_blank
        check (length(btrim(result)) > 0),
    add constraint career_evidences_context_project_not_blank
        check (length(btrim(context_project)) > 0),
    -- excerpt 가 한두 글자면 어떤 원문에도 들어 있어 원문 대조가 무의미해진다.
    -- SourceInput.MIN_EXCERPT_LENGTH / schema 의 minLength 와 같은 값이다.
    add constraint career_evidences_excerpt_min_length
        check (length(btrim(source_excerpt)) >= 20);

alter table career_evidence_metrics
    add constraint career_evidence_metrics_name_not_blank
        check (length(btrim(name)) > 0),
    add constraint career_evidence_metrics_before_not_blank
        check (length(btrim(before_value)) > 0),
    add constraint career_evidence_metrics_after_not_blank
        check (length(btrim(after_value)) > 0);

alter table career_evidence_categories
    add constraint career_evidence_categories_not_blank check (length(btrim(value)) > 0);

alter table career_evidence_skills
    add constraint career_evidence_skills_not_blank check (length(btrim(value)) > 0);

alter table career_evidence_usable_for
    add constraint career_evidence_usable_for_not_blank check (length(btrim(value)) > 0);

-- "category 최소 1개, skills 최소 1개"는 자식 테이블의 행 수에 대한 조건이라
-- CHECK 로 표현할 수 없다. 도메인 생성자에서만 강제된다는 점을 알고 있어야 한다.
comment on table career_evidence_skills is
    'Evidence 당 최소 1행이어야 하지만 DB 제약으로 표현할 수 없다. CareerEvidence 생성자가 강제한다.';
