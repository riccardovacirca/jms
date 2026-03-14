# Quickstart

Installation, build and benchmark guide for JMS.

## Prerequisites

- Docker installed and running
- Minimum 2GB free disk space
- Git

## Installation

### Clone repository

```bash
git clone https://github.com/your-org/jms.git myproject
cd myproject/
```

Project name (`myproject`) becomes:
- Container name
- Database name
- Database user

### Run installer

```bash
./install.sh
```

Creates:
- Docker container with Java 21, Maven, Node.js, PostgreSQL client, siege
- `config/application.properties` (substituted from `.env`)
- `gui/` directory with npm dependencies
- `cmd` tool registered globally in container

### Optional: Install PostgreSQL

```bash
./install.sh --postgres
```

Creates dedicated PostgreSQL container (`project-db`)

## Build

All commands run inside container via `docker exec`.

**Backend** (Java → `target/service.jar`):

```bash
docker exec -it myproject bash -c "cmd app build"
```

**Frontend** (Vite → `src/main/resources/static/`):

```bash
docker exec -it myproject bash -c "cmd gui build"
```

## Run

### Development mode (2 terminals)

**Terminal 1** - Backend with auto-reload:
```bash
docker exec -it myproject bash -c "cmd app run"
```

**Terminal 2** - Frontend dev server:
```bash
docker exec -it myproject bash -c "cmd gui run"
```

**Access:**
- Frontend: `http://localhost:2350` (VITE_PORT_HOST)
- API: `http://localhost:2310` (API_PORT_HOST)

### Production mode (single process)

```bash
docker exec -it myproject bash -c "cmd app build && cmd gui build && cmd app start"
```

Access: `http://localhost:2310`

## Benchmark

### Start application

```bash
docker exec -it myproject bash -c "cmd app start"
docker exec -it myproject bash -c "cmd app status"
```

### Run siege benchmark

**Basic** (10 users, 5 reps):
```bash
docker exec -it myproject bash -c "cmd bench -c 10 -r 5 http://localhost:8080/api/status"
```

**Stress test** (50 users, 30 seconds):
```bash
docker exec -it myproject bash -c "cmd bench -c 50 -t 30s http://localhost:8080/api/status"
```

**Load test with delays** (1-3s between requests):
```bash
docker exec -it myproject bash -c "cmd bench -c 10 -r 20 -d 1-3 http://localhost:8080/api/status"
```

**Multiple endpoints** (URLs file):
```bash
cat > urls.txt <<EOF
http://localhost:8080/api/status
http://localhost:8080/api/home/hello
EOF

docker cp urls.txt myproject:/workspace/
docker exec -it myproject bash -c "cmd bench -c 20 -r 10 -f urls.txt"
```

### View results

```bash
docker exec -it myproject bash -c "ls bench/"
docker exec -it myproject bash -c "cat bench/siege-*.log"
```

Expected output:
```
Transactions:                 50 hits
Availability:              100.00 %
Response time:              1.05 secs
Transaction rate:           9.56 trans/sec
Concurrency:               10.00
```

## Verify

**Backend API:**
```bash
curl http://localhost:2310/api/status
# Expected: {"err":false,"log":null,"out":"Application is running"}
```

**Frontend:**
```bash
curl -I http://localhost:2350/
# Expected: 200 OK (Vite dev server)

curl -I http://localhost:2310/
# Expected: 200 OK (production after gui build)
```

**Database:**
```bash
docker exec -it myproject bash -c "cmd db status"
# Expected: PostgreSQL connection OK, migrations applied
```

## Modules

**Install module:**

```bash
docker exec -it myproject bash -c "cmd module import modules/auth-1.0.0.tar.gz"
```

Follow printed README for manual steps (`pom.xml`, `App.java`, `config.js`)

**Available modules:**
- `auth-1.0.0.tar.gz`: authentication system
- `header-1.0.0.tar.gz`: navigation header
- `home-1.0.0.tar.gz`: home page
- `contatti-1.0.0.tar.gz`: contact management

## Development Workflow

**Enter container:**
```bash
docker exec -it myproject bash
```

**Inside container terminal 1:**
```bash
cmd app run
```

**Inside container terminal 2:**
```bash
cmd gui run
```

Edit files on host, changes auto-reload in container.

## Troubleshooting

**Container not found:**
```bash
docker ps -a | grep myproject
./install.sh
```

**Port in use:**
```bash
lsof -i :2310
lsof -i :2350
# Change API_PORT_HOST or VITE_PORT_HOST in .env, restart container
```

**`cmd` not found:**
```bash
docker exec myproject bash -c "which cmd"
./install.sh
```

**Database connection failed:**
```bash
./install.sh --postgres
docker exec -it myproject bash -c "cmd db setup"
```

**Rebuild container:**
```bash
docker stop myproject
docker rm myproject
./install.sh
```

## Production Build

```bash
./release.sh -v 1.0.0
```

Creates:
- Docker image: `myproject:1.0.0`
- Package: `dist/myproject-1.0.0.tar.gz`
