-- 취미 + 유저 200명 + user_hobbies(유저당 랜덤 3~5). docs/category.md, ERD 기반.
-- 실행 전: Flyway로 users, hobbies, user_hobbies, rooms 존재해야 함. match_queue는 FK 없음.
-- 이 스크립트는 idempotent하게 작성되어 여러 번 실행해도 안전합니다.

-- 1. hobbies (category.md) - 중복 방지: name이 이미 존재하면 건너뜀
INSERT INTO hobbies (category, subcategory, name, created_at, updated_at)
SELECT category, subcategory, name, now(), now()
FROM (VALUES
    ('A-1', '콘텐츠 감상', '독서'),
    ('A-1', '콘텐츠 감상', '영화 시청'),
    ('A-1', '콘텐츠 감상', '전시'),
    ('A-1', '콘텐츠 감상', '신문'),
    ('A-1', '콘텐츠 감상', '라디오'),
    ('A-1', '콘텐츠 감상', 'OTT'),
    ('A-2', '감정·휴식', '명상'),
    ('A-2', '감정·휴식', '다도'),
    ('A-2', '감정·휴식', '커피'),
    ('A-2', '감정·휴식', '티타임'),
    ('A-2', '감정·휴식', '휴식'),
    ('A-3', '예술·공예', '수묵화'),
    ('A-3', '예술·공예', '꽃꽂이'),
    ('A-3', '예술·공예', '자수'),
    ('A-3', '예술·공예', '뜨개질'),
    ('A-3', '예술·공예', '비즈공예'),
    ('B-1', '가벼운 활동', '산책'),
    ('B-1', '가벼운 활동', '요가'),
    ('B-1', '가벼운 활동', '필라테스'),
    ('B-2', '라켓/실내스포츠', '배드민턴'),
    ('B-2', '라켓/실내스포츠', '탁구'),
    ('B-2', '라켓/실내스포츠', '테니스'),
    ('B-2', '라켓/실내스포츠', '볼링'),
    ('B-3', '구기 종목', '축구'),
    ('B-3', '구기 종목', '족구'),
    ('B-3', '구기 종목', '골프'),
    ('B-3', '구기 종목', '게이트볼'),
    ('B-3', '구기 종목', '배구'),
    ('B-3', '구기 종목', '농구'),
    ('B-4', '물·레저', '수영'),
    ('B-4', '물·레저', '스노쿨링'),
    ('B-4', '물·레저', '서핑'),
    ('B-5', '아웃도어', '등산'),
    ('B-5', '아웃도어', '자전거'),
    ('B-5', '아웃도어', '낚시'),
    ('B-6', '두뇌게임', '장기'),
    ('B-6', '두뇌게임', '바둑'),
    ('B-6', '두뇌게임', '체스'),
    ('C-1', '제작', '요리'),
    ('C-1', '제작', '베이킹'),
    ('C-2', '식도락', '맛집 탐방'),
    ('C-3', '주류', '술'),
    ('C-3', '주류', '와인'),
    ('D', '여행', '여행'),
    ('E', '음악·표현', '노래'),
    ('E', '음악·표현', '기타'),
    ('E', '음악·표현', '피아노'),
    ('E', '음악·표현', '플룻'),
    ('E', '음악·표현', '베이스'),
    ('E', '음악·표현', '바이올린'),
    ('F', '자기계발', '재테크'),
    ('F', '자기계발', '영어'),
    ('F', '자기계발', '일본어'),
    ('F', '자기계발', '불어')
) AS v(category, subcategory, name)
WHERE NOT EXISTS (
    SELECT 1 FROM hobbies WHERE hobbies.name = v.name
);

