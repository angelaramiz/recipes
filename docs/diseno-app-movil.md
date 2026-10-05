# App Móvil — Calculadora de Recursos (Kotlin + SQLite)

Documento de diseño. Origen: evolución de la web actual (`sistem_recipe.html`)
hacia app nativa Android, y del sistema de recetas hacia planos y esquemas.

## 1. Visión

App Android instalable (APK) para calcular recursos y gestionar recetas anidadas
de Minecraft, con evolución a sistema de planos que dependen de máquinas,
estructuras y procesos. La web actual se mantiene viva como fallback ($0/mes)
para iPhone/PC y como demo enlazable.

## 2. Plataforma y distribución

- **Lenguaje:** Kotlin, Android-only (el público objetivo juega en Android).
- **UI:** Jetpack Compose.
- **Costo:** $0 — sin Google Play. Distribución por **GitHub Releases** (APK firmado).
- **Actualizaciones:** el usuario instala **Obtainium**, que vigila los Releases
  y actualiza la app sola. Sin código de updater propio.
- **Offline-first:** todo funciona sin internet tras la instalación.

## 3. Datos — SQLite (Room), no CSV como store

Fuente de verdad: Room. El CSV es solo artefacto de backup generado por la app
(ver §5). El usuario nunca escribe CSV a mano.

### 3.1 Esquema base (paridad con la web)

```sql
materiales(id TEXT PK, codigo TEXT UNIQUE, nombre TEXT, tipo TEXT)
-- tipo: lingote | polvo | gema

recetas(id TEXT PK, nombre TEXT, creada_en INTEGER)

ingredientes(id INTEGER PK, padre_id TEXT FK→recetas,
             tipo TEXT,               -- 'mat' | 'rec'
             ref_id TEXT,             -- codigo material o id de sub-receta
             cantidad INTEGER)
```

- Los prefijos `mat_`/`rec_` de la web se reemplazan por la columna `tipo`
  (más limpio en relacional; misma semántica).
- Índices en `ingredientes(padre_id)` y `ingredientes(ref_id, tipo)`.

### 3.2 Reglas de integridad

- **Ciclos:** antes de insertar arista `A → B`, verificar con CTE recursiva que
  `B` no contenga transitivamente a `A`. Rechazar con mensaje en español.
- **Transacciones:** guardar receta = transacción Room (`@Transaction`).
  Todo o nada; jamás recetas a medias.
- **Borrado:** al borrar receta referenciada por otras, bloquear o reasignar
  (decidir en implementación; nunca dejar aristas huérfanas).
- **Profundidad máxima:** 50 niveles en CTEs recursivas (defensa en profundidad
  junto a la detección de ciclos).

### 3.3 Recetas anidadas (BOM recursivo)

Caso guía: `Batery Jumbo = 4× Large = 16× Medium = 64× Small`
(+ materiales base del Small ×64). La profundidad son filas, no esquema.

Explosión de materiales base en una sola consulta:

```sql
WITH RECURSIVE explosion(id, cantidad) AS (
  SELECT :recetaId, 1
  UNION ALL
  SELECT i.ref_id, e.cantidad * i.cantidad
  FROM explosion e
  JOIN ingredientes i ON i.padre_id = e.id AND i.tipo = 'rec'
)
SELECT m.nombre, SUM(e.cantidad * i.cantidad) AS total
FROM explosion e
JOIN ingredientes i ON i.padre_id = e.id AND i.tipo = 'mat'
JOIN materiales m ON m.codigo = i.ref_id
GROUP BY m.codigo;
```

UI: árbol colapsable por nivel + vista explotada ("para 1 Jumbo: X hierro…").

## 4. Versionamiento (heredado de la web, sin borrar datos)

- `versionCode`/`versionName` = `APP_VERSION` actual (`1.0.0` en web).
- Clave `app_version` en DataStore/Preferences, equivalente a `mc_version`.
- Al abrir: si difiere, actualizar la marca y correr migraciones Room si toca.
  **Nunca borrar tablas de usuario en una migración.**
- Migración inicial web→app: importar el JSON que exporta la web
  (`minecraft_recetas_<fecha>.json`) en primera ejecución.

## 5. Backup CSV (generado por la app, solo lectura para el usuario)

