-- V2 의 주석이 낡았다.
--
-- "Evidence 당 최소 1행이어야 하지만 DB 제약으로 표현할 수 없다"고 써 뒀는데,
-- 그 정책 자체를 폐기했다. 반드시 채워야 하는 칸은 모델에게 지어낼 압력이 된다.
-- 원문에 기술명이 없으면 모델은 제약을 만족시키려고 하나를 만들어낸다.
-- 근거: docs/01-domain/career-evidence.md

comment on table career_evidence_skills is
    'Evidence 당 0행일 수 있다. 기술명이 드러나지 않는 경험(협업/리딩/요구사항 분석)도 Evidence다.';
