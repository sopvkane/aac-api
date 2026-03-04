-- Seed data for mood check-in pie charts and pain severity line chart.
-- Uses distinct notes to avoid conflicting with other seeds.

DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM wellbeing_entry WHERE notes = 'SEED_CHARTS mood') THEN
    RETURN;
  END IF;

  -- Mood check-ins for pie charts: Happy (5), Sad (2), Not sure (3)
  -- Distribution: ~33% each over last 60 days so all 3 pies show data
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
    'SEED_CHARTS mood',
    (now() - (n || ' days')::interval) - ((n % 14) || ' hours')::interval
  FROM generate_series(0, 59) AS n
  WHERE (n % 2) = 0;

  -- Pain severity over time: spread across last 60 days with varied severity 1-10
  INSERT INTO wellbeing_entry (id, mood_score, symptom_type, body_area, severity, notes, created_at)
  SELECT
    gen_random_uuid(),
    NULL,
    'PAIN',
    CASE (n % 8)
      WHEN 0 THEN 'HEAD'
      WHEN 1 THEN 'TUMMY'
      WHEN 2 THEN 'CHEST'
      WHEN 3 THEN 'LEFT_KNEE'
      WHEN 4 THEN 'RIGHT_HAND'
      WHEN 5 THEN 'LEFT_ARM'
      WHEN 6 THEN 'RIGHT_KNEE'
      ELSE 'LEFT_HAND'
    END,
    (1 + ((n * 7 + (n % 3)) % 10))::int,
    'SEED_CHARTS pain',
    (now() - (n || ' days')::interval) - ((n % 10) || ' hours')::interval
  FROM generate_series(0, 59) AS n
  WHERE (n % 3) = 0 OR (n % 5) = 0;
END $$;
