# Repository instructions

- Communicate with the user in same language the user is using unless requested otherwise. Write all
  repository code, comments, documentation, fixtures and commit messages in English.
- Use the [README documentation map](README.md#documentation-map) to find each
  topic's maintained source. Update the owner document and affected references;
  the former assignment draft is a migration index, not a requirements source.
- Before working on `server/`, or on shared protocol/fixture changes that affect
  the server, read and follow [server/AGENTS.md](server/AGENTS.md). Read it explicitly
  even when the task starts from the repository root.
- Server changes include documentation and client compatibility work. Follow the
  documentation-impact and verification rules in the server instructions.
- Before working on `clients/ios/`, or on specifications/generator prompts that
  affect the iOS client, read and follow [clients/ios/AGENTS.md](clients/ios/AGENTS.md).
  Read it explicitly even when the task starts from the repository root.
- Before working on `clients/android/`, or on specifications/generator prompts
  that affect Android, read and follow [clients/android/AGENTS.md](clients/android/AGENTS.md).
  Read it explicitly even when the task starts from the repository root.
- [clients/ios/README.md](clients/ios/README.md) and
  [clients/android/README.md](clients/android/README.md) are the single platform
  guides for project details, setup, architecture, assets and verification.
  Platform AGENTS files own coding rules; shared product, behavior, UI design,
  persistence and integration documents remain their context sources.
- During either client's generation and implementation, adapt the client to the existing
  server and shared contract. Do not change the server or redefine shared wire
  behavior to accommodate the client. Report suspected backend defects to the
  developer, who decides when and how to address them in a separate authorized task.
- Client changes must update the affected platform README and maintained inputs
  in the same change set. Follow that platform's agent for architecture, state modeling,
  backend-issue reporting and the test implementation checkpoint. Documentation
  maintenance is not authorization to change server behavior or its contract.
- Preserve unrelated local changes. Commit or push only when the user requests it;
  authorization to commit does not imply authorization to push.
