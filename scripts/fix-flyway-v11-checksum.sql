-- Fix Flyway checksum mismatch for V11 after the migration file was modified.
-- Run with: psql -U app -d aac -f scripts/fix-flyway-v11-checksum.sql
-- (adjust -U and -d if your credentials differ)

-- Update the stored checksum to match the current V11 file
UPDATE flyway_schema_history
SET checksum = -772944491
WHERE version = '11';
