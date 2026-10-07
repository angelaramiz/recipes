package com.recetas.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class OtaTest {

  @Test fun `detecta version mayor`() {
    assertEquals(true, hayActualizacion(1, VersionRemota(2, "1.0.1", "/recetas.apk")))
    assertEquals(false, hayActualizacion(2, VersionRemota(2, "1.0.1", "/recetas.apk")))
    assertEquals(false, hayActualizacion(3, VersionRemota(2, "1.0.1", "/recetas.apk")))
    assertEquals(false, hayActualizacion(1, null))
  }

  @Test fun `parsea version remota valida`() {
    val v = parseVersionRemota("""{"versionCode":2,"versionName":"1.0.1","apkUrl":"/recetas.apk"}""")
    assertEquals(2, v.versionCode)
    assertEquals("/recetas.apk", v.apkUrl)
  }

  @Test fun `rechaza json invalido`() {
    assertThrows(IllegalArgumentException::class.java) { parseVersionRemota("no json") }
    assertThrows(IllegalArgumentException::class.java) { parseVersionRemota("""{"versionName":"x"}""") }
  }
}
