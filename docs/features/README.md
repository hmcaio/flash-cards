# Feature docs

Each `FXX` folder = one feature slice of [../PRD.md](../PRD.md), with:
- `spec.md` — technical specification (scope, data/API touched, contracts, edge cases, acceptance criteria).
- `plan.md` — TDD action plan (ordered red/green/refactor tasks, test + prod file paths).

Build in order — each feature depends on the ones before it.

| # | Feature | PRD req(s) | Depends on |
|---|---|---|---|
| [F01](F01-data-layer-app-shell/spec.md) | Data Layer & App Shell | foundation | — |
| [F02](F02-card-set-management/spec.md) | Card Set Management | 1 | F01 |
| [F03](F03-card-tag-management/spec.md) | Card & Tag Management | 2, 3 | F01, F02 |
| [F04](F04-card-search/spec.md) | Card Search | 6 | F01, F03 |
| [F05](F05-practice-session/spec.md) | Practice Session | 4 | F01, F02, F03 |
| [F06](F06-practice-history/spec.md) | Practice History | 5 | F01, F05 |
| [F07](F07-import-export/spec.md) | Import / Export | 7 | F01, F02, F03 |

## TDD convention used in every plan.md
Each task is Red → Green → Refactor:
1. **Red**: write the test first, run it, confirm it fails for the right reason (missing symbol / wrong behavior, not a typo).
2. **Green**: write the minimum code to pass.
3. **Refactor**: clean up with tests green.

Room DAOs are declarative (no method body to write), so "Red" for a DAO test
means the entity/DAO doesn't exist yet or the `@Query` is wrong — the test
won't compile or will assert wrong data until the annotation is correct.

## Compose preview convention
Every new screen and reusable component gets an `@Preview`, added as part of
that feature's own Refactor step (not a separate pass). A screen composable
backed by `hiltViewModel()` is split into a public stateful entry point and a
private stateless overload that takes `UiState` + plain lambdas — the
stateless one is what gets previewed, since `hiltViewModel()` can't resolve
outside a running app. See `ui/setlist/SetListScreen.kt` (F02) for the
pattern: one `@Preview` per meaningful state (empty/populated, with/without
error), plus one for each extracted sub-component (row, dialogs).
