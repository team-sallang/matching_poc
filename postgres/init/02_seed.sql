-- 로컬 테스트용 시드 데이터 (k6 부하 테스트 기준)
-- 취미 3개: 독서(1), 영화 시청(2), 축구(3)
INSERT INTO hobbies (category, subcategory, name, created_at, updated_at) VALUES
('CULTURE',  'A-1. 콘텐츠 감상', '독서',    '2026-01-23 14:29:55.917336', '2026-01-23 14:29:55.917336'),
('CULTURE',  'A-1. 콘텐츠 감상', '영화 시청','2026-01-23 14:29:55.917336', '2026-01-23 14:29:55.917336'),
('SPORTS',   'B-3. 구기 종목',   '축구',     '2026-01-23 14:29:55.917336', '2026-01-23 14:29:55.917336');

-- 1000명 테스트 유저
-- UUID 패턴: 00000000-0000-0000-0000-000000001001 ~ 000000002000
-- i=1(load_user_0001, MALE) ~ i=1000(load_user_1000, FEMALE), 짝수=FEMALE, 홀수=MALE
INSERT INTO users (id, nickname, gender, birth_date, region, total_score, tier, created_at, updated_at)
SELECT
    ('00000000-0000-0000-0000-' || lpad((1000 + i)::text, 12, '0'))::UUID,
    'load_user_' || lpad(i::text, 4, '0'),
    CASE WHEN i % 2 = 0 THEN 'FEMALE' ELSE 'MALE' END,
    '1995-01-01'::DATE,
    'SEOUL',
    0,
    'SPROUT',
    '2026-01-23 14:29:55.917336',
    '2026-01-23 14:29:55.917336'
FROM generate_series(1, 1000) AS i;

-- 유저-취미 연결 (각 유저 → 취미 1, 2, 3)
INSERT INTO user_hobbies (user_id, hobby_id, created_at, updated_at)
SELECT
    ('00000000-0000-0000-0000-' || lpad((1000 + i)::text, 12, '0'))::UUID,
    h,
    '2026-01-23 14:29:55.917336',
    '2026-01-23 14:29:55.917336'
FROM generate_series(1, 1000) AS i
CROSS JOIN unnest(ARRAY[1, 2, 3]) AS h;
