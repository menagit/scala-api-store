# 0003. Compile-time dependency injection

- **Status:** Accepted
- **Date:** 2026-10-07
- **Deciders:** Erick
- **Supersedes:** the "Dependency injection: Guice (S4)" row of ADR 0001

## Context and problem statement

ADR 0001 chose Guice for dependency injection because it is Play's default. By the end of sign-in the project had:

- five Guice modules (config, database, Flyway, clock, identity),
- thirteen classes with `@Inject` or `@Singleton`,
- one object bound under two interfaces (`TokenIssuer` and `TokenVerifier`), which needed an extra binding.

Three problems showed up:

- A missing or wrong binding is found when the app starts, or on the first request in dev mode, not when the code compiles.
- The wiring is spread over modules, annotations and configuration. To know how one object is built, you read three places.
- Tests build the app with `GuiceApplicationBuilder`, so they depend on Guice behavior that production code only partly shares.

The project will grow to six contexts, each with its own classes. We need to decide how objects are built and connected before that growth, while a change is still cheap.

## Decision drivers

- Wiring mistakes should fail when the code compiles, not at startup.
- All the wiring for an area should be in one place.
- Classes are plain constructors, so tests create them with `new` and need no DI framework.
- Each context keeps its own wiring, so the contexts stay separate and the one-way dependency rule is visible.
- No extra library, and a change small enough to do in one pull request.
- Closer to the target stack.

## Decision

We use Play's compile-time dependency injection and remove Guice.

- `AppLoader` builds `AppComponents`, which extends Play's `BuiltInComponentsFromContext`.
- There is one components trait per area (`SharedComponents`, `IdentityComponents`, and one for each later context). `AppComponents` mixes them all in.
- Every object is a `lazy val` built with `new`: one instance, created on first use, in the right order.
- A trait reaches Play and `shared` through a self-type. When one context needs another, it declares an abstract member for the one interface it needs, never a self-type on the other context's trait.
- Classes carry no DI annotations.
- `AppComponents` owns the error handler, the router and the start of the Flyway migrations.

## Alternatives considered

- **Keep Guice.** It is Play's default and needs less typing, but it has the three problems above, and they grow with every context.
- **One single components class.** Simple today, but it grows into one large file that mixes every context, and the dependency rule between contexts is no longer visible.
- **MacWire.** It generates the `new` calls with macros, so there is less boilerplate. It adds a library and macro behavior to learn, and the explicit wiring is small enough for now. We can reconsider it when the wiring is large.
- **Compile-time DI with `jakarta.inject` annotations kept.** The classes would carry annotations that nothing reads.

## Consequences

- Wiring mistakes are compile errors. Tests build the app the same way production does (`OneAppPerSuiteWithComponents` with `AppComponents`).
- Classes are plain constructors, with no reflection and no annotations.
- A new class has to be added by hand to its context's trait, and a new controller to `new Routes(...)` in `AppComponents`, in the order of `conf/routes`. There is no automatic discovery.
- A cycle between `lazy val`s (A needs B and B needs A) fails at runtime on first use, not at compile time. The one-way dependency rule between contexts already forbids cycles.
- A library that only ships a Guice module needs its own components trait, or manual wiring. We check this when we add each library (mail, cache, swagger).
- The "Dependency injection" row of ADR 0001 is superseded by this record. ADR 0001 itself is not edited.
