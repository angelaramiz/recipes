package com.recetas.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private val CONVS = listOf(
  Conversion("pepita", "lingote", 1.0 / 9.0),
  Conversion("lingote", "pepita", 9.0),
  Conversion("lingote", "bloque", 1.0 / 9.0),
  Conversion("bloque", "lingote", 9.0),
  Conversion("pepita", "bloque", 1.0 / 81.0),
  Conversion("bloque", "pepita", 81.0)
)

class ConvertirTest {

  @Test fun `9 pepitas son 1 lingote y 1 bloque son 9 lingotes`() {
    assertEquals(1.0, convertir(9.0, "pepita", "lingote", CONVS)!!, 0.0001)
    assertEquals(9.0, convertir(1.0, "bloque", "lingote", CONVS)!!, 0.0001)
    assertEquals(18.0, convertir(2.0, "lingote", "pepita", CONVS)!!, 0.0001)
  }

  @Test fun `sin conversion o mismo tipo devuelve null`() {
    assertNull(convertir(1.0, "pepita", "pepita", CONVS))
    assertNull(convertir(1.0, "pepita", "gema", CONVS))
  }
}
