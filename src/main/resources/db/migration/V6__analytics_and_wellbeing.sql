CREATE TABLE IF NOT EXISTS interaction_event (
  id uuid PRIMARY KEY,
  event_type varchar(32) NOT NULL, -- SUGGESTIONS_SERVED / OPTION_SELECTED
  location varchar(16) NOT NULL,   -- LocationCategory
  prompt_type varchar(32),
  selected_text varchar(280),
  created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_event_created_at ON interaction_event (created_at);
CREATE INDEX IF NOT EXISTS idx_event_type ON interaction_event (event_type);

CREATE TABLE IF NOT EXISTS wellbeing_entry (
  id uuid PRIMARY KEY,
  mood_score int,
  symptom_type varchar(32),
  body_area varchar(32),
  severity int,
  notes varchar(280),
  created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_wellbeing_created_at ON wellbeing_entry (created_at);