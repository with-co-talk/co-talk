-- Add notification_preview_mode column
ALTER TABLE notification_settings ADD COLUMN notification_preview_mode VARCHAR(20) DEFAULT 'NAME_AND_MESSAGE' NOT NULL;

-- Migrate existing data
UPDATE notification_settings
SET notification_preview_mode = CASE
    WHEN show_message_content_in_notification = true THEN 'NAME_AND_MESSAGE'
    ELSE 'NAME_ONLY'
END;

-- Drop old column
ALTER TABLE notification_settings DROP COLUMN show_message_content_in_notification;
