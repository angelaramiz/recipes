package com.recetas.app.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.recetas.app.Datos
import com.recetas.app.IngredienteRef
import com.recetas.app.PrefsStore
import com.recetas.app.Receta
import com.recetas.app.Requisito
import com.recetas.app.TIPOS_REQUISITO
import com.recetas.app.TotalMaterial
import com.recetas.app.cuelloDeBotella
import com.recetas.app.rutasAlternativas
import com.recetas.app.energiaDe
import com.recetas.app.generarCsv
import com.recetas.app.maquinaDe
import com.recetas.app.nombreIngrediente
import com.recetas.app.nombreMaquina
import com.recetas.app.resolverReceta
import com.recetas.app.serializarRecetas
import com.recetas.app.tiempoDe
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

private sealed interface Pantalla {
  data object Lista : Pantalla
  data class Editor(val id: String?) : Pantalla
  data class Detalle(val id: String) : Pantalla
}

private class FilaIng(id: String, cant: String) {
  var id by mutableStateOf(id)
  var cant by mutableStateOf(cant)
}

private class FilaReq(tipo: String, detalle: String) {
  var tipo by mutableStateOf(tipo)
  var detalle by mutableStateOf(detalle)
}

@Composable
fun RecetasScreen(vm: RecetasViewModel, prefs: PrefsStore) {
  val recetas by vm.recetas.collectAsState()
  var pantalla by remember { mutableStateOf<Pantalla>(Pantalla.Lista) }
  var mensaje by remember { mutableStateOf<String?>(null) }
  var borrarId by remember { mutableStateOf<String?>(null) }
  var csvPendiente by remember { mutableStateOf<String?>(null) }
  val scope = rememberCoroutineScope()
  val ctx = LocalContext.current

  val importar = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
    if (uri == null) return@rememberLauncherForActivityResult
    scope.launch {
      mensaje = try {
        val texto = ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: ""
        "${vm.importar(texto)} receta(s) importada(s)"
      } catch (e: Exception) { "Error al importar: ${e.message}" }
    }
  }
  val exportar = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
    if (uri == null) return@rememberLauncherForActivityResult
    try {
      ctx.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(serializarRecetas(recetas)) }
      mensaje = "Exportado"
    } catch (e: Exception) { mensaje = "Error al exportar: ${e.message}" }
  }
  val exportarCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri: Uri? ->
    if (uri == null) return@rememberLauncherForActivityResult
    try {
      ctx.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(generarCsv(recetas)) }
      mensaje = "Respaldo CSV guardado"
    } catch (e: Exception) { mensaje = "Error al respaldar: ${e.message}" }
  }
  val restaurarCsv = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
    if (uri == null) return@rememberLauncherForActivityResult
    try {
      csvPendiente = ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
    } catch (e: Exception) { mensaje = "Error al leer: ${e.message}" }
  }

  BackHandler(enabled = pantalla != Pantalla.Lista) { pantalla = Pantalla.Lista }
  mensaje?.let { Text(it, color = MaterialTheme.colorScheme.primary) }

  when (val p = pantalla) {
    is Pantalla.Lista -> {
      Button(onClick = { pantalla = Pantalla.Editor(null) }, modifier = Modifier.fillMaxWidth()) { Text("+ Nueva receta") }
      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
          onClick = {
            if (recetas.isEmpty()) mensaje = "No hay recetas para exportar"
            else exportar.launch("recetas_${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())}.json")
          },
          modifier = Modifier.weight(1f)
        ) { Text("Exportar") }
        OutlinedButton(onClick = { importar.launch("application/json") }, modifier = Modifier.weight(1f)) { Text("Importar") }
      }
      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
          onClick = {
            if (recetas.isEmpty()) mensaje = "No hay recetas para respaldar"
            else {
              val v = ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "1.0.0"
              exportarCsv.launch("backup_v${v}_${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())}.csv")
            }
          },
          modifier = Modifier.weight(1f)
        ) { Text("Respaldo CSV") }
        OutlinedButton(onClick = { restaurarCsv.launch("text/csv") }, modifier = Modifier.weight(1f)) { Text("Restaurar CSV") }
      }
      LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(recetas, key = { it.id }) { r ->
          Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Text(r.nombre, style = MaterialTheme.typography.titleMedium)
            if (maquinaDe(r) != "manual") {
              Text("⚙ ${nombreMaquina(maquinaDe(r), Datos.maquinas)}" +
                (if (tiempoDe(r) > 0) " · ${fmtNum(tiempoDe(r))}s" else ""))
            }
            r.ingredientes.forEach { ing ->
              val n = nombreIngrediente(ing.id, Datos.materiales, recetas)
              Text("${fmtNum(ing.cantidad)} x ${n.nombre}")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              TextButton(onClick = { pantalla = Pantalla.Detalle(r.id) }) { Text("Ver") }
              TextButton(onClick = { pantalla = Pantalla.Editor(r.id) }) { Text("Editar") }
              TextButton(onClick = { borrarId = r.id }) { Text("Eliminar") }
            }
          }
        }
      }
    }
    is Pantalla.Editor -> EditorReceta(
      receta = recetas.firstOrNull { it.id == p.id },
      todas = recetas,
      onGuardar = { r -> scope.launch { val err = vm.guardar(r); if (err == null) pantalla = Pantalla.Lista else mensaje = err } },
      onCancelar = { pantalla = Pantalla.Lista }
    )
    is Pantalla.Detalle -> {
      val r = recetas.firstOrNull { it.id == p.id }
      if (r == null) pantalla = Pantalla.Lista
      else DetalleReceta(vm, prefs, r, recetas,
        onVolver = { pantalla = Pantalla.Lista },
        onVer = { pantalla = Pantalla.Detalle(it) })
    }
  }

  if (borrarId != null) {
    AlertDialog(
      onDismissRequest = { borrarId = null },
      confirmButton = { TextButton(onClick = { vm.borrar(borrarId!!); borrarId = null }) { Text("Eliminar") } },
      dismissButton = { TextButton(onClick = { borrarId = null }) { Text("Cancelar") } },
      title = { Text("Eliminar esta receta?") }
    )
  }

  if (csvPendiente != null) {
    AlertDialog(
      onDismissRequest = { csvPendiente = null },
      confirmButton = {
        TextButton(onClick = {
          val texto = csvPendiente
          csvPendiente = null
          scope.launch {
            mensaje = try {
              "${vm.restaurarCsv(texto!!)} receta(s) restaurada(s)"
            } catch (e: Exception) { "Respaldo inválido, no se cambió nada: ${e.message}" }
          }
        }) { Text("Restaurar") }
      },
      dismissButton = { TextButton(onClick = { csvPendiente = null }) { Text("Cancelar") } },
      title = { Text("Restaurar reemplaza TODAS las recetas. ¿Continuar?") }
    )
  }
}

