# YASIN AI APP STUDIO

An Android-first software creation studio. The repository is intentionally being built in verified stages.

## Current foundation
- Native Android app using Kotlin and Jetpack Compose
- Local project creation and persistence
- Editable project code area with save action
- Configurable OpenAI-compatible Chat Completions endpoint
- AI prompt/response screen
- GitHub Actions debug APK workflow

## Build
GitHub Actions builds a debug APK on every push to `main` and on manual workflow dispatch. Download the APK from the workflow run's Artifacts section after a successful build.

## Important security note
The first development build stores the API key in the app's private preferences. This is not suitable for a publicly distributed app. Before public release, AI requests must be routed through an authenticated server-side gateway; never embed a shared production API key in the APK.

## Roadmap
1. Verify reproducible Android builds and basic app flows.
2. Add secure Cloudflare Worker AI gateway and per-user settings.
3. Add structured project/file management and language-aware editor.
4. Add isolated build workers for APK/AAB and web projects.
5. Add GitHub import/export, previews, diagnostics, and game-engine adapters.

Features are marked complete only after implementation and testing; unsupported build targets will not be presented as working.
