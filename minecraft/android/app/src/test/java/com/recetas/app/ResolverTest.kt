package com.recetas.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private val MATS = listOf(
  Material("fe", "Hierro", "lingote"),
  Material("rd", "Redstone", "polvo"),
  Material("au", "Oro", "lingote"),
  Material("es", "Escoria", "polvo"),
  Material("ma", "Materia A", "polvo")
)
private val MAQS = listOf(Maquina("manual", "Crafteo manual"), Maquina("mesa", "Mesa de trabajo"), Maquina("horno", "Horno"), Maquina("fundicion", "Fundición"))

private val SMALL = Receta("r_small", "Batery Small",
  listOf(IngredienteRef("mat_fe", 2.0), IngredienteRef("mat_rd", 1.0)), "mesa", 10.0, 5.0)
private val MED = Receta("r_med", "Batery Medium",
  listOf(IngredienteRef("rec_r_small", 4.0)), "horno", 30.0, 20.0)
private val OLD = Receta("r_old", "Receta Vieja", listOf(IngredienteRef("mat_au", 1.0)))
private val TODAS = listOf(SMALL, MED, OLD)

class ResolverTest {

  @Test fun `acumula materiales tiempo y energia multinivel`() {
    val res = resolverReceta(MED, 1.0, TODAS, MATS, MAQS)
    assertEquals(8.0, res.materiales.first { it.codigo == "fe" }.cantidad, 0.0)
    assertEquals(4.0, res.materiales.first { it.codigo == "rd" }.cantidad, 0.0)
    assertEquals(70.0, res.tiempo, 0.0) // 30 + 4x10
    assertEquals(40.0, res.energia, 0.0) // 20 + 4x5
  }

  @Test fun `maquinas acumuladas y cuello de botella es la de mayor tiempo`() {
    val res = resolverReceta(MED, 1.0, TODAS, MATS, MAQS)
    assertEquals(40.0, res.maquinas.first { it.id == "mesa" }.tiempo, 0.0)
    assertEquals(30.0, res.maquinas.first { it.id == "horno" }.tiempo, 0.0)
    assertEquals("mesa", cuelloDeBotella(res)!!.id)
  }

  @Test fun `receta vieja sin plano da cero y sin bottleneck`() {
    val res = resolverReceta(OLD, 1.0, TODAS, MATS, MAQS)
    assertEquals(0.0, res.tiempo, 0.0)
    assertTrue(res.maquinas.isEmpty())
    assertNull(cuelloDeBotella(res))
  }

  @Test fun `detectarCiclo frena directo y transitivo`() {
    assertTrue(detectarCiclo(listOf(IngredienteRef("rec_r_med", 1.0)), "r_med", TODAS))
    val a = Receta("r_a", "A", listOf(IngredienteRef("rec_r_b", 1.0)))
    val b = Receta("r_b", "B", listOf(IngredienteRef("rec_r_a", 1.0)))
    assertTrue(detectarCiclo(listOf(IngredienteRef("rec_r_b", 1.0)), "r_a", listOf(a, b)))
    assertTrue(!detectarCiclo(listOf(IngredienteRef("mat_fe", 1.0)), "r_a", listOf(a, b)))
  }

  @Test fun `lote redondea hacia arriba las necesidades`() {
    val triple = Receta("r_triple", "Triple", listOf(IngredienteRef("mat_ma", 1.0)), cantidad = 2.0)
    val jumbo = Receta("r_jumbo", "Jumbo", listOf(IngredienteRef("rec_r_triple", 3.0)))
    val res = resolverReceta(jumbo, 1.0, listOf(triple, jumbo), MATS, MAQS)
    // 1 jumbo -> 3 triple -> ceil(3/2)=2 procesos -> 2 materia A
    assertEquals(2.0, res.materiales.first { it.codigo == "ma" }.cantidad, 0.0)
  }

  @Test fun `subproductos acreditan consumo y dejan sobrante`() {
    val acero = Receta(
      "r_acero", "Acero", listOf(IngredienteRef("mat_fe", 2.0)),
      "fundicion", 10.0, 0.0, subproductos = listOf(IngredienteRef("mat_es", 1.0))
    )
    val sup = Receta(
      "r_sup", "Super",
      listOf(IngredienteRef("rec_r_acero", 2.0), IngredienteRef("mat_es", 1.0))
    )
    val res = resolverReceta(sup, 1.0, listOf(acero, sup), MATS, MAQS)
    // 2 acero -> 4 fe + 2 escoria; 1 escoria pedida se acredita -> sobra 1
    assertEquals(4.0, res.materiales.first { it.codigo == "fe" }.cantidad, 0.0)
    assertTrue(res.materiales.none { it.codigo == "es" })
    assertEquals(1.0, res.subproductos.first { it.codigo == "es" }.cantidad, 0.0)
  }

  @Test fun `alternativas agrupa por nombre y ordena por tiempo`() {
    val rapida = Receta("r_a", "Hierro X", listOf(IngredienteRef("mat_fe", 1.0)), "horno", 5.0, 0.0)
    val lenta = Receta("r_b", "hierro x ", listOf(IngredienteRef("mat_fe", 1.0)), "horno", 50.0, 0.0)
    val alts = rutasAlternativas("Hierro X", listOf(rapida, lenta), MATS, MAQS)
    assertEquals(listOf("r_a", "r_b"), alts.map { it.receta.id })
  }
}
