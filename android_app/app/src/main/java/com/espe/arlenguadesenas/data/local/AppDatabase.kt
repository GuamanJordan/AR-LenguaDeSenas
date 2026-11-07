package com.espe.arlenguadesenas.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.espe.arlenguadesenas.data.local.dao.CategoryDao
import com.espe.arlenguadesenas.data.local.dao.GlossDao
import com.espe.arlenguadesenas.data.local.dao.HistoryDao
import com.espe.arlenguadesenas.data.local.entity.CategoryEntity
import com.espe.arlenguadesenas.data.local.entity.GlossEntity
import com.espe.arlenguadesenas.data.local.entity.HistoryEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [CategoryEntity::class, GlossEntity::class, HistoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun categoryDao(): CategoryDao
    abstract fun glossDao(): GlossDao
    abstract fun historyDao(): HistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "lsec_database.db"
                )
                .addCallback(DatabasePrepopulateCallback())
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabasePrepopulateCallback : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            CoroutineScope(Dispatchers.IO).launch {
                INSTANCE?.let { database ->
                    seedInitialData(database)
                }
            }
        }

        private suspend fun seedInitialData(db: AppDatabase) {
            val categories = listOf(
                CategoryEntity(1, "DACTILOLOGIA", "Alfabeto Dactilológico", "Letras manuales", "ic_alphabet", 1),
                CategoryEntity(2, "NUMEROS", "Números", "Dígitos 0 al 9", "ic_numbers", 2),
                CategoryEntity(3, "SALUDOS", "Saludos y Cortesía", "Interacciones diarias", "ic_greetings", 3),
                CategoryEntity(4, "EMERGENCIAS", "Necesidades y Ayuda", "Peticiones críticas", "ic_emergency", 4),
                CategoryEntity(5, "UNIVERSIDAD", "Ámbito Académico ESPE", "Clases y aulas", "ic_school", 5)
            )
            db.categoryDao().insertCategories(categories)

            val initialGlosses = listOf(
                GlossEntity(1, 1, "LETRA_A", "A", "A", "Puño cerrado pulgar lateral", "Puño", "Pectoral", "Estático", "Al frente", "Neutra", false, false, "sign_dact_a", 1000),
                GlossEntity(2, 1, "LETRA_B", "B", "B", "Mano 4 dedos arriba pulgar dentro", "4 dedos", "Pectoral", "Estático", "Al frente", "Neutra", false, false, "sign_dact_b", 1000),
                GlossEntity(3, 1, "LETRA_C", "C", "C", "Mano en forma de arco", "Semicírculo", "Pectoral", "Estático", "Lateral", "Neutra", false, false, "sign_dact_c", 1000),
                GlossEntity(4, 2, "NUM_1", "Uno", "1", "Dedo índice arriba", "Índice", "Pectoral", "Estático", "Al cuerpo", "Neutra", false, false, "sign_num_1", 1000),
                GlossEntity(5, 2, "NUM_2", "Dos", "2", "Índice y medio en V", "V", "Pectoral", "Estático", "Al cuerpo", "Neutra", false, false, "sign_num_2", 1000),
                GlossEntity(6, 3, "HOLA", "Hola", "HOLA", "Saludo junto a la sien", "Abierta", "Sien", "Horizontal", "Al frente", "Sonrisa", false, true, "sign_saludo_hola", 1500),
                GlossEntity(7, 3, "GRACIAS", "Gracias", "GRACIAS", "Dedos en barbilla se proyectan", "Plana", "Mentón", "Diagonal", "Al frente", "Amable", false, true, "sign_cortesia_gracias", 1600),
                GlossEntity(8, 3, "POR_FAVOR", "Por favor", "POR-FAVOR", "Palmas en el pecho", "Palma", "Pecho", "Circular", "Al cuerpo", "Cordial", true, true, "sign_cortesia_por_favor", 1800),
                GlossEntity(9, 4, "AYUDA", "Ayuda", "AYUDAR", "Puño sobre palma impulsa", "Compuesta", "Pectoral", "Frente", "Al receptor", "Urgente", true, true, "sign_emergencia_ayuda", 1800),
                GlossEntity(10, 4, "BANIO", "Baño", "BANO", "Oscilación en espacio neutro", "T", "Neutro", "Oscilatorio", "Al frente", "Neutra", false, true, "sign_necesidad_banio", 1500),
                GlossEntity(11, 5, "PROFESOR", "Profesor", "PROFESOR", "Movimiento desde la frente", "P", "Frente", "Adelante", "Encontradas", "Atenta", true, true, "sign_univ_profesor", 2000),
                GlossEntity(12, 5, "ENTENDER", "Entiendo", "ENTENDER", "Índice toca sien", "Índice", "Sien", "Contacto", "Hacia adentro", "Asentimiento", false, true, "sign_univ_entender", 1400),
                GlossEntity(13, 5, "NO_ENTIENDO", "No entiendo", "ENTENDER NO", "Índice en sien y negación", "Abanico", "Sien", "Rechazo", "Afuera", "Ceño fruncido", false, true, "sign_univ_no_entiendo", 1800),
                GlossEntity(14, 5, "SI", "Sí", "SI", "Puño cabecea", "Puño", "Neutro", "Vertical", "Al frente", "Afirmativa", false, true, "sign_comun_si", 1200),
                GlossEntity(15, 5, "NO", "No", "NO", "Cierre rápido de índice, medio y pulgar", "Pinza", "Neutro", "Cierre", "Al frente", "Negativa", false, true, "sign_comun_no", 1200)
            )
            db.glossDao().insertGlosses(initialGlosses)
        }
    }
}
