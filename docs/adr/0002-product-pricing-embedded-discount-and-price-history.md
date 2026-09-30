# 0002. Product pricing: embedded discount and price history

- **Status:** Accepted
- **Date:** 2026-09-30
- **Deciders:** Erick

## Context and problem statement

The catalog context owns product prices. Three behaviors depend on them:

- A manager can change a product's price, and every price change must be logged.
- An order total must include any discount on the products bought.
- A client who liked a product is emailed when that product gets a discount. The email shows the original price, the discounted price, and the savings percentage.

The specification leaves open how discounts are set and what "logged" means, so we decided: a discount is a percentage on the product, and price changes are stored in a history table (M4, M8).

We need a pricing model that:

- can be built and tested by one developer in a short time,
- keeps the price rules in one place, independent of HTTP, GraphQL, and the database,
- keeps money exact, following the database conventions (D1),
- leaves a durable record of every change to what clients pay,
- can grow into scheduled sales or promotions later with limited rework.

## Decision drivers

- Simplicity: build only what the specification needs.
- One place for the price rule, so the discounted price can never disagree with the discount percentage.
- A durable, queryable history that is written atomically with the change: no history row without a change, and no change without a history row.
- Exact money: `DECIMAL` columns and an explicit rounding rule.
- One email per real discount increase, not one per edit.
- Ability to evolve: a scheduled or multi-promotion model should be reachable by a migration, not a rewrite.

## Decision

### Discount

- A product has at most one discount, stored as `discount_percent DECIMAL(5,2) NOT NULL DEFAULT 0` on the catalog product table (M8). `0` means no discount. Valid values are 0 to 99.99, checked by the domain and by a `CHECK`, so a product can never be free.
- A discount has no start or end date. It stays until a manager changes or removes it through the normal product update, which only managers can call (A13).
- In Scala the discount is a small value object inside `Product`. It has no table of its own.

### Price calculation

- The discounted price is never stored. The domain calculates it in one place: `price × (100 − discount_percent) / 100`, rounded to 2 decimals, half up, using `BigDecimal`. It cannot disagree with the percentage.

### Price history

- Every change to a product's price or discount adds one row to a price history table, in the same transaction as the change (M4). The row holds the product, the old and new price, the old and new discount percent, the user who made the change, and the time. Rows are only added, never updated or deleted.
- The same change also writes one structured log line, for operations.

### Notifications

- Every change to the discount raises an event through the outbox (A8). The liked-product email is sent only when the new percentage is higher than the old one, for example from 0 to 25 or from 25 to 30. Lowering or removing a discount sends no email, and neither does a price change alone.

## Alternatives considered

- **A separate promotions table**, with a percentage, start and end dates, and several promotions per product. Deferred, not rejected. It needs overlap rules, prices that depend on the current time, a scheduled job that raises the event when a sale starts, and cache handling at the start and end times. That is a lot of work for a feature the specification does not ask for. Moving to it later is a migration: create the table, copy each current discount into it as a promotion with no end date, drop the column, and change the reads.
- **Start and end dates on the product**, for one scheduled discount per product. Rejected for now: it still needs the start-event job and the cache handling, without the flexibility of promotions.
- **A fixed sale price instead of a percentage.** Rejected: the email needs the savings percentage, and calculating it from two prices brings rounding differences.
- **Storing the discounted price next to the percentage.** Rejected: the two values could disagree. One calculation in the domain is enough.
- **A log line only for price changes.** Rejected: log files rotate and cannot be queried, so the history would be easy to lose.

## Consequences

**Positive**

- The price rule lives in one place, so the discounted price always matches the percentage.
- Reads stay simple: the current price needs no join and no time condition.
- Every change to what clients pay has a durable, queryable record, written in the same transaction as the change.
- A discount increase sends one email, not one per edit.
- The model can grow into scheduled promotions through a migration, as described above.

**Negative**

- There are no scheduled sales. A manager must remove a discount by hand, and a sale cannot start or end on its own.
- A product has one discount only. Coupon codes and several kinds of discount are not supported.
- Order items must copy the price charged and the discount at purchase time, so later changes never alter old orders.
- Sorting by price uses the discounted price, calculated in the SQL query, so a plain index on price cannot serve that sort.
- The price history table only grows. It needs no cleanup now, but it would need a retention rule at scale.
