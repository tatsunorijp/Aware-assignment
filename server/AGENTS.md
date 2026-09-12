# Server implementation instructions

Follow the [repository instructions](../AGENTS.md). This file owns backend coding,
maintenance and verification rules. It does not duplicate product or wire
requirements; locate those through the root
[documentation map](../README.md#documentation-map).

## Select context for the task

- Read [README.md](README.md), inspect the affected implementation/tests, and then
  load only the canonical domains affected by the change.
- Read [the protocol](../spec/protocol.md), relevant fixtures and
  [CLIENT_GUIDE.md](docs/CLIENT_GUIDE.md) for any wire or client-visible change.
  Read [acceptance criteria](../spec/acceptance-tests.md) for affected observable
  behavior and [CHANGELOG.md](CHANGELOG.md) when implementation, configuration or
  dependencies change.
- Do not use the former assignment draft as a requirement. When canonical sources
  conflict or omit a required decision, follow the root ambiguity workflow and
  wait for the developer before choosing behavior.

## Implementation boundaries

- Keep backend implementation in `app/`, tests in `tests/` and server tooling in
  `scripts/`. Shared specifications and fixtures remain at repository level.
- `app/main.py` owns lifecycle, routes and socket reader/writer tasks.
  `app/hub.py` owns messaging state transitions. `app/protocol.py` owns strict
  request models, parsing and normalization. `app/errors.py` owns typed server
  errors and correlation. `app/http_errors.py` owns HTTP error rendering and
  schema documentation. Preserve these boundaries unless an explicitly approved
  architecture change replaces them.
- Use the dedicated environment documented in README. Do not install into system
  Python or another project's environment. Keep `pyproject.toml` and
  `requirements.lock` consistent when dependencies change.
- Preserve the documented one-process, one-event-loop runtime. Keep hub state
  transitions free of network I/O, retain ordered per-socket output and coordinate
  task cleanup. Do not add blocking work to asynchronous paths.
- Keep transport validation, domain transitions and error rendering separated.
  Validate untrusted input before mutating state; bound diagnostics and avoid
  exposing sensitive input.
- Do not introduce deferred infrastructure or behavior as incidental refactoring.
  Keep absolute local paths, LAN addresses, credentials and machine-specific state
  out of committed defaults.

## Contract and compatibility

- Treat [spec/protocol.md](../spec/protocol.md) as the canonical wire contract.
  Do not reproduce its endpoint, event, ACK, ordering, retry or error rules here.
- Preserve compatibility unless the task explicitly authorizes a migration. For a
  client-visible change, update the protocol, relevant fixtures and acceptance
  criteria together, explain old/new behavior in CHANGELOG, and give concrete iOS
  and Android actions in CLIENT_GUIDE.
- An internal refactor must preserve observable behavior. Test the affected
  invariants rather than weakening the contract or editing a specification merely
  to make a test pass.
- Keep package/API release versions distinct from the wire `protocolVersion` and
  update each only when its own semantics require it.

## Proportional documentation

- Code and tests are sufficient documentation for straightforward internal work.
  Add concise comments only where a non-obvious invariant, concurrency constraint
  or design reason cannot be expressed clearly by names and structure.
- Update README only when setup, commands, architecture, configuration or durable
  operational limitations change. Do not narrate ordinary implementation steps.
- Update protocol/client guidance whenever consumers must behave differently;
  those contracts are not optional implementation notes.
- Create a separate explanatory document only for a large, durable structure that
  cannot be understood reasonably from code, tests, comments and existing owner
  documents. Link it from the documentation map or appropriate README instead of
  duplicating it in AGENTS.
- Record implementation, configuration and dependency changes under the Unreleased
  CHANGELOG section, including compatibility and required client/operator action.
  Documentation-only edits need no runtime change entry unless they correct a
  client-visible claim.

## Verification and handoff

Run affected checks from `server/` with its development environment active:

```sh
python -m pytest
ruff check app tests scripts
ruff format --check app tests scripts
```

- For behavior changes, add meaningful regression coverage and run the suite plus
  lint/format checks. Assert stable structured fields and behavior rather than
  illustrative prose.
- For dependency changes, also run `python -m pip check` and verify installation
  against declared constraints.
- For transport, lifecycle, launch/configuration or wire changes, start a temporary
  one-worker server and run
  `python scripts/smoke_test.py --base-url http://127.0.0.1:8000`. Stop only the
  process you started and do not run mutation-producing checks against an unknown
  server.
- Documentation-only changes require link/path/command consistency and
  `git diff --check`; backend tests are unnecessary unless the text introduces a
  new runtime claim.
- Report checks actually run, client impact and remaining limitations. Preserve
  unrelated changes and do not commit or push unless requested.

## Review focus

Flag changes that cross the module boundaries above, mutate state before
validation, alter a consumer contract without matching specifications/fixtures,
expose diagnostics, weaken task cleanup/order guarantees, or claim verification
without evidence.
