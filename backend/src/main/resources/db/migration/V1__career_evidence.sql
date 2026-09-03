-- Career Evidence: 사용자 경력의 유일한 사실 출처 (ADR-0003)
-- 정본 정의: docs/01-domain/career-evidence.md

create extension if not exists vector;

-- 사용자가 실제로 입력한 원문. Evidence 추적 체인의 끝.
create table source_inputs (
    id           uuid primary key,
    type         varchar(32)  not null,
    raw_text     text         not null,
    url          text,
    captured_at  timestamptz  not null,
    created_at   timestamptz  not null default now(),
    constraint source_inputs_type_check
        check (type in ('USER_INPUT', 'RESUME_UPLOAD', 'PROJECT_ENTRY', 'EXTERNAL_URL')),
    constraint source_inputs_raw_text_not_blank
        check (length(btrim(raw_text)) > 0)
);

create sequence career_evidence_code_seq start with 1;

create table career_evidences (
    id                 uuid         primary key,
    code               varchar(16)  not null unique,
    status             varchar(16)  not null,

    title              text         not null,
    context_project    text         not null,
    context_role       text,
    context_period     text,
    context_team_size  integer,

    problem            text         not null,
    analysis           text,                       -- 근거 없으면 null. 생략이 아니라 null이다.
    root_cause         text,                       -- 추측으로 채우지 않는다.
    action             text         not null,
    result             text         not null,

    -- source: 이 Evidence 레코드가 어떤 입력으로부터 만들어졌는가.
    -- context_project("어디서 있었던 일인가")와 다르다.
    source_type        varchar(32)  not null,
    source_origin_id   uuid         not null references source_inputs (id),
    source_excerpt     text         not null,      -- 원문 그대로. 요약본 금지.
    source_url         text,
    source_captured_at timestamptz  not null,

    created_at         timestamptz  not null default now(),
    updated_at         timestamptz  not null default now(),

    constraint career_evidences_status_check
        check (status in ('DRAFT', 'CONFIRMED')),
    constraint career_evidences_code_format
        check (code ~ '^CE-[0-9]{5}$'),
    constraint career_evidences_excerpt_not_blank
        check (length(btrim(source_excerpt)) > 0)
);

create index career_evidences_status_idx on career_evidences (status);
create index career_evidences_origin_idx on career_evidences (source_origin_id);

-- category / skills / usableFor: 값 목록
create table career_evidence_categories (
    evidence_id uuid not null references career_evidences (id) on delete cascade,
    value       text not null,
    primary key (evidence_id, value)
);

create table career_evidence_skills (
    evidence_id uuid not null references career_evidences (id) on delete cascade,
    value       text not null,
    primary key (evidence_id, value)
);

create table career_evidence_usable_for (
    evidence_id uuid not null references career_evidences (id) on delete cascade,
    value       text not null,
    primary key (evidence_id, value)
);

-- 정량 성과를 result 문자열에서 분리해 둔다.
-- 문자열에 묻혀 있으면 값이 변형됐는지 기계적으로 확인할 수 없다.
create table career_evidence_metrics (
    id           uuid primary key,
    evidence_id  uuid not null references career_evidences (id) on delete cascade,
    name         text not null,
    before_value text not null,
    after_value  text not null,
    unit         text
);

create index career_evidence_metrics_evidence_idx on career_evidence_metrics (evidence_id);
