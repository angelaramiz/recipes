package com.recetas.app

import kotlinx.serialization.Serializable

@Serializable
data class IngredienteRef(val id: String, val cantidad: Double)

@Serializable
data class Requisito(val tipo: String, val detalle: String)

val TIPOS_REQUISITO = listOf("estructura", "bioma", "dimension")

@Serializable
data class Receta(
  val id: String,
  val nombre: String,
  val ingredientes: List<IngredienteRef>,
  val maquina: String = "manual",
  val tiempo: Double = 0.0,
  val energia: Double = 0.0,
  // Planos Fase 4: unidades del producto por proceso + subproductos (solo mat_*).
  val cantidad: Double = 1.0,
  val subproductos: List<IngredienteRef> = emptyList(),
  val requisitos: List<Requisito> = emptyList()
)

data class Material(val codigo: String, val nombre: String, val tipo: String)
data class Maquina(val id: String, val nombre: String)
data class Conversion(val de: String, val a: String, val ratio: Double)
