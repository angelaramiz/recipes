package com.recetas.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.recetas.app.Datos
import com.recetas.app.PrefsStore

@Composable
fun MaterialesScreen(prefs: PrefsStore) {
  var propias by remember { mutableStateOf(prefs.maquinasPropias()) }
  LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    item {
      Text("Mi taller (máquinas que tengo)", style = MaterialTheme.typography.titleMedium)
      Datos.maquinas.filter { it.id != "manual" }.forEach { m ->
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
          Checkbox(propias.contains(m.id), onCheckedChange = {
            prefs.setMaquinaPropia(m.id, it)
            propias = prefs.maquinasPropias()
          })
          Text(m.nombre, modifier = Modifier.padding(start = 4.dp))
        }
      }
    }
    item {
      Text("Materiales", style = MaterialTheme.typography.titleMedium)
      Datos.materiales.forEach { m -> FilaDato("${m.codigo} — ${m.nombre}", m.tipo) }
    }
    item {
      Text("Conversiones disponibles", style = MaterialTheme.typography.titleMedium)
      Datos.conversiones.filter { it.ratio >= 1 }.forEach { c ->
        FilaDato("${c.de} → ${c.a}", "1:${fmtNum(c.ratio)}")
      }
    }
    item {
      Text("Máquinas (planos)", style = MaterialTheme.typography.titleMedium)
      Datos.maquinas.forEach { m -> FilaDato(m.id, m.nombre) }
    }
  }
}

@Composable
fun HistorialScreen(prefs: PrefsStore, tab: Tab) {
  var items by remember { mutableStateOf(prefs.historial()) }
  LaunchedEffect(tab) { items = prefs.historial() }
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text("Historial", style = MaterialTheme.typography.titleMedium)
    if (items.isEmpty()) Text("Aún no hay cálculos")
    items.forEach { h ->
      Column(Modifier.padding(vertical = 4.dp)) {
        Text(h.texto)
        Text(h.resultado, style = MaterialTheme.typography.bodyMedium)
      }
    }
    Button(onClick = { prefs.limpiarHistorial(); items = emptyList() }) { Text("Limpiar historial") }
  }
}
