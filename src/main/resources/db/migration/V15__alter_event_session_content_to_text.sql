-- V3 마이그레이션 대상 데이터의 event.content 최대 길이가 632자로 varchar(500) 한도를 초과한다.
-- session.content(최대 431자)도 여유가 적어 함께 TEXT로 통일한다.
ALTER TABLE event
    MODIFY COLUMN content TEXT;

ALTER TABLE session
    MODIFY COLUMN content TEXT;
