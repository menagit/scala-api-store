# 0004. JSON library: play-json

- **Status:** Accepted
- **Date:** 2026-10-08
- **Deciders:** Erick
- **Supersedes:** the "JSON: circe with play-circe (S7)" row of ADR 0001

## Context and problem statement

ADR 0001 chose circe with play-circe for JSON, because it is widely used in production Scala and likely what the target stack uses. After the identity context, we know more about the cost:

- Play already ships play-json, so circe is a second JSON library on top of it.
- We need four extra dependencies: `play-circe`, `circe-core`, `circe-generic` and `jwt-circe`.
- The controllers need an extra trait (`with Circe`) to read and write JSON.
- The target stack uses play-json, so the code we write here does not transfer directly.

We need to decide on the JSON library before more contexts add more request and response types, while the change is still small.

## Decision drivers

- Simpler code: Play's own JSON support, with no adapter between Play and the library.
- Closer to the target stack.
- Fewer dependencies.
- No change in the shape of any request or response.
- A change small enough to do in one pull request.

## Decision

We use play-json for all JSON and remove circe.

- Requests and responses use play-json `Reads`, `Writes` and `Format`, written by hand or with its macros.
- Controllers use Play's own JSON body parsing, with no `with Circe`.
- JWT claims use the play-json flavor of jwt-scala.
- `play-circe`, `circe-core`, `circe-generic` and `jwt-circe` are removed from `build.sbt`.
- No request or response changes shape: same fields, same status codes, same error JSON.

## Alternatives considered

- **Keep circe.** It has strong typing and good error messages, and it is widely used. But it is a second JSON library next to the one Play ships, it needs an adapter and four dependencies, and it is not what the target stack uses.
- **Keep circe only for JWT, play-json for the rest.** This removes most of the adapter code, but the project would still carry two JSON libraries and four more dependencies for one class.

## Consequences

- One JSON library, the one Play already ships. `play-circe`, `circe-core`, `circe-generic` and `jwt-circe` leave `build.sbt`.
- `jwt-play-json` is added, replacing `jwt-circe`. It is the play-json flavor of the same JWT library, so tokens keep the same claims and format.
- Request validation keeps reporting all field errors together. A play-json `JsError` is mapped to our `FieldError` list, so the 400 response does not change.
- Sealed traits such as `Role` are written as plain strings, so their formats are written by hand. The macros would add a type tag and change the shape.
- The "JSON" row of ADR 0001 is superseded by this record. ADR 0001 itself is not edited.