@Composable
private fun EditorReceta(receta: Receta?, todas: List<Receta>, onGuardar: (Receta) -> Unit, onCancelar: () -> Unit) {
  var nombre by remember(receta?.id) { mutableStateOf(receta?.nombre ?: "") }
  var maquina by remember(receta?.id) { mutableStateOf(receta?.let { maquinaDe(it) } ?: "manual") }
  var tiempoTxt by remember(receta?.id) { mutableStateOf(receta?.let { if (tiempoDe(it) > 0) fmtNum(tiempoDe(it)) else "" } ?: "") }
  var energiaTxt by remember(receta?.id) { mutableStateOf(receta?.let { if (energiaDe(it) > 0) fmtNum(energiaDe(it)) else "" } ?: "") }
  val filas = remember(receta?.id) {
    mutableStateListOf<FilaIng>().apply {
      (receta?.ingredientes ?: emptyList()).forEach { add(FilaIng(it.id, fmtNum(it.cantidad))) }
      if (isEmpty()) add(FilaIng("", "1"))
    }
  }
  val opciones = remember(receta?.id, todas) {
    Datos.materiales.map { "mat_${it.codigo}" to it.nombre } +
      todas.filter { it.id != receta?.id }.map { "rec_${it.id}" to it.nombre }
  }
  var error by remember(receta?.id) { mutableStateOf<String?>(null) }
  var cantidadTxt by remember(receta?.id) { mutableStateOf(receta?.let { if (it.cantidad != 1.0) fmtNum(it.cantidad) else "" } ?: "") }
  val subs = remember(receta?.id) {
    mutableStateListOf<FilaIng>().apply {
      (receta?.subproductos ?: emptyList()).forEach { add(FilaIng(it.id, fmtNum(it.cantidad))) }
    }
  }
  val reqs = remember(receta?.id) {
    mutableStateListOf<FilaReq>().apply {
      (receta?.requisitos ?: emptyList()).forEach { add(FilaReq(it.tipo, it.detalle)) }
    }
  }
  val opcionesMats = remember { Datos.materiales.map { "mat_${it.codigo}" to it.nombre } }
  val opcionesReqs = remember { TIPOS_REQUISITO.map { it to it.replaceFirstChar(Char::titlecase) } }

  Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(if (receta == null) "Nueva receta" else "Editar receta", style = MaterialTheme.typography.titleMedium)
    OutlinedTextField(nombre, { nombre = it }, label = { Text("Nombre de la receta") }, modifier = Modifier.fillMaxWidth())
    Text("Ingredientes")
    filas.forEachIndexed { i, f ->
      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        ComboBox(opciones, f.id, { filas[i] = FilaIng(it, f.cant); }, "Seleccionar", Modifier.weight(2f))
        OutlinedTextField(f.cant, { filas[i] = FilaIng(f.id, it) }, label = { Text("Cant.") }, modifier = Modifier.weight(1f))
        TextButton(onClick = { filas.removeAt(i) }) { Text("X") }
      }
    }
    Button(onClick = { filas.add(FilaIng("", "1")) }) { Text("+ Agregar ingrediente") }
    ComboBox(Datos.maquinas.map { it.id to it.nombre }, maquina, { maquina = it }, "Plano — máquina (opcional)")
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      OutlinedTextField(tiempoTxt, { tiempoTxt = it }, label = { Text("Tiempo (seg)") }, modifier = Modifier.weight(1f))
      OutlinedTextField(energiaTxt, { energiaTxt = it }, label = { Text("Energía") }, modifier = Modifier.weight(1f))
    }
    OutlinedTextField(cantidadTxt, { cantidadTxt = it }, label = { Text("Unidades por proceso (def. 1)") }, modifier = Modifier.fillMaxWidth())
    Text("Subproductos (solo materiales base)")
    subs.forEachIndexed { i, f ->
      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        ComboBox(opcionesMats, f.id, { subs[i] = FilaIng(it, f.cant) }, "Material", Modifier.weight(2f))
        OutlinedTextField(f.cant, { subs[i] = FilaIng(f.id, it) }, label = { Text("Cant.") }, modifier = Modifier.weight(1f))
        TextButton(onClick = { subs.removeAt(i) }) { Text("X") }
      }
    }
    Button(onClick = { subs.add(FilaIng("", "1")) }) { Text("+ Agregar subproducto") }
    Text("Requisitos (estructuras, biomas, dimensiones)")
    reqs.forEachIndexed { i, f ->
      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        ComboBox(opcionesReqs, f.tipo, { reqs[i] = FilaReq(it, f.detalle) }, "Tipo", Modifier.weight(1f))
        OutlinedTextField(f.detalle, { reqs[i] = FilaReq(f.tipo, it) }, label = { Text("Detalle") }, modifier = Modifier.weight(2f))
        TextButton(onClick = { reqs.removeAt(i) }) { Text("X") }
      }
    }
    Button(onClick = { reqs.add(FilaReq("estructura", "")) }) { Text("+ Agregar requisito") }
    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      OutlinedButton(onClick = onCancelar, modifier = Modifier.weight(1f)) { Text("Cancelar") }
      Button(
        onClick = {
          error = null
          if (nombre.trim().isBlank()) { error = "Ingresa un nombre para la receta"; return@Button }
          val ings = filas.map { f ->
            val c = f.cant.toDoubleOrNull()
            if (f.id.isBlank() || c == null || c <= 0) null else IngredienteRef(f.id, c)
          }
          if (ings.any { it == null }) { error = "Completa todos los ingredientes correctamente"; return@Button }
          val cantidad = if (cantidadTxt.isBlank()) 1.0 else cantidadTxt.toDoubleOrNull() ?: -1.0
          if (cantidad <= 0) { error = "Unidades por proceso inválidas"; return@Button }
          val subps = subs.map { f ->
            val c = f.cant.toDoubleOrNull()
            if (!f.id.startsWith("mat_") || c == null || c <= 0) null else IngredienteRef(f.id, c)
          }
          if (subps.any { it == null }) { error = "Subproducto inválido (solo materiales base)"; return@Button }
          val reqsl = reqs.map { f ->
            if (f.tipo !in TIPOS_REQUISITO || f.detalle.isBlank()) null else Requisito(f.tipo, f.detalle.trim())
          }
          if (reqsl.any { it == null }) { error = "Requisito inválido"; return@Button }
          onGuardar(
            Receta(
              receta?.id ?: "r_${System.currentTimeMillis()}_${(0..99999).random()}",
              nombre.trim(), ings.filterNotNull(), maquina,
              tiempoTxt.toDoubleOrNull()?.takeIf { it >= 0 } ?: 0.0,
              energiaTxt.toDoubleOrNull()?.takeIf { it >= 0 } ?: 0.0,
              cantidad, subps.filterNotNull(), reqsl.filterNotNull()
            )
          )
        },
        modifier = Modifier.weight(1f)
      ) { Text("Guardar") }
    }
  }
}

