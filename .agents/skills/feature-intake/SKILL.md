---
name: feature-intake
description: Clarify and route requirements for a new or materially changed Aware feature before implementation, especially when product, UX, persistence, protocol, acceptance, or platform decisions may be incomplete.
---

# Feature intake

Turn a feature idea into explicit, appropriately located decisions without making
the developer answer everything at once or creating unnecessary permanent
documentation.

## Start with discovery

1. Read the repository `AGENTS.md` and the documentation map in `README.md`.
2. Inspect the user's request, affected implementation and tests. Identify likely
   domains before opening specifications.
3. Read only the canonical documents for those domains and their relevant direct
   references. Do not read the entire repository by default.
4. Separate confirmed requirements from implementation facts, suggestions and
   unresolved product decisions. Existing code can reveal current behavior but
   cannot invent a requirement missing from canonical sources.

## Route information to its owner

Use these responsibilities when a confirmed decision needs documentation:

| Decision | Maintained owner |
| --- | --- |
| Product purpose, user value, scope or product boundary | `spec/product.md` |
| Cross-client flow, state dimension or responsibility boundary | `SYSTEM_DESIGN.md` |
| Visual behavior, interaction, accessibility or prototype | Relevant file under `spec/design/` |
| HTTP/WebSocket payload, event, validation, compatibility, ACK or retry contract | `spec/protocol.md` |
| Local model, durability, transaction, observation or synchronization invariant | `spec/persistence.md` |
| Observable success, failure, offline or interoperability outcome | `spec/acceptance-tests.md` |
| Durable platform setup, structure, architecture, limitation or verification fact | Relevant client/server `README.md` |
| Reusable implementation rule for future work | Narrowest applicable `AGENTS.md` |

A decision may affect multiple owners. Update each owner only for its own
responsibility and link across documents instead of copying the same explanation.

## Build the question set

Ask only questions whose answers can materially change behavior, data, contracts,
acceptance or implementation boundaries. Consider, when relevant:

- Goal, actor, user value, entry point and explicit out-of-scope behavior.
- Happy path, empty/loading/error/offline states, cancellation and recovery.
- Data ownership, identity, persistence lifetime, ordering and migration.
- Server operations, payloads, compatibility, errors and retry/idempotency rules.
- Navigation, visual reference, interaction and accessibility expectations.
- Platform parity versus intentional platform-specific behavior.
- Acceptance examples, edge cases and evidence required to call the work complete.

Do not ask the developer to repeat an answer already established by a canonical
source. Do not turn optional implementation choices into product questions when
the codebase already provides a safe, conventional answer.

## Present all questions, then ask one at a time

In the first intake response, present:

1. A short statement of the understood feature and known requirements.
2. The affected canonical documents and why each may need a change.
3. A numbered overview of **all currently known open questions**. For every
   question, state why the answer matters, its likely owner document and whether it
   blocks implementation or can be deferred.
4. Any unaffected domains that do not need to be loaded or changed.
5. Only after the overview, ask `Question 1 of N` and wait.

Do not request answers to the entire list at once. On each following turn:

- Confirm the accepted answer in one concise sentence.
- Update the question count if the answer resolves or introduces a dependency.
- Ask exactly one next unresolved question and wait.
- If the developer wants to pause, return a resumable checkpoint containing the
  confirmed decisions and remaining numbered questions. Do not create a draft file
  unless the developer explicitly asks to persist that checkpoint.

If a new question appears because of an answer, add it to the visible list and
explain the dependency. If the developer defers a blocking question, stop
decision-dependent documentation and implementation; continue only independent
work that cannot prejudge it.

## Resolve ambiguity explicitly

When the canonical sources do not determine a necessary behavior:

1. Report the exact ambiguity and the alternatives supported by current evidence.
2. Identify the canonical document that should own the eventual answer.
3. Search that document, its relevant direct references, code and tests once more.
4. If the answer is still absent, ask the developer the next focused question and
   wait. Never silently choose a product behavior.

## Continue after intake

When every blocking question is answered:

1. Present a compact decision summary and the files that should change.
2. Respect the original authorization. For a planning-only request, stop with the
   plan. For an implementation request, update affected canonical documents first
   or alongside the code, then implement and verify without reopening settled
   product choices.
3. Follow the scoped platform/server AGENTS file and run checks proportional to
   the change. Report unresolved non-blocking decisions and unavailable checks.

## Keep documentation proportional

- For straightforward feature mechanics, let clear code and meaningful tests be
  the implementation documentation.
- Add a focused code comment only for a non-obvious reason, invariant or constraint;
  do not narrate what the code already says.
- Create a feature-local explanatory document only when a large, durable structure
  remains difficult to understand through code, tests, comments and existing
  canonical documents.
- Put durable platform facts in the relevant README and reusable coding rules in
  AGENTS. Do not add product behavior to either location.
- Do not create speculative documents, duplicate requirements, or update unrelated
  sources merely to show that the feature touched them.
