# Android generation entry point

Before an authorized generation task, read [shared instructions](shared.md),
[the Android README](../../clients/android/README.md) and
[the Android agent](../../clients/android/AGENTS.md), then load the relevant shared
context they reference.

The README is the single platform guide; AGENTS owns implementation rules,
component/token reuse, state/ownership boundaries, backend compatibility and the
test checkpoint. Follow those rules without maintaining competing copies here.
Extend the existing project using the README's structure and actual configuration.

Preserve [declared output ownership](../README.md#output-ownership-and-reproducibility),
update affected maintained inputs, and report generation/build/test results and
unverified integration honestly. Reading this prompt does not authorize generating
code, deleting the existing project or changing the server.
