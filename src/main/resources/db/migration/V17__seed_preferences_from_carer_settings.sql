-- Seed preference items from carer settings (user_profile fav_*, matching V7 and user_profile.json)
-- Enables "what is your favourite TV show?" to return Bluey, SpongeBob etc. correctly

DO $$
BEGIN
  -- Only seed if no preference items for these kinds exist yet
  IF (SELECT count(*) FROM preference_item WHERE kind IN ('FOOD', 'DRINK', 'ACTIVITY')) > 0 THEN
    RETURN;
  END IF;

  -- Foods (matches V7 fav_food and user_profile.json)
  INSERT INTO preference_item (id, kind, label, category, scope, priority, created_at, updated_at)
  VALUES
    (gen_random_uuid(), 'FOOD', 'Banana', null, 'BOTH', 10, now(), now()),
    (gen_random_uuid(), 'FOOD', 'Toast', null, 'HOME', 9, now(), now()),
    (gen_random_uuid(), 'FOOD', 'Sandwich', null, 'BOTH', 8, now(), now()),
    (gen_random_uuid(), 'FOOD', 'Pasta', null, 'HOME', 7, now(), now()),
    (gen_random_uuid(), 'FOOD', 'Fruit', null, 'BOTH', 6, now(), now());

  -- Drinks (matches V7 fav_drink)
  INSERT INTO preference_item (id, kind, label, category, scope, priority, created_at, updated_at)
  VALUES
    (gen_random_uuid(), 'DRINK', 'Apple juice', null, 'HOME', 10, now(), now()),
    (gen_random_uuid(), 'DRINK', 'Water', null, 'BOTH', 9, now(), now()),
    (gen_random_uuid(), 'DRINK', 'Milk', null, 'HOME', 8, now(), now());

  -- Activities: TV shows (category TV_SHOW), games (GAME), and general
  -- Matches V7 fav_show=Bluey and user_profile.json shows: Bluey, SpongeBob
  INSERT INTO preference_item (id, kind, label, category, scope, priority, created_at, updated_at)
  VALUES
    (gen_random_uuid(), 'ACTIVITY', 'Bluey', 'TV_SHOW', 'HOME', 12, now(), now()),
    (gen_random_uuid(), 'ACTIVITY', 'SpongeBob', 'TV_SHOW', 'HOME', 11, now(), now()),
    (gen_random_uuid(), 'ACTIVITY', 'Minecraft', 'GAME', 'HOME', 10, now(), now()),
    (gen_random_uuid(), 'ACTIVITY', 'Drawing', null, 'BOTH', 9, now(), now()),
    (gen_random_uuid(), 'ACTIVITY', 'Park', null, 'HOME', 8, now(), now()),
    (gen_random_uuid(), 'ACTIVITY', 'Music', null, 'HOME', 7, now(), now());
END $$;
