-- Presigned object-storage URLs can exceed 255 characters.
ALTER TABLE messages
    ALTER COLUMN file_url TYPE VARCHAR(2048),
    ALTER COLUMN thumbnail_url TYPE VARCHAR(2048);

ALTER TABLE users
    ALTER COLUMN avatar_url TYPE VARCHAR(2048),
    ALTER COLUMN background_url TYPE VARCHAR(2048);

ALTER TABLE chat_rooms
    ALTER COLUMN image_url TYPE VARCHAR(2048);
