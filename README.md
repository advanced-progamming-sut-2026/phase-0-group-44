# PVZ2

Command-line implementation of the mandatory Phase-1 Plants vs. Zombies 2 project.

## Creators

- Amirreza SeyedHossaini - 404105948
- Setareh Frozan - 404171166
- Kiana Amani - 404170997

## Requirements

- Java 21
- The checked-in Gradle wrapper

## Build and verification

```bash
gradle clean test checkstyleMain checkstyleTest pmdMain pmdTest build
```

The normal executable entry point is `Main`. After a successful build:

```bash
gradle run
```

Phase-1 reviewer commands and requirement evidence are in
[`docs/phase1-audit/reviewer-guide.md`](docs/phase1-audit/reviewer-guide.md).
The reconciled architecture is in [`UML.puml`](UML.puml) and
[`docs/uml/final.png`](docs/uml/final.png).

## Scope

Mandatory Adventure, collection, greenhouse, shop, quests, leaderboard, and the
three mandatory minigames are implemented. Boss fights remain deferred to
Phase 2 as required. Implemented bonus work—including all canonical blue-row
plants and zombies—is explicitly identified in
[`docs/phase1-audit/bonus-scope.md`](docs/phase1-audit/bonus-scope.md).

## Audit status

The Phase-1 integration audit is complete but intentionally **not committed,
pushed, or tagged**. Create the Phase-1 tag only after reviewer approval.
