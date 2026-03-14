#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ENV_FILE="$SCRIPT_DIR/.env"

# ---------------------------------------------------------------------------
# Load .env
# ---------------------------------------------------------------------------

[ -f "$ENV_FILE" ] || { echo "ERROR: .env not found in $SCRIPT_DIR" >&2; exit 1; }

set -a
. "$ENV_FILE"
set +a

PROJECT="${PROJECT_NAME:?PROJECT_NAME not set in .env}"

# ---------------------------------------------------------------------------
# Container list (only those that actually exist in Docker)
# ---------------------------------------------------------------------------

# Start order: dependencies first, dev container last
# Stop order: reverse
START_ORDER=()
STOP_ORDER=()

if [ "${PGSQL_ENABLED:-n}" = "y" ] && [ -n "$PGSQL_HOST" ]; then
    if docker ps -a --format '{{.Names}}' | grep -q "^${PGSQL_HOST}$"; then
        START_ORDER+=("$PGSQL_HOST")
    fi
fi

if [ -n "$MAILPIT_CONTAINER" ]; then
    if docker ps -a --format '{{.Names}}' | grep -q "^${MAILPIT_CONTAINER}$"; then
        START_ORDER+=("$MAILPIT_CONTAINER")
    fi
fi

if docker ps -a --format '{{.Names}}' | grep -q "^${PROJECT}$"; then
    START_ORDER+=("$PROJECT")
fi

if [ ${#START_ORDER[@]} -eq 0 ]; then
    echo "ERROR: no containers found for project '$PROJECT'" >&2
    exit 1
fi

# Stop order is the reverse of start order
for (( i=${#START_ORDER[@]}-1; i>=0; i-- )); do
    STOP_ORDER+=("${START_ORDER[$i]}")
done

# ---------------------------------------------------------------------------
# Actions
# ---------------------------------------------------------------------------

do_start() {
    echo "Starting project '$PROJECT'..."
    for c in "${START_ORDER[@]}"; do
        if [ "$(docker inspect -f '{{.State.Running}}' "$c" 2>/dev/null)" = "true" ]; then
            echo "  $c — already running"
        else
            echo "  $c — starting"
            docker start "$c"
        fi
    done
    echo "Done."
}

do_stop() {
    echo "Stopping project '$PROJECT'..."
    for c in "${STOP_ORDER[@]}"; do
        if [ "$(docker inspect -f '{{.State.Running}}' "$c" 2>/dev/null)" = "true" ]; then
            echo "  $c — stopping"
            docker stop "$c"
        else
            echo "  $c — already stopped"
        fi
    done
    echo "Done."
}

do_status() {
    echo "Project '$PROJECT' containers:"
    for c in "${START_ORDER[@]}"; do
        local state
        state=$(docker inspect -f '{{.State.Status}}' "$c" 2>/dev/null || echo "not found")
        printf "  %-30s %s\n" "$c" "$state"
    done
}

# ---------------------------------------------------------------------------
# CLI
# ---------------------------------------------------------------------------

usage() {
    echo "Usage: $0 --start | --stop | --restart | --status"
    exit 1
}

[ $# -eq 1 ] || usage

case "$1" in
    --start)   do_start ;;
    --stop)    do_stop ;;
    --restart) do_stop; do_start ;;
    --status)  do_status ;;
    *) usage ;;
esac