@Composable
private fun DetalleReceta(vm: RecetasViewModel, prefs: PrefsStore, r: Receta, todas: List<Receta>, onVolver: () -> Unit, onVer: (String) -> Unit) {
  val res = remember(r, todas) { resolverReceta(r, 1.0, todas, Datos.materiales, Datos.maquinas) }
  val cuello = cuelloDeBotella(res)
  val alts = remember(r, todas) { rutasAlternativas(r.nombre, todas, Datos.materiales, Datos.maquinas).filter { it.receta.id != r.id } }
  val maqId = maquinaDe(r)
  val factible = maqId == "manual" || maqId in prefs.maquinasPropias()
  var base by remember(r.id) { mutableStateOf<List<TotalMaterial>?>(null) }
  var expandidos by remember(r.id) { mutableStateOf(setOf(r.id)) }
  LaunchedEffect(r.id) { base = vm.explosion(r.id) }
  Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(r.nombre, style = MaterialTheme.typography.titleLarge)
    TextButton(onClick = onVolver) { Text("← Volver") }
    Text(
      if (factible) "✓ Puedes hacerlo con tu taller" else "✗ Te falta: ${nombreMaquina(maqId, Datos.maquinas)}",
      color = if (factible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    )
    Text("Ingredientes directos:", style = MaterialTheme.typography.titleMedium)
    r.ingredientes.forEach { ing ->
      val n = nombreIngrediente(ing.id, Datos.materiales, todas)
      Text("${fmtNum(ing.cantidad)} x ${n.nombre}")
    }
    Text("Plano:", style = MaterialTheme.typography.titleMedium)
    FilaDato("Máquina", nombreMaquina(maqId, Datos.maquinas))
    if (tiempoDe(r) > 0) FilaDato("Tiempo (1 unidad)", "${fmtNum(tiempoDe(r))}s")
    if (energiaDe(r) > 0) FilaDato("Energía (1 unidad)", fmtNum(energiaDe(r)))
    if (r.cantidad != 1.0) FilaDato("Unidades por proceso", fmtNum(r.cantidad))
    if (r.requisitos.isNotEmpty()) {
      Text("Requisitos:", style = MaterialTheme.typography.titleMedium)
      r.requisitos.forEach { FilaDato(it.tipo.replaceFirstChar(Char::titlecase), it.detalle) }
    }
    Text("Materiales base (para 1 unidad):", style = MaterialTheme.typography.titleMedium)
    val b = base
    if (b == null) Text("Calculando…")
    else if (b.isEmpty()) Text("Solo contiene otras recetas")
    else b.forEach { FilaDato(it.nombre, fmtNum(it.cantidad)) }
    if (res.subproductos.isNotEmpty()) {
      Text("Subproductos netos:", style = MaterialTheme.typography.titleMedium)
      res.subproductos.forEach { FilaDato(it.nombre, fmtNum(it.cantidad)) }
    }
    Text("Proceso total (para 1 unidad):", style = MaterialTheme.typography.titleMedium)
    FilaDato("Tiempo total", "${fmtNum(res.tiempo)}s")
    FilaDato("Energía total", fmtNum(res.energia))
    FilaDato("Cuello de botella", cuello?.let { "${it.nombre} (${fmtNum(it.tiempo)}s)" } ?: "Todo manual")
    if (alts.isNotEmpty()) {
      Text("Otras formas de hacerlo:", style = MaterialTheme.typography.titleMedium)
      alts.forEach { a ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text("${a.receta.nombre} (${fmtNum(a.resolucion.tiempo)}s)")
          TextButton(onClick = { onVer(a.receta.id) }) { Text("Ver") }
        }
      }
    }
    Text("Árbol de receta:", style = MaterialTheme.typography.titleMedium)
    ArbolReceta(r, 1.0, todas, expandidos = expandidos, onToggle = { id ->
      expandidos = if (id in expandidos) expandidos - id else expandidos + id
    })
  }
}

