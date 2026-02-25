#!/bin/bash
set -e

WORKSPACE="/workspace"

# Helper functions
error()   { printf 'ERROR: %s\n' "$1" >&2; exit 1; }
info()    { printf 'INFO: %s\n' "$1"; }
warn()    { printf 'WARNING: %s\n' "$1" >&2; }
success() { printf '✓ SUCCESS: %s\n' "$1"; }

confirm() {
    printf '%s [y/N]: ' "$1"
    read -r answer || true
    case "$answer" in
        [Yy]|[Yy][Ee][Ss]) return 0 ;;
        *) return 1 ;;
    esac
}

load_env() {
    if [ -f "$WORKSPACE/.env" ]; then
        set -a
        . "$WORKSPACE/.env"
        set +a
    fi
}

show_help() {
    cat << 'HELPEOF'
Usage: cmd COMMAND [OPTIONS]

  app build                        Compile Java backend (Maven)
  app run                          Watch src/, compile on change, hot-restart
  app debug                        Watch src/, compile on change, hot-restart WITH remote debug on :5005
  app start                        Start in background
  app stop                         Stop background process
  app restart                      Restart background process
  app status                       Show status

  gui build                        Build for production → src/main/resources/static/
  gui run                          Run Vite dev server (foreground, with API proxy)

  git push [-r <path>]             Stage, commit and push
  git pull [-r <path>]             Pull from remote
  git sync [-r <path>]             Pull + commit + push
  git fetch                        Initialize repo from remote (no .git required)
  git branch [-r <path>]           Show current branch name
  git branch -b <name> [-r <path>] [-f <base>]   Create branch from base (default: main)
  git merge -b <name> [-r <path>]  Merge branch to main and delete
  git update                       Update git config and remote URL from .env

  db                               Open interactive PostgreSQL CLI
  db -f <file>                     Execute SQL or CSV file
  db reset                         Reset database (drop and recreate)

  sync                             Sync using .sync config file in project root

  -h, --help                       Show this help
HELPEOF
}

# ============================================================================
# App Operations (Java Undertow)
# ============================================================================

APP_JAR="$WORKSPACE/target/service.jar"
APP_PID_FILE="/run/service.pid"
APP_LOG_FILE="/app/logs/service.log"

app_is_running() {
    if [ -f "$APP_PID_FILE" ]; then
        local PID
        PID=$(cat "$APP_PID_FILE")
        if kill -0 "$PID" 2>/dev/null; then
            return 0
        fi
        rm -f "$APP_PID_FILE"
    fi
    return 1
}

app_build() {
    load_env
    info "Compiling Java backend (Maven)..."
    cd "$WORKSPACE"
    mvn -q package -DskipTests
    success "Build completed → target/service.jar"
}

app_start() {
    load_env

    if app_is_running; then
        warn "Application is already running (PID: $(cat $APP_PID_FILE))"
        return 0
    fi

    [ -f "$APP_JAR" ] || error "JAR not found: $APP_JAR. Run 'cmd app build' first."

    info "Starting Java server in background..."
    cd "$WORKSPACE"
    nohup java -jar "$APP_JAR" > "$APP_LOG_FILE" 2>&1 &
    echo $! > "$APP_PID_FILE"

    sleep 1
    if app_is_running; then
        success "Application started (PID: $(cat $APP_PID_FILE))"
        info "Log: $APP_LOG_FILE"
        info "Internal: http://localhost:${API_PORT:-8080}/"
        info "External: http://localhost:${API_PORT_HOST:-2310}/"
    else
        error "Failed to start application. Check $APP_LOG_FILE"
    fi
}

app_stop() {
    if ! app_is_running; then
        info "Application is not running"
        return 0
    fi

    local PID
    PID=$(cat "$APP_PID_FILE")
    info "Stopping application (PID: $PID)..."

    kill "$PID" 2>/dev/null || true

    local COUNT=0
    while kill -0 "$PID" 2>/dev/null && [ $COUNT -lt 10 ]; do
        sleep 1
        COUNT=$((COUNT + 1))
    done

    if kill -0 "$PID" 2>/dev/null; then
        warn "Force killing application..."
        kill -9 "$PID" 2>/dev/null || true
    fi

    rm -f "$APP_PID_FILE"
    success "Application stopped"
}

