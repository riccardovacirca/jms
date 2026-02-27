# QUICKSTART — JMS Installation & Benchmark

Quick guide for AI agents and developers to install JMS, build, and run benchmarks.

---

## Prerequisites

- Docker installed and running
- Minimum 2GB free disk space
- Git (to clone the repository)

---

## Installation

### 1. Clone Repository

Clone the JMS template with your project name:

```bash
git clone https://github.com/your-org/jms.git myproject
cd myproject/
```

**Note:** Replace `myproject` with your desired project name. This will be used as:
- Project directory name
- Docker container name
- Database name
- Database user

### 2. Run Installer

```bash
./install.sh
```

**Creates:**
- Docker container `myproject` with Java 21, Maven, Node.js, PostgreSQL client, siege
- Project structure: `src/`, `vite/`, `config/`, `logs/`, `bin/`
- Utility library (dev.jms.util) in `src/main/java/`
- Frontend base (router, stores) in `vite/src/`
- `cmd` tool in `bin/`

**Output:**
```
[✓] Creating Docker container...
[✓] Scaffolding project...
[✓] Installing npm dependencies...
Done
```

---

## Build

All commands run **inside the container** via `docker exec`:

### Backend

```bash
docker exec -it myproject bash -c "cmd app build"
```

Compiles Java → `target/service.jar`

### Frontend

```bash
docker exec -it myproject bash -c "cmd gui build"
```

Compiles Vite → `src/main/resources/static/`

---

## Run

### Development Mode (2 terminals)

**Terminal 1 — Backend:**
```bash
docker exec -it myproject bash -c "cmd app run"
# Auto-reloads on file changes
# Logs to stdout
```

**Terminal 2 — Frontend:**
```bash
docker exec -it myproject bash -c "cmd gui run"
# Dev server: http://localhost:2350 (VITE_PORT_HOST)
# Proxies /api → http://localhost:8080
```

**Access points:**
- Frontend: http://localhost:2350
- API: http://localhost:2310

### Production Mode (single process)

```bash
docker exec -it myproject bash -c "cmd app build && cmd gui build && cmd app start"
```

Access: http://localhost:2310

---

## Benchmark

### 1. Start Application

```bash
docker exec -it myproject bash -c "cmd app start"
docker exec -it myproject bash -c "cmd app status"  # Verify running
```

### 2. Run Siege Benchmark

**Basic test (10 users, 5 repetitions):**
```bash
docker exec -it myproject bash -c "cmd bench -c 10 -r 5 http://localhost:8080/api/test/slow"
```

**Stress test (50 users, 30 seconds):**
```bash
docker exec -it myproject bash -c "cmd bench -c 50 -t 30s http://localhost:8080/api/home/hello"
```

**Load test with delays (1-3 seconds between requests):**
```bash
docker exec -it myproject bash -c "cmd bench -c 10 -r 20 -d 1-3 http://localhost:8080/api/auth/session"
```

**Multiple endpoints (URLs file):**
```bash
# Create URLs file on host
cat > urls.txt <<EOF
http://localhost:8080/api/home/hello
http://localhost:8080/api/test/slow
EOF

# Copy to container and run benchmark
docker cp urls.txt myproject:/workspace/
docker exec -it myproject bash -c "cmd bench -c 20 -r 10 -f urls.txt"
```

### 3. View Results

```bash
docker exec -it myproject bash -c "ls bench/"
# Lists: bench/siege-20260227-163045.log

docker exec -it myproject bash -c "cat bench/siege-*.log"
# Shows: transactions, response time, throughput, etc.
```

**Expected output:**
```
Transactions:                 50 hits
Availability:              100.00 %
Response time:              1.05 secs
Transaction rate:           9.56 trans/sec
Concurrency:               10.00
```

---

## Verify

### Backend API

```bash
curl http://localhost:2310/api/home/hello
# Expected: {"err":false,"log":null,"out":"Hello World"}
```

### Async Handler

```bash
time curl http://localhost:2310/api/test/slow
# Expected: ~1 second (pg_sleep simulation)
```

### Frontend

```bash
curl -I http://localhost:2350/
# Expected: 200 OK (Vite dev server)

curl -I http://localhost:2310/
# Expected: 200 OK (production, after gui build)
```

### Database

