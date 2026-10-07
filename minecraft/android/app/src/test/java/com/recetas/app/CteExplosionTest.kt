package com.recetas.app

import java.sql.DriverManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// Valida el SQL EXACTO que usa el DAO (const compartida) sobre SQLite real.
// Sin esto, un error en la CTE solo apareceria en el telefono.
class CteExplosionTest {

  private fun db(): java.sql.Connection {
    val c = DriverManager.getConnection("jdbc:sqlite::memory:")
    c.createStatement().use { st ->
      st.executeUpdate("CREATE TABLE recetas(id TEXT PRIMARY KEY, nombre TEXT, maquina TEXT, tiempo REAL, energia REAL, cantidad REAL)")
      st.executeUpdate("CREATE TABLE ingredientes(rowId INTEGER PRIMARY KEY AUTOINCREMENT, padreId TEXT, refId TEXT, cantidad REAL)")
      st.executeUpdate("INSERT INTO recetas VALUES('r_small','Batery Small','mesa',10,5,1)")
      st.executeUpdate("INSERT INTO recetas VALUES('r_med','Batery Medium','horno',30,20,1)")
      st.executeUpdate("INSERT INTO recetas VALUES('r_triple','Triple','manual',0,0,2)")
      st.executeUpdate("INSERT INTO recetas VALUES('r_jumbo','Jumbo','manual',0,0,1)")
      st.executeUpdate("INSERT INTO ingredientes(padreId,refId,cantidad) VALUES('r_small','mat_fe',2)")
      st.executeUpdate("INSERT INTO ingredientes(padreId,refId,cantidad) VALUES('r_small','mat_rd',1)")
      st.executeUpdate("INSERT INTO ingredientes(padreId,refId,cantidad) VALUES('r_med','rec_r_small',4)")
      st.executeUpdate("INSERT INTO ingredientes(padreId,refId,cantidad) VALUES('r_triple','mat_ma',1)")
      st.executeUpdate("INSERT INTO ingredientes(padreId,refId,cantidad) VALUES('r_jumbo','rec_r_triple',3)")
    }
    return c
  }

  private fun explosion(c: java.sql.Connection, id: String): Map<String, Double> {
    val out = mutableMapOf<String, Double>()
    // Room usa :recetaId; JDBC usa ?. Misma consulta, distinto placeholder.
    c.prepareStatement(QUERY_EXPLOSION.replace(":recetaId", "?")).use { ps ->
      ps.setString(1, id)
      ps.executeQuery().use { rs ->
        while (rs.next()) out[rs.getString(1)] = rs.getDouble(2)
      }
    }
    return out
  }

  @Test fun `1 medium explota a 8 fe y 4 rd`() {
    db().use { c ->
      assertEquals(mapOf("fe" to 8.0, "rd" to 4.0), explosion(c, "r_med"))
    }
  }

  @Test fun `receta directa sin subrecetas`() {
    db().use { c ->
      assertEquals(mapOf("fe" to 2.0, "rd" to 1.0), explosion(c, "r_small"))
    }
  }

  @Test fun `lote redondea hacia arriba`() {
    db().use { c ->
      // 1 jumbo -> 3 triple (de a 2) -> ceil(3/2)=2 procesos -> 2 ma
      assertEquals(mapOf("ma" to 2.0), explosion(c, "r_jumbo"))
    }
  }

  @Test(timeout = 10000) fun `ciclo en datos no cuelga la consulta`() {
    db().use { c ->
      c.createStatement().use { st ->
        // ciclo artificial r_med -> r_med (el DAO real lo impide al escribir)
        st.executeUpdate("INSERT INTO ingredientes(padreId,refId,cantidad) VALUES('r_med','rec_r_med',1)")
      }
      val res = explosion(c, "r_med") // debe terminar por el tope de nivel
      assertTrue(res.containsKey("fe"))
    }
  }
}
