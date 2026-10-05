package com.recetas.app

// Respaldo CSV: una fila por arista padre->hijo. El CSV lo genera la app;
// el usuario nunca lo escribe. Restaurar valida TODO antes de tocar la base
// y reemplaza en una sola transaccion (rollback total si algo falla).
const val CSV_HEADER = "receta_id,receta_nombre,maquina,tiempo,energia,tipo_ingrediente,ref_id,cantidad"

class CsvInvalidoException(msg: String) : IllegalArgumentException(msg)

data class FilaCsv(
  val recetaId: String,
  val recetaNombre: String,
  val maquina: String,
  val tiempo: Double,
  val energia: Double,
  val tipo: String,
  val refId: String,
  val cantidad: Double
)

private fun escCsv(s: String): String =
  if (s.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + s.replace("\"", "\"\"") + "\"" else s

fun generarCsv(recetas: List<Receta>): String {
  val sb = StringBuilder(CSV_HEADER)
  for (r in recetas) {
    for (ing in r.ingredientes) {
      val tipo = if (ing.id.startsWith("rec_")) "rec" else "mat"
      sb.append('\n').append(
        listOf(
          r.id, r.nombre, maquinaDe(r), tiempoDe(r).toString(),
          energiaDe(r).toString(), tipo, ing.id, ing.cantidad.toString()
        ).joinToString(",") { escCsv(it) }
      )
    }
  }
  return sb.toString()
}

// Divide en filas logicas respetando campos entrecomillados multilinea.
private fun partirFilas(texto: String): List<String> {
  val filas = mutableListOf<String>()
  val cur = StringBuilder()
  var entreComillas = false
  var i = 0
  while (i < texto.length) {
    val c = texto[i]
    if (c == '"') {
      if (entreComillas && i + 1 < texto.length && texto[i + 1] == '"') { cur.append("\"\""); i += 2; continue }
      entreComillas = !entreComillas
      cur.append(c); i++
    } else if ((c == '\n' || c == '\r') && !entreComillas) {
      filas.add(cur.toString()); cur.clear()
      if (c == '\r' && i + 1 < texto.length && texto[i + 1] == '\n') i++
      i++
    } else {
      cur.append(c); i++
    }
  }
  if (entreComillas) throw CsvInvalidoException("Comillas sin cerrar")
  filas.add(cur.toString())
  return filas
}

private fun partirLinea(linea: String): List<String> {
  val out = mutableListOf<String>()
  val cur = StringBuilder()
  var entreComillas = false
  var i = 0
  while (i < linea.length) {
    val c = linea[i]
    if (entreComillas) {
      if (c == '"') {
        if (i + 1 < linea.length && linea[i + 1] == '"') { cur.append('"'); i += 2; continue }
        entreComillas = false; i++; continue
      }
      cur.append(c); i++
    } else {
      when (c) {
        '"' -> { entreComillas = true; i++ }
        ',' -> { out.add(cur.toString()); cur.clear(); i++ }
        else -> { cur.append(c); i++ }
      }
    }
  }
  out.add(cur.toString())
  if (entreComillas) throw CsvInvalidoException("Comillas sin cerrar")
  return out
}

fun parsearCsvFilas(texto: String): List<FilaCsv> {
  val filas = partirFilas(texto).filter { it.isNotBlank() }
  if (filas.isEmpty() || filas[0].trim() != CSV_HEADER) throw CsvInvalidoException("Header inválido")
  return filas.drop(1).mapIndexed { n, linea ->
    val c = partirLinea(linea)
    if (c.size != 8) throw CsvInvalidoException("Fila ${n + 2}: se esperaban 8 columnas")
    val rid = c[0]; val rnombre = c[1]; val maq = c[2]
    val tiempo = c[3].toDoubleOrNull() ?: throw CsvInvalidoException("Fila ${n + 2}: tiempo inválido")
    val energia = c[4].toDoubleOrNull() ?: throw CsvInvalidoException("Fila ${n + 2}: energía inválida")
    val tipo = c[5]; val ref = c[6]
    val cant = c[7].toDoubleOrNull() ?: throw CsvInvalidoException("Fila ${n + 2}: cantidad inválida")
    if (rid.isBlank() || rnombre.isBlank() || ref.isBlank()) throw CsvInvalidoException("Fila ${n + 2}: campos vacíos")
    if (tipo != "mat" && tipo != "rec") throw CsvInvalidoException("Fila ${n + 2}: tipo inválido")
    val prefijo = if (tipo == "rec") "rec_" else "mat_"
    if (!ref.startsWith(prefijo)) throw CsvInvalidoException("Fila ${n + 2}: tipo no cuadra con el id")
    if (tiempo < 0 || energia < 0) throw CsvInvalidoException("Fila ${n + 2}: tiempo/energía negativos")
    if (cant <= 0) throw CsvInvalidoException("Fila ${n + 2}: cantidad inválida")
    FilaCsv(rid, rnombre, maq.ifBlank { "manual" }, tiempo, energia, tipo, ref, cant)
  }
}

fun filasARecetas(filas: List<FilaCsv>): List<Receta> {
  val grupos = linkedMapOf<String, MutableList<FilaCsv>>()
  filas.forEach { grupos.getOrPut(it.recetaId) { mutableListOf() }.add(it) }
  return grupos.values.map { g ->
    val p = g[0]
    if (g.any { it.recetaNombre != p.recetaNombre || it.maquina != p.maquina || it.tiempo != p.tiempo || it.energia != p.energia }) {
      throw CsvInvalidoException("Plano inconsistente en: ${p.recetaId}")
    }
    Receta(p.recetaId, p.recetaNombre, g.map { IngredienteRef(it.refId, it.cantidad) }, p.maquina, p.tiempo, p.energia)
  }
}

// Valida todo ANTES de tocar la base; un CSV corrupto o vacio no borra nada.
suspend fun restaurarRespaldo(repo: RepositorioRecetas, texto: String): Int {
  val recetas = filasARecetas(parsearCsvFilas(texto))
  if (recetas.isEmpty()) throw CsvInvalidoException("Respaldo vacío")
  repo.reemplazarTodo(recetas)
  return recetas.size
}
