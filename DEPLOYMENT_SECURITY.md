# Production Deployment Security Checklist

This checklist assumes one Tencent Cloud server and an empty MySQL 8.0 database.

## 1. Recreate the database safely

1. Make one final backup of the old database before deleting anything:

   ```bash
   mysqldump --single-transaction --routines --triggers -u root -p points_mall > points_mall_before_reset.sql
   ```

2. Delete the old database only after confirming the backup file exists and is non-empty.
3. Create the new schema without demo data:

   ```bash
   mysql -u root -p < sql/00_fresh_production_schema.sql
   ```

4. Create a dedicated application account. Do not use MySQL `root` in `DB_USERNAME`:

   ```sql
   CREATE USER 'pointsmall_app'@'%' IDENTIFIED BY 'replace-with-a-long-random-password';
   GRANT SELECT, INSERT, UPDATE, DELETE ON points_mall.* TO 'pointsmall_app'@'%';
   FLUSH PRIVILEGES;
   ```

5. Configure `BOOTSTRAP_SUPER_ADMIN_ENABLED=true` and the three `BOOTSTRAP_SUPER_ADMIN_*` values for the first application start only. Verify login, remove those variables, then restart.

## 2. Network exposure

In the Tencent Cloud security group, allow only these public inbound ports:

- `80`: temporary HTTP only while the current mini-program API still uses HTTP.
- `443`: enable when the domain and certificate are ready.
- `22`: only from your own fixed public IP. Do not allow `0.0.0.0/0`.

Never expose `8080`, `3306`, or `6379` to the public internet. The backend, MySQL, and Redis must be reachable only through the local Docker/private network. In Docker Compose, publish only the Nginx `80`/`443` ports, not backend/database/Redis ports.

The bundled Nginx configuration overwrites `X-Forwarded-For` with the actual connecting IP before proxying to the backend. Do not change it back to `$proxy_add_x_forwarded_for` unless you also configure and trust a specific upstream load balancer; otherwise callers can forge an IP and evade backend rate limits.

Use SSH keys and disable password-based SSH login after confirming key login works. Keep the operating system and Docker images updated.

## 3. Limits and abuse protection

The repository now enforces these limits:

- Backend: Redis limits customer/WeChat login to 20 requests per IP per minute, admin login to 10, and refresh to 60. Excess requests return HTTP `429`.
- Backend: five wrong passwords lock that customer account for 15 minutes.
- Backend: request uploads are capped at 10 MB and only authenticated administrators can upload JPEG/PNG images.
- Nginx: per-IP connection cap, 10 MB request body limit, short header/body timeouts, login rate limit, and general API rate limit.
- Tomcat: maximum connections, request queue, worker threads, header size, and connection timeout are bounded in `application-prod.yml`.

After deploying the PC frontend container, validate its Nginx configuration before reloading:

```bash
nginx -t
```

Nginx and Redis limits reduce ordinary abuse. For public exposure to more hostile traffic, enable Tencent Cloud WAF/CC protection in front of the public domain. WAF is designed to protect against common web attacks including SQL injection, XSS, malicious uploads, and CC attacks: <https://cloud.tencent.com/product/waf>.

## 4. Injection and secret handling

- Application database access is JPA parameterized access; do not add string-concatenated native SQL.
- Keep all passwords, JWT secrets, mail passwords, and WeChat secrets in deployment environment variables or a secret store. Never put them in Git, the frontend, or a database seed SQL file.
- Do not log Authorization headers, passwords, temporary passwords, or JWT values. The operation record intentionally excludes them.
- Keep Swagger disabled in production, as configured by `application-prod.yml`.

## 5. Backup, monitoring, and recovery

- Back up MySQL every day and test restoring a backup before relying on it. If you use TencentDB for MySQL, enable automatic backups and retain enough data and binlog history for rollback. TencentDB supports automatic/manual backups and point-in-time recovery within retained data and log backups: <https://cloud.tencent.com/document/product/236/35172>.
- If MySQL is self-hosted on the CVM, schedule encrypted `mysqldump` backups to a separate COS bucket or a separate machine. A backup kept only on the same server does not protect against server loss.
- Set Tencent Cloud Monitor alarms for CPU, memory, disk usage, network traffic, process/container restart count, MySQL connection count, and Redis memory. Alert before disk usage reaches 80%.
- Rotate Nginx and application logs so a flood cannot fill the disk.

## 6. HTTPS remains a required follow-up

Do not enable HSTS yet because the project deliberately remains on HTTP until the domain, certificate, and mini-program business domain are ready. Once HTTPS is enabled, redirect all HTTP traffic to HTTPS, switch `api.config.js` to the HTTPS domain, configure the domain in the WeChat Mini Program console, and then enable OpenID login if desired.