app_restart() {
    app_stop
    sleep 1
    app_start
}

app_run() {
    _app_run_common ""
}

app_debug() {
    load_env
    local DEBUG_PORT="${DEBUG_PORT:-5005}"
    info "[debug] Remote debug enabled on port $DEBUG_PORT"
    info "[debug] Attach your debugger to localhost:$DEBUG_PORT"
    _app_run_common "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:$DEBUG_PORT"
}

_app_run_common() {
    local JAVA_DEBUG_OPTS="$1"
    load_env
    cd "$WORKSPACE"

    command -v inotifywait >/dev/null 2>&1 || \
        error "inotifywait not found. Install: apt-get install -y inotify-tools"

    local DEV_LOG="$APP_LOG_FILE"
    local DEV_CP_FILE="$WORKSPACE/.dev-classpath"
    local DEV_PID=""
    local TAIL_PID=""

    _dev_classpath() {
        info "[dev] Resolving dependency classpath..."
        mvn -q dependency:build-classpath -Dmdep.outputFile="$DEV_CP_FILE" \
            || error "[dev] Failed to resolve classpath"
        info "[dev] Classpath cached → .dev-classpath"
    }

    _dev_start() {
        printf '\n=== %s restart ===\n' "$(date '+%H:%M:%S')" >> "$DEV_LOG"
        local CP="$WORKSPACE/target/classes:$(cat "$DEV_CP_FILE")"
        java $JAVA_DEBUG_OPTS -cp "$CP" com.example.App >> "$DEV_LOG" 2>&1 &
        DEV_PID=$!
        echo "$DEV_PID" > "$APP_PID_FILE"
        sleep 1
        if kill -0 "$DEV_PID" 2>/dev/null; then
            success "[dev] App started (PID: $DEV_PID)"
        else
            warn "[dev] App failed to start — check $DEV_LOG"
        fi
    }

    _dev_stop() {
        if [ -n "$DEV_PID" ] && kill -0 "$DEV_PID" 2>/dev/null; then
            kill "$DEV_PID" 2>/dev/null || true
            local C=0
            while kill -0 "$DEV_PID" 2>/dev/null && [ $C -lt 10 ]; do
                sleep 0.5; C=$((C+1))
            done
            kill -9 "$DEV_PID" 2>/dev/null || true
            sleep 0.5  # attendi rilascio porta OS
        fi
        rm -f "$APP_PID_FILE"
        DEV_PID=""
    }

    _dev_cleanup() {
        _dev_stop
        [ -n "$TAIL_PID" ] && kill "$TAIL_PID" 2>/dev/null || true
        printf '\n'
        info "[dev] Dev mode stopped"
        exit 0
    }

    trap '_dev_cleanup' INT TERM

    # Classpath: resolve on first run or when pom.xml is newer
    if [ ! -f "$DEV_CP_FILE" ] || [ "$WORKSPACE/pom.xml" -nt "$DEV_CP_FILE" ]; then
        _dev_classpath
    fi

    info "[dev] Compiling..."
    mvn -q compile -DskipTests || error "Initial compilation failed"

    > "$DEV_LOG"
    tail -F "$DEV_LOG" &
    TAIL_PID=$!

    info "[dev] Watching src/ for changes (Ctrl+C to stop)"
    info "Internal: http://localhost:${API_PORT:-8080}/"
    info "External: http://localhost:${API_PORT_HOST:-2310}/"

    _dev_start

    while true; do
        inotifywait -r -q -e modify,close_write,create,delete,moved_to \
            --exclude '.*/resources/static(/.*)?$' \
            "$WORKSPACE/src/" "$WORKSPACE/pom.xml" 2>/dev/null || true
        sleep 0.3
        info "[dev] Change detected — recompiling..."
        if [ "$WORKSPACE/pom.xml" -nt "$DEV_CP_FILE" ]; then
            _dev_classpath
        fi
        if mvn -q compile -DskipTests; then
            _dev_stop
            _dev_start
        else
            warn "[dev] Compilation failed — keeping running version"
        fi
    done
}

