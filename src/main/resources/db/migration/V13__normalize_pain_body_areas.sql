-- Normalize legacy body_area values to match Speak page heuristics (HEAD, TUMMY, CHEST, etc.).
-- Ensures dashboard heatmap correctly aggregates pain data from V7 seed and any old entries.

UPDATE wellbeing_entry SET body_area = 'TUMMY' WHERE body_area = 'stomach';
UPDATE wellbeing_entry SET body_area = 'HEAD' WHERE body_area = 'head';
