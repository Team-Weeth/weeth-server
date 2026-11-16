-- 릴리즈 노트: 동아리와 무관한 플랫폼 전역 콘텐츠 (경로/소유에 club_id 없음)

CREATE TABLE release_notes (
    release_note_id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(100) NOT NULL,
    content LONGTEXT NOT NULL,
    thumbnail_storage_key VARCHAR(500) NULL,
    status VARCHAR(20) NOT NULL,
    released_at DATETIME(6) NULL,
    published_at DATETIME(6) NULL,
    author_id BIGINT NOT NULL,
    created_at DATETIME(6) NULL,
    modified_at DATETIME(6) NULL,
    PRIMARY KEY (release_note_id),
    CONSTRAINT fk_release_note_author
        FOREIGN KEY (author_id) REFERENCES users (user_id)
);

-- 공개 목록(status + 업데이트 일자 정렬), 백오피스 목록(status + 생성 시각 정렬)
CREATE INDEX idx_release_note_status_released
    ON release_notes (status, released_at);
CREATE INDEX idx_release_note_status_created
    ON release_notes (status, created_at);
