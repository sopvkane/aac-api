# Contributing

This repository is part of the AAC University Project and follows a lightweight professional workflow.

## Workflow
- Work is tracked using GitHub Issues with AAC ticket IDs (e.g., `AAC-05`).
- Each change is delivered via a Pull Request from an `AAC-xx` branch into `main`.
- CI must pass before merging.

## Branch naming
- `AAC-xx` (e.g., `AAC-02`)

## Commit messages
Use a conventional prefix with the ticket ID:
- `feat(AAC-xx): ...`
- `fix(AAC-xx): ...`
- `test(AAC-xx): ...`
- `ci(AAC-xx): ...`
- `docs(AAC-xx): ...`
- `chore(AAC-xx): ...`

Examples:
- `ci(AAC-02): add GitHub Actions workflows`
- `feat(AAC-05): add pinned phrases CRUD endpoints`

## Definition of Done
A ticket is considered complete when:
- Acceptance criteria are met
- Relevant tests are added/updated
- GitHub Actions checks pass
- PR description includes evidence and “How to test”
