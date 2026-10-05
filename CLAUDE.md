# CLAUDE.md

Shared instructions for Claude Code in this repository: how the code is organized and the conventions to follow. The decisions behind them live in `docs/` (start with `docs/adr/0001-architecture-style-and-technology-stack.md` and `docs/README.md`). Personal preferences live in `CLAUDE.local.md`, which is not committed.

## Project

API Store: an e-commerce backend with REST and GraphQL, built as a modular monolith in Scala on a production-style stack.

## Working agreements

- Don't re-open decisions already recorded in an ADR. If something seems wrong, say so and suggest a new ADR.
- Plain, short wording in code comments, docs and commit messages.
- Explain the design reason, not just the syntax.
- When you show a small change, show only what changes, not the whole file.

## Stack

Scala 2.13.18 with `-Xsource:3` · Play 3.0.11 on Pekko · JDK 17 · sbt 1.13.0 · MySQL 8.4 (Docker) · Relate 5.1.0 over JDBC/HikariCP (no ORM) · Flyway (13.x, Flyway 14 needs Java 21) · circe + play-circe · Sangria · Guice · PureConfig · ScalaTest + scalatestplus-play + testcontainers-scala · sbt-scoverage · play-mailer + Twirl (Mailpit in dev) · Thumbnailator · Play cache (Caffeine) · Bucket4j · Argon2id (password4j) · jwt-scala · play-swagger.

A library is added to `build.sbt` only by the task that first needs it.

## Commands

```bash
docker compose up -d            # MySQL (3306) and Mailpit (1025, UI on 8025), bound to 127.0.0.1
./dev.sh run                    # loads .env into the shell, then sbt run
./dev.sh test                   # same, then sbt test (needs Docker: tests start a MySQL container)
./dev.sh "testOnly com.mendev.apistore.shared.config.AppConfigSpec"             # one spec
./dev.sh "testOnly com.mendev.apistore.shared.config.AppConfigSpec -- -z \"valid\""   # tests whose name contains "valid"
./dev.sh clean coverage test coverageReport   # coverage report, then run ./dev.sh clean
./dev.sh                        # interactive sbt shell with .env loaded
```

- `dev.sh` passes its arguments to sbt. It stops at once if `.env` is missing, so copy `.env.example` first.
- Coverage report: `target/scala-2.13/scoverage-report/index.html`. Always run `./dev.sh clean` afterwards so the next normal build has no coverage counters. Planned minimum: 50% statements, enforced with CI.
- Formatting: `.scalafmt.conf` exists (dialect scala213source3), but the sbt-scalafmt plugin is not installed yet, so `sbt scalafmtAll` does not work. Follow the existing style by hand.
- Play and sbt don't read `.env`. The app only reads environment variables, the same as in production. `dev.sh` exports them.
- `.env` is local and ignored. `.env.example` is committed with secrets blank. A new variable goes in both, and in `conf/application.conf` under `app`.
- In dev mode Play builds the app on the first request, so a config error shows then, not at startup.

## Layout

```
app/com/mendev/apistore/
  shared/          cross-cutting only: config, db (Flyway, TxRunner), error (AppError), web (ErrorResponse, JsonErrorHandler), health, filters, actions, Actor/Role
  identity/        bounded contexts, each with:
  catalog/           domain/          entities, value objects, rules (no Play, no SQL)
  shopping/          application/     use cases and ports (traits)
  ordering/          infrastructure/
  payments/            web/           thin REST controllers
  notifications/       persistence/   Relate repositories
  graphql/         one top-level adapter; may call any context's use cases
  controllers/     the first endpoint (HealthController); context controllers move to each context's web/
conf/              application.conf, routes, db/migration
test/com/mendev/apistore/   mirrors app/ (shared/db/TestDatabase is the shared test MySQL)
docs/              ADRs, C4 diagrams
```

All code is under the package prefix `com.mendev.apistore` (folder `app/com/mendev/apistore/`, tests mirror it under `test/`). Package names in this file are written without the prefix, for example `shared.config`. So far code exists only in `shared/` and `controllers/`; add folders as each task needs them.

## Architecture rules

