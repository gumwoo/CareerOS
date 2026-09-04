-- 이 Evidence 를 무엇이 만들었는가.
--
-- 지금까지 모델과 프롬프트 버전은 debug 로그에만 남았다. 그러면 나중에 Evidence 하나를
-- 보고 "이건 Sonnet 5가 만든 건가, Haiku 실험 때 만든 건가", "프롬프트 v1인가 v2인가"를
-- DB 에서 알 수 없다. AI Eval 로 프롬프트·모델을 비교할 때 이미 쌓인 Evidence 를
-- 대조군으로 쓸 수 없게 된다.
--
-- source_* 가 "어떤 원문에서 왔는가"라면 이 두 컬럼은 "어떤 추출기가 뽑았는가"다.

alter table career_evidences
    -- default 'UNKNOWN' 은 이 마이그레이션 이전에 만들어진 행을 위한 것이다.
    -- 그때는 정말로 무엇이 만들었는지 알 수 없으므로 지어내지 않고 UNKNOWN 으로 둔다.
    -- 이후 행은 애플리케이션이 항상 채우므로 default 는 곧바로 걷어낸다.
    add column extraction_model text not null default 'UNKNOWN',
    -- 프롬프트를 쓰지 않는 추출기(stub)도 있으므로 nullable.
    add column prompt_version text;

alter table career_evidences
    alter column extraction_model drop default;

alter table career_evidences
    add constraint career_evidences_extraction_model_not_blank
        check (length(btrim(extraction_model)) > 0);

create index career_evidences_extraction_model_idx on career_evidences (extraction_model);

comment on column career_evidences.extraction_model is
    '이 Evidence 를 만든 모델 id. 모델이 아닌 추출기는 자기 이름(예: stub).';
comment on column career_evidences.prompt_version is
    '사용한 프롬프트 버전. 프롬프트를 쓰지 않는 추출기는 null.';
