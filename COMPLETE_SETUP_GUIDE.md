# Complete Setup Guide - Oracle Load Generators with New Relic

This is your complete step-by-step guide to set up and run extreme load testing on Oracle Database with New Relic APM monitoring.

**Total Time:** ~15 minutes

---

## 📋 Prerequisites

Before you begin, ensure you have:

- [ ] **Oracle Database** installed and running (11g, 12c, 18c, 19c, or 21c)
- [ ] **SYSDBA access** to Oracle (to create users and tables)
- [ ] **Java 11 or higher** installed
- [ ] **Maven 3.6+** installed
- [ ] **10GB+ free disk space** in Oracle tablespace
- [ ] **New Relic account** (free tier works) - https://newrelic.com/signup
- [ ] **New Relic license key** ready
- [ ] **New Relic Java agent** at `/Users/tbalanagu/Downloads/newrelic.jar`

**Verify your setup:**
```bash
# Check Java
java -version    # Should show 11 or higher

# Check Maven
mvn -version     # Should show 3.6 or higher

# Check Oracle is running
sqlplus sys/your_password@localhost:1521/ORCL as sysdba  # Should connect

# Check New Relic JAR exists
ls -lh /Users/tbalanagu/Downloads/newrelic.jar  # Should show 37MB file
```

---

## 📍 Part 1: Database Setup (5 minutes)

### Step 1.1: Connect to Oracle as SYSDBA

```bash
cd /Users/tbalanagu/Documents/load-generators/apm-load-generator
sqlplus sys/your_password@localhost:1521/ORCL as sysdba
```

Replace:
- `your_password` with your SYS password
- `localhost:1521/ORCL` with your Oracle connection string

### Step 1.2: Run the Setup Script

```sql
@oracle-setup.sql
```

**This creates:**
- ✅ 3 database users (oltp_user, analytics_user, otel_monitor)
- ✅ 11 tables with indexes and foreign keys
- ✅ 1,000 sample customers
- ✅ 500 sample products
- ✅ 500 inventory records

**Wait for completion** (takes 1-2 minutes). You should see:
```
User created.
Grant succeeded.
Table created.
...
1000 rows created.
Commit complete.
```

### Step 1.3: Verify Database Setup

```sql
-- Check users were created
SELECT username FROM dba_users
WHERE username IN ('OLTP_USER', 'ANALYTICS_USER', 'OTEL_MONITOR');

-- Should return 3 rows

-- Check tables for oltp_user
SELECT table_name FROM all_tables
WHERE owner = 'OLTP_USER'
ORDER BY table_name;

-- Should return 8 tables

-- Check sample data
SELECT COUNT(*) FROM oltp_user.CUSTOMERS;  -- Should return 1000
SELECT COUNT(*) FROM oltp_user.PRODUCTS;   -- Should return 500
SELECT COUNT(*) FROM oltp_user.INVENTORY;  -- Should return 500

-- Exit SQL*Plus
EXIT;
```

✅ **Database setup complete!**

---

## 🔨 Part 2: Build Applications (2 minutes)

### Step 2.1: Build Both Applications

```bash
cd /Users/tbalanagu/Documents/load-generators/apm-load-generator
./build-all.sh
```

**You should see:**
```
==========================================
Building Oracle Load Generator Applications
==========================================
Maven version: Apache Maven 3.x.x
Java version: 11.x.x

Building Application 1: OLTP Load Generator
--------------------------------------------
[INFO] BUILD SUCCESS
✓ OLTP Load Generator built successfully

Building Application 2: Analytics Load Generator
------------------------------------------------
[INFO] BUILD SUCCESS
✓ Analytics Load Generator built successfully

==========================================
Build Complete!
==========================================
```

### Step 2.2: Verify Build

```bash
# Check JAR files were created
ls -lh app1-oltp-load-generator/target/*.jar
ls -lh app2-analytics-load-generator/target/*.jar

# Should show two JAR files ~10-20MB each
```

✅ **Applications built successfully!**

---

## ⚙️ Part 3: Configure Applications (3 minutes)

### Step 3.1: Configure OLTP Application

Edit: `app1-oltp-load-generator/.env`

