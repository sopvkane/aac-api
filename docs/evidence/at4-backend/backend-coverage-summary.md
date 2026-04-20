# Backend Coverage Summary (AT4 Evidence)

Generated from local run on 2026-04-20.

## JaCoCo Threshold Configuration
- JaCoCo plugin defined in `pom.xml`.
- Coverage check rule:
  - Counter: `LINE`
  - Value: `COVEREDRATIO`
  - Minimum: `0.80` (80%)
- Source: `pom.xml` (`jacoco-maven-plugin` check execution).

## Enforcement Phase
- JaCoCo `check` is enforced at Maven **`verify`** phase, not at `test` phase.
- `ci.yml` now runs `./mvnw -B clean verify` so the JaCoCo gate is enforced in primary CI.
- `sonar.yml` runs `./mvnw -B clean verify`.

## Actual Coverage Result (This Run)
From `target/site/jacoco/jacoco.csv` aggregate after `clean verify`:
- Line coverage: **84.32%** (`2377 covered / 442 missed`)
- Branch coverage: **54.41%** (`926 covered / 776 missed`)

`clean verify` completed with **BUILD SUCCESS**, indicating the configured JaCoCo line threshold passed.

## Coverage Change vs Earlier Baseline (same day)
- Previous evidence run: **80.26%** line coverage
- Current evidence run: **84.32%** line coverage
- Net change: **+4.06 percentage points**

## Sonar Exclusions / Excluded Packages
`sonar-project.properties` contains:
- `sonar.coverage.exclusions=**/dialogue/**,**/speech/**,**/tts/**`

These exclusions remove major packages from Sonar coverage accounting.

## Explicit Coverage Warning
Reported coverage should **not** be presented as whole-system backend coverage without qualification, because Sonar coverage excludes `dialogue`, `speech`, and `tts` packages.
