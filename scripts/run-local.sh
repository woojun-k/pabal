#!/usr/bin/env bash
set -euo pipefail

ENV_FILE=".env.local"

if [ ! -f "$ENV_FILE" ]; then
  echo "Missing $ENV_FILE"
  echo "Copy .env.example to $ENV_FILE and fill local values."
  exit 1
fi

set -a
source "$ENV_FILE"
set +a

./gradlew bootRun --args='--spring.profiles.active=local'