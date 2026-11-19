# YASIN AI APP STUDIO

Android-first AI-assisted software development studio. The full mandatory product scope is tracked in [PRODUCT_REQUIREMENTS.md](docs/PRODUCT_REQUIREMENTS.md).

## Implemented in the current Android foundation
- Kotlin + Jetpack Compose Android application.
- Create and persist multiple local projects.
- Real app-private workspace folders and files.
- Create nested files and folders, open/edit/save files, rename and delete entries.
- File saves create timestamped snapshots under the project's hidden `.history` directory.
- The app UI now exposes project templates, project duplicate/rename/delete, and ZIP import/export; the workspace service provides bounded archives, text search, safe path checks, and timestamped snapshots.
- Starter templates: blank, static web, React/Vite, Android/Kotlin, and Godot.
- Unit tests cover workspace operations, templates, archive round-trips, and ZIP path traversal rejection. Pull requests run tests before APK packaging.
- Configurable OpenAI-compatible Chat Completions connection (development-only direct key mode).
- GitHub Actions workflow for a debug APK build.

## Build and install
Every push to `main` starts the Android GitHub Actions workflow. After a successful run, download the `yasin-ai-app-studio-debug` artifact from that run. An APK is not considered available until the workflow succeeds and the artifact is present.

## Security
The current direct AI key setting is a development convenience only and is not suitable for a public release. Before public distribution, route AI requests through an authenticated server-side gateway, store provider secrets only server-side, and add per-user authorization, quotas, and audit logging. Do not commit secrets or signing keys.

## Implementation status
See [docs/IMPLEMENTATION_STATUS.md](docs/IMPLEMENTATION_STATUS.md) for the honest status and acceptance gates of all 13 mandatory capabilities. Items marked planned are not yet implemented.

## Required product scope
All requirements in [docs/PRODUCT_REQUIREMENTS.md](docs/PRODUCT_REQUIREMENTS.md) remain in scope, including Android/web/game targets, real previews and builds, automatic diagnostics and repair, GitHub, account/security, cloud build orchestration, and AI cost controls. Roadmap items are not represented as implemented until their workflows are built and tested.