app_status() {
    load_env
    echo "=== Java Undertow Application Status ==="

    if app_is_running; then
        local PID
        PID=$(cat "$APP_PID_FILE")
        echo "Status:   RUNNING"
        echo "PID:      $PID"
        echo "JAR:      $APP_JAR"
        echo "Log:      $APP_LOG_FILE"
        echo "Internal: http://localhost:${API_PORT:-8080}/"
        echo "External: http://localhost:${API_PORT_HOST:-2310}/"
        echo ""
        echo "Process info:"
        ps -p "$PID" -o pid,ppid,%cpu,%mem,etime,command 2>/dev/null || true
    else
        echo "Status: STOPPED"
        echo ""
        if [ -f "$APP_JAR" ]; then
            echo "JAR exists: YES ($APP_JAR)"
        else
            echo "JAR exists: NO (run 'cmd app build')"
        fi
    fi
}

# ============================================================================
# Vite Operations
# ============================================================================

VITE_DIR="$WORKSPACE/vite"

vite_build() {
    load_env

    [ -d "$VITE_DIR" ] || error "Vite directory not found: $VITE_DIR"

    info "Building Vite application..."
    cd "$VITE_DIR"
    npm run build
    success "Vite build completed → src/main/resources/static/"
}

vite_run() {
    load_env

    [ -d "$VITE_DIR" ] || error "Vite directory not found: $VITE_DIR"

    info "Starting Vite dev server (foreground)..."
    info "URL: http://localhost:${VITE_PORT:-5173}/"
    info "API proxy: /api → http://localhost:${API_PORT:-8080}"
    info "Press Ctrl+C to stop"
    cd "$VITE_DIR"
    npm run dev -- --host 0.0.0.0
}

# ============================================================================
# Git Operations
# ============================================================================

update_git_repo() {
    local REPO_DIR="$1"

    info "Updating repository in $REPO_DIR..."
    cd "$REPO_DIR"

    git config --global --add safe.directory "$REPO_DIR" 2>/dev/null || true
    git config user.name "$GIT_USER"
    git config user.email "$GIT_EMAIL"

    local CURRENT_REMOTE
    CURRENT_REMOTE=$(git config --get remote.origin.url 2>/dev/null || true)
    if [ -n "$CURRENT_REMOTE" ]; then
        local REPO_NAME
        REPO_NAME=$(basename -s .git "$CURRENT_REMOTE")
        git remote set-url origin "https://${GIT_USER}:${GIT_TOKEN}@github.com/${GIT_USER}/${REPO_NAME}.git"
    fi

    info "  Staging changes..."
    git add . || { warn "Failed to stage changes"; cd "$WORKSPACE"; return 1; }

    info "  Creating commit..."
    if git commit -m "updated"; then
        info "  Changes committed"
    else
        info "  No changes to commit"
        cd "$WORKSPACE"
        return 0
    fi

    info "  Pushing to remote..."
    if git push; then
        success "Successfully pushed"
    else
        warn "Failed to push changes. Check credentials and network."
        cd "$WORKSPACE"
        return 1
    fi

    cd "$WORKSPACE"
}

pull_git_repo() {
    local REPO_DIR="$1"

    info "Pulling repository in $REPO_DIR..."
    cd "$REPO_DIR"

    git config --global --add safe.directory "$REPO_DIR" 2>/dev/null || true
    git config user.name "$GIT_USER"
    git config user.email "$GIT_EMAIL"

    info "  Pulling from remote..."
    if git pull; then
        success "Successfully pulled"
    else
        warn "Failed to pull changes. Check credentials and network."
        cd "$WORKSPACE"
        return 1
    fi

    cd "$WORKSPACE"
}

_resolve_repo_dir() {
    local REPO_PATH="$1"
    if [ -z "$REPO_PATH" ]; then
        echo "$WORKSPACE"
    elif [ "${REPO_PATH#/}" != "$REPO_PATH" ]; then
        echo "$REPO_PATH"
    else
        echo "$WORKSPACE/$REPO_PATH"
    fi
}

