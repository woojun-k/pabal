KNOWN_JWT_LOCAL_SECRET_PLACEHOLDER="0123456789"
KNOWN_JWT_LOCAL_SECRET_PLACEHOLDER="${KNOWN_JWT_LOCAL_SECRET_PLACEHOLDER}${KNOWN_JWT_LOCAL_SECRET_PLACEHOLDER}${KNOWN_JWT_LOCAL_SECRET_PLACEHOLDER}01"

ensure_jwt_local_secret() {
  local variable_name="$1"
  local env_file="$2"
  local current_value="${!variable_name:-}"

  if [[ -n "$current_value" && "$current_value" != "$KNOWN_JWT_LOCAL_SECRET_PLACEHOLDER" ]]; then
    return
  fi

  if command -v openssl >/dev/null 2>&1; then
    export "$variable_name=$(openssl rand -hex 32)"
    echo "Generated ephemeral $variable_name for this process. Persist a unique value in $env_file if tokens must survive restarts."
    return
  fi

  echo "OpenSSL not found; the application will generate an in-process ephemeral $variable_name." >&2
}
