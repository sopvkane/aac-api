CREATE TABLE IF NOT EXISTS user_profile (
  id uuid PRIMARY KEY,
  display_name varchar(50) NOT NULL,
  wake_name varchar(50) NOT NULL,

  details_default boolean NOT NULL DEFAULT true,
  voice_default boolean NOT NULL DEFAULT false,

  ai_enabled boolean NOT NULL DEFAULT true,
  memory_enabled boolean NOT NULL DEFAULT true,
  analytics_enabled boolean NOT NULL DEFAULT false,

  default_location varchar(16) NOT NULL DEFAULT 'HOME',

  allow_home boolean NOT NULL DEFAULT true,
  allow_school boolean NOT NULL DEFAULT true,
  allow_work boolean NOT NULL DEFAULT false,
  allow_other boolean NOT NULL DEFAULT true,

  max_options int NOT NULL DEFAULT 3,

  fav_food varchar(50),
  fav_drink varchar(50),
  fav_show varchar(50),
  fav_topic varchar(50),

  updated_at timestamptz NOT NULL DEFAULT now()
);

-- Seed a single profile row (simplifies the whole app)
INSERT INTO user_profile (
  id, display_name, wake_name,
  details_default, voice_default,
  ai_enabled, memory_enabled, analytics_enabled,
  default_location,
  allow_home, allow_school, allow_work, allow_other,
  max_options
) VALUES (
  '00000000-0000-0000-0000-000000000001',
  'User',
  'Hey',
  true,
  false,
  true,
  true,
  false,
  'HOME',
  true,
  true,
  false,
  true,
  3
)
ON CONFLICT (id) DO NOTHING;