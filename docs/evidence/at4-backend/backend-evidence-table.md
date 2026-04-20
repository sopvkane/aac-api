# Backend Evidence Table

| Evidence item | File/source | What it proves | Limitation |
|---|---|---|---|
| Executed command log | `docs/evidence/at4-backend/backend-commands-run.txt` | Exact reproducible commands and run window used for this pack | Local run evidence only; does not include CI UI screenshots |
| Full test/verify console output | `docs/evidence/at4-backend/backend-test-output.txt` | `clean test` and `clean verify` were executed and succeeded | Long log; contains debug SQL noise and should be interpreted with summary files |
| Surefire machine-readable test reports | `target/surefire-reports/TEST-*.xml` | 143 tests executed, 0 failures/errors/skips for this run | Reflects this run only; not trend over time |
| JaCoCo report artifacts | `target/site/jacoco/jacoco.csv`, `target/site/jacoco/index.html` | Coverage reports generated from verify run; line coverage reached 80.26% | Coverage is metric-based; does not prove test depth or scenario completeness |
| JaCoCo threshold config | `pom.xml` | 80% line threshold configured and enforced at `verify` | Not enforced in `api-ci` workflow that runs only `test` |
| Sonar coverage exclusions | `sonar-project.properties` | Explicit exclusion of `dialogue`, `speech`, `tts` from Sonar coverage | Means Sonar-reported coverage is not whole-backend coverage |
| CI test workflow config | `.github/workflows/ci.yml` | PR/main automation for test execution and report artifact upload exists | Runs `test` only, not `verify` |
| Sonar workflow config | `.github/workflows/sonar.yml` | PR/main automation for `clean verify` + Sonar scan exists | Requires Sonar token and remote workflow context to evidence live cloud outcome |
| Dependency review workflow | `.github/workflows/dependency-review.yml` | Dependency review automation is configured on PRs | Configuration evidence only in this pack |
| Coverage interpretation summary | `docs/evidence/at4-backend/backend-coverage-summary.md` | Cautious interpretation of threshold/enforcement/exclusions | Summary depends on correctness of generated run artifacts |
