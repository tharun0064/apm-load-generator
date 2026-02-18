# Oracle Database APM Load Generator

Two Java applications designed to generate **EXTREME HEAVY LOAD** on Oracle Database for testing New Relic APM integration and OpenTelemetry Oracle receiver.

---

## 🚀 **[→ COMPLETE SETUP GUIDE ←](COMPLETE_SETUP_GUIDE.md)**

**Follow the [COMPLETE_SETUP_GUIDE.md](COMPLETE_SETUP_GUIDE.md) for complete step-by-step instructions (~15 minutes).**

---

## Quick Overview

### Application 1: OLTP Load Generator
- **10,000-40,000 operations/second**
- Massive INSERTs, UPDATEs, DELETEs
- 100 threads (default)

### Application 2: Analytics Load Generator
- **500-2,000 complex queries/second**
- Heavy multi-table joins, aggregations, full table scans
- 40 threads (default)

### Features
- ✅ Automatic data cleanup (no disk space issues!)
- ✅ New Relic Java agent included
- ✅ Complete database schema with sample data
- ✅ Easy .env configuration

---

## Project Structure

```
apm-load-generator/
├── COMPLETE_SETUP_GUIDE.md ⭐ READ THIS FIRST
├── README.md (this file)
├── oracle-setup.sql (database setup)
├── build-all.sh (build both apps)
│
├── app1-oltp-load-generator/
│   ├── newrelic.jar (included)
│   ├── newrelic.yml (edit license_key)
│   ├── .env (configure DB connection)
│   ├── run.sh (start app)
│   └── pom.xml
│
└── app2-analytics-load-generator/
    ├── newrelic.jar (included)
    ├── newrelic.yml (edit license_key)
    ├── .env (configure DB connection)
    ├── run.sh (start app)
    └── pom.xml
```

---

## Quick Start

```bash
# 1. Setup Oracle database
sqlplus sys/password@host:1521/ORCL as sysdba
@oracle-setup.sql

# 2. Build applications
./build-all.sh

# 3. Configure .env files
# Edit app1-oltp-load-generator/.env (DB + New Relic)
# Edit app2-analytics-load-generator/.env (DB + New Relic)

# 4. Run applications
cd app1-oltp-load-generator && ./run.sh
cd app2-analytics-load-generator && ./run.sh
```

**For detailed instructions, see [COMPLETE_SETUP_GUIDE.md](COMPLETE_SETUP_GUIDE.md)**

---

## What You'll Get

- **Oracle Database**: 130-150 active sessions
- **New Relic**: Thousands of transactions/minute with full DB visibility
- **Extreme Load**: 50,000+ combined database operations/second

---

**📖 See [COMPLETE_SETUP_GUIDE.md](COMPLETE_SETUP_GUIDE.md) for complete documentation!**
