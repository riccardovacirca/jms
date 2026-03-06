# Benchmarks

HTTP load testing with siege for JMS applications.

## Installation

Siege is pre-installed in the development Docker container.

**Verify installation:**

```bash
docker exec -it myproject bash -c "siege --version"
```

If missing, rebuild container:

```bash
docker stop myproject
docker rm myproject
./install.sh
```

Siege is installed via `Dockerfile.dev`:
- Package: `siege`
- Location: `/usr/bin/siege`
- Version: Latest available in Debian repositories

## Command Syntax

**Basic syntax:**

```bash
cmd bench [siege-options] <url>
```

All siege options are passed directly to the siege command.

**Common options:**

| Option | Description |
|--------|-------------|
| `-c N` | Concurrent users (default: 10) |
| `-r N` | Repetitions per user (number of times each user hits the target) |
| `-t Ns` | Time-based test (duration in seconds, e.g., `-t 30s`) |
| `-d N` | Delay between requests (seconds) |
| `-d N-M` | Random delay range (e.g., `-d 1-3` for 1-3 seconds) |
| `-f FILE` | URL list file (one URL per line) |
| `--content-type="TYPE"` | Set Content-Type header |
| `--header="NAME: VALUE"` | Add custom header |
| `-v` | Verbose output |
| `-q` | Quiet mode (no progress) |

**Full siege documentation:**

```bash
man siege
siege --help
```

## Log Files

**Location:**

```
bench/siege-YYYYMMDD-HHMMSS.log
```

**Format:** `YYYYMMDD-HHMMSS`
- `YYYY`: year (4 digits)
- `MM`: month (2 digits)
- `DD`: day (2 digits)
- `HH`: hour (24-hour format)
- `MM`: minute
- `SS`: second

**Example:**

```
bench/siege-20260306-143022.log
```

**Log directory creation:**

The `bench/` directory is created automatically on first benchmark run. Directory is ignored by git (not tracked in `.gitignore` explicitly but excluded by pattern).

**Log retention:**

Logs are never automatically deleted.

**Manual cleanup:**

```bash
rm bench/siege-*.log                           # Remove all logs
rm bench/siege-202603*.log                     # Remove March 2026 logs
find bench/ -name "*.log" -mtime +30 -delete   # Remove logs older than 30 days
```

## Example Usage

### 1. Basic test (10 concurrent users, 5 repetitions each)

```bash
docker exec -it myproject bash -c "cmd bench -c 10 -r 5 http://localhost:8080/api/status"
```

Total requests: 10 users × 5 reps = 50 hits

### 2. Stress test (50 concurrent users, 30 seconds duration)

```bash
docker exec -it myproject bash -c "cmd bench -c 50 -t 30s http://localhost:8080/api/status"
```

Runs for 30 seconds regardless of request count.

### 3. Load test with delays (10 users, 20 reps, 1-3s random delay)

```bash
docker exec -it myproject bash -c "cmd bench -c 10 -r 20 -d 1-3 http://localhost:8080/api/home/hello"
```

Simulates realistic user behavior with think time.

### 4. POST request with JSON payload

```bash
docker exec -it myproject bash -c 'cmd bench -c 5 -r 10 --content-type="application/json" --header="Content-Length: 45" "http://localhost:8080/api/auth/login POST {\"username\":\"admin\",\"password\":\"test\"}"'
```

### 5. Multiple endpoints from file

**Create URLs file:**

```bash
cat > urls.txt <<EOF
http://localhost:8080/api/status
http://localhost:8080/api/home/hello
http://localhost:8080/api/auth/session
EOF
```

**Copy to container:**

```bash
docker cp urls.txt myproject:/workspace/
```

**Run benchmark:**

```bash
docker exec -it myproject bash -c "cmd bench -c 20 -r 10 -f urls.txt"
```

Siege randomly selects URLs from the file for each request.

## Output Interpretation

**Typical siege output** (saved to log file):

```
Transactions:                 50 hits
Availability:              100.00 %
Elapsed time:                5.23 secs
Data transferred:            0.01 MB
Response time:               0.98 secs
Transaction rate:            9.56 trans/sec
Throughput:                  0.00 MB/sec
Concurrency:                 9.38
Successful transactions:     50
Failed transactions:         0
Longest transaction:         1.12
Shortest transaction:        0.87
```

**Key metrics:**

| Metric | Description |
|--------|-------------|
| **Transactions** | Total successful HTTP requests |
| **Availability** | Percentage of successful requests (200-299 status codes). **Target: 100.00%** |
| **Elapsed time** | Total duration of the test |
| **Response time** | Average time to receive a complete response. **Lower is better** |
| **Transaction rate** | Requests per second (throughput). **Higher is better** |
| **Concurrency** | Average number of simultaneous connections. Should be close to `-c` value under normal load. If significantly lower, server may be bottlenecked |
| **Failed transactions** | Requests that returned errors (4xx, 5xx) or timeouts. **Target: 0** |
| **Longest/Shortest transaction** | Min/max response times. Large difference indicates inconsistent performance |

## Viewing Results

**List all logs:**

```bash
docker exec -it myproject bash -c "ls -lh bench/"
```

**View latest log:**

```bash
docker exec -it myproject bash -c "cat bench/\$(ls -t bench/ | head -1)"
```

**View specific log:**

