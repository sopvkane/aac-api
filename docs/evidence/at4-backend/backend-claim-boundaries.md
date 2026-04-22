# Backend Claim Boundaries (AT4)

## Safe claims for AT4
- The backend has an automated Maven test suite that ran successfully in this evidence run (`clean test` and `clean verify`).
- 165 tests executed with 0 failures/errors/skips in Surefire XML outputs for this run.
- JaCoCo line coverage threshold is configured at 80% and is enforced at Maven `verify`.
- This run produced 84.32% aggregated JaCoCo line coverage and passed the verify gate.
- CI workflows are configured for test automation (`api-ci`), Sonar + verify (`sonar`), and dependency review.
- Security configuration now applies secure-by-default authorization for `/api/**` routes, with explicit public endpoint exceptions (auth login/register/logout and health/docs endpoints).

## Claims to avoid
- Avoid claiming whole-system backend coverage without qualification.
  - Reason: Sonar coverage exclusions omit `dialogue`, `speech`, and `tts` packages.
- Avoid claiming comprehensive integration/E2E assurance across all runtime dependencies from this evidence alone.
- Avoid claiming production-grade security assurance from test pass status alone.
- Avoid claiming accessibility compliance from this backend repository evidence pack.
- Avoid claiming historical CI reliability trends unless GitHub Actions run history is also evidenced separately.

## Missed areas and small fixes (actionable)
- Missed area: main CI did not enforce JaCoCo threshold because it ran `test` only.
  - Small fix: update `.github/workflows/ci.yml` to run `./mvnw -B clean verify`.
  - Claim unlocked: "Coverage gate is enforced in primary CI, not only in Sonar workflow."
- Missed area: CI artifacts only contained Surefire reports.
  - Small fix: upload `target/site/jacoco` and `target/jacoco.exec` as CI artifacts.
  - Claim unlocked: "Coverage evidence is retained per CI run and can be audited."
- Missed area: Sonar exclusions reduce whole-system coverage representativeness.
  - Small fix: keep exclusions explicit in report language; optionally reduce exclusions incrementally when stable tests exist for `dialogue/speech/tts`.
  - Claim unlocked: "Coverage claims are bounded and transparent rather than over-claimed."
- Missed area: limited evidence for non-happy-path security assurance.
  - Small fix: add targeted negative integration tests for protected endpoints (unauthorized/forbidden/session-expired).
  - Claim unlocked: "Automated security behavior checks include denial paths, not just success paths."

## Next coverage boosts (small and high-yield)
- Add focused unit tests for `InteractionEventService` and `InteractionEventController`.
  - Why: both remain low-covered and are deterministic to test with mocks.
- Add additional `AuthController` negative-path tests (invalid role normalization, missing cookie on select-profile, invalid session token in `/me`).
  - Why: improves claim strength around auth/session robustness.
- Add pure-unit tests for `SecurityAuthContext` map parsing edge cases.
  - Why: cheap branch coverage gain in auth context extraction logic.
- Defer broader `dialogue/speech/tts` coverage expansion unless exclusions are reduced in Sonar.
  - Why: prevents over-claiming whole-system coverage while external-dependency-heavy modules remain excluded.
