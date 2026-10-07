package com.recetas.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.recetas.app.Datos
import com.recetas.app.EntradaHistorial
import com.recetas.app.PrefsStore
import com.recetas.app.convertir
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CalcularScreen(prefs: PrefsStore) {
  var tipo by remember { mutableStateOf("") }
  var de by remember { mutableStateOf("") }
  var a by remember { mutableStateOf("") }
  var material by remember { mutableStateOf("") }
  var cantidadTxt by remember { mutableStateOf("1") }
  var resultado by remember { mutableStateOf<String?>(null) }
  var error by remember { mutableStateOf<String?>(null) }

  val tipos = listOf("lingote" to "Lingote (Hierro, Oro, Cobre...)", "polvo" to "Polvo (Redstone, Pólvora...)", "gema" to "Gema (Diamante, Esmeralda...)")
  val unidades = Datos.typeOptions[tipo] ?: emptyList()
  val mats = Datos.materiales.filter { it.tipo == tipo }

  Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text("Conversión bidireccional", style = MaterialTheme.typography.titleMedium)
    ComboBox(tipos, tipo, { tipo = it; de = ""; a = ""; material = "" }, "Tipo de material")
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      ComboBox(unidades.map { it to it.replaceFirstChar(Char::titlecase) }, de, { de = it }, "De", Modifier.weight(1f))
      ComboBox(unidades.map { it to it.replaceFirstChar(Char::titlecase) }, a, { a = it }, "A", Modifier.weight(1f))
    }
    ComboBox(mats.map { it.codigo to "${it.nombre} (${it.codigo})" }, material, { material = it }, "Material")
    OutlinedTextField(cantidadTxt, { cantidadTxt = it }, label = { Text("Cantidad") }, modifier = Modifier.fillMaxWidth())
    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    Button(onClick = {
      error = null
      resultado = null
      val qty = cantidadTxt.toDoubleOrNull()
      val mat = mats.firstOrNull { it.codigo == material }
      if (tipo.isBlank() || de.isBlank() || a.isBlank() || mat == null || qty == null || qty < 1) {
        error = "Completa todos los campos"; return@Button
      }
      val r = convertir(qty, de, a, Datos.conversiones)
      if (r == null) { error = if (de == a) "Selecciona tipos diferentes" else "Conversión no encontrada"; return@Button }
      val rf = fmtNum(r)
      resultado = "$qty ${Datos.unitNames[de]} de ${mat.nombre} = $rf ${Datos.unitNames[a]}"
      val hora = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
      prefs.agregarHistorial(EntradaHistorial("$qty $de -> $a (${mat.nombre})", "$rf $a", hora))
    }, modifier = Modifier.fillMaxWidth()) { Text("Calcular") }
    resultado?.let {
      Text("Necesitas", style = MaterialTheme.typography.labelMedium)
      Text(it, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 4.dp))
    }
  }
}
