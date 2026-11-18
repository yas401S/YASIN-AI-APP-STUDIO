# YASIN AI APP STUDIO — Product Requirements

Status: Required product scope (not a claim that every feature is implemented yet).

## Product goal
Build an Android-first, AI-assisted software development studio that can create, edit, preview, test, build, version, and manage multiple kinds of software projects. The Android app is the control center; heavyweight builds and untrusted generated code run in isolated cloud workers, not inside the phone app or public API Worker.

## Mandatory capabilities

1. **Android application development**
   - Kotlin and Jetpack Compose as first-class targets.
   - Support other Android-compatible stacks through project templates and build adapters (for example Java, Flutter, and React Native).
   - Generate project structure, source files, resources, manifests, and build configuration.

2. **Websites and web applications**
   - HTML, CSS, JavaScript/TypeScript and common frameworks such as React/Next.js and Vue.
   - Install dependencies, run development builds, preview, test, and export deployable artifacts.

3. **Game development**
   - Engine adapters for Godot, Unity, and other supported game engines.
   - Engine-specific project templates, asset organization, build/export profiles, and logs.
   - Clearly report when an engine or target platform requires external tooling or licensing.

4. **Code editor and file management**
   - Project tree with folders and files; create, rename, move, and delete operations.
   - Syntax-aware editing, search, multi-file navigation, and safe save/restore.
   - Preserve the actual project structure rather than keeping source as a single text field.

5. **Preview and project execution**
   - Live web preview where supported.
   - Android preview/emulator or device-run workflow where infrastructure permits.
   - Game preview through the relevant engine/runtime.
   - Show explicit unsupported-target states instead of fake previews.

6. **Automatic programming error repair**
   - Capture compiler, linter, test, and runtime diagnostics.
   - Explain the cause, propose a patch, show changed files/diff, and let the user approve or revert.
   - Re-run the relevant checks after repair; never claim success without a passing check.

7. **APK and AAB production**
   - Real Android Gradle builds for debug and signed release outputs.
   - Secure signing-key management; signing secrets must never be committed to Git or embedded in source.
   - Store build logs, build status, and downloadable artifacts per project.

8. **Version management and change history**
   - Git-backed commits, diffs, branches, tags, restore/revert, and checkpoints.
   - Explain changes made by AI and allow review before applying them.

9. **Multiple project management**
   - Create, import, duplicate, archive, rename, and delete projects.
   - Project-specific files, settings, build profiles, secrets references, and history.
   - Local-first workspace with optional authenticated cloud sync.

10. **Templates and component library**
    - Searchable starter templates by platform, framework, and use case.
    - Reusable UI components, layouts, snippets, and project presets.
    - Templates must be real, buildable project files, not screenshots or mockups.

11. **Account, security, and administration**
    - Account/profile and session management, access control, secure credential handling, privacy controls, and audit events.
    - Server-side authorization for administrative actions.
    - Do not store shared production API keys in the Android APK or public repository.

12. **GitHub integration**
    - Secure OAuth-based connection with least-privilege permissions.
    - Import and export repositories, branch selection, commit/push/pull, and conflict reporting.
    - Never expose personal access tokens in logs or generated source.

13. **Cloud build engine, diagnostics, and AI cost management**
    - Queue and isolate build jobs with time, memory, and storage limits.
    - Build logs, structured diagnostics, retry/cancel, artifact retention, and job status.
    - Track AI provider/model, token usage where available, estimated cost, budgets, quotas, and per-project usage.
    - Warn before expensive operations and allow the user to set spending limits.

## Target architecture

- **Android client:** Kotlin + Jetpack Compose; project workspace, editor, AI interaction, settings, and build dashboard.
- **API/control plane:** Cloudflare Workers for authentication, project APIs, job orchestration, usage accounting, and provider routing.
- **Project storage:** local device storage plus optional encrypted cloud object storage and metadata database.
- **Build workers:** isolated, ephemeral workers/containers appropriate to each toolchain. Never execute untrusted generated code inside the public API Worker.
- **Provider adapters:** interchangeable AI model providers; keep credentials server-side.
- **Engine/toolchain adapters:** Android Gradle, web toolchains, Godot/Unity where licensed and available, and additional targets through adapters.
- **Git provider adapter:** GitHub OAuth and repository operations.

## Delivery and acceptance rules

- All 13 capabilities above remain in the final product scope.
- Deliver in testable stages; do not remove a requirement merely because it is not in the first APK.
- A feature is complete only when its main workflow, error workflow, and security implications have been tested.
- A visible control must perform its stated action or be clearly marked as not yet available.
- Never fabricate build success, previews, test results, usage figures, or AI savings.
- APK/AAB links are provided only after a successful build and artifact verification.
- The first release should establish the secure foundations and then add each platform/toolchain through adapters without rebuilding the product from scratch.

## Suggested implementation sequence

A. Stabilize Android workspace, real file tree/editor, project persistence, and tests.
B. Add secure Cloudflare API gateway, account/authentication, AI provider routing, usage budgets, and secrets handling.
C. Add GitHub OAuth, repository import/export, Git history, diffs, and restore.
D. Add cloud job queue, isolated Android Gradle builds, APK/AAB signing, logs, and artifact delivery.
E. Add web project templates, dependency installation, live preview, and web builds.
F. Add diagnostic collection and reviewable AI repair loops.
G. Add game-engine adapters (starting with Godot; Unity support subject to licensing and available build infrastructure).
H. Add reusable templates/components, administration, quotas, monitoring, and expanded compatibility.