_require_git_env() {
    [ -n "$GIT_USER" ]  || error "GIT_USER not set in .env file"
    [ -n "$GIT_EMAIL" ] || error "GIT_EMAIL not set in .env file"
    [ -n "$GIT_TOKEN" ] || error "GIT_TOKEN not set in .env file"
}

_get_repo_name() {
    local REPO_DIR="$1"
    local NAME
    NAME=$(basename -s .git "$(git -C "$REPO_DIR" config --get remote.origin.url 2>/dev/null)")
    [ -n "$NAME" ] || error "Cannot determine repository name. No remote.origin.url found in $REPO_DIR"
    echo "$NAME"
}

git_push() {
    local REPO_PATH="$1"
    load_env
    local GIT_REPO_DIR
    GIT_REPO_DIR=$(_resolve_repo_dir "$REPO_PATH")
    [ -d "$GIT_REPO_DIR/.git" ] || error "$GIT_REPO_DIR is not a git repository"
    git config --global --add safe.directory "$GIT_REPO_DIR" 2>/dev/null || true
    local REPO_NAME
    REPO_NAME=$(_get_repo_name "$GIT_REPO_DIR")
    _require_git_env
    confirm "Push changes to remote ($REPO_NAME at $GIT_REPO_DIR)?" || exit 0
    update_git_repo "$GIT_REPO_DIR"
}

git_pull() {
    local REPO_PATH="$1"
    load_env
    local GIT_REPO_DIR
    GIT_REPO_DIR=$(_resolve_repo_dir "$REPO_PATH")
    [ -d "$GIT_REPO_DIR/.git" ] || error "$GIT_REPO_DIR is not a git repository"
    git config --global --add safe.directory "$GIT_REPO_DIR" 2>/dev/null || true
    local REPO_NAME
    REPO_NAME=$(_get_repo_name "$GIT_REPO_DIR")
    _require_git_env
    confirm "Pull changes from remote ($REPO_NAME at $GIT_REPO_DIR)?" || exit 0
    pull_git_repo "$GIT_REPO_DIR"
}

git_sync() {
    local REPO_PATH="$1"
    load_env
    local GIT_REPO_DIR
    GIT_REPO_DIR=$(_resolve_repo_dir "$REPO_PATH")
    [ -d "$GIT_REPO_DIR/.git" ] || error "$GIT_REPO_DIR is not a git repository"
    git config --global --add safe.directory "$GIT_REPO_DIR" 2>/dev/null || true
    local REPO_NAME
    REPO_NAME=$(_get_repo_name "$GIT_REPO_DIR")
    _require_git_env
    confirm "Sync repository ($REPO_NAME at $GIT_REPO_DIR)?" || exit 0
    pull_git_repo "$GIT_REPO_DIR"
    update_git_repo "$GIT_REPO_DIR"
}

git_fetch() {
    load_env
    [ ! -d "$WORKSPACE/.git" ] || error "Git repository already exists. Remove .git folder first if you want to reinitialize."
    [ -n "$PROJECT_NAME" ] || error "PROJECT_NAME not set in .env file"
    _require_git_env

    local GIT_URL="https://${GIT_USER}:${GIT_TOKEN}@github.com/${GIT_USER}/${PROJECT_NAME}.git"
    info "Initializing repository from remote..."
    info "  Project: $PROJECT_NAME"
    info "  User: $GIT_USER"
    cd "$WORKSPACE"
    git init || error "Failed to initialize git repository"
    git remote add origin "$GIT_URL" || error "Failed to add remote origin"
    git fetch origin || error "Failed to fetch from origin"
    git reset --hard origin/main || error "Failed to reset to origin/main"
    git branch -m master main 2>/dev/null || true
    git branch -u origin/main || error "Failed to set upstream"
    success "Repository initialized from remote"
}

auto_stash_save() {
    local CURRENT_BRANCH="$1"
    if ! git diff-index --quiet HEAD -- 2>/dev/null; then
        info "  Saving uncommitted changes to stash..."
        git stash push -u -m "autostash-${CURRENT_BRANCH}" >/dev/null 2>&1 || { warn "Failed to stash changes"; return 1; }
        success "  Uncommitted changes stashed"
        return 0
    fi
    return 1
}

