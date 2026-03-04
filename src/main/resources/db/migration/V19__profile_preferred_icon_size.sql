-- Preferred icon size for accessibility: "small" | "medium" | "large"
-- Use large by default – assume user may not be able to read.
ALTER TABLE user_profile ADD COLUMN IF NOT EXISTS preferred_icon_size varchar(16) NOT NULL DEFAULT 'large';