```bash
# Oracle Database Configuration
DB_URL=jdbc:oracle:thin:@localhost:1521:ORCL
DB_USERNAME=oltp_user
DB_PASSWORD=OltpPass123!

# Connection Pool Settings
DB_POOL_MAX=100
DB_POOL_MIN=20

# Application Settings
THREADS=100

# New Relic Configuration
NEW_RELIC_LICENSE_KEY=NRAK-YOUR_LICENSE_KEY_HERE
NEW_RELIC_APP_NAME=OLTP Load Generator
NEW_RELIC_LOG_LEVEL=info
NEW_RELIC_DISTRIBUTED_TRACING_ENABLED=true
```

**Update these values:**
1. `DB_URL` - Your Oracle connection string
2. `DB_USERNAME` - Keep as `oltp_user` (or change if you used different name)
3. `DB_PASSWORD` - Change if you set a different password in oracle-setup.sql
4. `NEW_RELIC_LICENSE_KEY` - Your New Relic license key (starts with NRAK-)
5. `THREADS` - Start with 100 for extreme load (or 25 for moderate)

### Step 3.2: Configure Analytics Application

Edit: `app2-analytics-load-generator/.env`

```bash
# Oracle Database Configuration
DB_URL=jdbc:oracle:thin:@localhost:1521:ORCL
DB_USERNAME=analytics_user
DB_PASSWORD=AnalyticsPass123!

# Connection Pool Settings
DB_POOL_MAX=50
DB_POOL_MIN=10

# Application Settings
THREADS=40

# New Relic Configuration
NEW_RELIC_LICENSE_KEY=NRAK-YOUR_LICENSE_KEY_HERE
NEW_RELIC_APP_NAME=Analytics Load Generator
NEW_RELIC_LOG_LEVEL=info
NEW_RELIC_DISTRIBUTED_TRACING_ENABLED=true
```

**Update these values:**
1. `DB_URL` - Same as OLTP (your Oracle connection string)
2. `DB_USERNAME` - Keep as `analytics_user`
3. `DB_PASSWORD` - Change if you set a different password
4. `NEW_RELIC_LICENSE_KEY` - Same license key as OLTP
5. `THREADS` - Start with 40 for heavy load (or 10 for moderate)

### Step 3.3: Get Your New Relic License Key

If you don't have your license key:

1. Go to https://one.newrelic.com/
2. Log in to your account
3. Click your name (top right) → **API Keys**
4. Find or create a **License Key** (starts with NRAK-)
5. Copy and paste it into both `.env` files

### Step 3.4: Connection String Formats

**For SID (most common):**
```bash
DB_URL=jdbc:oracle:thin:@hostname:port:SID
DB_URL=jdbc:oracle:thin:@localhost:1521:ORCL
```

**For Service Name:**
```bash
DB_URL=jdbc:oracle:thin:@//hostname:port/service_name
DB_URL=jdbc:oracle:thin:@//localhost:1521/ORCLPDB1
```

**For Remote Server:**
```bash
DB_URL=jdbc:oracle:thin:@192.168.1.100:1521:PROD
```

✅ **Configuration complete!**

---

## 🚀 Part 4: Run the Applications (1 minute)

### Step 4.1: Start OLTP Load Generator

**Terminal 1:**
```bash
cd /Users/tbalanagu/Documents/load-generators/apm-load-generator/app1-oltp-load-generator
./run.sh
```

**You should see:**
```
Loading configuration from .env file...
Found New Relic agent at: /Users/tbalanagu/Downloads/newrelic.jar
Running with New Relic Java Agent...
App Name: OLTP Load Generator

==========================================
Starting OLTP Load Generator
==========================================
Database: jdbc:oracle:thin:@localhost:1521:ORCL
Username: oltp_user
Threads: 100
Pool Max: 100
==========================================

2026-02-18 14:30:15.123 [main] INFO  OltpLoadGenerator - OLTP Load Generator initialized with 100 threads
2026-02-18 14:30:15.456 [main] INFO  DatabaseManager - Database connection pool initialized
2026-02-18 14:30:15.789 [pool-1-thread-1] INFO  OltpLoadGenerator - Worker thread 0 started
2026-02-18 14:30:15.790 [pool-1-thread-2] INFO  OltpLoadGenerator - Worker thread 1 started
...
2026-02-18 14:30:25.123 [main] INFO  OltpLoadGenerator - Active DB connections: 95
```

### Step 4.2: Start Analytics Load Generator