auto_stash_pop() {
    local TARGET_BRANCH="$1"
    local STASH_NAME="autostash-${TARGET_BRANCH}"
    if git stash list | grep -q "$STASH_NAME"; then
        local STASH_INDEX
        STASH_INDEX=$(git stash list | grep -n "$STASH_NAME" | head -1 | cut -d: -f1)
        STASH_INDEX=$((STASH_INDEX - 1))
        info "  Restoring stash '$STASH_NAME'..."
        git stash pop "stash@{$STASH_INDEX}" >/dev/null 2>&1 && success "  Uncommitted changes restored" || warn "Failed to restore stash"
    fi
}

git_branch() {
    local BRANCH_NAME="$1"
    local REPO_PATH="$2"
    local BASE_BRANCH="${3:-main}"
    load_env
    local GIT_REPO_DIR
    GIT_REPO_DIR=$(_resolve_repo_dir "$REPO_PATH")
    [ -d "$GIT_REPO_DIR/.git" ] || error "$GIT_REPO_DIR is not a git repository"

    if [ -z "$BRANCH_NAME" ]; then
        git -C "$GIT_REPO_DIR" branch --show-current
        return 0
    fi

    git config --global --add safe.directory "$GIT_REPO_DIR" 2>/dev/null || true
    local REPO_NAME
    REPO_NAME=$(_get_repo_name "$GIT_REPO_DIR")
    _require_git_env

    cd "$GIT_REPO_DIR"
    git config user.name "$GIT_USER"
    git config user.email "$GIT_EMAIL"

    local CURRENT_BRANCH
    CURRENT_BRANCH=$(git branch --show-current)
    local STASHED=false
    auto_stash_save "$CURRENT_BRANCH" && STASHED=true

    if git show-ref --verify --quiet "refs/heads/$BRANCH_NAME"; then
        info "  Branch '$BRANCH_NAME' exists locally, switching..."
        git switch "$BRANCH_NAME" || error "Failed to switch to branch"
        auto_stash_pop "$BRANCH_NAME"
        if ! git ls-remote --exit-code --heads origin "$BRANCH_NAME" >/dev/null 2>&1; then
            git push -u origin "$BRANCH_NAME" && success "Branch '$BRANCH_NAME' pushed to remote" || warn "Failed to push branch to remote"
        fi
        cd "$WORKSPACE"
        success "Switched to branch '$BRANCH_NAME' in $REPO_NAME"
        return 0
    fi

    if ! git show-ref --verify --quiet "refs/heads/$BASE_BRANCH"; then
        git ls-remote --exit-code --heads origin "$BASE_BRANCH" >/dev/null 2>&1 || error "Base branch '$BASE_BRANCH' does not exist"
        git fetch origin "$BASE_BRANCH:$BASE_BRANCH" || error "Failed to fetch base branch"
    fi

    git switch "$BASE_BRANCH" || error "Failed to switch to base branch"
    git pull origin "$BASE_BRANCH" || warn "Failed to pull from remote. Using local state."
    git switch -c "$BRANCH_NAME" || error "Failed to create branch '$BRANCH_NAME'"
    [ "$STASHED" = true ] && auto_stash_pop "$CURRENT_BRANCH"
    git push -u origin "$BRANCH_NAME" && success "Branch '$BRANCH_NAME' created and pushed" || warn "Branch created locally but failed to push"
    cd "$WORKSPACE"
}

