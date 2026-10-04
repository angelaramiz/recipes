# AGENTS.md

## Project
- Single-file app: `sistem_recipe.html` (1098 lines) is the entire app. No `package.json`, bundler, tests, lint, or CI.
- Stack: vanilla HTML + CSS + JS (ES5-style `var`/`function`, IIFE + `'use strict'`, no modules). UI language is Spanish — keep new strings in Spanish.
- Filename `sistem_recipe.html` is misspelled but is the current entrypoint. Do not rename without explicit approval.

## Run / verify
- No build step. Open `sistem_recipe.html` directly in a browser (`file://` works; no server needed).
- No test runner. Verify by opening in browser and checking console for errors + clicking through touched tabs.
- No formatter/linter config. Match surrounding style (2-space indent, `var`, single quotes in JS).

## Architecture (`sistem_recipe.html`)
- Tabs: `Calcular` (bidirectional conversion) | `Recetas` (CRUD) | `Materiales` (read-only tables) | `Historial`.
- Hardcoded data in `<script>` IIFE: `materiales[]` (`{codigo, nombre, tipo}`), `conversiones[]` (`{de, a, ratio}`), `typeOptions`, `unitNames`.
- Recipes: `{ id: 'r_<ts>_<rand>', nombre, ingredientes: [{ id, cantidad }] }`.
- **Ingredient ID prefixes (critical):** `mat_<codigo>` = base material, `rec_<id>` = sub-recipe. Preserve on every edit/import/export.
- Cycle safety: `detectarCiclo()` on save + visited-set guards in `resolverReceta()` / `renderArbol()`. Do not remove.
- Persistence: `localStorage` keys `mc_recetas` and `mc_history` (max 20 entries). Changing keys/shape needs a migration, not a silent rename.
- Manual calculator drawer (`calcTab`/`calcDrawer`) evaluates via `Function(...)` — known eval use, do not "fix" unasked.
- Rendering is `innerHTML` string concatenation; import/export is JSON file (`minecraft_recetas_<fecha>.json`, Agregar vs Reemplazar prompt).

## Deploy (Render static site)
- Entrypoint for Render is `index.html` — exact mirror of `sistem_recipe.html` (Render serves `/` from `index.html`; the misspelled name can't be the public URL).
- Source of truth is `sistem_recipe.html`. After editing it, re-sync: `Copy-Item sistem_recipe.html index.html` and verify hashes match.
- `render.yaml` blueprint: `type: web` + `runtime: static`, no build step, `staticPublishPath: .`, `Cache-Control: no-cache` on `/*` (forces fresh HTML on each deploy; file is ~49KB so no CDN benefit lost). No SPA rewrite needed (single page, no client-side router).
- Versioning: `APP_VERSION` const in `<script>` + `checkAppVersion()` on `init()` stamps `localStorage.mc_version` and appends `· vX` to the subtitle. It never deletes `mc_recetas`/`mc_history` (same origin = data survives deploys). Release bump = edit version in `sistem_recipe.html` → re-sync → commit + push.
- `localStorage` (`mc_recetas`, `mc_history`) works as-is on the https origin — no code change needed for deploy. No backend, no env vars, no external requests (fully self-contained).
- To go live: commit + push to `main`, then Dashboard → New → Static Site → connect `angelaramiz/recipes` (or deploy via `render.yaml` Blueprint).

## Workflow
- Remote: `https://github.com/angelaramiz/recipes.git` (not yet wired locally — `git remote -v` is empty; local branch is `master` with no commits yet).
- User convention: commit + push to `main`. If adding remote: `git remote add origin https://github.com/angelaramiz/recipes.git`; reconcile `master` vs `main` before first push.
- Keep changes minimal and single-file unless the user asks for a split/build setup.
