#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"

echo "== Health =="
curl -sS "$BASE_URL/actuator/health" | sed 's/.*/&\n/'

echo "== GET /api/phrases (empty) =="
curl -sS "$BASE_URL/api/phrases" | sed 's/.*/&\n/'

echo "== POST /api/phrases (valid) =="
CREATE_RES=$(curl -sS -i -X POST "$BASE_URL/api/phrases" \
  -H "Content-Type: application/json" \
  -d '{"text":"I need help"}')

echo "$CREATE_RES" | sed 's/.*/&\n/'

ID=$(echo "$CREATE_RES" | tr -d '\r' | sed -n 's/.*"id":"\([^"]*\)".*/\1/p' | head -n 1)
if [[ -z "${ID:-}" ]]; then
  echo "ERROR: could not extract id from response" >&2
  exit 1
fi
echo "Created id=$ID"

echo "== GET /api/phrases (now 1) =="
curl -sS "$BASE_URL/api/phrases" | sed 's/.*/&\n/'

echo "== POST /api/phrases (blank -> 400) =="
curl -sS -i -X POST "$BASE_URL/api/phrases" \
  -H "Content-Type: application/json" \
  -d '{"text":"   "}' | sed 's/.*/&\n/'

echo "== DELETE /api/phrases/{id} (204) =="
curl -sS -i -X DELETE "$BASE_URL/api/phrases/$ID" | sed 's/.*/&\n/'

echo "== DELETE /api/phrases/{missing} (404) =="
curl -sS -i -X DELETE "$BASE_URL/api/phrases/00000000-0000-0000-0000-000000000000" | sed 's/.*/&\n/'

echo "== Done =="
