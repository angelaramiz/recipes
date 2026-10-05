package com.recetas.app

import kotlin.math.ceil

data class TotalMaterial(val codigo: String, val nombre: String, var cantidad: Double)
data class TotalMaquina(val id: String, val nombre: String, var tiempo: Double)
data class Resolucion(
  val materiales: List<TotalMaterial>,
  val tiempo: Double,
  val energia: Double,
  val maquinas: List<TotalMaquina>,
  val subproductos: List<TotalMaterial> = emptyList()
)

fun cuelloDeBotella(res: Resolucion): TotalMaquina? = res.maquinas.maxByOrNull { it.tiempo }

fun resolverReceta(
  receta: Receta,
  necesidad: Double,
  recetas: List<Receta>,
  materiales: List<Material>,
  maquinas: List<Maquina>
): Resolucion {
  val credito = mutableMapOf<String, Double>()
  val r = resolverInterno(receta, necesidad, recetas, materiales, maquinas, mutableSetOf(), credito)
  val subs = credito.filter { it.value > 0 }.map { (codigo, cant) ->
    TotalMaterial(codigo, materiales.firstOrNull { it.codigo == codigo }?.nombre ?: codigo, cant)
  }
  return r.copy(subproductos = subs)
}

private fun resolverInterno(
  receta: Receta,
  necesidad: Double,
  recetas: List<Receta>,
  materiales: List<Material>,
  maquinas: List<Maquina>,
  visitados: MutableSet<String>,
  credito: MutableMap<String, Double>
): Resolucion {
  val runs = ceil(necesidad / cantidadDe(receta))
  val mats = mutableListOf<TotalMaterial>()
  var tiempo = tiempoDe(receta) * runs
  var energia = energiaDe(receta) * runs
  val maqs = mutableListOf<TotalMaquina>()
  val mid = maquinaDe(receta)
  if (mid != "manual" && tiempoDe(receta) > 0) {
    maqs.add(TotalMaquina(mid, nombreMaquina(mid, maquinas), tiempoDe(receta) * runs))
  }
  val vistos = visitados.toMutableSet()
  vistos.add(receta.id)
  for (ing in receta.ingredientes) {
    val cant = ing.cantidad * runs
    if (ing.id.startsWith("rec_")) {
      val subId = ing.id.removePrefix("rec_")
      if (subId in vistos) continue
      val sub = recetas.firstOrNull { it.id == subId } ?: continue
      val r = resolverInterno(sub, cant, recetas, materiales, maquinas, vistos, credito)
      for (m in r.materiales) {
        mats.firstOrNull { it.codigo == m.codigo }?.let { it.cantidad += m.cantidad }
          ?: mats.add(m.copy())
      }
      tiempo += r.tiempo
      energia += r.energia
      for (q in r.maquinas) {
        maqs.firstOrNull { it.id == q.id }?.let { it.tiempo += q.tiempo }
          ?: maqs.add(q.copy())
      }
    } else {
      val codigo = ing.id.removePrefix("mat_")
      val mat = materiales.firstOrNull { it.codigo == codigo } ?: continue
      // Acredita subproductos generados antes de pedir material nuevo.
      val disp = credito.getOrDefault(codigo, 0.0)
      val usa = minOf(disp, cant)
      credito[codigo] = disp - usa
      val resto = cant - usa
      if (resto > 0) {
        mats.firstOrNull { it.codigo == codigo }?.let { it.cantidad += resto }
          ?: mats.add(TotalMaterial(codigo, mat.nombre, resto))
      }
    }
  }
  // Subproductos propios se suman al credito (no se autoconsumen en este nivel).
  for (sub in receta.subproductos) {
    val codigo = sub.id.removePrefix("mat_")
    credito[codigo] = credito.getOrDefault(codigo, 0.0) + sub.cantidad * runs
  }
  return Resolucion(mats, tiempo, energia, maqs)
}

data class Alternativa(val receta: Receta, val resolucion: Resolucion)

// Rutas alternativas v1: recetas con el mismo nombre (ignora mayusculas y
// espacios) ordenadas por tiempo total. Sin mapeo producto->recetas por ahora.
fun rutasAlternativas(
  nombre: String,
  recetas: List<Receta>,
  materiales: List<Material>,
  maquinas: List<Maquina>
): List<Alternativa> {
  val clave = nombre.trim().lowercase()
  return recetas.filter { it.nombre.trim().lowercase() == clave }
    .map { Alternativa(it, resolverReceta(it, 1.0, recetas, materiales, maquinas)) }
    .sortedBy { it.resolucion.tiempo }
}

fun detectarCiclo(
  ingredientes: List<IngredienteRef>,
  recetaActualId: String?,
  recetas: List<Receta>,
  visitados: MutableSet<String> = mutableSetOf()
): Boolean {
  for (ing in ingredientes) {
    if (!ing.id.startsWith("rec_")) continue
    val recId = ing.id.removePrefix("rec_")
    if (recId == recetaActualId) return true
    if (!visitados.add(recId)) continue
    val rec = recetas.firstOrNull { it.id == recId } ?: continue
    if (detectarCiclo(rec.ingredientes, recetaActualId, recetas, visitados)) return true
  }
  return false
}
