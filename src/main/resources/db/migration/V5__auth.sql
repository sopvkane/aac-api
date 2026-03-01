CREATE TABLE IF NOT EXISTS caregiver_account (
  id uuid PRIMARY KEY,
  role varchar(16) NOT NULL UNIQUE, -- PARENT/CARER/CLINICIAN
  pin_hash varchar(100) NOT NULL,
  is_active boolean NOT NULL DEFAULT true,
  created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS auth_session (
  id uuid PRIMARY KEY,
  role varchar(16) NOT NULL,
  token_hash varchar(64) NOT NULL UNIQUE, -- sha256 hex
  expires_at timestamptz NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_auth_session_expires
  ON auth_session (expires_at);