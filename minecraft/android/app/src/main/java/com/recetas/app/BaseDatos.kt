package com.recetas.app

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context

@Entity(tableName = "recetas")
data class RecetaEntity(
  @PrimaryKey val id: String,
  val nombre: String,
  val maquina: String = "manual",
  val tiempo: Double = 0.0,
  val energia: Double = 0.0,
  val cantidad: Double = 1.0
)

@Entity(
  tableName = "ingredientes",
  foreignKeys = [ForeignKey(
    entity = RecetaEntity::class,
    parentColumns = ["id"],
    childColumns = ["padreId"],
    onDelete = ForeignKey.CASCADE
  )],
  indices = [Index("padreId")]
)
data class IngredienteEntity(
  @PrimaryKey(autoGenerate = true) val rowId: Long = 0,
  val padreId: String,
  val refId: String, // mat_<codigo> | rec_<id>, igual que la web
  val cantidad: Double
)

@Entity(
  tableName = "subproductos",
  foreignKeys = [ForeignKey(
    entity = RecetaEntity::class,
    parentColumns = ["id"],
    childColumns = ["padreId"],
    onDelete = ForeignKey.CASCADE
  )],
  indices = [Index("padreId")]
)
data class SubproductoEntity(
  @PrimaryKey(autoGenerate = true) val rowId: Long = 0,
  val padreId: String,
  val refId: String, // solo mat_*, se valida al importar/editar
  val cantidad: Double
)

@Entity(
  tableName = "requisitos",
  foreignKeys = [ForeignKey(
    entity = RecetaEntity::class,
    parentColumns = ["id"],
    childColumns = ["padreId"],
    onDelete = ForeignKey.CASCADE
  )],
  indices = [Index("padreId")]
)
data class RequisitoEntity(
  @PrimaryKey(autoGenerate = true) val rowId: Long = 0,
  val padreId: String,
  val tipo: String, // estructura | bioma | dimension
  val detalle: String
)

@Dao
interface RecetaDao {
  @Query("SELECT * FROM recetas ORDER BY nombre")
  suspend fun todas(): List<RecetaEntity>

  @Query("SELECT * FROM ingredientes WHERE padreId = :id")
  suspend fun ingredientesDe(id: String): List<IngredienteEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun guardarReceta(r: RecetaEntity)

  @Insert
  suspend fun guardarIngredientes(l: List<IngredienteEntity>)

  @Query("DELETE FROM ingredientes WHERE padreId = :id")
  suspend fun borrarIngredientesDe(id: String)

  @Query("DELETE FROM recetas WHERE id = :id")
  suspend fun borrarReceta(id: String)

  @Query("DELETE FROM ingredientes")
  suspend fun borrarTodosIngredientes()

  @Query("DELETE FROM recetas")
  suspend fun borrarTodasRecetas()

  @Query("SELECT * FROM subproductos WHERE padreId = :id")
  suspend fun subproductosDe(id: String): List<SubproductoEntity>

  @Query("SELECT * FROM requisitos WHERE padreId = :id")
  suspend fun requisitosDe(id: String): List<RequisitoEntity>

  @Insert
  suspend fun guardarSubproductos(l: List<SubproductoEntity>)

  @Insert
  suspend fun guardarRequisitos(l: List<RequisitoEntity>)

  @Query("DELETE FROM subproductos WHERE padreId = :id")
  suspend fun borrarSubproductosDe(id: String)

  @Query("DELETE FROM requisitos WHERE padreId = :id")
  suspend fun borrarRequisitosDe(id: String)

  @Query("DELETE FROM subproductos")
  suspend fun borrarTodosSubproductos()

  @Query("DELETE FROM requisitos")
  suspend fun borrarTodosRequisitos()

  @Transaction
  suspend fun guardarCompleta(
    r: RecetaEntity,
    ings: List<IngredienteEntity>,
    subs: List<SubproductoEntity>,
    reqs: List<RequisitoEntity>
  ) {
    guardarReceta(r)
    borrarIngredientesDe(r.id)
    borrarSubproductosDe(r.id)
    borrarRequisitosDe(r.id)
    guardarIngredientes(ings)
    guardarSubproductos(subs)
    guardarRequisitos(reqs)
  }

  @Query(QUERY_EXPLOSION)
  suspend fun explosionBase(recetaId: String): List<TotalBase>

  @Transaction
  suspend fun reemplazarTodoE(
    recetas: List<RecetaEntity>,
    ings: List<IngredienteEntity>,
    subs: List<SubproductoEntity>,
    reqs: List<RequisitoEntity>
  ) {
    borrarTodosIngredientes()
    borrarTodosSubproductos()
    borrarTodosRequisitos()
    borrarTodasRecetas()
    recetas.forEach { guardarReceta(it) }
    guardarIngredientes(ings)
    guardarSubproductos(subs)
    guardarRequisitos(reqs)
  }
}

