package com.recetas.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.recetas.app.Receta
import com.recetas.app.RepositorioRecetas
import com.recetas.app.TIPOS_REQUISITO
import com.recetas.app.detectarCiclo
import com.recetas.app.parsearRecetas
import com.recetas.app.restaurarRespaldo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RecetasViewModel(private val repo: RepositorioRecetas) : ViewModel() {
  private val _recetas = MutableStateFlow<List<Receta>>(emptyList())
  val recetas: StateFlow<List<Receta>> = _recetas.asStateFlow()

  fun cargar() {
    viewModelScope.launch { _recetas.value = repo.todas() }
  }

  /** Guarda si pasa validación. Devuelve mensaje de error o null si OK. */
  suspend fun guardar(r: Receta): String? {
    if (r.nombre.isBlank()) return "Ingresa un nombre para la receta"
    if (r.ingredientes.isEmpty()) return "Agrega al menos un ingrediente"
    if (r.ingredientes.any { it.id.isBlank() || it.cantidad <= 0 }) return "Completa todos los ingredientes correctamente"
    if (r.cantidad <= 0) return "Unidades por proceso inválidas"
    if (r.subproductos.any { !it.id.startsWith("mat_") || it.cantidad <= 0 }) return "Subproducto inválido (solo materiales base)"
    if (r.requisitos.any { it.tipo !in TIPOS_REQUISITO || it.detalle.isBlank() }) return "Requisito inválido"
    val todas = repo.todas()
    val editando = todas.any { it.id == r.id }
    if (detectarCiclo(r.ingredientes, if (editando) r.id else null, todas)) {
      return "Se detectó un ciclo. Una receta no puede depender de sí misma."
    }
    repo.guardar(r)
    _recetas.value = repo.todas()
    return null
  }

  suspend fun importar(texto: String): Int {
    val lista = parsearRecetas(texto)
    lista.forEach { repo.guardar(it) }
    _recetas.value = repo.todas()
    return lista.size
  }

  suspend fun restaurarCsv(texto: String): Int {
    val n = restaurarRespaldo(repo, texto)
    _recetas.value = repo.todas()
    return n
  }

  suspend fun explosion(id: String) = repo.baseNecesaria(id)

  fun borrar(id: String) {
    viewModelScope.launch {
      repo.borrar(id)
      _recetas.value = repo.todas()
    }
  }
}

class VMFactory(private val repo: RepositorioRecetas) : ViewModelProvider.Factory {
  @Suppress("UNCHECKED_CAST")
  override fun <T : ViewModel> create(modelClass: Class<T>): T = RecetasViewModel(repo) as T
}
