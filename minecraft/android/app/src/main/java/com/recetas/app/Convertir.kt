package com.recetas.app

fun convertir(cantidad: Double, de: String, a: String, conversiones: List<Conversion>): Double? {
  if (de == a) return null
  val conv = conversiones.firstOrNull { it.de == de && it.a == a } ?: return null
  return cantidad * conv.ratio
}
