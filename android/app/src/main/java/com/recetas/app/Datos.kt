package com.recetas.app

// Datos fijos, espejo de la web. Ratios = unidades de SALIDA por 1 de entrada
// (9 pepitas -> 1 lingote). OJO: la web tiene los ratios invertidos (bug
// conocido, pendiente de corregir alla); aqui van correctos segun Minecraft.
object Datos {
  val materiales = listOf(
    Material("fe", "Hierro", "lingote"),
    Material("au", "Oro", "lingote"),
    Material("cu", "Cobre", "lingote"),
    Material("nq", "Netherite", "lingote"),
    Material("rd", "Redstone", "polvo"),
    Material("pv", "Polvora", "polvo"),
    Material("di", "Diamante", "gema"),
    Material("em", "Esmeralda", "gema"),
    Material("la", "Lapislazuli", "gema"),
    Material("qi", "Cuarzo del Nether", "gema"),
    Material("am", "Amatista", "gema")
  )

  val conversiones = listOf(
    Conversion("pepita", "lingote", 1.0 / 9.0),
    Conversion("lingote", "pepita", 9.0),
    Conversion("lingote", "bloque", 1.0 / 9.0),
    Conversion("bloque", "lingote", 9.0),
    Conversion("pepita", "bloque", 1.0 / 81.0),
    Conversion("bloque", "pepita", 81.0)
  )

  val typeOptions = mapOf(
    "lingote" to listOf("pepita", "lingote", "bloque"),
    "polvo" to listOf("polvo"),
    "gema" to listOf("gema")
  )

  val unitNames = mapOf(
    "pepita" to "Pepita(s)", "lingote" to "Lingote(s)", "bloque" to "Bloque(s)",
    "polvo" to "Polvo(s)", "gema" to "Gema(s)"
  )

  val maquinas = listOf(
    Maquina("manual", "Crafteo manual"),
    Maquina("mesa", "Mesa de trabajo"),
    Maquina("horno", "Horno"),
    Maquina("fundicion", "Alto horno / Fundición"),
    Maquina("yunque", "Yunque"),
    Maquina("encantamiento", "Mesa de encantamientos"),
    Maquina("alquimia", "Soporte de alquimia")
  )
}
