# Database migrations

Flyway reads the SQL files in this folder.

## File name

`V<number>__<context>_<description>.sql`, for example `V3__identity_create_user.sql`.

- Two underscores after the number.
- The context is one of identity, catalog, shopping, ordering, payments or notifications. Use shared for cross-cutting tables, such as the outbox.
- The description is snake_case.

## Numbering

- One global sequence across all contexts. The next number is the highest existing number plus one.
- Never reuse a number.

## Rules

- One migration is one logical change. A table is created together with its indexes and constraints.
- Never edit a migration that has been applied. Add a new one.
- A context's tables are added in the phase that builds that context, not up front.
- Names follow the database conventions: snake_case, singular, context prefix, and constraint names starting with uk_, ix_ or ck_.