-- Extra seed data for wellbeing check-ins (mood 2, 3, 5 for pie charts) and pain severity.
-- Adds variety for the dashboard demo. Guarded to avoid duplicates.

DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM wellbeing_entry WHERE notes LIKE 'SEED_DEMO%') THEN
    RETURN;
  END IF;

  -- Mood check-ins: Happy (5), Sad (2), Not sure (3) - spread over last 14 days
  INSERT INTO wellbeing_entry (id, mood_score, symptom_type, body_area, severity, notes, created_at)
  SELECT
    gen_random_uuid(),
    CASE (n % 3)
      WHEN 0 THEN 5
      WHEN 1 THEN 2
      ELSE 3
    END,
    NULL,
    NULL,
    NULL,
    'SEED_DEMO mood',
    (now() - (n || ' days')::interval) - (n % 12 || ' hours')::interval
  FROM generate_series(0, 13) AS n;

  -- Pain with varied severities: last 14 days, mixed body areas and severity 1-10
  INSERT INTO wellbeing_entry (id, mood_score, symptom_type, body_area, severity, notes, created_at)
  SELECT
    gen_random_uuid(),
    NULL,
    'PAIN',
    CASE (n % 7)
      WHEN 0 THEN 'HEAD'
      WHEN 1 THEN 'TUMMY'
      WHEN 2 THEN 'LEFT_KNEE'
      WHEN 3 THEN 'RIGHT_HAND'
      WHEN 4 THEN 'CHEST'
      WHEN 5 THEN 'LEFT_ARM'
      ELSE 'RIGHT_KNEE'
    END,
    (1 + (n * 3 + (n % 5)) % 10)::int,
    'SEED_DEMO pain',
    (now() - (n || ' days')::interval) - (n % 8 || ' hours')::interval
  FROM generate_series(0, 13) AS n;
END $$;
