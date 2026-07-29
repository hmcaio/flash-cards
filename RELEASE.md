# Release process

This project has no release automation yet — cutting a release is a manual,
documented process. Follow these steps in order.

> **During development**: every feature PR merges straight into `main`
> (GitHub flow, no `develop` buffer). Add your change under
> `CHANGELOG.md`'s `[Unreleased]` section as part of the PR that introduces
> it, not later — there's no staging branch to catch up on before a release.

## 1. Decide the version bump
Follow [Semantic Versioning](https://semver.org/):
- **MAJOR** — breaking change to persisted data or app behavior a user would
  notice as incompatible (rare for a personal single-user app).
- **MINOR** — a new feature lands (e.g. one of F02–F07 is completed).
- **PATCH** — a bug fix or small polish, no new feature.

## 2. Bump the version
In `gradle.properties`, update both properties together:

    VERSION_NAME=X.Y.Z
    VERSION_CODE=<computed>

`VERSION_CODE` formula: `major * 10_000 + minor * 100 + patch`
(e.g. `0.2.1` -> `0*10_000 + 2*100 + 1` = `201`). It must always increase —
Android requires each installed build to have a strictly higher `versionCode`
than the last.

## 3. Update the changelog
In `CHANGELOG.md`:
1. Rename the `[Unreleased]` heading to `[X.Y.Z] - YYYY-MM-DD` (today's
   date), keeping its existing entries.
2. Add a fresh empty `[Unreleased]` section above it.
3. Update the link references at the bottom: point `[Unreleased]` at the new
   tag, and add an `[X.Y.Z]` compare link (previous tag -> new tag, or the
   initial commit -> new tag for the first release).

## 4. Commit, tag, push

    git add gradle.properties CHANGELOG.md
    git commit -m "Release vX.Y.Z"
    git tag -a vX.Y.Z -m "vX.Y.Z"
    git push origin main --tags

## 5. Verify
- CI (`.github/workflows/ci.yml`) is green on the tagged commit.
- `./gradlew assembleDebug` succeeds locally and the installed app reports
  the new version (`adb shell dumpsys package com.chm.flashcards | grep
  version`).

No tag-triggered release workflow exists yet — that can be added later once
there's a reason to distribute builds outside local installs.
