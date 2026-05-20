# Realtime-Db-Sync

## What This Does

This project show real time update in browser whenever data is been changed in database.

For eg, if someone update order status from "pending" to "shipped" in database,
every browser will automatically show that change without refreshing the page.

## Why I Built It This Way

The requirement was: **do not use polling**.

Polling means asking server/database frequenty about update by refreshing browser. That wastes resources.

Instead I used two things:

**1. PostgreSQL LISTEN/NOTIFY**
Built in feature in postgreSQL where you can set "trigger" on a table. What trigger does Whenever insertion, update, or delete function are been performed over database the trigger automatically sends a notification. I used this so the database itself tells Java when something changes.

**2. WebSocket**
This keep permanent connection between the browser and the server. so when Java receive notification from PostgreSQL, it immediately push it to browser. The browser does not ask for it.

## Technologies Used

- **Java + Spring Boot** — backend server
- **PostgreSQL** — database (LISTEN/NOTIFY feature)
- **WebSocket (STOMP)** — for pushing update to browser in real time
- **HTML + JavaScript** — for frontend dashboard

## How to Run

### Step 1 — Set up the database

Open PowerShell and connect to PostgreSQL:
```
psql -U postgres -p 5433
```

Create the database:
```sql
CREATE DATABASE ordersdb;
\c ordersdb
\i 'C:/Users/ragha/Downloads/files/schema.sql'
\q
```

### Step 2 — Run the app

```powershell
cd "C:\Users\ragha\Downloads\files"
mvn spring-boot:run
```

### Step 3 — Open the browser

Go to: `http://localhost:8080`

You will see a dashboard.

---

## How to Test :

- Use the Insert / Update / Delete on the page itself
- connect first
  psql -U postgres -p 5433 -d ordersdb
Once Done then Run any Query, the Update on Brower will be Instant

## Challenges I Faced

- **pg_notify + Java** — I learned that Java's JDBC connection need a `SELECT 1`
  keepalive query to receive buffered notification from PostgreSQL.
  Without it, notification just sit in a TCP buffer and never comes.

## What I Learned

- How WebSocket is different from HTTP request
- How real time system avoid polling using event driven design
