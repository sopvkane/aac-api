-- Additional pain “heatmap” seeding (denser + clustered) for demos.
-- Uses the same body_area keys produced by the Speak page heuristics.
-- Guarded to avoid re-inserting if already applied to an existing DB.

DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM wellbeing_entry WHERE notes LIKE 'SEED_PAIN_HEATMAP%') THEN
    RETURN;
  END IF;

  -- Cluster: head + tummy + chest (common), with variety across arms/elbows/hands/knees/legs.
  -- Body areas match Speak page pain tap heuristics (figure 30% width, 60% height, centered).
  INSERT INTO wellbeing_entry (id, mood_score, symptom_type, body_area, severity, notes, created_at)
  SELECT
    gen_random_uuid(),
    NULL,
    'PAIN',
    CASE
      WHEN (n % 12) IN (0,1) THEN 'HEAD'
      WHEN (n % 12) IN (2,3) THEN 'TUMMY'
      WHEN (n % 12) = 4 THEN 'CHEST'
      WHEN (n % 12) = 5 THEN 'LEFT_KNEE'
      WHEN (n % 12) = 6 THEN 'RIGHT_KNEE'
      WHEN (n % 12) = 7 THEN 'LEFT_ELBOW'
      WHEN (n % 12) = 8 THEN 'RIGHT_ELBOW'
      WHEN (n % 12) = 9 THEN 'LEFT_ARM'
      ELSE 'RIGHT_ARM'
    END,
    (3 + (n % 7))::int,
    'SEED_PAIN_HEATMAP v1',
    (now() - (n || ' days')::interval) - interval '2 hours'
  FROM generate_series(0, 364) AS n
  WHERE (n % 11) = 0; -- about 33 events/year

  -- Extra sparse variety across hands/legs so the map isn't one-sided.
  INSERT INTO wellbeing_entry (id, mood_score, symptom_type, body_area, severity, notes, created_at)
  SELECT
    gen_random_uuid(),
    NULL,
    'PAIN',
    CASE (n % 4)
      WHEN 0 THEN 'LEFT_HAND'
      WHEN 1 THEN 'RIGHT_HAND'
      WHEN 2 THEN 'LEFT_LEG'
      ELSE 'RIGHT_LEG'
    END,
    (2 + (n % 8))::int,
    'SEED_PAIN_HEATMAP variety',
    (now() - (n || ' days')::interval) - interval '10 hours'
  FROM generate_series(0, 364) AS n
  WHERE (n % 37) = 0; -- ~10 events/year
END $$;

