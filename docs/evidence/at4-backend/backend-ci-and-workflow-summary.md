# Backend CI and Workflow Summary (AT4 Evidence)

## Workflows Present
- `api-ci` (`.github/workflows/ci.yml`)
- `SonarCloud` (`.github/workflows/sonar.yml`)
- `api-dependency-review` (`.github/workflows/dependency-review.yml`)

## What They Do
- `api-ci`
  - Triggers: pull requests and pushes to `main`
  - Provisions Postgres service
  - Runs: `./mvnw -B clean verify`
  - Uploads Surefire reports artifact
  - Uploads JaCoCo coverage artifacts (`target/site/jacoco`, `target/jacoco.exec`)
- `SonarCloud`
  - Triggers: pull requests and pushes to `main`
  - Runs: `./mvnw -B clean verify`
  - Executes SonarCloud scan
- `api-dependency-review`
  - Triggers: pull requests
  - Runs GitHub dependency review action

## Assurance Caveats from Workflow Design
- Coverage interpretation requires caution due to Sonar exclusions (`dialogue`, `speech`, `tts`).
- Dependency review exists, but this evidence pack does not include a live workflow run artifact from GitHub UI; it only evidences repository configuration and local command execution.
