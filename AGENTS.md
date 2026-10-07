# AGENTS.md (monorepo raíz)

Dos proyectos con la misma lógica de recetas. Cada uno tiene su propio
`AGENTS.md`; este archivo solo dice dónde está cada cosa.

- `minecraft/` — Sistema de recetas Minecraft (web + app Android). Ver
  `minecraft/AGENTS.md`. Es lo único desplegado: Render publica `minecraft/`
  (`render.yaml` en esta raíz, `staticPublishPath: minecraft`).
- `panaderia/` — Panadería/repostería (placeholder con `README.md`, sin código
  aún). Al arrancar: espejar estructura de `minecraft/`, no lógica duplicada
  a mano — el dominio (recetas anidadas, planos, explosión) se porta, los datos
  se reemplazan.

## Separación entre proyectos (regla del usuario)
- Lo único compartido es el repo. Todo lo demás va separado por proyecto:
  un Static Site en Render por proyecto, y una app/base Android por proyecto
  (distinto `applicationId`, distinta DB, distinto `version.json` para OTA).
- No crear backend ni almacenamiento compartido entre `minecraft/` y
  `panaderia/`.

## Workflow (aplica a todo el repo)
- Remote `origin` wired (`angelaramiz/recipes`), branch `main` tracking `origin/main`.
- Commit + push a `main` **solo cuando se pida explícitamente**.
