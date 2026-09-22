CREATE TABLE club_position_option (
    club_position_option_id BIGINT NOT NULL AUTO_INCREMENT,
    club_id BIGINT NOT NULL,
    name VARCHAR(10) NOT NULL,
    color_hex VARCHAR(7) NOT NULL,
    display_order INT NOT NULL,
    created_at DATETIME(6) NULL,
    modified_at DATETIME(6) NULL,
    PRIMARY KEY (club_position_option_id),
    CONSTRAINT fk_club_position_option_club
        FOREIGN KEY (club_id) REFERENCES club (club_id)
);

CREATE INDEX idx_club_position_option_club
    ON club_position_option (club_id);

ALTER TABLE club_member
    ADD COLUMN position_option_id BIGINT NULL,
    ADD CONSTRAINT fk_club_member_position_option
        FOREIGN KEY (position_option_id) REFERENCES club_position_option (club_position_option_id);
