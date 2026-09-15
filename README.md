# Points Mall Backend

## Profiles

- Production is the default profile. It requires every infrastructure credential from the environment and validates the existing database schema.
- Use `SPRING_PROFILES_ACTIVE=dev` only for local development. The development profile enables sample data and local service defaults.

## Production prerequisites

- Create a non-root database account and apply the schema before starting the application. Production uses `ddl-auto=validate` and never creates tables or sample administrators.
- To completely rebuild the database, first take and verify a backup, then run `sql/00_fresh_production_schema.sql`. This file intentionally drops the whole `points_mall` database and recreates an empty production schema; do not run it when existing business data must be retained. Incremental upgrade scripts are only for installations that retain their current database, and must never be run after the full rebuild script.
- Provide every value in `.env.production.example` through the deployment secret store. Generate a unique `JWT_SECRET` with at least 32 random bytes.
- Terminate TLS at the public gateway, and keep the backend reachable only from the gateway or private network.

## First production administrator

- Existing production administrators are preserved. The development-only `DataInitializer` never runs in production.
- For an empty production database only, set `BOOTSTRAP_SUPER_ADMIN_ENABLED=true` and provide the three `BOOTSTRAP_SUPER_ADMIN_*` values. The password must be 6-18 letters or digits.
- Start the service once, verify that the super administrator can log in, then remove all four bootstrap variables and restart. The password is stored only as a BCrypt hash; never insert a plaintext password directly into the database.

## Customer password and WeChat login

- New or reset customers receive a server-generated six-digit temporary password that is valid for seven days. It is returned to the administrator once and is never stored in plaintext.
- Customers must change the temporary password before using the mini-program. New passwords are 6-18 letters or digits, and the password change revokes the current session.
- WeChat OpenID login is implemented but disabled by default. Do not set `WECHAT_OPEN_ID_LOGIN_ENABLED=true` until the mini-program AppID/AppSecret and the HTTPS business domain are configured.
- The mini-program production API remains HTTP for now by request. Move it to HTTPS only after a domain and certificate have been configured in Tencent Cloud and the WeChat Mini Program console.

## Deployment security

- Follow [DEPLOYMENT_SECURITY.md](DEPLOYMENT_SECURITY.md) before exposing the service. It covers cloud firewall ports, Nginx verification, database permissions, backups, monitoring and the limits implemented by the application.
