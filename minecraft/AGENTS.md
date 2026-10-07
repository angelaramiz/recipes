# AGENTS.md

## Project
- Single-file app: `sistem_recipe.html` is the entire app. No `package.json`, bundler, lint, or CI. Tests live in `test/` (Node native, zero deps — not a build system).
- Stack: vanilla HTML + CSS + JS (ES5-style `var`/`function`, IIFE + `'use strict'`, no modules). UI language is Spanish — keep new strings in Spanish.
- Filename `sistem_recipe.html` is misspelled but is the current entrypoint. Do not rename without explicit approval.
- `docs/diseno-app-movil.md` is the design doc for the future Kotlin + SQLite app (schema, CSV backup, planos evolution). Consult it before porting logic; don't implement it here unasked.

## Run / verify
- No build step. Open `sistem_recipe.html` directly in a browser (`file://` works; no server needed).
- Tests (TDD, obligatorio): `node --test "test/*.test.mjs"` — Node nativo, cero dependencias. `test/helpers.mjs` ejecuta el `<script>` en sandbox con DOM/`localStorage` falsos; la lógica pura se expone vía `window.__mcTest` al final del IIFE (solo para tests, no afecta al navegador).
- Flujo: ROJO (test que falla primero) → VERDE (implementación mínima) → refactor → `node --test` + check en navegador (consola sin errores + clicar tabs tocados). Lógica nueva siempre con test; el navegador verifica integración.
- No formatter/linter config. Match surrounding style (2-space indent, `var`, single quotes in JS).

## Architecture (`sistem_recipe.html`)
- Tabs: `Calcular` (bidirectional conversion) | `Recetas` (CRUD) | `Materiales` (read-only tables) | `Historial` | `Grafo` (SVG flow viewer, read-only; layout via `computeGrafo()`, tap node → detail).
- Hardcoded data in `<script>` IIFE: `materiales[]` (`{codigo, nombre, tipo}`), `conversiones[]` (`{de, a, ratio}`), `typeOptions`, `unitNames`.
- Recipes: `{ id: 'r_<ts>_<rand>', nombre, ingredientes: [{ id, cantidad }], maquina?, tiempo?, energia? }`. Plano fields are optional (`manual`/0 = crafteo manual, backward compatible with old data — use `maquinaDe()`/`tiempoDe()`/`energiaDe()` accessors, never assume presence).
- Planos prototype: hardcoded `maquinas[]` (`manual, mesa, horno, fundicion, yunque, encantamiento, alquimia`); `resolverReceta()` also returns `{ tiempo, energia, maquinas[] }` with per-machine accumulated time; bottleneck = max accumulated machine time (`Todo manual` if none).
- **Ingredient ID prefixes (critical):** `mat_<codigo>` = base material, `rec_<id>` = sub-recipe. Preserve on every edit/import/export. Resolve display names only via `nombreIngrediente()` (raw `ing.id` comparisons never match — that bug was already fixed once).
- Cycle safety: `detectarCiclo()` on save + visited-set guards in `resolverReceta()` / `renderArbol()`. Do not remove.
- Persistence: `localStorage` keys `mc_recetas` and `mc_history` (max 20 entries). Changing keys/shape needs a migration, not a silent rename.
- Manual calculator drawer (`calcTab`/`calcDrawer`) evaluates via `Function(...)` — known eval use, do not "fix" unasked.
- Rendering is `innerHTML` string concatenation; import/export is JSON file (`minecraft_recetas_<fecha>.json`, Agregar vs Reemplazar prompt).