-- 2-1. 고정 테스트 사용자 (예측 가능한 데이터) - 매칭 테스트용
INSERT INTO users (id, nickname, gender, birth_date, region, total_score, tier, created_at, updated_at) VALUES
-- 매칭 테스트용 사용자 (공통 취미 보유)
('00000000-0000-0000-0000-000000000001'::uuid, 'test_user', 'MALE', '1995-01-01', 'SEOUL', 0, 'SPROUT', now(), now()),
('00000000-0000-0000-0000-000000000002'::uuid, 'partner_user', 'FEMALE', '1996-01-01', 'SEOUL', 0, 'SPROUT', now(), now()),
-- 다양한 Tier 테스트용
('00000000-0000-0000-0000-000000000003'::uuid, 'tier_fruit', 'MALE', '1990-01-01', 'SEOUL', 25, 'FRUIT', now(), now()),
('00000000-0000-0000-0000-000000000004'::uuid, 'tier_petal', 'FEMALE', '1992-01-01', 'BUSAN', 15, 'PETAL', now(), now()),
('00000000-0000-0000-0000-000000000005'::uuid, 'tier_wilting', 'MALE', '1985-01-01', 'DAEGU', -15, 'WILTING', now(), now()),
('00000000-0000-0000-0000-000000000006'::uuid, 'tier_fertilizer', 'FEMALE', '1980-01-01', 'GWANGJU', -25, 'FERTILIZER', now(), now())
ON CONFLICT (nickname) DO UPDATE SET
    gender = EXCLUDED.gender,
    birth_date = EXCLUDED.birth_date,
    region = EXCLUDED.region,
    total_score = EXCLUDED.total_score,
    tier = EXCLUDED.tier,
    updated_at = now();

-- 2-2. users 200명 (랜덤 - 기존 유지)
INSERT INTO users (id, nickname, gender, birth_date, region, total_score, tier, created_at, updated_at)
SELECT
  gen_random_uuid(),
  'user_' || i,
  (ARRAY['MALE','FEMALE'])[1 + (i % 2)],
  (DATE '1990-01-01' + (i % 12000)::int)::date,
  (ARRAY['SEOUL','GYEONGGI','INCHEON','BUSAN','DAEGU','GWANGJU','JEJU','GYEONGNAM','JEONBUK'])[1 + (i % 9)],
  0,
  'SPROUT',
  now(),
  now()
FROM generate_series(1, 200) i
ON CONFLICT (nickname) DO NOTHING;

-- 3-1. 고정 테스트 사용자의 취미 매핑 (공통 취미 보유 - 매칭 테스트용)
INSERT INTO user_hobbies (user_id, hobby_id, created_at, updated_at)
SELECT u.id, h.id, now(), now()
FROM users u
CROSS JOIN hobbies h
WHERE (u.nickname = 'test_user' AND h.name IN ('축구', '영화 시청', '독서'))
   OR (u.nickname = 'partner_user' AND h.name IN ('축구', '영화 시청', '요리'))
   OR (u.nickname = 'tier_fruit' AND h.name IN ('등산', '독서', '요리', '여행'))
   OR (u.nickname = 'tier_petal' AND h.name IN ('영화 시청', '요가', '베이킹'))
   OR (u.nickname = 'tier_wilting' AND h.name IN ('독서', '명상'))
   OR (u.nickname = 'tier_fertilizer' AND h.name IN ('독서'))
ON CONFLICT (user_id, hobby_id) DO NOTHING;

-- 3-2. user_hobbies: 유저당 랜덤 3~5개 취미 (기존 유지 - 고정 사용자 제외)
INSERT INTO user_hobbies (user_id, hobby_id, created_at, updated_at)
SELECT u.id, h.id, now(), now()
FROM users u
CROSS JOIN LATERAL (
  SELECT id FROM hobbies
  ORDER BY random()
  LIMIT 3 + (floor(random() * 3)::int)
) h
WHERE u.nickname NOT IN ('test_user', 'partner_user', 'tier_fruit', 'tier_petal', 'tier_wilting', 'tier_fertilizer')
  AND u.nickname LIKE 'user_%'
  AND NOT EXISTS (
    SELECT 1
    FROM user_hobbies uh
    WHERE uh.user_id = u.id
  )
ON CONFLICT (user_id, hobby_id) DO NOTHING;
