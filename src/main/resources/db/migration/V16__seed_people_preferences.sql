-- Seed people for the Speak tab "who to ask" picker
-- FAMILY_MEMBER (home), TEACHER (school), BUS_STAFF (bus)

-- Only seed if no people have been added yet (avoids duplicates on re-run)
DO $$
BEGIN
  IF (SELECT count(*) FROM preference_item WHERE kind IN ('FAMILY_MEMBER', 'TEACHER', 'BUS_STAFF')) > 0 THEN
    RETURN;
  END IF;

  -- Family members (scope HOME)
  INSERT INTO preference_item (id, kind, label, scope, priority, created_at, updated_at)
  VALUES
    (gen_random_uuid(), 'FAMILY_MEMBER', 'Mum', 'HOME', 10, now(), now()),
    (gen_random_uuid(), 'FAMILY_MEMBER', 'Dad', 'HOME', 9, now(), now()),
    (gen_random_uuid(), 'FAMILY_MEMBER', 'Grandma', 'HOME', 5, now(), now());

  -- Teachers (scope SCHOOL)
  INSERT INTO preference_item (id, kind, label, scope, priority, created_at, updated_at)
  VALUES
    (gen_random_uuid(), 'TEACHER', 'Mrs Patel', 'SCHOOL', 10, now(), now()),
    (gen_random_uuid(), 'TEACHER', 'Mr Jones', 'SCHOOL', 9, now(), now());

  -- Bus staff (scope SCHOOL for bus commute)
  INSERT INTO preference_item (id, kind, label, scope, priority, created_at, updated_at)
  VALUES
    (gen_random_uuid(), 'BUS_STAFF', 'Dave (driver)', 'SCHOOL', 10, now(), now()),
    (gen_random_uuid(), 'BUS_STAFF', 'Sarah (assistant)', 'SCHOOL', 9, now(), now());
END $$;
