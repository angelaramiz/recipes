package com.recetas.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import com.recetas.app.ui.App

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    val repo = RoomRepositorio(DbProvider.db(this).recetas())
    val prefs = PrefsStore(this)
    val vname = packageManager.getPackageInfo(packageName, 0).versionName ?: "?"
    if (prefs.versionGuardada() != vname) prefs.marcarVersion(vname)
    setContent { MaterialTheme { App(repo, prefs) } }
  }
}
