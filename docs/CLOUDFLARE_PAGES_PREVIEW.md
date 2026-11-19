# Cloudflare Pages preview setup

The web build creates a GitHub Actions artifact for every dispatched project. To enable an isolated Cloudflare Pages preview deployment:

1. In the GitHub repository, open **Settings → Secrets and variables → Actions**.
2. Add repository secret `CLOUDFLARE_ACCOUNT_ID` with the Cloudflare account ID.
3. Add repository secret `CLOUDFLARE_API_TOKEN` with an API token scoped to the required account and the **Cloudflare Pages Edit** permission. Do not paste this token into the Android app or commit it.
4. Under **Variables**, add `CLOUDFLARE_PAGES_DEPLOY` with value `true`.
5. No Pages project needs to be created manually. The workflow creates a Pages project using the safe project slug if it does not already exist.
6. Run a web build from the Studio. The deploy job publishes the build to a preview branch, not the production branch. Open the **Cloudflare Pages preview deployed** link in the GitHub Actions run summary.

The preview alias follows this pattern:

`https://<workflow-branch>.<project-slug>.pages.dev`

The project slug is derived from the Studio project name. Each project therefore gets its own Pages project. The first successful deployment may take longer while Wrangler creates that project.

## What this does and does not do

- HTML/CSS/JS projects are copied into `dist/`.
- React/Vite projects install dependencies and run `npm run build`; the resulting `dist/` is deployed.
- The local Android preview serves project files and built Vite assets from the selected project's `dist/` folder.
- Cloudflare Pages preview is a hosted preview of the production build. It is not a Vite hot-module-reload development server.
- No Cloudflare credential is stored in the APK, source tree, or generated project.
- If the deployment variable is not enabled, the GitHub build artifact still works and the deploy job is intentionally skipped.
