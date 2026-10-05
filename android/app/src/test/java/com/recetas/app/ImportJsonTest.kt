package com.recetas.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ImportJsonTest {

  @Test fun `parsea formato nuevo con plano`() {
    val json = """[{"id":"r_1","nombre":"Bateria","ingredientes":[{"id":"mat_fe","cantidad":3}],"maquina":"horno","tiempo":30,"energia":5}]"""
    val r = parsearRecetas(json)
    assertEquals(1, r.size)
    assertEquals("horno", r[0].maquina)
    assertEquals(30.0, r[0].tiempo, 0.0)
  }

  @Test fun `formato viejo sin plano usa defaults`() {
    val json = """[{"id":"r_1","nombre":"Vieja","ingredientes":[{"id":"mat_fe","cantidad":1}]}]"""
    val r = parsearRecetas(json)
    assertEquals("manual", r[0].maquina)
    assertEquals(0.0, r[0].tiempo, 0.0)
  }

  @Test fun `parsea cantidad subproductos y requisitos`() {
    val json = """[{"id":"r_1","nombre":"Acero","ingredientes":[{"id":"mat_fe","cantidad":2}],"maquina":"fundicion","tiempo":10,"cantidad":2,"subproductos":[{"id":"mat_es","cantidad":1}],"requisitos":[{"tipo":"estructura","detalle":"Multibloque 3x3"}]}]"""
    val r = parsearRecetas(json)
    assertEquals(2.0, r[0].cantidad, 0.0)
    assertEquals(1, r[0].subproductos.size)
    assertEquals("estructura", r[0].requisitos[0].tipo)
  }

  @Test fun `rechaza cantidad y subproductos invalidos`() {
    assertThrows(IllegalArgumentException::class.java) {
      parsearRecetas("""[{"id":"r_1","nombre":"X","ingredientes":[{"id":"mat_fe","cantidad":1}],"cantidad":0}]""")
    }
    assertThrows(IllegalArgumentException::class.java) {
      parsearRecetas("""[{"id":"r_1","nombre":"X","ingredientes":[{"id":"mat_fe","cantidad":1}],"subproductos":[{"id":"rec_r_2","cantidad":1}]}]""")
    }
    assertThrows(IllegalArgumentException::class.java) {
      parsearRecetas("""[{"id":"r_1","nombre":"X","ingredientes":[{"id":"mat_fe","cantidad":1}],"requisitos":[{"tipo":"nave","detalle":"?"}]}]""")
    }
  }

  @Test fun `rechaza recetas invalidas`() {    assertThrows(IllegalArgumentException::class.java) {
      parsearRecetas("""[{"id":"r_1","ingredientes":[]}]""") // sin nombre
    }
    assertThrows(IllegalArgumentException::class.java) {
      parsearRecetas("""[{"id":"r_1","nombre":"X","ingredientes":[{"id":"mat_fe","cantidad":0}]}]""")
    }
    assertThrows(IllegalArgumentException::class.java) {
      parsearRecetas("""[{"id":"r_1","nombre":"X","ingredientes":[],"tiempo":-5}]""")
    }
    assertThrows(IllegalArgumentException::class.java) {
      parsearRecetas("""{"no":"es un arreglo"}""")
    }
  }
}
