package com.recetas.app

fun maquinaDe(r: Receta): String = if (r.maquina.isNotBlank()) r.maquina else "manual"
fun tiempoDe(r: Receta): Double = if (r.tiempo > 0) r.tiempo else 0.0
fun energiaDe(r: Receta): Double = if (r.energia > 0) r.energia else 0.0
fun cantidadDe(r: Receta): Double = if (r.cantidad > 0) r.cantidad else 1.0

fun nombreMaquina(id: String, maquinas: List<Maquina>): String =
  maquinas.firstOrNull { it.id == id }?.nombre ?: "Crafteo manual"

data class NombreIngrediente(val nombre: String, val esReceta: Boolean)

fun nombreIngrediente(id: String, materiales: List<Material>, recetas: List<Receta>): NombreIngrediente {
  if (id.startsWith("rec_")) {
    val recId = id.removePrefix("rec_")
    val rec = recetas.firstOrNull { it.id == recId }
    return NombreIngrediente(rec?.nombre ?: id, true)
  }
  val codigo = id.removePrefix("mat_") // tolera ids viejos sin prefijo
  val mat = materiales.firstOrNull { it.codigo == codigo }
  return NombreIngrediente(mat?.nombre ?: id, false)
}
