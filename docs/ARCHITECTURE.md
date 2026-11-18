# Architecture and delivery plan

## Android client
- Kotlin and Jetpack Compose
- Local project metadata and editable source files
- AI assistant using a configurable OpenAI-compatible endpoint

## Cloud services (next stage)
- Cloudflare Worker API gateway
- User authentication, quotas, and server-side provider secrets
- Project sync and source archives in object storage

## Build execution (next stage)
- Isolated build jobs with per-job resource limits
- Android APK/AAB and web build pipelines
- No generated project code executes inside the API Worker
- Build logs and artifacts are attached to each project

## Quality gates
- A feature is not considered complete until its happy path and failure path are tested.
- Build failures must be visible; never display a fabricated success state.