git_merge() {
    local BRANCH_NAME="$1"
    local REPO_PATH="$2"
    load_env
    [ -n "$BRANCH_NAME" ] || error "Branch name is required. Use: cmd git merge -b <branch-name>"
    if [ "$BRANCH_NAME" = "main" ] || [ "$BRANCH_NAME" = "master" ]; then error "Cannot merge main/master into itself"; fi

    local GIT_REPO_DIR
    GIT_REPO_DIR=$(_resolve_repo_dir "$REPO_PATH")
    [ -d "$GIT_REPO_DIR/.git" ] || error "$GIT_REPO_DIR is not a git repository"
    git config --global --add safe.directory "$GIT_REPO_DIR" 2>/dev/null || true
    local REPO_NAME
    REPO_NAME=$(_get_repo_name "$GIT_REPO_DIR")
    _require_git_env
    confirm "Merge '$BRANCH_NAME' into main and delete it ($REPO_NAME)?" || exit 0

    cd "$GIT_REPO_DIR"
    git config user.name "$GIT_USER"
    git config user.email "$GIT_EMAIL"

    local MAIN_BRANCH="main"
    git show-ref --verify --quiet "refs/heads/master" && ! git show-ref --verify --quiet "refs/heads/main" && MAIN_BRANCH="master"

    git show-ref --verify --quiet "refs/heads/$BRANCH_NAME" || error "Branch '$BRANCH_NAME' does not exist locally"
    git switch "$MAIN_BRANCH" || error "Failed to switch to $MAIN_BRANCH"
    git merge "$BRANCH_NAME" --no-edit || error "Merge failed. Resolve conflicts manually."
    git branch -d "$BRANCH_NAME" && info "  Local branch deleted" || warn "Failed to delete local branch"
    if git ls-remote --exit-code --heads origin "$BRANCH_NAME" >/dev/null 2>&1; then
        git push origin --delete "$BRANCH_NAME" && success "Remote branch '$BRANCH_NAME' deleted" || warn "Failed to delete remote branch"
    fi
    cd "$WORKSPACE"
    info "Note: Use 'cmd git push' to push merged changes to remote"
}

git_update() {
    local REPO_PATH="$1"
    load_env
    [ -n "$GIT_USER" ]  || error "GIT_USER not set in .env file"
    [ -n "$GIT_TOKEN" ] || error "GIT_TOKEN not set in .env file"

    local GIT_REPO_DIR
    GIT_REPO_DIR=$(_resolve_repo_dir "$REPO_PATH")
    git config --global --add safe.directory "$GIT_REPO_DIR" 2>/dev/null || true
    git config --global user.name "$GIT_USER"
    [ -n "$GIT_EMAIL" ] && git config --global user.email "$GIT_EMAIL"

    if [ -d "$GIT_REPO_DIR/.git" ]; then
        local CURRENT_REMOTE REPO_NAME NEW_URL
        CURRENT_REMOTE=$(git -C "$GIT_REPO_DIR" config --get remote.origin.url 2>/dev/null || echo "")
        if [ -n "$CURRENT_REMOTE" ]; then
            REPO_NAME=$(basename -s .git "$CURRENT_REMOTE")
            NEW_URL="https://${GIT_USER}:${GIT_TOKEN}@github.com/${GIT_USER}/${REPO_NAME}.git"
            git -C "$GIT_REPO_DIR" remote set-url origin "$NEW_URL"
            success "Remote URL updated for $REPO_NAME"
        else
            warn "No remote origin found"
        fi
    else
        warn "Not a git repository, skipping remote URL update"
    fi

    success "Git configuration updated (user: $GIT_USER)"
}

# ============================================================================
# Database Operations (PostgreSQL)
# ============================================================================

db_cli() {
    load_env
    local FILE="$1"

    [ "$PGSQL_ENABLED" = "y" ] || error "PostgreSQL is not enabled. Set PGSQL_ENABLED=y in .env"

    export PGPASSWORD="$PGSQL_ROOT_PASSWORD"
    local PG_HOST="${PGSQL_HOST:-postgres}"
    local PG_PORT="${PGSQL_PORT:-5432}"

    if [ -n "$FILE" ]; then
        [ -f "$FILE" ] || error "File not found: $FILE"
        local FILE_EXT="${FILE##*.}"

        if [ "$FILE_EXT" = "csv" ]; then
            local TABLE_NAME
            TABLE_NAME=$(basename "$FILE" .csv)
            info "Loading CSV into table: $TABLE_NAME"
            psql -h "$PG_HOST" -p "$PG_PORT" -U "$PGSQL_ROOT_USER" -d "$PGSQL_NAME" \
                -c "\\COPY $TABLE_NAME FROM '$FILE' WITH (FORMAT CSV, HEADER true, DELIMITER ';')"
            success "CSV file loaded"
        elif [ "$FILE_EXT" = "sql" ]; then
            info "Executing SQL file: $FILE"
            psql -h "$PG_HOST" -p "$PG_PORT" -U "$PGSQL_ROOT_USER" -d "$PGSQL_NAME" -f "$FILE"
            success "SQL file executed"
        else
            error "Unsupported file type. Use .sql or .csv"
        fi
    else
        info "Connecting to PostgreSQL ($PGSQL_NAME)..."
        psql -h "$PG_HOST" -p "$PG_PORT" -U "$PGSQL_ROOT_USER" -d "$PGSQL_NAME"
    fi
}