## Android (`android/`, Fase 1 paridad con la web)
- Módulo Gradle (AGP 8.7 + Kotlin 2.0 + Compose BOM + Room + KSP), paquete `com.recetas.app`. Catálogo en `gradle/libs.versions.toml`.
- Comandos (desde `android/`): `.\gradlew.bat :app:testDebugUnitTest` y `.\gradlew.bat :app:assembleDebug`. Si muere con `ChildProcess.kill`, reintentar volcando a archivo (flaky conocido).
- **Java:** Gradle corre con el JBR 17 de Android Studio (fijado en `android/gradle.properties`); el JDK 25 del sistema NO sirve como runtime de Gradle.
- Dominio puro y testeable en la raíz del paquete (`Resolver.kt`, `Accesores.kt`, `Convertir.kt`, `ImportJson.kt`); Room (`BaseDatos.kt`) y UI (`ui/`) delgados. Tests JUnit en `app/src/test`.
- El SQL de la CTE vive como `const QUERY_EXPLOSION` (`Explosion.kt`) compartida entre el DAO y los tests JVM (sqlite-jdbc ejecuta el mismo texto; Room usa `:recetaId`, el test lo sustituye por `?`). No dupliques la consulta.
- Migraciones Room manuales (`MIGRACION_X_Y` en `BaseDatos.kt`, registradas en `DbProvider`) que preservan datos de usuario. Nunca `fallbackToDestructiveMigration`.
- **Ratios de conversión corregidos** en `Datos.kt` (la web los tiene invertidos — bug pendiente allá, ver comentario en el archivo). No "sincronizar" ratios desde la web sin revisar.
- Smoke test: AVD `Medium_Phone_API_35` + `adb install -r` + `am start -n com.recetas.app/.MainActivity` + `screencap`/`uiautomator dump`.
- OTA sin backend: `Ota.kt` lee `OTA_VERSION_URL` (`version.json` estático en Render), descarga por stream (HttpURLConnection, sin deps nuevas) e instala vía FileProvider. Requiere `REQUEST_INSTALL_PACKAGES` + `file_paths.xml` (ya puestos).
- Release: `android/release.ps1 -VersionCode N -VersionName "x.y.z"` (bump + `assembleRelease` firmado + copia `recetas.apk` junto a `version.json` en `minecraft/`). Firma con `android/release.keystore` + `keystore.properties` (gitignorados, los genera el script; **respaldarlos** o se pierde el canal OTA). No commitea.
- `android/local.properties` (con `sdk.dir`) está gitignoreado; cada máquina lo genera.

## Deploy (Render static site)
- Entrypoint for Render is `index.html` — exact mirror of `sistem_recipe.html` (Render serves `/` from `index.html`; the misspelled name can't be the public URL).
- Source of truth is `sistem_recipe.html`. After editing it, re-sync: `Copy-Item sistem_recipe.html index.html` and verify hashes match.
- `render.yaml` (en la RAÍZ del repo) blueprint: `type: web` + `runtime: static`, no build step, `staticPublishPath: minecraft`, `Cache-Control: no-cache` on `/*` (forces fresh HTML on each deploy; file is ~57KB so no CDN benefit lost). No SPA rewrite needed (single page, no client-side router).
- Versioning: `APP_VERSION` const in `<script>` + `checkAppVersion()` on `init()` stamps `localStorage.mc_version` and appends `· vX` to the subtitle. It never deletes `mc_recetas`/`mc_history` (same origin = data survives deploys). Release bump = edit version in `sistem_recipe.html` → re-sync → commit + push.
- `localStorage` (`mc_recetas`, `mc_history`) works as-is on the https origin — no code change needed for deploy. No backend, no env vars, no external requests (fully self-contained).
- El sitio sirve `recetas.apk` (botón global "Descargar APK" bajo el título, visible en todos los tabs) + `version.json` (fuente OTA) en su raíz — el publish dir `minecraft/` se vuelve la raíz del sitio, SIN prefijo `/minecraft` en las URLs. Tras crear el Static Site, verificar que `OTA_VERSION_URL` en `Ota.kt` sea el dominio real.
- To go live: commit + push to `main`, then Dashboard → New → Static Site → connect `angelaramiz/recipes` (or deploy via `render.yaml` Blueprint).

## Workflow
- Remote `origin` wired (`angelaramiz/recipes`), branch `main` tracking `origin/main`.
- User convention: commit + push to `main` **only when explicitly requested** — default is to leave changes uncommitted for the user to review.
- Keep changes minimal and single-file unless the user asks for a split/build setup.
