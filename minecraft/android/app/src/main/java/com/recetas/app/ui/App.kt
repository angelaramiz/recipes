package com.recetas.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.pm.PackageInfoCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.recetas.app.Datos
import com.recetas.app.PrefsStore
import com.recetas.app.RepositorioRecetas
import com.recetas.app.VersionRemota
import com.recetas.app.chequearActualizacion
import com.recetas.app.convertir
import com.recetas.app.descargarEInstalar
import com.recetas.app.hayActualizacion
import kotlinx.coroutines.launch

enum class Tab(val titulo: String) {
  CALCULAR("Calcular"), RECETAS("Recetas"), MATERIALES("Materiales"), HISTORIAL("Historial")
}

@Composable
fun App(repo: RepositorioRecetas, prefs: PrefsStore) {
  var tab by remember { mutableStateOf(Tab.CALCULAR) }
  val vm: RecetasViewModel = viewModel(factory = remember(repo) { VMFactory(repo) })
  val scope = rememberCoroutineScope()
  val ctx = LocalContext.current
  var update by remember { mutableStateOf<VersionRemota?>(null) }
  var progreso by remember { mutableStateOf<Float?>(null) }
  var otaMsg by remember { mutableStateOf<String?>(null) }
  LaunchedEffect(Unit) {
    vm.cargar()
    try {
      val info = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
      val local = PackageInfoCompat.getLongVersionCode(info).toInt()
      val rem = chequearActualizacion()
      if (hayActualizacion(local, rem)) update = rem
    } catch (e: Exception) { /* sin version local: no OTA */ }
  }
  Column(Modifier.fillMaxSize().padding(16.dp)) {
    Text("Calculadora de Recursos", style = MaterialTheme.typography.headlineSmall)
    val u = update
    if (u != null) {
      Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text("Nueva versión ${u.versionName} disponible")
        otaMsg?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(
          onClick = {
            otaMsg = null
            scope.launch {
              val err = descargarEInstalar(ctx, u.apkUrl) { p -> progreso = p }
              if (err != null) otaMsg = err else { update = null; progreso = null }
            }
          },
          enabled = progreso == null,
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(progreso?.let { "Descargando… ${(it * 100).toInt()}%" } ?: "Descargar e instalar")
        }
      }
    }
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
      Tab.values().forEach { t ->
        Button(
          onClick = { tab = t },
          modifier = Modifier.weight(1f),
          enabled = tab != t,
          contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
        ) { Text(t.titulo, maxLines = 1, fontSize = 11.sp) }
      }
    }
    when (tab) {
      Tab.CALCULAR -> CalcularScreen(prefs)
      Tab.RECETAS -> RecetasScreen(vm, prefs)
      Tab.MATERIALES -> MaterialesScreen(prefs)
      Tab.HISTORIAL -> HistorialScreen(prefs, tab)
    }
  }
}