**Terminal 2:**
```bash
cd /Users/tbalanagu/Documents/load-generators/apm-load-generator/app2-analytics-load-generator
./run.sh
```

**You should see:**
```
Loading configuration from .env file...
Found New Relic agent at: /Users/tbalanagu/Downloads/newrelic.jar
Running with New Relic Java Agent...
App Name: Analytics Load Generator

==========================================
Starting Analytics Load Generator
==========================================
Database: jdbc:oracle:thin:@localhost:1521:ORCL
Username: analytics_user
Threads: 40
Pool Max: 50
==========================================

2026-02-18 14:31:00.123 [main] INFO  AnalyticsLoadGenerator - Analytics Load Generator initialized with 40 threads
2026-02-18 14:31:00.456 [main] INFO  DatabaseManager - Analytics database connection pool initialized
2026-02-18 14:31:00.789 [pool-1-thread-1] INFO  AnalyticsLoadGenerator - Analytics worker thread 0 started
...
```

✅ **Both applications running!**

**Expected Load:**
- **OLTP**: 10,000-20,000 operations/second
- **Analytics**: 500-1,000 complex queries/second
- **Combined**: Extreme database load!

---

## 🔍 Part 5: Verify Everything is Working (2 minutes)

### Step 5.1: Check Console Output

**In Terminal 1 (OLTP)**, you should see:
```
Active DB connections: 95
Worker thread 23 completed 15,234 operations
```

**In Terminal 2 (Analytics)**, you should see:
```
Active DB connections: 38
Analytics worker thread 5 completed 2,456 queries
```

### Step 5.2: Check Oracle Database

**Terminal 3:**
```bash
sqlplus sys/your_password@localhost:1521/ORCL as sysdba
```

```sql
-- Check active sessions
SELECT username, status, COUNT(*) as session_count
FROM v$session
WHERE username IN ('OLTP_USER', 'ANALYTICS_USER')
GROUP BY username, status;

-- Should show:
-- OLTP_USER      ACTIVE    90-100
-- ANALYTICS_USER ACTIVE    35-45

-- Check top SQL by executions
SELECT sql_id, executions, SUBSTR(sql_text, 1, 80) as sql_snippet
FROM v$sqlarea
WHERE parsing_schema_name IN ('OLTP_USER', 'ANALYTICS_USER')
ORDER BY executions DESC
FETCH FIRST 5 ROWS ONLY;

-- Should show high execution counts (thousands)

-- Check data is being created
SELECT 'ORDERS' as table_name, COUNT(*) as row_count FROM oltp_user.ORDERS
UNION ALL SELECT 'ORDER_ITEMS', COUNT(*) FROM oltp_user.ORDER_ITEMS
UNION ALL SELECT 'TRANSACTIONS', COUNT(*) FROM oltp_user.TRANSACTIONS;

-- Counts should be increasing rapidly

-- Check tablespace usage
SELECT
    tablespace_name,
    ROUND(used_space * 8192 / 1024 / 1024, 2) as used_mb,
    ROUND(tablespace_size * 8192 / 1024 / 1024, 2) as total_mb,
    ROUND(used_percent, 2) as used_pct
FROM dba_tablespace_usage_metrics
WHERE tablespace_name = 'USERS';

-- Monitor this to ensure you don't run out of space
```

### Step 5.3: Check New Relic (Wait 2-3 minutes)

1. Go to https://one.newrelic.com/
2. Click **APM & Services** in the left menu
3. You should see TWO applications:
   - **OLTP Load Generator**
   - **Analytics Load Generator**

**Click on "OLTP Load Generator":**
- **Throughput**: Should show 10,000-20,000 requests/minute
- **Response Time**: 1-50ms average
- **Error Rate**: <1%
- **Transactions**: Multiple transaction types (CreateOrder, UpdateCustomer, etc.)
- **Databases**: Should show Oracle database queries

**Click on "Analytics Load Generator":**
- **Throughput**: Should show 500-1,000 requests/minute
- **Response Time**: 50-500ms average (some queries 1-5 seconds)
- **Slow Queries**: Many expected (complex analytics)
- **Transactions**: Sales analytics, customer analytics, etc.

✅ **Everything is working!**

---

## 🎛️ Part 6: Adjusting Load Levels

### Reduce Load (If Database is Saturated)

