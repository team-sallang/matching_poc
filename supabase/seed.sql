-- 취미 + 유저 200명 + user_hobbies(유저당 랜덤 3~5). docs/category.md, ERD 기반.
-- 실행 전: Supabase migrations(create_core_tables) 적용 후 users, hobbies, user_hobbies, rooms 존재해야 함. match_queue는 FK 없음.
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

-- 2. 기존 사용자/매칭 데이터 초기화 (동시성 테스트용)
TRUNCATE TABLE match_queue, user_hobbies, rooms, users RESTART IDENTITY;

-- 3-1. 고정 테스트 사용자 10명 (확인용)
INSERT INTO users (id, nickname, gender, birth_date, region, total_score, tier, created_at, updated_at) VALUES
('00000000-0000-0000-0000-000000000001'::uuid, 'fixed_user_01', 'MALE',   '1995-01-01', 'SEOUL', 0, 'SPROUT', now(), now()),
('00000000-0000-0000-0000-000000000002'::uuid, 'fixed_user_02', 'FEMALE', '1995-01-01', 'SEOUL', 0, 'SPROUT', now(), now()),
('00000000-0000-0000-0000-000000000003'::uuid, 'fixed_user_03', 'MALE',   '1995-01-01', 'SEOUL', 0, 'SPROUT', now(), now()),
('00000000-0000-0000-0000-000000000004'::uuid, 'fixed_user_04', 'FEMALE', '1995-01-01', 'SEOUL', 0, 'SPROUT', now(), now()),
('00000000-0000-0000-0000-000000000005'::uuid, 'fixed_user_05', 'MALE',   '1995-01-01', 'SEOUL', 0, 'SPROUT', now(), now()),
('00000000-0000-0000-0000-000000000006'::uuid, 'fixed_user_06', 'FEMALE', '1995-01-01', 'SEOUL', 0, 'SPROUT', now(), now()),
('00000000-0000-0000-0000-000000000007'::uuid, 'fixed_user_07', 'MALE',   '1995-01-01', 'SEOUL', 0, 'SPROUT', now(), now()),
('00000000-0000-0000-0000-000000000008'::uuid, 'fixed_user_08', 'FEMALE', '1995-01-01', 'SEOUL', 0, 'SPROUT', now(), now()),
('00000000-0000-0000-0000-000000000009'::uuid, 'fixed_user_09', 'MALE',   '1995-01-01', 'SEOUL', 0, 'SPROUT', now(), now()),
('00000000-0000-0000-0000-000000000010'::uuid, 'fixed_user_10', 'FEMALE', '1995-01-01', 'SEOUL', 0, 'SPROUT', now(), now());

-- 3-2. 동시성 테스트용 동일 조건 사용자 1000명 (gender 50:50)
INSERT INTO users (id, nickname, gender, birth_date, region, total_score, tier, created_at, updated_at)
SELECT
  ('00000000-0000-0000-0000-' || lpad((1000 + i)::text, 12, '0'))::uuid,
  'load_user_' || lpad(i::text, 4, '0'),
  CASE WHEN i % 2 = 0 THEN 'FEMALE' ELSE 'MALE' END,
  DATE '1995-01-01',
  'SEOUL',
  0,
  'SPROUT',
  now(),
  now()
FROM generate_series(1, 1000) i
ON CONFLICT (nickname) DO NOTHING;

-- 3-3. 동시성 테스트용 동일 조건 취미 매핑 (고정 3개)
INSERT INTO user_hobbies (user_id, hobby_id, created_at, updated_at)
SELECT u.id, h.id, now(), now()
FROM users u
JOIN hobbies h ON h.name IN ('축구', '영화 시청', '독서')
WHERE u.nickname LIKE 'load_user_%'
   OR u.nickname LIKE 'fixed_user_%'
ON CONFLICT (user_id, hobby_id) DO NOTHING;
