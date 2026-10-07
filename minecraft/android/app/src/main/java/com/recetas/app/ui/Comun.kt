package com.recetas.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

fun fmtNum(d: Double): String = if (d % 1.0 == 0.0) d.toLong().toString() else d.toString()

@Composable
fun ComboBox(
  opciones: List<Pair<String, String>>,
  valor: String,
  onCambio: (String) -> Unit,
  etiqueta: String,
  modifier: Modifier = Modifier
) {
  var abierto by remember { mutableStateOf(false) }
  Box(modifier) {
    OutlinedTextField(
      value = opciones.firstOrNull { it.first == valor }?.second ?: "",
      onValueChange = {},
      readOnly = true,
      label = { Text(etiqueta) },
      modifier = Modifier.fillMaxWidth().clickable { abierto = true }
    )
    DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
      opciones.forEach { (id, nombre) ->
        DropdownMenuItem(text = { Text(nombre) }, onClick = { onCambio(id); abierto = false })
      }
    }
  }
}

@Composable
fun FilaDato(nombre: String, valor: String) {
  Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
    Text(nombre)
    Text(valor, fontWeight = FontWeight.Bold)
  }
}
