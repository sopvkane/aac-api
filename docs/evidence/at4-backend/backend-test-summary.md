# Backend Test Summary (AT4 Evidence)

Generated on: 2026-04-20 (Europe/London)
Repository: `aac-api`
Commit: `47a049a`

## What Was Run
- `./mvnw -B clean test`
- `./mvnw -B clean verify`
- Final retained log capture used filtered output to avoid oversized debug logs while preserving build/test result lines.

## Pass/Fail Outcome
- `clean test`: **PASS** (build success)
- `clean verify`: **PASS** (build success, JaCoCo check passed)
- Surefire totals: **0 failures**, **0 errors**, **0 skipped**.
- No Maven check command failed in the final evidence run.

## How Many Tests Ran
- Surefire XML aggregate (latest run): **165 tests**.
- Source: `target/surefire-reports/TEST-*.xml` (aggregated by command in `backend-test-output.txt`).

## Assurance Improvements Applied
- Added security denial-path integration coverage in:
  - `src/test/java/com/sophie/aac/auth/security/SecurityIntegrationTest.java`
  - Invalid cookie token rejected
  - Expired session rejected
  - `/api/auth/me` rejects anonymous access
  - `/api/interactions` rejects anonymous and allows authenticated session
- Added icon suggestion endpoint tests in:
  - `src/test/java/com/sophie/aac/icons/controller/IconSuggestionsControllerTest.java`
- Added profile setup service unit tests in:
  - `src/test/java/com/sophie/aac/profile/service/ProfileSetupServiceTest.java`
- Added interaction event service/controller tests in:
  - `src/test/java/com/sophie/aac/analytics/service/InteractionEventServiceTest.java`
  - `src/test/java/com/sophie/aac/analytics/controller/InteractionEventControllerTest.java`
- Added auth context unit tests in:
  - `src/test/java/com/sophie/aac/auth/util/SecurityAuthContextTest.java`

## Strongest Evidence Files
- `target/surefire-reports/TEST-*.xml` (machine-readable executed test results)
- `target/site/jacoco/jacoco.csv` and `target/site/jacoco/index.html` (coverage outputs from verify run)
- `pom.xml` (JaCoCo threshold and verify-phase gate)
- `.github/workflows/ci.yml` and `.github/workflows/sonar.yml` (CI execution policy)
- `sonar-project.properties` (Sonar coverage exclusions)

## What This Supports in an AT4 Report
- Supports a cautious claim that this backend has a reproducible automated test suite and passes both `test` and `verify` locally.
- Supports a claim that a JaCoCo line coverage threshold exists and currently passes on `verify`.
- Supports a claim that CI workflows are configured for automated test and Sonar runs.
- Does **not** support a claim of comprehensive whole-system assurance without caveats (see `backend-claim-boundaries.md`).
