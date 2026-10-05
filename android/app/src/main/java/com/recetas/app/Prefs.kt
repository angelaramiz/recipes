package com.recetas.app

import android.content.Context
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

// Historial (max 20) y marca de version. Espejo de mc_history / mc_version.
@kotlinx.serialization.Serializable
data class EntradaHistorial(val texto: String, val resultado: String, val hora: String)

class PrefsStore(ctx: Context) {
  private val prefs = ctx.getSharedPreferences("mc", Context.MODE_PRIVATE)
  private val json = Json { ignoreUnknownKeys = true }
  private val histSer = ListSerializer(EntradaHistorial.serializer())

  fun versionGuardada(): String? = prefs.getString("mc_version", null)
  fun marcarVersion(v: String) { prefs.edit().putString("mc_version", v).apply() }

  fun historial(): List<EntradaHistorial> {
    val raw = prefs.getString("mc_history", null) ?: return emptyList()
    return try {
      json.decodeFromString<List<EntradaHistorial>>(raw)
    } catch (e: Exception) { emptyList() }
  }

  fun agregarHistorial(e: EntradaHistorial) {
    val lista = (listOf(e) + historial()).take(20)
    prefs.edit().putString("mc_history", json.encodeToString(histSer, lista)).apply()
  }

  fun limpiarHistorial() { prefs.edit().remove("mc_history").apply() }

  fun maquinasPropias(): Set<String> = prefs.getStringSet("mc_maquinas", emptySet()) ?: emptySet()
  fun setMaquinaPropia(id: String, propia: Boolean) {
    val s = maquinasPropias().toMutableSet()
    if (propia) s.add(id) else s.remove(id)
    prefs.edit().putStringSet("mc_maquinas", s).apply()
  }
}