**Stop applications:** Press `Ctrl+C` in both terminals

**Edit `.env` files to reduce threads:**

```bash
# OLTP - reduce from 100 to 50
THREADS=50

# Analytics - reduce from 40 to 20
THREADS=20
```

**Restart:**
```bash
./run.sh
```

### Increase Load (For More Stress)

**Option 1 - Increase threads in `.env`:**
```bash
# OLTP
THREADS=200

# Analytics
THREADS=80
```

**Option 2 - Override on command line:**
```bash
THREADS=150 ./run.sh
```

**Option 3 - Run multiple instances:**
```bash
# Terminal 1
cd app1-oltp-load-generator && THREADS=100 ./run.sh

# Terminal 2
cd app1-oltp-load-generator && THREADS=100 ./run.sh

# Terminal 3
cd app2-analytics-load-generator && THREADS=40 ./run.sh

# Terminal 4
cd app2-analytics-load-generator && THREADS=40 ./run.sh
```

---

## 🛑 Stopping the Applications

Press `Ctrl+C` in each terminal running the applications.

**You should see:**
```
Shutdown signal received, stopping load generator...
Worker thread 0 completed 45,678 operations
Worker thread 1 completed 46,123 operations
...
OLTP Load Generator shutdown complete
```

The applications will:
- ✅ Gracefully stop all worker threads
- ✅ Close database connections
- ✅ Log final statistics

---

## 📊 What You Should See

### Expected Metrics

**OLTP Application:**
- Operations/second: 10,000-20,000
- Database connections: 90-100 active
- Response time: 1-50ms
- Error rate: <1%
- CPU usage: 60-80%

**Analytics Application:**
- Queries/second: 500-1,000
- Database connections: 35-45 active
- Response time: 50-500ms (some 1-5 seconds)
- Error rate: <1%
- CPU usage: 40-60%

**Oracle Database:**
- Total sessions: 130-150 active
- CPU utilization: 70-90%
- Top wait events: CPU, log file sync, db file sequential read
- Redo generation: High
- Logical reads: Millions per second

**New Relic:**
- Transaction throughput: Thousands per minute
- Database queries: Hundreds of different SQL statements
- Slow queries: Many from Analytics app (expected!)
- Custom metrics: Active connections, etc.

---

## 🐛 Troubleshooting

### Issue: "newrelic.jar not found"

**Verify file exists:**
```bash
ls -lh /Users/tbalanagu/Downloads/newrelic.jar
```

**If missing:**
```bash
curl -O https://download.newrelic.com/newrelic/java-agent/newrelic-agent/current/newrelic-java.zip
unzip newrelic-java.zip
mv newrelic/newrelic.jar ~/Downloads/
```

### Issue: "Unable to get connection from pool"

**Cause:** Too many threads for available connections

**Fix:** Increase pool size in `.env`:
```bash
DB_POOL_MAX=200
```

Or reduce threads:
```bash
THREADS=50
```

### Issue: "ORA-01017: invalid username/password"

**Verify users exist:**
```sql
SELECT username FROM dba_users WHERE username IN ('OLTP_USER', 'ANALYTICS_USER');
```

**Check passwords match** what you set in `oracle-setup.sql` and `.env`

### Issue: "ORA-01653: unable to extend table"

**Cause:** Tablespace full

**Check space:**
```sql
SELECT tablespace_name, used_percent
FROM dba_tablespace_usage_metrics
WHERE tablespace_name = 'USERS';
```

**Fix:** Increase tablespace or run cleanup:
```sql
DELETE FROM oltp_user.ORDER_ITEMS WHERE order_id IN (
  SELECT order_id FROM oltp_user.ORDERS WHERE order_date < SYSDATE - 1
);
DELETE FROM oltp_user.TRANSACTIONS WHERE order_id IN (
  SELECT order_id FROM oltp_user.ORDERS WHERE order_date < SYSDATE - 1
);
DELETE FROM oltp_user.ORDERS WHERE order_date < SYSDATE - 1;
COMMIT;
```

### Issue: Database performance degrading

**Symptoms:** Response times increasing, queries taking longer

**Check for locks:**
```sql
SELECT s.username, s.sid, s.serial#, s.blocking_session
FROM v$session s
WHERE s.blocking_session IS NOT NULL;
```

