# Repository implementation instructions

Communicate with the developer in the language they use. Write all repository
code, comments, documentation, fixtures and commit messages in English.

## Route context before reading it

- Start with the task, the affected implementation/tests and the
  [documentation map](README.md#documentation-map). Identify which domains are
  affected, then read only their maintained owner documents and relevant links.
  Do not load every specification by default.
- Treat each topic's maintained owner as canonical. Update that owner and affected
  references instead of copying requirements into prompts, AGENTS files or
  platform guides. The former assignment draft is a migration index, not a
  requirements source.
- For a new or materially changed feature, follow the repository
  [feature-intake skill](.agents/skills/feature-intake/SKILL.md) when it is
  available. Use it to discover affected domains and settle missing decisions
  before implementation.
- Never invent a missing product requirement. First search the affected canonical
  sources, their direct references, and relevant existing tests/code. If a
  decision remains ambiguous, report the ambiguity, name the document that should
  own the answer, ask the developer one focused question, and wait for the answer
  before doing decision-dependent work. Continue only independent work that cannot
  prejudge that answer.

## Apply scoped implementation instructions

- Before changing `server/`, or shared protocol/fixtures that affect it, read and
  follow [server/AGENTS.md](server/AGENTS.md).
- Before changing `clients/ios/`, or specifications/prompts that affect iOS, read
  and follow [clients/ios/AGENTS.md](clients/ios/AGENTS.md).
- Before changing `clients/android/`, or specifications/prompts that affect
  Android, read and follow [clients/android/AGENTS.md](clients/android/AGENTS.md).
- During client work, adapt clients to the existing server and shared wire
  contract. Do not change the backend to accommodate a client. Report suspected
  backend defects with evidence and wait for separate authorization before any
  backend correction.

## Keep documentation proportional

- For straightforward feature implementation, readable code and meaningful tests
  are the developer documentation. Do not add a feature README that merely
  narrates the code.
- Add concise comments only for non-obvious intent, constraints or tradeoffs. Do
  not comment what the code already states.
- Create or expand prose documentation when the change introduces a durable
  contract, cross-cutting invariant, setup/operational requirement, architectural
  boundary, or a structure too large to understand safely from code and tests.
- Keep AGENTS files focused on implementation workflow and engineering rules.
  Product behavior, system behavior, UX, persistence and wire requirements belong
  to the canonical documents in the README map.
- Platform READMEs own durable platform setup, structure, architecture,
  integration, limitations and verification facts. They are not per-feature
  journals.

## Preserve scope and evidence

- Preserve unrelated local changes and user-owned files. Commit or push only when
  explicitly requested; authorization to commit does not imply authorization to
  push.
- Run verification in proportion to the change and report only checks actually
  completed. Documentation-only changes require path/link consistency and
  `git diff --check`; they do not require native builds unless they introduce a
  new runtime claim.
