# Implementation status — 13 required capabilities

This file distinguishes shipped, tested functionality from planned work. A capability is not complete merely because a screen, button, interface, or adapter declaration exists.

## Delivery rule
Every capability must have a working user workflow, visible failure states, automated or repeatable tests, and documented security boundaries. The Android app is the mobile control center; untrusted builds execute only in isolated build infrastructure.

## Current verified foundation
- Android client: Kotlin + Jetpack Compose app foundation.
- Local workspace: project directories, nested file creation, text read/write, timestamped save snapshots.
- New workspace service on feature/13-capabilities-foundation: project CRUD, rename/duplicate/delete, safe path validation, bounded ZIP import/export, text search, and starter-template file generation.
- Starter templates: blank, static web, React/Vite, Android/Kotlin, and Godot 2D. Static preview serves project assets from a read-only, path-confined WebView request handler and applies a restrictive CSP; production Vite dist/ output is served with module/chunk paths preserved. React single-file preview remains a separate restricted mode. The Studio web-build workflow creates static/React/Vite dist artifacts; the app can download the artifact safely into the project and open it in the local preview. Optional Cloudflare Pages branch previews are supported through repository secrets and a repository variable; they are not active until configured and tested. Generated Android projects include a CI workflow for tests/debug APK and optional signed APK/AAB when repository signing secrets exist. Generated Godot projects include a Linux export preset and CI workflow. These workflows build after the user pushes the generated project to their own GitHub repository; they are not in-app cloud builds.
- Unit tests added for template creation, search, snapshots, project operations, archive round-trip, and ZIP path traversal rejection.
- Pull requests now run Android unit tests before building the debug APK.

## The 13 capability tracks

| # | Capability | Current state | Completion gate |
|---|---|---|---|
| 1 | Android app development | Kotlin/Compose starter with generated-project GitHub Actions workflow for tests and debug APK; signed APK/AAB when the user supplies repository signing secrets | In-app project generation-to-build handoff, device install/verification, managed signing and build logs |
| 2 | Websites and web apps | In-app GitHub upload/dispatch, static and React/Vite cloud builds, artifact download into local preview, repeatable static/Vite smoke tests; optional Cloudflare Pages branch deployment via repository secrets/variable | End-to-end test with a real phone project, configure Cloudflare credentials and verify live preview, full Vite HMR dev server |
| 3 | Game development | Playable Godot 4 2D starter, Linux export preset, and GitHub Actions workflow for Linux export | In-app Godot editor/runtime preview, export profile UI, Android/Windows/macOS exports; Unity requires licensed tooling |
| 4 | Code editor and file management | Editor and file actions use workspace service; safe CRUD, search, snapshots, and ZIP archives | Tree navigation, syntax-aware editor, multi-file workflow, diff and restore |
| 5 | Preview and execution | Static and downloaded production Vite builds preview locally through a read-only asset handler; preserves ES modules/chunks and common web assets; restrictive CSP; restricted single-file React/JSX preview; hosted Cloudflare Pages branch preview when configured | Full Vite dev server/hot reload, Android app execution, and Godot runtime/export remain outstanding |
| 6 | Automatic error repair | Local deterministic diagnostics for common delimiter mismatch/unclosed delimiters, Git conflict markers and whitespace; safe whitespace fixes are saved with history | Compiler/linter diagnostics, AI proposed diff review, explicit approval/revert and automated rerun of the relevant build |
| 7 | APK/AAB production | Generated Android starter has a CI workflow that builds debug APK; optional signed release APK/AAB requires user-managed keystore secrets | Studio-managed per-project build queue, artifact/log delivery and secure signing-secret provisioning |
| 8 | Version management | Local save snapshots; Git integration pending | Git commits, branches, diffs, tags, restore/revert |
| 9 | Multiple project management | Local project CRUD, duplicate/rename/delete UI, and ZIP import/export UI | Import/export UI, duplication/rename/delete, project-specific profiles and sync |
| 10 | Templates and component library | Five starter templates with template search; 10 searchable, insertable HTML/CSS/JavaScript/React/Kotlin/Godot snippets | Versioned template updates, user-managed component packs, richer language/category filters |
| 11 | Account/security/admin | Provider key persistence migrated to AES-GCM encryption backed by Android Keystore; one-time migration removes the previous plaintext preference | Cloud authentication, server-side authorization/sessions, admin roles, audit events and server-side provider secret proxy |
| 12 | GitHub integration | Not implemented | OAuth, least-privilege repo import/export, branches, push/pull, conflict handling |
| 13 | Cloud build/diagnostics/AI cost | Per-project GitHub Actions workflows are generated for React, Android and Godot; local AI token usage is counted when provider reports it | Integrated authenticated build queue, isolated workers, app-side logs/retry/cancel/artifact delivery, real cost accounting and budgets/quotas |

## Implementation sequence

1. Complete and integrate the local workspace workflows into the Compose UI; add import/export pickers and tests.
2. Build the Cloudflare control plane: authenticated sessions, project metadata, AI provider routing, quotas, and usage records. Provider secrets remain server-side.
3. Add GitHub OAuth and repository operations with least-privilege permissions.
4. Add isolated build orchestration for Android and web; add Godot when a supported isolated toolchain is available.
5. Add real previews, structured diagnostics, reviewable AI patches, and rerun checks.
6. Finish account/admin controls, reusable component library, cost budgets, observability, and end-to-end acceptance tests.

## Explicit non-goals
- Do not execute arbitrary generated code inside a public Cloudflare API Worker or Android app process.
- Do not put provider keys, GitHub tokens, signing keys, or admin credentials in the APK or repository.
- Do not show a successful preview/build/repair state unless the underlying process returned success and its output was verified.
- Do not claim Unity builds are available unless licensing and build infrastructure are actually configured.
