package com.recetas.app

import android.content.Context
import android.content.Intent
import android.os.Environment
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

// OTA sin backend: version.json estatico servido por Render.
// Cambiar si el sitio usa otro dominio.
const val OTA_VERSION_URL = "https://recipes.onrender.com/version.json"

@Serializable
data class VersionRemota(val versionCode: Int, val versionName: String, val apkUrl: String)

fun hayActualizacion(localCode: Int, remota: VersionRemota?): Boolean =
  remota != null && remota.versionCode > localCode

fun parseVersionRemota(texto: String): VersionRemota {
  try {
    return Json { ignoreUnknownKeys = true }.decodeFromString<VersionRemota>(texto)
  } catch (e: SerializationException) {
    throw IllegalArgumentException("version.json inválido: ${e.message}")
  }
}

suspend fun chequearActualizacion(url: String = OTA_VERSION_URL): VersionRemota? = withContext(Dispatchers.IO) {
  var conn: HttpURLConnection? = null
  try {
    conn = (URL(url).openConnection() as HttpURLConnection).apply {
      connectTimeout = 10000
      readTimeout = 10000
    }
    if (conn.responseCode != 200) return@withContext null
    parseVersionRemota(conn.inputStream.bufferedReader().readText())
  } catch (e: Exception) {
    null // sin red o servidor caido: silencioso, la app sigue offline
  } finally {
    conn?.disconnect()
  }
}

// Descarga por stream (sin cargar el APK en memoria) e instala via FileProvider.
// Devuelve null si OK o mensaje de error. urlBase inyectable para pruebas.
suspend fun descargarEInstalar(
  ctx: Context,
  apkUrl: String,
  urlBase: String = OTA_VERSION_URL,
  onProgreso: (Float) -> Unit = {}
): String? = withContext(Dispatchers.IO) {
  var conn: HttpURLConnection? = null
  try {
    val root = urlBase.substringBeforeLast("/")
    val full = if (apkUrl.startsWith("/")) root + apkUrl else apkUrl
    conn = (URL(full).openConnection() as HttpURLConnection).apply {
      connectTimeout = 15000
      readTimeout = 30000
    }
    if (conn.responseCode != 200) return@withContext "Descarga falló (HTTP ${conn.responseCode})"
    val total = conn.contentLengthLong
    val dir = ctx.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: return@withContext "Sin almacenamiento"
    dir.listFiles()?.forEach { if (it.name.endsWith(".apk")) it.delete() }
    val apk = File(dir, "recetas_update.apk")
    conn.inputStream.use { inp ->
      apk.outputStream().use { out ->
        val buf = ByteArray(64 * 1024)
        var bajados = 0L
        while (true) {
          val n = inp.read(buf)
          if (n < 0) break
          out.write(buf, 0, n)
          bajados += n
          if (total > 0) onProgreso(bajados.toFloat() / total)
        }
      }
    }
    val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", apk)
    val intent = Intent(Intent.ACTION_VIEW).apply {
      setDataAndType(uri, "application/vnd.android.package-archive")
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    ctx.startActivity(intent)
    null
  } catch (e: Exception) {
    "Error OTA: ${e.message}"
  } finally {
    conn?.disconnect()
  }
}
