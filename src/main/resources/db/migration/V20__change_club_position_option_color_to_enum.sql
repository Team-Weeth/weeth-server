ALTER TABLE club_position_option
    MODIFY COLUMN color_hex VARCHAR(20) NOT NULL;

UPDATE club_position_option
SET color_hex = 'PRIMARY';

ALTER TABLE club_position_option
    RENAME COLUMN color_hex TO color;
