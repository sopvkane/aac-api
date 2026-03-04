# AAC API (`aac-api`)

Java 17 + Spring Boot backend for the AAC platform.

This API powers the `aac-web` frontend and provides:
- authenticated caregiver/profile workflows
- phrase and preference management
- dialogue reply generation (semantic + optional LLM)
- speech token + TTS support
- wellbeing, interaction tracking, and caregiver analytics

## Architecture Overview

The codebase is organized by feature, with consistent layering:
- `controller`: HTTP endpoints and request/response handling
- `service`: business rules and orchestration
- `domain/repository`: JPA entities and persistence access
- `web`: API DTOs

Key domains:
- `auth`: account/session/pin-based authentication and role checks
- `dialogue`: intent detection and AAC reply generation
- `preferences`: user-specific communication options (food, drink, activity, people)
- `phrases`: custom AAC phrase CRUD
- `profile`: carer/user profile and settings
- `wellbeing`, `analytics`, `suggestions`: reporting and insights
- `speech`, `tts`: speech token and text-to-speech integration

## Dialogue System (Refactored Design)

Dialogue generation is split into focused components for maintainability:
- `DialogueService`: orchestration and mode selection
- `DialogueSemanticEngine`: deterministic intent-based reply routing
- `DialoguePreferenceReplyBuilder`: food/drink/activity/school preference replies
- `DialogueSocialReplyBuilder`: pets/family/teacher/social replies
- `DialogueReplySelection`: option extraction, fallback replies, normalization
- `DialogueReplyCommon` and `DialogueReplyConstants`: shared helpers/constants
- `FoundryAiReplyClient`: optional Azure/OpenAI-compatible JSON reply client

Request flow:
1. frontend calls `/api/dialogue/replies`
2. service loads profile + preference context
3. intent is detected from question text
4. semantic engine generates reliable replies and option groups
5. if configured (`LLM`/`HYBRID`), LLM generation can supplement/override
6. response is normalized to frontend-safe output (exactly 3 top replies)

This design keeps AAC behavior predictable even when LLM is unavailable.

## Core API Areas

Base examples (not exhaustive):
- `POST /api/dialogue/replies`
- `GET|POST|PUT|DELETE /api/phrases`
- `GET|POST|PUT|DELETE /api/carer/preferences`
- `GET /api/carer/preferences/who-to-ask`
- `POST /api/auth/login`, `/api/auth/register`, `/api/auth/logout`, `/api/auth/select-profile`
- `GET /api/auth/me`
- `POST /api/wellbeing/mood`, `POST /api/wellbeing/pain`
- `GET /api/carer/dashboard`
- `POST /api/interactions`
- `GET /api/speech/token`
- `POST /api/tts`
- `GET /api/healthz`

Swagger UI is available through springdoc when running locally.

## Tech Stack

- Java 17
- Spring Boot 4.0.2
- Spring Web MVC, Security, Validation, Actuator
- Spring Data JPA + Flyway
- PostgreSQL (runtime), H2 (tests)
- Testcontainers
- JaCoCo coverage checks (minimum 80% line coverage in `verify`)

## Local Development

### Prerequisites
- Java 17
- Docker + Docker Compose

### 1) Start Postgres

```bash
docker compose up -d
docker compose ps
docker exec -it aac-postgres pg_isready -U app -d aac
```

### 2) Configure environment

Copy `.env.sample` to `.env` and set values as needed.

Common variables:
- `DB_URL`, `DB_USER`, `DB_PASS`
- `TTS_PROVIDER` (`azure` or `elevenlabs`)
- `ELEVENLABS_API_KEY`, `ELEVENLABS_VOICE_ID` (if using ElevenLabs)
- `AZURE_SPEECH_KEY`, `AZURE_SPEECH_REGION`
- `AZURE_EXISTING_AIPROJECT_ENDPOINT`, `AZURE_OPENAI_API_KEY`, `AZURE_OPENAI_DEPLOYMENT` (for dialogue LLM client)

### 3) Run the API

```bash
./mvnw spring-boot:run
```

Or use:

```bash
./run.sh
```

App default: `http://localhost:8080`

### 4) Verify health

```bash
curl http://localhost:8080/api/healthz
curl http://localhost:8080/actuator/health
```

## Testing and Coverage

Run tests:

```bash
./mvnw test
```

Run full verification (includes JaCoCo coverage gate):

```bash
./mvnw verify
```

JaCoCo reports are generated under:
- `target/site/jacoco/index.html`

## Security Model (Current)

`SecurityConfig` uses stateless auth with cookie/session filter support.

Protected routes include:
- `/api/auth/me`
- `/api/carer/**`
- `/api/phrases/**`

Public routes include:
- `/api/auth/login`
- `/api/auth/register`
- `/api/auth/logout`
- health/info endpoints

## Database and Migrations

- Flyway migrations are under `src/main/resources/db/migration`
- Schema + seed evolution is versioned (`V2` ... `V21`)
- Default local DB is PostgreSQL (`aac`)

## Notes for `aac-web` Integration

- Dialogue responses always provide normalized top replies suitable for AAC tiles.
- Option groups are included for dynamic frontend rendering.
- Preference and phrase endpoints are stable and role-aware for caregiver flows.

## Contribution

See [CONTRIBUTING.md](CONTRIBUTING.md).
