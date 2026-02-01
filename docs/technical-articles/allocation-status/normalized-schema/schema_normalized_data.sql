-- 0. 데이터 초기화 (기존 데이터 삭제 및 일련번호 리셋)
TRUNCATE TABLE allocations, seats, blocks, sections, areas, matches RESTART IDENTITY CASCADE;

-- 1. Areas 생성
-- ID를 직접 지정해서 넣습니다. (IDENTITY 옵션이 있어도 직접 입력이 가능합니다)
INSERT INTO areas (id, name) VALUES 
(1, 'Infield'),
(2, 'Outfield');

-- 2. Sections 생성
INSERT INTO sections (id, area_id, name) VALUES 
(1, 1, 'Home'),
(2, 1, 'Away'),
(3, 2, 'Left'),
(4, 2, 'Right');

-- 3. Blocks 생성 (Section당 25개씩, 총 100개)
INSERT INTO blocks (id, section_id, name)
SELECT 
    i,
    ((i-1) / 25) + 1,
    'Block ' || ((i-1) % 25 + 1)
FROM generate_series(1, 100) i;

-- 4. Seats 생성 (Block당 100개씩, 총 10,000개)
INSERT INTO seats (id, block_id, row_number, seat_number)
SELECT 
    i,
    ((i-1) / 100) + 1,
    ((i-1) % 100 / 10) + 1,
    (i-1) % 10 + 1
FROM generate_series(1, 10000) i;

-- 5. Matches 생성
INSERT INTO matches (id, stadium, home_team, away_team, date_time) VALUES 
(1, 'Gocheok Sky Dome', 'Kiwoom Heroes', 'SSG Landers', NOW() + INTERVAL '1 day');

-- 6. Allocations (초기 데이터 - 일부 좌석 점유 상태)
INSERT INTO allocations (match_id, seat_id, state, updated_at) -- id 컬럼 제외
SELECT 
    1,          -- match_id
    id,         -- seat_id (seats 테이블의 id)
    'OCCUPIED',
    NOW()
FROM seats
WHERE id % 5 = 0;

-- 7. 인덱스 통계 갱신
ANALYZE areas, sections, blocks, seats, matches, allocations;