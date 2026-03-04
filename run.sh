#!/bin/bash
# Load .env and run the app. Use this if .env isn't picked up automatically.
cd "$(dirname "$0")"
if [ -f .env ]; then
  set -a
  # Only export KEY=value lines (no spaces around =)
  while IFS= read -r line; do
    [[ "$line" =~ ^[A-Za-z_][A-Za-z0-9_]*= ]] && export "$line"
  done < <(grep -v '^\s*#' .env | grep -v '^\s*$')
  set +a
fi
exec ./mvnw spring-boot:run "$@"
