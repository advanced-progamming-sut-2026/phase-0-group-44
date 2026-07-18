# Test, lint, and package results

Audit date: 2026-07-18.

## Native Gradle attempt

Requested command:

```bash
bash gradlew clean test checkstyleMain checkstyleTest pmdMain pmdTest build --no-daemon
```

Result: **BLOCKED BY AUDIT ENVIRONMENT** before Gradle started. The checked-in
wrapper attempted to download Gradle 9.3.0 from `services.gradle.org`, but the
audit container has no external DNS/network access and raised
`UnknownHostException`. No test or lint failure was reported by Gradle because
the Gradle runtime itself could not be obtained.

The repository now configures the normal tasks in `build.gradle`:

- JUnit 5 `test`
- `checkstyleMain` and `checkstyleTest`
- `pmdMain` and `pmdTest`
- executable `run`
- manifest-bearing `jar`
- normal `build`

## Full unit-test fallback

Because native Gradle was unavailable, all project test sources were compiled
against Java 21 and executed with a small local JUnit-5-compatible reflection
runner supporting the annotations/assertions used by this repository.

Result:

```text
297 tests completed, 0 failed
```

This includes all controller, persistence, Adventure, mandatory and bonus plant/zombie,
greenhouse, shop, quest, leaderboard, scored-game, and minigame tests. The
fallback verifies test behavior but is not represented as a substitute for the
official Gradle/JUnit Platform run; reviewers should run the Gradle command
above in a networked or cached environment.

## Bonus-content deterministic tests

The completed bonus-content pass adds deterministic tests for all 12 blue-row
plants and all 6 blue-row zombies. Coverage includes stacking, homing,
hypnosis, armor bypass, area damage, timed growth/digestion, lane attraction,
ram collisions, hooking, projectile reflection, transformation restoration, and
King armor promotion. Registry/collection/command tests also verify that bonus
classification is preserved while the content is constructible and playable.

Result is included in the full fallback total above: **297 tests, 0 failed**.

## Integration/end-to-end command tests

The final audit adds command-routing tests that send the exact text forms
through `MenuRouter`, not directly to controllers.

```text
Phase1CommandContractTest > everyDocumentedExactCommandFormIsRegistered PASSED
Phase1CommandContractTest > malformedVariantsDoNotAccidentallyMatch PASSED
Phase1CommandFlowTest > documentedCommandsReachAPlayableAdventureSession PASSED
Phase1CommandFlowTest > rejectedGameplayCommandsDoNotMutateTheSession PASSED

4 tests completed, 0 failed (0.302s)
```

The flow covers registration, security question, login, Main-to-Game routing,
chapter entry, automatic starter-plant selection, gameplay creation, and
non-mutation after malformed/invalid gameplay commands.

## Compilation and package smoke test

Production and test sources compiled successfully with Java 21.

A manifest-bearing jar was built with the equivalent manual package operation
and started successfully from the repository root:

```text
loaded 0 user(s)
progress saved; program finished
```

Artifact verified during the audit: `build/libs/PVZ2.jar`, entry point `Main`.
The ignored build directory is not included in the audit patch.

## Checkstyle-equivalent static pass

The documented naming and line-length rules were checked over `src` and `test`:

- Type naming: 0 unsuppressed findings
- Method naming: 0 unsuppressed findings
- Local/member naming: 0 unsuppressed findings
- Constant naming: 0 unsuppressed findings
- Lines longer than 120 characters: 0
- Method length: 19 explicit targeted suppressions
- Standard Java `serialVersionUID`: 1 explicit naming suppression

The method suppressions are listed in
`config/checkstyle/suppressions.xml`. They preserve stable, heavily tested
aggregate/parser methods during the final integration pass rather than mixing a
large structural rewrite into defect correction.

## PMD-equivalent static pass

The PMD ruleset contains exactly the Phase-1 requested checks:

- `UnusedPrivateMethod`
- `UnusedPrivateField`
- `UnusedLocalVariable`
- `ExcessiveMethodLength` with threshold 50
- `ExcessiveClassLength` with threshold 500

No unused private/local item was found by compilation/static inspection.
Excessive-length findings are explicitly annotated and documented:

- 19 methods with `PMD.ExcessiveMethodLength`
- 3 aggregate/state classes with `PMD.ExcessiveClassLength`:
  `GameEngine`, `User`, and `BeghouledState`

Native PMD execution remains pending the reviewer Gradle run because the audit
environment could not download Gradle/PMD.

## Additional Java compiler diagnostics

`javac -Xlint:all` compiles successfully and reports 22 warnings:

- 8 warnings are missing optional Error Prone annotation metadata inside the
  checked-in Gson jar.
- 14 warnings concern package-private zombie ability classes grouped in
  `MandatoryZombieAbilities.java` and referenced by the factory.

The prior serialization and constructor `this`-escape warnings were fixed by
this audit. The remaining diagnostics do not affect compilation or any
mandatory Checkstyle/PMD rule.