val MIGRACION_1_2 = object : Migration(1, 2) {
  override fun migrate(db: SupportSQLiteDatabase) {
    db.execSQL("ALTER TABLE recetas ADD COLUMN cantidad REAL NOT NULL DEFAULT 1.0")
    db.execSQL("CREATE TABLE IF NOT EXISTS `subproductos` (`rowId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `padreId` TEXT NOT NULL, `refId` TEXT NOT NULL, `cantidad` REAL NOT NULL, FOREIGN KEY(`padreId`) REFERENCES `recetas`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
    db.execSQL("CREATE INDEX IF NOT EXISTS `index_subproductos_padreId` ON `subproductos` (`padreId`)")
    db.execSQL("CREATE TABLE IF NOT EXISTS `requisitos` (`rowId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `padreId` TEXT NOT NULL, `tipo` TEXT NOT NULL, `detalle` TEXT NOT NULL, FOREIGN KEY(`padreId`) REFERENCES `recetas`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
    db.execSQL("CREATE INDEX IF NOT EXISTS `index_requisitos_padreId` ON `requisitos` (`padreId`)")
  }
}

@Database(
  entities = [RecetaEntity::class, IngredienteEntity::class, SubproductoEntity::class, RequisitoEntity::class],
  version = 2
)
abstract class AppDatabase : RoomDatabase() {
  abstract fun recetas(): RecetaDao
}

interface RepositorioRecetas {
  suspend fun todas(): List<Receta>
  suspend fun guardar(r: Receta)
  suspend fun borrar(id: String)
  suspend fun reemplazarTodo(rs: List<Receta>)
  suspend fun baseNecesaria(id: String): List<TotalMaterial>
}

class RoomRepositorio(private val dao: RecetaDao) : RepositorioRecetas {
  override suspend fun todas(): List<Receta> {
    return dao.todas().map { e ->
      val ings = dao.ingredientesDe(e.id).map { IngredienteRef(it.refId, it.cantidad) }
      val subs = dao.subproductosDe(e.id).map { IngredienteRef(it.refId, it.cantidad) }
      val reqs = dao.requisitosDe(e.id).map { Requisito(it.tipo, it.detalle) }
      Receta(e.id, e.nombre, ings, e.maquina, e.tiempo, e.energia, e.cantidad, subs, reqs)
    }
  }

  override suspend fun guardar(r: Receta) {
    dao.guardarCompleta(
      RecetaEntity(r.id, r.nombre, r.maquina, r.tiempo, r.energia, r.cantidad),
      r.ingredientes.map { IngredienteEntity(padreId = r.id, refId = it.id, cantidad = it.cantidad) },
      r.subproductos.map { SubproductoEntity(padreId = r.id, refId = it.id, cantidad = it.cantidad) },
      r.requisitos.map { RequisitoEntity(padreId = r.id, tipo = it.tipo, detalle = it.detalle) }
    )
  }

  override suspend fun borrar(id: String) {
    dao.borrarIngredientesDe(id)
    dao.borrarSubproductosDe(id)
    dao.borrarRequisitosDe(id)
    dao.borrarReceta(id)
  }

  override suspend fun reemplazarTodo(rs: List<Receta>) {
    dao.reemplazarTodoE(
      rs.map { RecetaEntity(it.id, it.nombre, it.maquina, it.tiempo, it.energia, it.cantidad) },
      rs.flatMap { r -> r.ingredientes.map { IngredienteEntity(padreId = r.id, refId = it.id, cantidad = it.cantidad) } },
      rs.flatMap { r -> r.subproductos.map { SubproductoEntity(padreId = r.id, refId = it.id, cantidad = it.cantidad) } },
      rs.flatMap { r -> r.requisitos.map { RequisitoEntity(padreId = r.id, tipo = it.tipo, detalle = it.detalle) } }
    )
  }

  override suspend fun baseNecesaria(id: String): List<TotalMaterial> {
    return dao.explosionBase(id).map { b ->
      val nombre = Datos.materiales.firstOrNull { it.codigo == b.codigo }?.nombre ?: b.codigo
      TotalMaterial(b.codigo, nombre, b.total)
    }
  }
}

object DbProvider {
  @Volatile private var db: AppDatabase? = null

  fun db(ctx: Context): AppDatabase {
    return db ?: synchronized(this) {
      db ?: Room.databaseBuilder(ctx.applicationContext, AppDatabase::class.java, "recetas.db")
        .addMigrations(MIGRACION_1_2).build().also { db = it }
    }
  }
}
