# Documentation

Architecture documentation for API Store.

## Architecture decision records

Each ADR records one significant decision, why it was made, and what it costs. They use the MADR format and are numbered in the order they were written.

| ADR | Title | Status |
| --- | --- | --- |
| [0001](adr/0001-architecture-style-and-technology-stack.md) | Architecture style and technology stack | Accepted |
| [0002](adr/0002-product-pricing-embedded-discount-and-price-history.md) | Product pricing: embedded discount and price history | Accepted |
| [0003](adr/0003-compile-time-dependency-injection.md) | Compile-time dependency injection | Accepted |

### Planned ADRs

These decisions are summarized in ADR 0001 and will get their own record.

| Topic | What it will cover |
| --- | --- |
| Scala version and syntax | Scala 2.13 with `-Xsource:3`, braces, sealed traits instead of `enum`, and why not Scala 3. |
| Database access and transactions | Relate over JDBC and HikariCP, explicit connections, the `TxRunner`, and the dedicated thread pool. |
| Identity | In-process identity, the token strategy, and the fallbacks (Keycloak, Cognito). |
| Events and outbox | The transactional outbox, the poller, at-least-once delivery, and a later move to a broker. |
| Stock and payment consistency | Stock reservation, atomic stock updates, payment intents, and idempotent webhooks. |
| Order state machine | Order statuses, allowed transitions, who can trigger each one, and cancellation. |

## C4 model

| Level | Diagram |
| --- | --- |
| 1. System context | [c4/c4-level1-context.md](c4/c4-level1-context.md) |
| 2. Containers | Planned |

## Conventions

- A new ADR gets the next number and is never rewritten once accepted. A changed decision gets a new ADR that supersedes the old one.
- Diagram sources (Mermaid) are kept next to the rendered SVG so both are versioned together.