**Check wait events:**
```sql
SELECT event, total_waits, time_waited
FROM v$system_event
WHERE wait_class != 'Idle'
ORDER BY time_waited DESC
FETCH FIRST 10 ROWS ONLY;
```

**Fix:** Reduce load (fewer threads) or tune database (add indexes, increase SGA)

### Issue: No data in New Relic after 5 minutes

**Check agent log:**
```bash
cat app1-oltp-load-generator/logs/newrelic_agent.log
```

**Common issues:**
- Invalid license key
- Firewall blocking outbound connections (port 443)
- Network connectivity problems

**Test connectivity:**
```bash
curl -I https://collector.newrelic.com
# Should return HTTP 200
```

### Issue: Java OutOfMemoryError

**Increase heap size:**
```bash
# Edit run.sh and change java command to:
java -Xmx4G -Xms2G -javaagent:"${NEWRELIC_JAR}" ...
```

---

## 🧹 Cleanup

### Option 1: Keep Database, Stop Applications

Press `Ctrl+C` in both terminals. Database data remains.

### Option 2: Clear Data, Keep Schema

```sql
TRUNCATE TABLE oltp_user.ORDER_ITEMS;
TRUNCATE TABLE oltp_user.TRANSACTIONS;
TRUNCATE TABLE oltp_user.ORDERS;
TRUNCATE TABLE oltp_user.AUDIT_LOG;
TRUNCATE TABLE oltp_user.SESSION_DATA;
COMMIT;
```

### Option 3: Remove Everything

```sql
-- Connect as SYSDBA
DROP USER oltp_user CASCADE;
DROP USER analytics_user CASCADE;
DROP USER otel_monitor CASCADE;
```

---

## 📚 Additional Documentation

- **[HIGH_LOAD_GUIDE.md](HIGH_LOAD_GUIDE.md)** - Detailed load characteristics, monitoring queries, tuning recommendations
- **[QUICK_START.md](QUICK_START.md)** - Condensed quick start guide
- **[NEWRELIC_QUICKSTART.md](NEWRELIC_QUICKSTART.md)** - New Relic specific setup
- **[ENV_SETUP.md](ENV_SETUP.md)** - Environment configuration details
- **[ORACLE_SETUP_SUMMARY.md](ORACLE_SETUP_SUMMARY.md)** - Database setup reference
- **[WHATS_CHANGED.md](WHATS_CHANGED.md)** - What makes this extreme load

---

## ✅ Success Checklist

- [x] Oracle database setup complete (3 users, 11 tables, sample data)
- [x] Applications built successfully (2 JAR files)
- [x] `.env` files configured (DB credentials + New Relic license key)
- [x] OLTP application running (10,000-20,000 ops/sec)
- [x] Analytics application running (500-1,000 queries/sec)
- [x] Oracle showing 130-150 active sessions
- [x] New Relic showing both applications with metrics
- [x] No errors in application logs
- [x] Tablespace not filling up (auto-cleanup working)

---

## 🎯 What's Next?

1. **Monitor for 15-30 minutes** - Let it generate significant metrics
2. **Check New Relic dashboards** - Explore transaction traces, slow queries, database performance
3. **Test your OTEL Oracle receiver** - Use the `otel_monitor` user to collect Oracle metrics
4. **Experiment with load levels** - Try different thread counts
5. **Run long-duration tests** - Test for several hours to find issues
6. **Break your database** - Find the maximum capacity!

---

## 💡 Tips

- **Start moderate** (50 OLTP threads, 20 Analytics) then increase
- **Monitor tablespace** - Don't run out of disk space
- **Watch for locks** - Long-running analytics queries may block OLTP
- **Check New Relic costs** - High throughput = more data
- **Use multiple instances** - For extreme load beyond single process limits
- **Tune your database** - This will expose bottlenecks in your Oracle configuration

---

## 🆘 Need Help?

If you encounter issues not covered here:

1. Check application logs: `app1-oltp-load-generator/logs/oltp-load-generator.log`
2. Check New Relic agent logs: `app1-oltp-load-generator/logs/newrelic_agent.log`
3. Check Oracle alert log: `$ORACLE_BASE/diag/rdbms/.../alert_*.log`
4. Review other documentation files in this directory

---

**You're all set! Start generating extreme load!** 🚀🔥