@Composable
private fun ArbolReceta(
  r: Receta,
  mult: Double,
  todas: List<Receta>,
  nivel: Int = 0,
  visitados: Set<String> = emptySet(),
  expandidos: Set<String>,
  onToggle: (String) -> Unit
) {
  if (r.id in visitados) {
    Text("Ciclo detectado", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(start = (nivel * 16).dp))
    return
  }
  val v = visitados + r.id
  val tag = if (maquinaDe(r) != "manual") " [${nombreMaquina(maquinaDe(r), Datos.maquinas)}]" else ""
  val tieneHijos = r.ingredientes.any { it.id.startsWith("rec_") && todas.any { t -> t.id == it.id.removePrefix("rec_") } }
  val abierto = !tieneHijos || r.id in expandidos
  Text(
    (if (tieneHijos) (if (abierto) "[-] " else "[+] ") else "") + "${r.nombre} x ${fmtNum(mult)}$tag",
    modifier = Modifier.padding(start = (nivel * 16).dp).clickable(enabled = tieneHijos) { onToggle(r.id) }
  )
  if (!abierto) return
  r.ingredientes.forEach { ing ->
    val cant = ing.cantidad * mult
    if (ing.id.startsWith("rec_")) {
      todas.firstOrNull { it.id == ing.id.removePrefix("rec_") }?.let {
        ArbolReceta(it, cant, todas, nivel + 1, v, expandidos, onToggle)
      }
    } else {
      val n = nombreIngrediente(ing.id, Datos.materiales, todas)
      Text("${fmtNum(cant)} x ${n.nombre}", modifier = Modifier.padding(start = ((nivel + 1) * 16).dp))
    }
  }
}
