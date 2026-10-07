package com.recetas.app

import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

class RecetaInvalidaException(msg: String) : IllegalArgumentException(msg)

private val json = Json { ignoreUnknownKeys = true; isLenient = false }
private val recetaListSer = ListSerializer(Receta.serializer())

fun parsearRecetas(texto: String): List<Receta> {
  val lista = try {
    json.decodeFromString(recetaListSer, texto)
  } catch (e: SerializationException) {
    throw RecetaInvalidaException("Formato inválido: ${e.message}")
  }
  for (r in lista) {
    if (r.id.isBlank() || r.nombre.isBlank()) throw RecetaInvalidaException("Receta sin id o nombre")
    if (r.ingredientes.isEmpty()) throw RecetaInvalidaException("Sin ingredientes: ${r.nombre}")
    for (ing in r.ingredientes) {
      if (ing.id.isBlank() || ing.cantidad <= 0) throw RecetaInvalidaException("Ingrediente inválido en: ${r.nombre}")
    }
    if (r.tiempo < 0 || r.energia < 0) throw RecetaInvalidaException("Tiempo/energía inválidos en: ${r.nombre}")
    if (r.cantidad <= 0) throw RecetaInvalidaException("Cantidad inválida en: ${r.nombre}")
    for (sub in r.subproductos) {
      if (!sub.id.startsWith("mat_") || sub.cantidad <= 0) {
        throw RecetaInvalidaException("Subproducto inválido en: ${r.nombre} (solo mat_*)")
      }
    }
    for (req in r.requisitos) {
      if (req.tipo !in TIPOS_REQUISITO || req.detalle.isBlank()) {
        throw RecetaInvalidaException("Requisito inválido en: ${r.nombre}")
      }
    }
  }
  return lista
}

fun serializarRecetas(recetas: List<Receta>): String =
  json.encodeToString(recetaListSer, recetas)
