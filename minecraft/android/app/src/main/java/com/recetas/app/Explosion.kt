package com.recetas.app

// Explorsion de materiales base en UNA sola consulta (BOM recursivo).
// La misma const la usa el DAO (@Query) y el test JVM (sqlite-jdbc),
// asi un error SQL se detecta sin telefono. Tope nivel 50 por seguridad.
// Redondeo por lote: cada nivel calcula procesos = ceil(necesidad / cantidad
// que produce la receta) uniendo con recetas. Los creditos de subproductos
// NO se aplican aqui (solo en el resolver Kotlin); con subproductos la CTE
// puede sobrestimar ligeramente los materiales base.
const val QUERY_EXPLOSION = """
WITH RECURSIVE explosion(id, cantidad, nivel) AS (
  SELECT :recetaId AS id, 1.0 AS cantidad, 0 AS nivel
  UNION ALL
  SELECT substr(i.refId, 5) AS id,
         CAST((e.cantidad + rp.cantidad - 1.0) / rp.cantidad + 0.000001 AS INTEGER) * i.cantidad AS cantidad,
         e.nivel + 1 AS nivel
  FROM explosion e
  JOIN ingredientes i ON i.padreId = e.id AND substr(i.refId, 1, 4) = 'rec_'
  JOIN recetas rp ON rp.id = e.id
  WHERE e.nivel < 50
)
SELECT substr(i.refId, 5) AS codigo,
       SUM(CAST((e.cantidad + rp.cantidad - 1.0) / rp.cantidad + 0.000001 AS INTEGER) * i.cantidad) AS total
FROM explosion e
JOIN ingredientes i ON i.padreId = e.id AND substr(i.refId, 1, 4) = 'mat_'
JOIN recetas rp ON rp.id = e.id
GROUP BY codigo
"""

data class TotalBase(val codigo: String, val total: Double)