- One-way dependencies between contexts, no cycles. Identity, catalog and payments depend on no other context. Shopping uses catalog. Ordering uses catalog, payments and shopping. Notifications only reacts to events; nothing depends on it.
- When a context needs something from one that already depends on it, it defines a small trait and the other implements it (`PendingOrderChecker` in catalog, implemented by ordering; `TokenVerifier` in shared, implemented by identity).
- Only IDs, plain values and read models cross a context boundary. Never another context's entity or repository.
- `shared` contains no domain concepts and depends on no context.
- No Play types in the application layer (use cases and ports). `TxRunner` uses only `java.sql.Connection`, `Future` and `Either`. Play's `Database` appears only in `PlayDbTxRunner` and `DbModule`.
- Controllers and resolvers are thin: parse, call a use case, map the result. No business rules in them.
- Authorization in two layers: action builders at the edge (401/403), and an explicit `Actor` in every use case that acts for a user (404 for records that aren't yours).
- Events go through a transactional outbox (`shared_event`): written in the same transaction as the change, delivered by a poller, at least once. Handlers must be idempotent.

## Scala conventions

- Braces style, Scala 2.13 with `-Xsource:3`. No Scala 3 `enum`: use `sealed trait` + `case object` / `case class` for statuses and errors.
- Expected failures are values: `Future[Either[AppError, A]]`. Throw only for bugs. `AppError` lives in `shared.error`; `ErrorResponse` turns it into a status and the one JSON error shape. Never send stack traces or exception messages to the client.
- Play 3 uses `jakarta.inject` (`@Inject`, `@Singleton`), not `javax.inject`.
- No custom annotations for behavior. Use composable Play actions.
- Nullable values are `Option`. Never `null`, never `.get` on `Option`.
- Money is `BigDecimal`, never `Double`. Rounding is explicit (half up, 2 decimals).
- Time is `java.time.Instant`, taken from an injected `java.time.Clock` (a fixed clock in tests). Never `Instant.now()` directly.
- Secrets use the `Secret` value class so they never print. Never log passwords, tokens or token hashes.
- Configuration: one root `AppConfig`, read once at startup by `ConfigModule`. Required variables use `${NAME}` (no `?`) so a missing one stops the app. Optional ones have a default (for example `DB_POOL_SIZE`).
- JDBC is blocking: run it through `TxRunner` on the dedicated `db-dispatcher` pool, never on Play's request threads. Repository methods take the connection explicitly. `TxRunner` rolls back on `Left` and on exception.
- Relate: `import com.lucidchart.relate._`, `sql"..."`, and pass the connection explicitly as the second parameter list, for example `.executeUpdate()(conn)`.

## Database conventions

- Tables: snake_case, singular, context prefix (`identity_user`). Columns snake_case, references `<thing>_id`, flags as facts (`is_disabled`).
- Keys: `id BIGINT UNSIGNED AUTO_INCREMENT`. Users and orders also have `public_id BINARY(16)` (UUID v7 from the app). Only `public_id` goes in URLs.
- Money `DECIMAL(12,2)`, percentages `DECIMAL(5,2)`. Time `DATETIME(6)` in UTC.
- `created_at` and `updated_at` on every table, set by the app. Append-only tables (history, `shared_event`) have only `created_at` (`shared_event` also has `processed_at`).
- Statuses: `VARCHAR(32)` with a `CHECK`, no MySQL `ENUM`.
- `NOT NULL` by default. `VARCHAR` always has an explicit length.
- Constraint names: `uk_`, `ix_`, `ck_` + `<table>_<cols>`.
- No foreign keys at all. Integrity comes from application rules and unique indexes. Add an index on every join and filter column.
- Soft delete only for products (`deleted_at`). Everything else is hard-deleted.
- Stock changes use one atomic statement (`... WHERE id = ? AND stock >= ?`) and check that exactly one row changed.
- Flyway: one flat folder `conf/db/migration`, one global version sequence, context in the file name (`V3__identity_create_user.sql`, `V1__shared_create_event.sql`). The runner (`FlywayMigrator`) starts with the app. Never edit a migration that has been applied; add a new one.
- Each context's tables are added in the phase that builds that context, not up front.

## Testing

- Test business rules, use cases and error paths. Simple wiring (modules, config, trivial controllers) doesn't need its own test. The project minimum is 50% statements (planned to be enforced in CI, see Commands).
- Unit tests: domain and use cases, with fake repositories (a small hand-made fake of a trait is fine) and a fixed `Clock`. Style: ScalaTest `AnyWordSpec` with `Matchers` (`should`).
- Integration tests: real MySQL through Testcontainers. One shared container for the whole test run, in `test/com/mendev/apistore/shared/db/TestDatabase`; it is migrated once, and each test cleans its own tables in `beforeEach`. It is never the development database.
- Controller tests: `PlaySpec` with `GuiceOneAppPerSuite` (`must`), building the application with the `TestDatabase` values. Override `app.database.*`, `db.default.*` and `app.mail.*`.
- The app fails at startup when MySQL is unreachable (Flyway runs eagerly), so the "database down" case is tested with a fake `TxRunner`.
- Name the spec to run and the result to expect before saying a change works.

## Git

- Branches: `feat/...`, `docs/...` or `chore/...`. `main` is protected and a local hook blocks commits on it.
- Commit subjects start with `feat:`, `docs:` or `chore:`, short and lowercase after the prefix. One pull request per goal: closely related tasks may share a pull request, each task as its own commit. Squash merge. Keep pull request descriptions short.

## Docs and ADRs

- ADRs use MADR with the headings of ADR 0001: Status, Date, Deciders; Context and problem statement; Decision drivers; Decision; Alternatives considered; Consequences.
- An accepted ADR is never rewritten. A changed decision gets a new ADR that supersedes it. Add every new ADR to the table in `docs/README.md`.