db_reset() {
    load_env
    [ "$PGSQL_ENABLED" = "y" ] || error "PostgreSQL is not enabled. Set PGSQL_ENABLED=y in .env"

    warn "This will DELETE all data in $PGSQL_NAME and recreate it!"
    confirm "Are you sure you want to reset the database?" || exit 0

    local APP_WAS_RUNNING=false
    if app_is_running; then
        info "Stopping application..."
        app_stop
        APP_WAS_RUNNING=true
    fi

    export PGPASSWORD="$PGSQL_ROOT_PASSWORD"
    local PG_HOST="${PGSQL_HOST:-postgres}"
    local PG_PORT="${PGSQL_PORT:-5432}"

    info "Dropping database $PGSQL_NAME..."
    psql -h "$PG_HOST" -p "$PG_PORT" -U "$PGSQL_ROOT_USER" -d postgres \
        -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '$PGSQL_NAME' AND pid <> pg_backend_pid();" 2>/dev/null || true
    psql -h "$PG_HOST" -p "$PG_PORT" -U "$PGSQL_ROOT_USER" -d postgres \
        -c "DROP DATABASE IF EXISTS $PGSQL_NAME;" 2>/dev/null || true

    info "Creating database $PGSQL_NAME..."
    psql -h "$PG_HOST" -p "$PG_PORT" -U "$PGSQL_ROOT_USER" -d postgres \
        -c "CREATE DATABASE $PGSQL_NAME;" || error "Failed to create database"

    info "Granting privileges to $PGSQL_USER..."
    psql -h "$PG_HOST" -p "$PG_PORT" -U "$PGSQL_ROOT_USER" -d "$PGSQL_NAME" \
        -c "GRANT ALL ON SCHEMA public TO \"$PGSQL_USER\";" 2>/dev/null || true

    success "Database reset completed"

    if [ "$APP_WAS_RUNNING" = true ]; then
        info "Restarting application..."
        app_start
    fi
}

db_setup() {
    load_env
    [ "$PGSQL_ENABLED" = "y" ] || error "PostgreSQL is not enabled. Set PGSQL_ENABLED=y in .env"

    export PGPASSWORD="$PGSQL_ROOT_PASSWORD"
    local PG_HOST="${PGSQL_HOST:-postgres}"
    local PG_PORT="${PGSQL_PORT:-5432}"

    info "Creating role $PGSQL_USER..."
    psql -h "$PG_HOST" -p "$PG_PORT" -U "$PGSQL_ROOT_USER" -d postgres \
        -c "DO \$\$ BEGIN IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = '$PGSQL_USER') THEN CREATE ROLE \"$PGSQL_USER\" WITH LOGIN PASSWORD '$PGSQL_PASSWORD'; END IF; END \$\$;"

    info "Creating database $PGSQL_NAME..."
    psql -h "$PG_HOST" -p "$PG_PORT" -U "$PGSQL_ROOT_USER" -d postgres \
        -c "SELECT 1 FROM pg_database WHERE datname = '$PGSQL_NAME'" | grep -q 1 || \
    psql -h "$PG_HOST" -p "$PG_PORT" -U "$PGSQL_ROOT_USER" -d postgres \
        -c "CREATE DATABASE \"$PGSQL_NAME\" OWNER \"$PGSQL_USER\";"

    psql -h "$PG_HOST" -p "$PG_PORT" -U "$PGSQL_ROOT_USER" -d "$PGSQL_NAME" \
        -c "GRANT ALL ON SCHEMA public TO \"$PGSQL_USER\";"

    success "Database $PGSQL_NAME configured for user $PGSQL_USER"
}

