-- Multi-tenant: scope data by user_profile (communicator).
-- Each caregiver account can access one or more profiles; session tracks active profile.

-- 1. Account-to-profile access (many-to-many)
CREATE TABLE IF NOT EXISTS caregiver_account_profile (
  account_id uuid NOT NULL REFERENCES caregiver_account(id) ON DELETE CASCADE,
  profile_id uuid NOT NULL REFERENCES user_profile(id) ON DELETE CASCADE,
  PRIMARY KEY (account_id, profile_id)
);

CREATE INDEX IF NOT EXISTS idx_caregiver_account_profile_profile
  ON caregiver_account_profile(profile_id);

-- 2. Add profile_id to data tables (nullable initially for backfill)
ALTER TABLE preference_item ADD COLUMN IF NOT EXISTS profile_id uuid REFERENCES user_profile(id) ON DELETE CASCADE;
ALTER TABLE phrases ADD COLUMN IF NOT EXISTS profile_id uuid REFERENCES user_profile(id) ON DELETE CASCADE;
ALTER TABLE interaction_event ADD COLUMN IF NOT EXISTS profile_id uuid REFERENCES user_profile(id) ON DELETE CASCADE;
ALTER TABLE wellbeing_entry ADD COLUMN IF NOT EXISTS profile_id uuid REFERENCES user_profile(id) ON DELETE CASCADE;

-- 3. Backfill existing rows with default profile
UPDATE preference_item SET profile_id = '00000000-0000-0000-0000-000000000001' WHERE profile_id IS NULL;
UPDATE phrases SET profile_id = '00000000-0000-0000-0000-000000000001' WHERE profile_id IS NULL;
UPDATE interaction_event SET profile_id = '00000000-0000-0000-0000-000000000001' WHERE profile_id IS NULL;
UPDATE wellbeing_entry SET profile_id = '00000000-0000-0000-0000-000000000001' WHERE profile_id IS NULL;

-- 4. Make profile_id NOT NULL
ALTER TABLE preference_item ALTER COLUMN profile_id SET NOT NULL;
ALTER TABLE phrases ALTER COLUMN profile_id SET NOT NULL;
ALTER TABLE interaction_event ALTER COLUMN profile_id SET NOT NULL;
ALTER TABLE wellbeing_entry ALTER COLUMN profile_id SET NOT NULL;

-- 5. Indexes for profile-scoped queries
CREATE INDEX IF NOT EXISTS idx_preference_item_profile ON preference_item(profile_id);
CREATE INDEX IF NOT EXISTS idx_preference_item_profile_kind ON preference_item(profile_id, kind);
CREATE INDEX IF NOT EXISTS idx_phrases_profile ON phrases(profile_id);
CREATE INDEX IF NOT EXISTS idx_interaction_event_profile ON interaction_event(profile_id);
CREATE INDEX IF NOT EXISTS idx_wellbeing_entry_profile ON wellbeing_entry(profile_id);

-- 6. Add profile_id to auth_session
ALTER TABLE auth_session ADD COLUMN IF NOT EXISTS profile_id uuid REFERENCES user_profile(id) ON DELETE SET NULL;

-- 7. Seed account-profile links: all accounts get access to default profile
INSERT INTO caregiver_account_profile (account_id, profile_id)
  SELECT id, '00000000-0000-0000-0000-000000000001'::uuid FROM caregiver_account
ON CONFLICT (account_id, profile_id) DO NOTHING;