```bash
docker exec -it myproject bash -c "cat bench/siege-20260306-143022.log"
```

**Compare multiple logs:**

```bash
docker exec -it myproject bash -c "grep 'Transaction rate' bench/siege-*.log"
```

Example output:
```
bench/siege-20260306-143022.log:Transaction rate:            9.56 trans/sec
bench/siege-20260306-144501.log:Transaction rate:           15.32 trans/sec
bench/siege-20260306-145203.log:Transaction rate:           12.87 trans/sec
```

**Extract key metrics from all logs:**

```bash
docker exec -it myproject bash -c 'for f in bench/*.log; do echo "=== $f ==="; grep -E "(Transaction rate|Response time|Availability)" "$f"; done'
```

## Typical Workflow

### 1. Start application in background

```bash
docker exec -it myproject bash -c "cmd app build && cmd app start"
```

### 2. Verify application is running

```bash
docker exec -it myproject bash -c "cmd app status"
curl http://localhost:2310/api/status
```

### 3. Run baseline benchmark (light load)

```bash
docker exec -it myproject bash -c "cmd bench -c 10 -r 10 http://localhost:8080/api/status"
```

### 4. Run stress test (heavy load)

```bash
docker exec -it myproject bash -c "cmd bench -c 100 -t 30s http://localhost:8080/api/status"
```

### 5. Compare results

```bash
docker exec -it myproject bash -c "ls -lt bench/ | head -3"
docker exec -it myproject bash -c "cat bench/\$(ls -t bench/ | head -1)"
```

### 6. Test specific endpoint with realistic load

```bash
docker exec -it myproject bash -c "cmd bench -c 20 -r 50 -d 1-2 http://localhost:8080/api/home/hello"
```

## Benchmarking Async Handlers

JMS supports `@Async` annotation for non-blocking handlers.

**Example async handler:**

```java
@Async
public class SlowQueryHandler implements Handler {
  @Override
  public void get(HttpRequest req, HttpResponse res, DB db) throws Exception {
    // Simulates slow operation (1 second)
    List<HashMap<String, Object>> results = db.select(
      "SELECT pg_sleep(1), 'slow query' as message"
    );
    res.status(200).contentType("application/json")
       .err(false).log(null).out(results.get(0)).send();
  }
}
```

**Benchmark async vs sync:**

Async handler:
```bash
docker exec -it myproject bash -c "cmd bench -c 50 -r 10 http://localhost:8080/api/test/async-slow"
```

Sync handler (same logic, no `@Async`):
```bash
docker exec -it myproject bash -c "cmd bench -c 50 -r 10 http://localhost:8080/api/test/sync-slow"
```

**Expected results:**

| Type | Concurrency | Transaction rate |
|------|-------------|------------------|
| **Async** | ~48.50 | ~49.20 trans/sec |
| **Sync** | ~20.00 | ~19.80 trans/sec |

Async: Higher concurrency, better transaction rate
Sync: Lower concurrency (blocked threads), lower transaction rate

## Troubleshooting

### "siege not found"

**Solution:** Rebuild dev container:
```bash
docker stop myproject
docker rm myproject
./install.sh
```

### "Connection refused"

**Cause:** Application not running

**Solution:**
```bash
docker exec -it myproject bash -c "cmd app status"
docker exec -it myproject bash -c "cmd app start"
```

**Cause:** Wrong port in URL

Use internal port `8080` (not host-mapped port `2310`)
- ✅ Correct: `http://localhost:8080/api/status`
- ❌ Wrong: `http://localhost:2310/api/status`

### "socket: unable to connect sock.c:249: Connection timed out"

**Cause:** Server overloaded

**Solution:** Reduce concurrency:
```bash
cmd bench -c 10 -r 5 http://localhost:8080/api/status  # Lower -c value
```

Increase server resources in docker-compose or Dockerfile.

### "bench/ directory not found"

**Cause:** Directory created automatically on first run

**Solution:**
```bash
docker exec -it myproject bash -c "cmd bench -c 1 -r 1 http://localhost:8080/api/status"
```

**Verify creation:**
```bash
docker exec -it myproject bash -c "ls -la bench/"
```

### Low availability (<100%)

**Check application logs:**
```bash
docker exec -it myproject bash -c "tail -f logs/app.log"
```

**Verify endpoint manually:**
```bash
curl http://localhost:2310/api/status
```

**Check for errors in specific log:**
```bash
docker exec -it myproject bash -c "cat bench/siege-*.log | grep -i error"
```

## Best Practices

1. **Always start application before benchmarking:**
   `cmd app start && cmd app status`

2. **Use internal container port (8080) in URLs, not host-mapped port (2310)**

3. **Start with light load, increase gradually:**
   `-c 10 -r 5` → `-c 50 -r 10` → `-c 100 -t 30s`

4. **Use delays (`-d`) for realistic simulation:**
   `-d 1-3` (Random delay 1-3 seconds between requests)

5. **Test both async and sync endpoints to compare performance**

6. **Keep benchmark logs for performance regression tracking**

7. **Run benchmarks on dedicated test database to avoid production impact**

8. **Document baseline metrics for comparison after code changes**

9. **Use URL files (`-f`) for testing multiple endpoints with realistic distribution**

10. **Monitor server resources (CPU, memory) during benchmarks:**
    `docker stats myproject`