```bash
docker exec -it myproject bash -c "cmd db status"
# Expected: PostgreSQL connection OK, migrations applied
```

---

## Troubleshooting

**Container not found:**
```bash
docker ps -a | grep myproject
./install.sh  # Re-run installer
```

**Port in use:**
```bash
lsof -i :2310  # API port
lsof -i :2350  # Vite port
# Change API_PORT_HOST or VITE_PORT_HOST in .env, then restart container
```

**`cmd` not found:**
```bash
docker exec myproject bash -c "which cmd"
# Expected: /workspace/bin/cmd
./install.sh  # Re-run to recreate symlink
```

**Database connection failed:**
```bash
./install.sh --postgres           # Install PostgreSQL container
docker exec -it myproject bash -c "cmd db setup"  # Create database
```

**Rebuild container:**
```bash
docker stop myproject
docker rm myproject
./install.sh  # Recreates container preserving ./src, ./vite, ./config
```

---

## Next Steps

### Install Modules

```bash
docker exec -it myproject bash -c "cmd module import modules/auth-1.0.0.tar.gz"
docker exec -it myproject bash -c "cmd module import modules/home-1.0.0.tar.gz"
# Follow modules/*/README.md for manual integration
```

### Configure Database

```bash
./install.sh --postgres  # Install PostgreSQL container
docker exec -it myproject bash -c "cmd db setup"  # Create user and database
```

### Add Custom Handler

**1. Create handler (in `src/main/java/<groupId>/handler/MyHandler.java`):**
```java
package com.example.handler;

import dev.jms.util.*;

@Async  // Optional: non-blocking execution
public class MyHandler implements Handler {
  @Override
  public void get(HttpRequest req, HttpResponse res, DB db) throws Exception {
    res.status(200).contentType("application/json")
       .err(false).log(null).out("Hello from MyHandler").send();
  }
}
```

**2. Register in `App.java`:**
```java
import com.example.handler.MyHandler;
// ...
.add("/api/my", route(new MyHandler(), ds))
```

**3. Rebuild and test:**
```bash
docker exec -it myproject bash -c "cmd app build && cmd app start"
curl http://localhost:2310/api/my
```

### Add Frontend Module

**1. Create module directory:**
```bash
docker exec -it myproject bash -c "mkdir -p vite/src/modules/mymodule"
```

**2. Create `vite/src/modules/mymodule/index.js`:**
```javascript
export default {
  mount(container) {
    container.innerHTML = `<h1>My Module</h1>`;
  }
};
```

**3. Register in `vite/src/modules.config.js`:**
```javascript
export const MODULE_CONFIG = {
  mymodule: {
    context: 'public',
    path: '/mymodule',
    title: 'My Module'
  }
};
```

**4. Rebuild and access:**
```bash
docker exec -it myproject bash -c "cmd gui build"
# Access: http://localhost:2350/#mymodule
```

### Production Build

```bash
./release.sh -v 1.0.0
# Creates:
# - Docker image: myproject:1.0.0
# - Package: dist/myproject-1.0.0.tar.gz
```

---

## Complete Workflow Example

```bash
# 1. Clone and install (3-5 min)
git clone https://github.com/your-org/jms.git myproject
cd myproject/
./install.sh

# 2. Build backend and frontend (~1 min)
docker exec -it myproject bash -c "cmd app build && cmd gui build"

# 3. Start application
docker exec -it myproject bash -c "cmd app start"

# 4. Verify
curl http://localhost:2310/api/home/hello

# 5. Benchmark async handler
docker exec -it myproject bash -c "cmd bench -c 10 -r 5 http://localhost:8080/api/test/slow"

# 6. View benchmark results
docker exec -it myproject bash -c "cat bench/siege-*.log"
```

**Total time:** ~5 minutes (first install) | 30 seconds (restart)

---

## Interactive Development

For active development, work **inside the container** with auto-reload:

```bash
# Enter container
docker exec -it myproject bash

# Terminal 1 (inside container): Backend with auto-reload
cmd app run

# Terminal 2 (inside container): Frontend dev server
cmd gui run

# Access:
# - http://localhost:2350 (Vite dev server with HMR)
# - http://localhost:2310 (API)
```

Files are bind-mounted from host → container, so you can edit on the host and see changes automatically.

---

End of Quickstart