# ============================================================================
# Sync Operations
# ============================================================================

sync_run() {
    local CONFIG_FILE="$WORKSPACE/.sync"
    [ -f "$CONFIG_FILE" ] || error "File .sync non trovato in $WORKSPACE"

    confirm "Avviare la sincronizzazione da $CONFIG_FILE?" || exit 0

    info "Starting sync from $CONFIG_FILE..."

    while IFS= read -r line || [ -n "$line" ]; do
        [ -z "$line" ] && continue
        case "$line" in \#*) continue ;; esac

        local left right orig dest tmp sed_expr
        left="${line% -> *}"
        right="${line#* -> }"

        orig="${left%% (*}"
        dest="${right%% (*}"

        [ -n "$orig" ] && [ -n "$dest" ] || continue

        sed_expr=""
        case "$right" in
            *"("*)
                tmp="${right##*(}"
                sed_expr="${tmp%)}"
                ;;
        esac

        if [ -f "$orig" ]; then
            rsync -av "$orig" "$dest" || { warn "  Failed: $orig -> $dest"; continue; }
        else
            rsync -av --delete "$orig/" "$dest/" || { warn "  Failed: $orig -> $dest"; continue; }
        fi
        success "  Synced: $orig -> $dest"

        if [ -n "$sed_expr" ]; then
            if [ -f "$orig" ]; then
                sed -i "$sed_expr" "$dest"
            else
                find "$dest" -type f -exec sed -i "$sed_expr" {} +
            fi
            success "  Applied substitutions to: $dest"
        fi

    done < "$CONFIG_FILE"

    success "Sync completed"
}

# ============================================================================
# Command parsing
# ============================================================================

case "$1" in
    app)
        case "$2" in
            build)   app_build ;;
            run)     app_run ;;
            debug)   app_debug ;;
            start)   app_start ;;
            stop)    app_stop ;;
            restart) app_restart ;;
            status)  app_status ;;
            *) error "Unknown app command: $2. Use: build, run, debug, start, stop, restart, status" ;;
        esac
        ;;
    gui)
        case "$2" in
            build) vite_build ;;
            run)   vite_run ;;
            *) error "Unknown gui command: $2. Use: build, run" ;;
        esac
        ;;
    git)
        GIT_CMD="$2"
        REPO_PATH=""
        BASE_BRANCH=""
        BRANCH_NAME=""
        shift 2

        while [ $# -gt 0 ]; do
            case "$1" in
                -r|--repo)   REPO_PATH="$2";   shift 2 ;;
                -b|--branch) BRANCH_NAME="$2"; shift 2 ;;
                -f|--from)   BASE_BRANCH="$2"; shift 2 ;;
                *) error "Unknown option: $1" ;;
            esac
        done

        case "$GIT_CMD" in
            push)   git_push "$REPO_PATH" ;;
            pull)   git_pull "$REPO_PATH" ;;
            sync)   git_sync "$REPO_PATH" ;;
            fetch)  git_fetch ;;
            branch) git_branch "$BRANCH_NAME" "$REPO_PATH" "$BASE_BRANCH" ;;
            merge)  git_merge "$BRANCH_NAME" "$REPO_PATH" ;;
            update) git_update "$REPO_PATH" ;;
            *) error "Unknown git command: $GIT_CMD. Use: push, pull, sync, fetch, branch, merge, update" ;;
        esac
        ;;
    db)
        shift 1
        if [ $# -eq 0 ]; then
            db_cli ""
        elif [ "$1" = "-f" ]; then
            [ -n "$2" ] || error "Option -f requires a file path"
            db_cli "$2"
        elif [ "$1" = "reset" ]; then
            db_reset
        elif [ "$1" = "setup" ]; then
            db_setup
        else
            error "Unknown db option: $1. Use: db, db -f <file>, db reset, db setup"
        fi
        ;;
    sync)
        sync_run
        ;;
    -h|--help|help|"")
        show_help
        ;;
    *)
        error "Unknown command: $1. Use 'cmd --help' for usage."
        ;;
esac
