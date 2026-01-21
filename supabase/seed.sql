-- 취미 + 유저 200명 + user_hobbies(유저당 랜덤 3~5). docs/category.md, ERD 기반.
-- 실행 전: Flyway로 users, hobbies, user_hobbies, rooms 존재해야 함. match_queue는 FK 없음.

-- 1. hobbies (category.md)
INSERT INTO hobbies (category, subcategory, name, created_at, updated_at) VALUES
('A-1', '콘텐츠 감상', '독서', now(), now()),
('A-1', '콘텐츠 감상', '영화 시청', now(), now()),
('A-1', '콘텐츠 감상', '전시', now(), now()),
('A-1', '콘텐츠 감상', '신문', now(), now()),
('A-1', '콘텐츠 감상', '라디오', now(), now()),
('A-1', '콘텐츠 감상', 'OTT', now(), now()),
('A-2', '감정·휴식', '명상', now(), now()),
('A-2', '감정·휴식', '다도', now(), now()),
('A-2', '감정·휴식', '커피', now(), now()),
('A-2', '감정·휴식', '티타임', now(), now()),
('A-2', '감정·휴식', '휴식', now(), now()),
('A-3', '예술·공예', '수묵화', now(), now()),
('A-3', '예술·공예', '꽃꽂이', now(), now()),
('A-3', '예술·공예', '자수', now(), now()),
('A-3', '예술·공예', '뜨개질', now(), now()),
('A-3', '예술·공예', '비즈공예', now(), now()),
('B-1', '가벼운 활동', '산책', now(), now()),
('B-1', '가벼운 활동', '요가', now(), now()),
('B-1', '가벼운 활동', '필라테스', now(), now()),
('B-2', '라켓/실내스포츠', '배드민턴', now(), now()),
('B-2', '라켓/실내스포츠', '탁구', now(), now()),
('B-2', '라켓/실내스포츠', '테니스', now(), now()),
('B-2', '라켓/실내스포츠', '볼링', now(), now()),
('B-3', '구기 종목', '축구', now(), now()),
('B-3', '구기 종목', '족구', now(), now()),
('B-3', '구기 종목', '골프', now(), now()),
('B-3', '구기 종목', '게이트볼', now(), now()),
('B-3', '구기 종목', '배구', now(), now()),
('B-3', '구기 종목', '농구', now(), now()),
('B-4', '물·레저', '수영', now(), now()),
('B-4', '물·레저', '스노쿨링', now(), now()),
('B-4', '물·레저', '서핑', now(), now()),
('B-5', '아웃도어', '등산', now(), now()),
('B-5', '아웃도어', '자전거', now(), now()),
('B-5', '아웃도어', '낚시', now(), now()),
('B-6', '두뇌게임', '장기', now(), now()),
('B-6', '두뇌게임', '바둑', now(), now()),
('B-6', '두뇌게임', '체스', now(), now()),
('C-1', '제작', '요리', now(), now()),
('C-1', '제작', '베이킹', now(), now()),
('C-2', '식도락', '맛집 탐방', now(), now()),
('C-3', '주류', '술', now(), now()),
('C-3', '주류', '와인', now(), now()),
('D', '여행', '여행', now(), now()),
('E', '음악·표현', '노래', now(), now()),
('E', '음악·표현', '기타', now(), now()),
('E', '음악·표현', '피아노', now(), now()),
('E', '음악·표현', '플룻', now(), now()),
('E', '음악·표현', '베이스', now(), now()),
('E', '음악·표현', '바이올린', now(), now()),
('F', '자기계발', '재테크', now(), now()),
('F', '자기계발', '영어', now(), now()),
('F', '자기계발', '일본어', now(), now()),
('F', '자기계발', '불어', now(), now());

-- 2. users 200명 (generate_series + gen_random_uuid, gender/region/tier = enum name)
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

-- 3. user_hobbies: 유저당 랜덤 3~5개 취미 (ON CONFLICT DO NOTHING)
INSERT INTO user_hobbies (user_id, hobby_id, created_at, updated_at)
SELECT u.id, h.id, now(), now()
FROM users u
CROSS JOIN LATERAL (
  SELECT id FROM hobbies
  ORDER BY random()
  LIMIT 3 + (floor(random() * 3)::int)
) h
ON CONFLICT (user_id, hobby_id) DO NOTHING;