- Una fila por arista: `receta_id,receta_nombre,tipo_ingrediente,ref_id,cantidad`.
- Nombre versionado: `backup_v<version>_<fecha>.csv` (ej. `backup_v1.0.0_2026-10-04.csv`).
- Escritura RFC 4180 (comillas/escapes correctos aunque el usuario nunca lo toque).
- **Restaurar:** leer → validar esquema + versión → pasada 1 inserta recetas,
  pasada 2 inserta aristas (orden-independiente) → todo en **una transacción**
  con rollback total si algo falla. CSV corrupto se rechaza sin tocar la base.

## 6. Evolución: de recetas a planos y esquemas

El grafo de crafteo manual se generaliza a grafo de producción.
Migración **aditiva**: campos nuevos opcionales; las recetas existentes
(`maquina_id NULL` = crafteo manual) siguen funcionando sin cambios.

### 6.1 Nuevas entidades

```sql
maquinas(id TEXT PK, nombre TEXT, tier INTEGER,
         velocidad REAL, consumo_energia REAL)

-- Campos nuevos en recetas (plano):
--   maquina_id TEXT FK→maquinas NULL, tiempo_s REAL NULL,
--   energia REAL NULL
-- Salidas múltiples: tabla receta_salidas(receta_id, tipo, ref_id, cantidad)
--   (ej. acero + escoria). La receta "simple" actual = 1 salida.

requisitos(receta_id FK, tipo TEXT, ref_id TEXT, detalle TEXT)
-- tipo: estructura | bioma | dimension  (ej. multibloque 3×3, Nether, cerca de agua)
```

### 6.2 Nuevas preguntas que el sistema responde

- **Factibilidad:** "¿puedo hacerlo?" = tengo materiales Y máquina Y requisitos.
  Rama inalcanzable se marca (no se borra).
- **Cuello de botella:** la máquina con mayor tiempo acumulado limita el
  throughput del plano completo.
- **Costo total:** materiales + `tiempo_s × cantidad` + `energía × cantidad`.
- **Rutas alternativas:** mismo producto por varias cadenas → camino mínimo
  (más barato / más rápido) sobre el grafo.

### 6.3 Orden sugerido

Prototipar el modelo de planos en la web primero (campos máquina/tiempo en el
modal + vista de cuello de botella con datos reales) y portar a Kotlin ya
validado, en vez de diseñar el esquema complejo a ciegas.

## 7. Roadmap

- **Fase 1 — Paridad:** ✅ hecha en `android/` (2026-10-04): Room + CRUD recetas + conversiones + historial + importar/exportar JSON + versionamiento. 13 tests JUnit verdes + `assembleDebug` OK + smoke en emulador.
  Importar JSON de la web. Versionamiento.
- **Fase 2 — Respaldo:** ✅ hecha en `android/` (2026-10-04): `CsvBackup.kt` (generar RFC4180 + parseo validado + `restaurarRespaldo`), `reemplazarTodo` transaccional en Room, UI Respaldo/Restaurar CSV con confirmación. 8 tests nuevos (21/21 verdes).
- **Fase 3 — Anidación profunda:** ✅ hecha en `android/` (2026-10-04): CTE recursiva (`QUERY_EXPLOSION` en `Explosion.kt`, tope nivel 50) para materiales base en una consulta + árbol colapsable `[+]/[-]` + totales CTE en detalle. 3 tests nuevos con sqlite-jdbc sobre el SQL exacto del DAO (24/24 verdes).
- **Fase 4 — Planos:** ✅ hecha en `android/` (2026-10-04): `cantidad` (unidades por proceso, redondeo ceil en resolver y CTE), `subproductos` (solo `mat_*`, acreditan consumo, sobrante neto visible), `requisitos` (estructura|bioma|dimension), factibilidad vs "Mi taller", rutas alternativas por nombre ordenadas por tiempo. Migración Room 1→2 sin pérdida de datos (verificada en emulador con DB v1 real). 31/31 tests verdes.
- **Fase 5 — Grafo + Release + OTA:** ✅ (2026-10-04): visor de flujo SVG en la web (`computeGrafo()` por camino más largo, tap→detalle, tests incluidos); OTA sin backend (`version.json` estático, check al arrancar, descarga por stream, install vía FileProvider); `release.ps1` (keystore + bump + APK firmado `recetas.apk` en raíz) + botón Descargar APK. 34/34 tests Android, 10/10 web.

## 8. Decisiones abiertas

- Borrado de receta referenciada: ¿bloquear o reasignar?
- iOS/PC más allá de la web actual: fuera de alcance (web = fallback).
- Publicar en Play Store más adelante: solo si se necesita descubrimiento;
  el esquema y el código no cambian por el canal de distribución.
