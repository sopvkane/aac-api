-- Full accounts (email + password) and delegated PIN access for shared devices.
-- Roles: PARENT_CARER (owns system), CLINICIAN (can modify). PIN users get delegated access.

-- 1. User account (full registration - email + password)
CREATE TABLE IF NOT EXISTS user_account (
  id uuid PRIMARY KEY,
  email varchar(255) NOT NULL UNIQUE,
  password_hash varchar(255) NOT NULL,
  display_name varchar(100) NOT NULL,
  role varchar(16) NOT NULL,  -- PARENT_CARER | CLINICIAN
  is_active boolean NOT NULL DEFAULT true,
  created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_user_account_email ON user_account(email);
CREATE INDEX IF NOT EXISTS idx_user_account_role ON user_account(role);

-- 2. Which profiles a user can access
CREATE TABLE IF NOT EXISTS user_account_profile (
  user_id uuid NOT NULL REFERENCES user_account(id) ON DELETE CASCADE,
  profile_id uuid NOT NULL REFERENCES user_profile(id) ON DELETE CASCADE,
  PRIMARY KEY (user_id, profile_id)
);

CREATE INDEX IF NOT EXISTS idx_user_account_profile_profile ON user_account_profile(profile_id);

-- 3. Joining codes (clinician creates for registration)
CREATE TABLE IF NOT EXISTS joining_code (
  id uuid PRIMARY KEY,
  code varchar(16) NOT NULL UNIQUE,
  created_by_user_id uuid NOT NULL REFERENCES user_account(id) ON DELETE CASCADE,
  expires_at timestamptz NOT NULL,
  max_uses int,  -- NULL = unlimited
  used_count int NOT NULL DEFAULT 0,
  created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_joining_code_code ON joining_code(code);

-- 4. Delegated PINs (parent/clinician creates for shared device access)
CREATE TABLE IF NOT EXISTS delegated_pin (
  id uuid PRIMARY KEY,
  pin_hash varchar(100) NOT NULL,
  label varchar(50) NOT NULL,  -- e.g. "Grandma", "Teacher"
  created_by_user_id uuid NOT NULL REFERENCES user_account(id) ON DELETE CASCADE,
  profile_id uuid NOT NULL REFERENCES user_profile(id) ON DELETE CASCADE,
  is_active boolean NOT NULL DEFAULT true,
  created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_delegated_pin_creator ON delegated_pin(created_by_user_id);

-- 5. Extend auth_session for new auth types
ALTER TABLE auth_session ADD COLUMN IF NOT EXISTS user_account_id uuid REFERENCES user_account(id) ON DELETE CASCADE;
ALTER TABLE auth_session ADD COLUMN IF NOT EXISTS delegated_pin_id uuid REFERENCES delegated_pin(id) ON DELETE CASCADE;

-- Demo users and joining codes are seeded by UserAccountSeeder on application boot
