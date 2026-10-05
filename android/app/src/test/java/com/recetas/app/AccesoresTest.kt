package com.recetas.app

import org.junit.Assert.assertEquals
import org.junit.Test

private val MATS = listOf(Material("fe", "Hierro", "lingote"))
private val MAQS = listOf(Maquina("manual", "Crafteo manual"), Maquina("mesa", "Mesa de trabajo"))
private val RECS = listOf(Receta("r_small", "Batery Small", listOf(IngredienteRef("mat_fe", 2.0))))

class AccesoresTest {

  @Test fun `defaults para datos viejos sin plano`() {
    val vieja = Receta("r_old", "Vieja", listOf(IngredienteRef("mat_fe", 1.0)))
    assertEquals("manual", maquinaDe(vieja))
    assertEquals(0.0, tiempoDe(vieja), 0.0)
    assertEquals(0.0, energiaDe(vieja), 0.0)
  }

  @Test fun `nombreMaquina cae a manual si id desconocido`() {
    assertEquals("Mesa de trabajo", nombreMaquina("mesa", MAQS))
    assertEquals("Crafteo manual", nombreMaquina("inexistente", MAQS))
  }

  @Test fun `cantidadDe default 1 y respeta valor`() {
    assertEquals(1.0, cantidadDe(Receta("r_x", "X", emptyList())), 0.0)
    assertEquals(4.0, cantidadDe(Receta("r_x", "X", emptyList(), cantidad = 4.0)), 0.0)
    assertEquals(1.0, cantidadDe(Receta("r_x", "X", emptyList(), cantidad = 0.0)), 0.0)
  }

  @Test fun `nombreIngrediente resuelve mat_ y rec_`() {
    val mat = nombreIngrediente("mat_fe", MATS, RECS)
    assertEquals("Hierro", mat.nombre)
    assertEquals(false, mat.esReceta)
    val rec = nombreIngrediente("rec_r_small", MATS, RECS)
    assertEquals("Batery Small", rec.nombre)
    assertEquals(true, rec.esReceta)
  }

  @Test fun `nombreIngrediente tolera ids desconocidos y sin prefijo`() {
    assertEquals("mat_xx", nombreIngrediente("mat_xx", MATS, RECS).nombre)
    assertEquals("Hierro", nombreIngrediente("fe", MATS, RECS).nombre)
  }
}
