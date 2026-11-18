# YASIN AI APP STUDIO

Android-first AI-assisted software development studio. The full mandatory product scope is tracked in [PRODUCT_REQUIREMENTS.md](docs/PRODUCT_REQUIREMENTS.md).

## Implemented in the current Android foundation
- Kotlin + Jetpack Compose Android application.
- Create and persist multiple local projects.
- Real app-private workspace folders and files.
- Create nested files and folders, open/edit/save files, rename and delete entries.
- File saves create timestamped snapshots under the project's hidden `.history` directory.
- Configurable OpenAI-compatible Chat Completions connection (development-only direct key mode).
- GitHub Actions workflow for a debug APK build.

## Build and install
Every push to `main` starts the Android GitHub Actions workflow. After a successful run, download the `yasin-ai-app-studio-debug` artifact from that run. An APK is not considered available until the workflow succeeds and the artifact is present.

## Security
The current direct AI key setting is a development convenience only and is not suitable for a public release. Before public distribution, route AI requests through an authenticated server-side gateway, store provider secrets only server-side, and add per-user authorization, quotas, and audit logging. Do not commit secrets or signing keys.

## Required product scope
All requirements in [docs/PRODUCT_REQUIREMENTS.md](docs/PRODUCT_REQUIREMENTS.md) remain in scope, including Android/web/game targets, real previews and builds, automatic diagnostics and repair, GitHub, account/security, cloud build orchestration, and AI cost controls. Roadmap items are not represented as implemented until their workflows are built and tested.
