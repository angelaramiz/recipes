package com.recetas.app

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

private val SMALL = Receta(
  "r_small", "Batery, \"Small\"\nEdición",
  listOf(IngredienteRef("mat_fe", 2.0)), "mesa", 10.0, 5.0
)
private val MED = Receta(
  "r_med", "Batery Medium",
  listOf(IngredienteRef("rec_r_small", 4.0), IngredienteRef("mat_rd", 1.0)), "horno", 30.0, 20.0
)

private class FakeRepo(var datos: List<Receta> = emptyList(), val llamadas: MutableList<String> = mutableListOf()) : RepositorioRecetas {
  override suspend fun todas(): List<Receta> = datos
  override suspend fun guardar(r: Receta) { llamadas.add("guardar"); datos = datos + r }
  override suspend fun borrar(id: String) { llamadas.add("borrar"); datos = datos.filter { it.id != id } }
  override suspend fun reemplazarTodo(rs: List<Receta>) { llamadas.add("reemplazarTodo"); datos = rs }
  override suspend fun baseNecesaria(id: String): List<TotalMaterial> = emptyList()
}

class CsvBackupTest {

  @Test fun `generarCsv escribe header y una fila por arista`() {
    val filas = parsearCsvFilas(generarCsv(listOf(SMALL, MED)))
    assertEquals(3, filas.size) // small 1 arista + med 2 aristas
  }

  @Test fun `generarCsv escapa RFC4180`() {
    val csv = generarCsv(listOf(SMALL))
    assertTrue(csv.contains("r_small,\"Batery, \"\"Small\"\"\nEdición\",mesa,10.0,5.0,mat,mat_fe,2.0"))
  }

  @Test fun `round trip conserva todo`() {
    val original = listOf(SMALL, MED)
    assertEquals(original, filasARecetas(parsearCsvFilas(generarCsv(original))))
  }

  @Test fun `parseo rechaza header columnas cantidades y tipo`() {
    assertThrows(IllegalArgumentException::class.java) { parsearCsvFilas("otro,header\n1,2") }
    assertThrows(IllegalArgumentException::class.java) {
      parsearCsvFilas(CSV_HEADER + "\nr_1,X,manual,0,0,mat,mat_fe")
    }
    assertThrows(IllegalArgumentException::class.java) {
      parsearCsvFilas(CSV_HEADER + "\nr_1,X,manual,0,0,mat,mat_fe,0")
    }
    assertThrows(IllegalArgumentException::class.java) {
      parsearCsvFilas(CSV_HEADER + "\nr_1,X,manual,-1,0,mat,mat_fe,1")
    }
    assertThrows(IllegalArgumentException::class.java) {
      parsearCsvFilas(CSV_HEADER + "\nr_1,X,manual,0,0,rec,mat_fe,1") // tipo no cuadra con prefijo
    }
  }

  @Test fun `rechaza plano inconsistente en la misma receta`() {
    val csv = CSV_HEADER +
      "\nr_1,X,horno,30,0,mat,mat_fe,1" +
      "\nr_1,X,mesa,30,0,mat,mat_rd,1"
    assertThrows(IllegalArgumentException::class.java) { filasARecetas(parsearCsvFilas(csv)) }
  }

  @Test fun `restaurarRespaldo reemplaza todo en una llamada`() {
    val fake = FakeRepo(listOf(SMALL))
    val n = runBlocking { restaurarRespaldo(fake, generarCsv(listOf(MED))) }
    assertEquals(1, n)
    assertEquals(listOf("reemplazarTodo"), fake.llamadas)
    assertEquals(listOf(MED), runBlocking { fake.todas() })
  }

  @Test fun `csv corrupto no toca la base`() {
    val fake = FakeRepo()
    assertThrows(IllegalArgumentException::class.java) {
      runBlocking { restaurarRespaldo(fake, "basura total") }
    }
    assertEquals(emptyList<String>(), fake.llamadas)
  }

  @Test fun `respaldo vacio no borra nada`() {
    val fake = FakeRepo(listOf(SMALL))
    assertThrows(IllegalArgumentException::class.java) {
      runBlocking { restaurarRespaldo(fake, CSV_HEADER + "\n") }
    }
    assertEquals(emptyList<String>(), fake.llamadas)
    assertEquals(listOf(SMALL), runBlocking { fake.todas() })
  }
}
