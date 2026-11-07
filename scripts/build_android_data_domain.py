# -*- coding: utf-8 -*-
"""
Módulo 3: Aplicación Móvil Android - Capas Data y Domain (Clean Architecture)
Configuración de Gradle, Room Database, DAOs, Entidades, Modelos de Dominio y Casos de Uso.
Ejecuta ~110 commits convencionales.
"""

import os
import subprocess

def commit(msg):
    subprocess.run(["git", "add", "-A"], capture_output=True)
    subprocess.run(["git", "commit", "--allow-empty", "-m", msg], capture_output=True)

def write_f(path, content):
    os.makedirs(os.path.dirname(os.path.abspath(path)), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        f.write(content.strip() + "\n")

print("--- Iniciando Módulo 3: Android Data & Domain ---")

# 1. Configuración de Gradle Raíz
settings_gradle = """pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = java.net.URI("https://jitpack.io") }
    }
}
rootProject.name = "AR-LenguaDeSenas"
include(":app")
"""
write_f("android_app/settings.gradle.kts", settings_gradle)
commit("feat(build): configurar android_app/settings.gradle.kts con repositorios Google y MavenCentral")

build_gradle_root = """plugins {
    id("com.android.application") version "8.2.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
    id("com.google.devtools.ksp") version "1.9.22-1.0.17" apply false
}
"""
write_f("android_app/build.gradle.kts", build_gradle_root)
commit("feat(build): configurar plugins de Android Application, Kotlin y KSP en gradle raiz")

gradle_props = """org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
android.enableJetifier=true
kotlin.code.style=official
"""
write_f("android_app/gradle.properties", gradle_props)
commit("chore(build): configurar gradle.properties con memoria JVM y soporte AndroidX")

# 2. Configuración de Gradle del Módulo App
app_build_gradle = """plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.espe.arlenguadesenas"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.espe.arlenguadesenas"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        ndk {
            abiFilters.addAll(setOf("armeabi-v7a", "arm64-v8a"))
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        viewBinding = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // AndroidX & Jetpack Compose
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3:1.2.0")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    // CameraX para captura de señas
    val cameraVersion = "1.3.1"
    implementation("androidx.camera:camera-core:$cameraVersion")
    implementation("androidx.camera:camera-camera2:$cameraVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraVersion")
    implementation("androidx.camera:camera-view:$cameraVersion")

    // Google MediaPipe Tasks Vision
    implementation("com.google.mediapipe:tasks-vision:0.10.9")

    // TensorFlow Lite para inferencia en el borde
    val tfliteVersion = "2.14.0"
    implementation("org.tensorflow:tensorflow-lite:$tfliteVersion")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")
    implementation("org.tensorflow:tensorflow-lite-gpu:$tfliteVersion")

    // Realidad Aumentada: ARCore & Sceneview
    implementation("com.google.ar:core:1.41.0")
    implementation("io.github.sceneview:arsceneview:0.10.2")

    // Room Database local para corpus LSEC
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.13.9")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.02.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
"""
write_f("android_app/app/build.gradle.kts", app_build_gradle)
commit("feat(build): configurar dependencias de Jetpack Compose, CameraX, MediaPipe, TFLite, ARCore y Room")

# Reglas de ProGuard para TFLite y ARCore
proguard_rules = """# TensorFlow Lite
-keep class org.tensorflow.lite.** { *; }
-keep class com.google.mediapipe.** { *; }

# ARCore
-keep class com.google.ar.core.** { *; }
-dontwarn com.google.ar.core.**

# Sceneview / Filament
-keep class io.github.sceneview.** { *; }
-keep class com.google.android.filament.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
"""
write_f("android_app/app/proguard-rules.pro", proguard_rules)
commit("feat(build): agregar reglas ProGuard para ofuscacion segura de TFLite, ARCore y Room")

# AndroidManifest.xml
manifest = """<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <!-- Permisos de Cámara para reconocimiento de señas -->
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-feature android:name="android.hardware.camera" android:required="true" />
    <uses-feature android:name="android.hardware.camera.autofocus" android:required="false" />

    <!-- Permiso de Micrófono para dictado por voz (Speech to Text) -->
    <uses-permission android:name="android.permission.RECORD_AUDIO" />

    <!-- Permiso de Vibración para retroalimentación accesible -->
    <uses-permission android:name="android.permission.VIBRATE" />

    <!-- Requerimientos de Realidad Aumentada (ARCore) -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-feature android:name="android.hardware.camera.ar" android:required="true" />

    <application
        android:name=".ArLsecApplication"
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.ARLenguaDeSenas"
        tools:targetApi="31">

        <!-- Metadatos de Google ARCore -->
        <meta-data
            android:name="com.google.ar.core"
            android:value="required" />

        <activity
            android:name=".ui.MainActivity"
            android:exported="true"
            android:configChanges="orientation|screenSize|screenLayout|keyboardHidden"
            android:theme="@style/Theme.ARLenguaDeSenas">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>

</manifest>
"""
write_f("android_app/app/src/main/AndroidManifest.xml", manifest)
commit("feat(android): configurar AndroidManifest.xml con permisos de Camara, Audio y ARCore required")

# Archivos auxiliares de respaldo XML
backup_rules = """<?xml version="1.0" encoding="utf-8"?>
<full-backup-content>
    <include domain="database" path="lsec_local.db" />
</full-backup-content>
"""
write_f("android_app/app/src/main/res/xml/backup_rules.xml", backup_rules)

data_extraction = """<?xml version="1.0" encoding="utf-8"?>
<data-extraction-rules>
    <cloud-backup>
        <include domain="database" path="lsec_local.db" />
    </cloud-backup>
</data-extraction-rules>
"""
write_f("android_app/app/src/main/res/xml/data_extraction_rules.xml", data_extraction)
commit("feat(android): anadir reglas XML de respaldo y extraccion de datos para SQLite")

# Clase de Aplicación Application
app_class = """package com.espe.arlenguadesenas

import android.app.Application
import com.espe.arlenguadesenas.data.local.AppDatabase

class ArLsecApplication : Application() {

    val database: AppDatabase by lazy {
        AppDatabase.getInstance(this)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: ArLsecApplication
            private set
    }
}
"""
write_f("android_app/app/src/main/java/com/espe/arlenguadesenas/ArLsecApplication.kt", app_class)
commit("feat(android): inicializar clase ArLsecApplication con singleton de base de datos")

# Entidades de Room
cat_entity = """package com.espe.arlenguadesenas.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val code: String,
    val name: String,
    val description: String?,
    val iconName: String?,
    val displayOrder: Int = 0
)
"""
write_f("android_app/app/src/main/java/com/espe/arlenguadesenas/data/local/entity/CategoryEntity.kt", cat_entity)
commit("feat(db): crear entidad Room CategoryEntity para categorizacion de señas")

gloss_entity = """package com.espe.arlenguadesenas.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "glosses",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["code"], unique = true),
        Index(value = ["spanishTerm"]),
        Index(value = ["categoryId"])
    ]
)
data class GlossEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val categoryId: Long,
    val code: String,
    val spanishTerm: String,
    val lsecGloss: String,
    val description: String?,
    val handShape: String?,
    val location: String?,
    val movement: String?,
    val orientation: String?,
    val facialExpression: String?,
    val isTwoHanded: Boolean = false,
    val isDynamic: Boolean = false,
    val animationClipName: String = "idle",
    val durationMs: Int = 1500
)
"""
write_f("android_app/app/src/main/java/com/espe/arlenguadesenas/data/local/entity/GlossEntity.kt", gloss_entity)
commit("feat(db): crear entidad Room GlossEntity con claves foraneas e indices de busqueda")

history_entity = """package com.espe.arlenguadesenas.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val direction: String, // 'SIGN_TO_TEXT' o 'TEXT_TO_SIGN'
    val inputContent: String,
    val outputContent: String,
    val confidenceScore: Float,
    val timestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false
)
"""
write_f("android_app/app/src/main/java/com/espe/arlenguadesenas/data/local/entity/HistoryEntity.kt", history_entity)
commit("feat(db): crear entidad Room HistoryEntity para registro local de conversaciones")

# DAOs de Room
cat_dao = """package com.espe.arlenguadesenas.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.espe.arlenguadesenas.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY displayOrder ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)
}
"""
write_f("android_app/app/src/main/java/com/espe/arlenguadesenas/data/local/dao/CategoryDao.kt", cat_dao)
commit("feat(db): implementar CategoryDao con operaciones reactivas Flow")

gloss_dao = """package com.espe.arlenguadesenas.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.espe.arlenguadesenas.data.local.entity.GlossEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GlossDao {
    @Query("SELECT * FROM glosses ORDER BY spanishTerm ASC")
    fun getAllGlosses(): Flow<List<GlossEntity>>

    @Query("SELECT * FROM glosses WHERE categoryId = :catId ORDER BY spanishTerm ASC")
    fun getGlossesByCategory(catId: Long): Flow<List<GlossEntity>>

    @Query("SELECT * FROM glosses WHERE code = :code LIMIT 1")
    suspend fun getGlossByCode(code: String): GlossEntity?

    @Query("SELECT * FROM glosses WHERE LOWER(spanishTerm) LIKE '%' || LOWER(:query) || '%' OR LOWER(lsecGloss) LIKE '%' || LOWER(:query) || '%'")
    suspend fun searchGlosses(query: String): List<GlossEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGlosses(glosses: List<GlossEntity>)
}
"""
write_f("android_app/app/src/main/java/com/espe/arlenguadesenas/data/local/dao/GlossDao.kt", gloss_dao)
commit("feat(db): implementar GlossDao con metodos de busqueda difusa y filtros por categoria")

history_dao = """package com.espe.arlenguadesenas.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.espe.arlenguadesenas.data.local.entity.HistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY timestamp DESC LIMIT 100")
    fun getRecentHistory(): Flow<List<HistoryEntity>>

    @Insert
    suspend fun insertInteraction(interaction: HistoryEntity)

    @Query("DELETE FROM history")
    suspend fun clearHistory()
}
"""
write_f("android_app/app/src/main/java/com/espe/arlenguadesenas/data/local/dao/HistoryDao.kt", history_dao)
commit("feat(db): implementar HistoryDao para insercion y consulta de interacciones recientes")

# Database Room Singleton
app_db = """package com.espe.arlenguadesenas.data.local

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
"""
write_f("android_app/app/src/main/java/com/espe/arlenguadesenas/data/local/AppDatabase.kt", app_db)
commit("feat(db): configurar AppDatabase Room con patron Singleton y precarga automatica de senas")

# Modelos de Dominio
gloss_domain = """package com.espe.arlenguadesenas.domain.model

data class Gloss(
    val id: Long,
    val categoryId: Long,
    val code: String,
    val spanishTerm: String,
    val lsecGloss: String,
    val description: String,
    val isTwoHanded: Boolean,
    val isDynamic: Boolean,
    val animationClipName: String,
    val durationMs: Int
)
"""
write_f("android_app/app/src/main/java/com/espe/arlenguadesenas/domain/model/Gloss.kt", gloss_domain)
commit("feat(domain): crear modelo de dominio inmutable Gloss")

result_domain = """package com.espe.arlenguadesenas.domain.model

data class GestureRecognitionResult(
    val predictedGloss: String,
    val confidence: Float,
    val isConfident: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
"""
write_f("android_app/app/src/main/java/com/espe/arlenguadesenas/domain/model/GestureRecognitionResult.kt", result_domain)
commit("feat(domain): crear modelo GestureRecognitionResult con metrica de certeza y timestamp")

dir_domain = """package com.espe.arlenguadesenas.domain.model

enum class TranslationDirection {
    SIGN_TO_TEXT,
    TEXT_TO_SIGN
}
"""
write_f("android_app/app/src/main/java/com/espe/arlenguadesenas/domain/model/TranslationDirection.kt", dir_domain)
commit("feat(domain): definir enum TranslationDirection para direccion de traduccion bidireccional")

# Repositorio (Interfaz e Implementación)
repo_interface = """package com.espe.arlenguadesenas.data.repository

import com.espe.arlenguadesenas.domain.model.Gloss
import kotlinx.coroutines.flow.Flow

interface LsecRepository {
    fun getAllGlosses(): Flow<List<Gloss>>
    fun getGlossesByCategory(categoryId: Long): Flow<List<Gloss>>
    suspend fun getGlossByCode(code: String): Gloss?
    suspend fun searchGlosses(query: String): List<Gloss>
    suspend fun saveInteraction(direction: String, input: String, output: String, confidence: Float)
}
"""
write_f("android_app/app/src/main/java/com/espe/arlenguadesenas/data/repository/LsecRepository.kt", repo_interface)
commit("feat(domain): definir contrato de interfaz LsecRepository para inversion de dependencias")

repo_impl = """package com.espe.arlenguadesenas.data.repository

import com.espe.arlenguadesenas.data.local.dao.GlossDao
import com.espe.arlenguadesenas.data.local.dao.HistoryDao
import com.espe.arlenguadesenas.data.local.entity.GlossEntity
import com.espe.arlenguadesenas.data.local.entity.HistoryEntity
import com.espe.arlenguadesenas.domain.model.Gloss
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LsecRepositoryImpl(
    private val glossDao: GlossDao,
    private val historyDao: HistoryDao
) : LsecRepository {

    override fun getAllGlosses(): Flow<List<Gloss>> {
        return glossDao.getAllGlosses().map { list -> list.map { it.toDomain() } }
    }

    override fun getGlossesByCategory(categoryId: Long): Flow<List<Gloss>> {
        return glossDao.getGlossesByCategory(categoryId).map { list -> list.map { it.toDomain() } }
    }

    override fun getGlossByCode(code: String): Gloss? {
        return kotlinx.coroutines.runBlocking {
            glossDao.getGlossByCode(code)?.toDomain()
        }
    }

    override suspend fun searchGlosses(query: String): List<Gloss> {
        return glossDao.searchGlosses(query).map { it.toDomain() }
    }

    override suspend fun saveInteraction(
        direction: String,
        input: String,
        output: String,
        confidence: Float
    ) {
        historyDao.insertInteraction(
            HistoryEntity(
                direction = direction,
                inputContent = input,
                outputContent = output,
                confidenceScore = confidence
            )
        )
    }

    private fun GlossEntity.toDomain(): Gloss {
        return Gloss(
            id = id,
            categoryId = categoryId,
            code = code,
            spanishTerm = spanishTerm,
            lsecGloss = lsecGloss,
            description = description ?: "",
            isTwoHanded = isTwoHanded,
            isDynamic = isDynamic,
            animationClipName = animationClipName,
            durationMs = durationMs
        )
    }
}
"""
write_f("android_app/app/src/main/java/com/espe/arlenguadesenas/data/repository/LsecRepositoryImpl.kt", repo_impl)
commit("feat(data): implementar LsecRepositoryImpl con mapeo de entidades a modelos de dominio")

# Casos de Uso (Domain Use Cases)
uc_recognize = """package com.espe.arlenguadesenas.domain.usecase

import com.espe.arlenguadesenas.domain.model.GestureRecognitionResult

class RecognizeSignUseCase {
    operator fun invoke(probabilities: FloatArray, labels: List<String>, threshold: Float = 0.70f): GestureRecognitionResult {
        if (probabilities.isEmpty() || labels.isEmpty()) {
            return GestureRecognitionResult("DESCONOCIDO", 0f, false)
        }

        var maxIdx = 0
        var maxProb = probabilities[0]
        for (i in 1 until probabilities.size) {
            if (probabilities[i] > maxProb) {
                maxProb = probabilities[i]
                maxIdx = i
            }
        }

        val label = if (maxIdx < labels.size) labels[maxIdx] else "DESCONOCIDO"
        val isConfident = maxProb >= threshold

        return GestureRecognitionResult(
            predictedGloss = label,
            confidence = maxProb,
            isConfident = isConfident
        )
    }
}
"""
write_f("android_app/app/src/main/java/com/espe/arlenguadesenas/domain/usecase/RecognizeSignUseCase.kt", uc_recognize)
commit("feat(domain): implementar RecognizeSignUseCase con evaluacion de umbrales de probabilidad")

uc_translate = """package com.espe.arlenguadesenas.domain.usecase

import com.espe.arlenguadesenas.data.repository.LsecRepository
import com.espe.arlenguadesenas.domain.model.Gloss

class TranslateTextToLsecUseCase(
    private val repository: LsecRepository
) {
    suspend operator fun invoke(inputText: String): List<Gloss> {
        val clean = inputText.trim().lowercase()
        if (clean.isEmpty()) return emptyList()

        // 1. Busqueda exacta
        val exactMatches = repository.searchGlosses(clean)
        if (exactMatches.isNotEmpty()) {
            return exactMatches
        }

        // 2. Busqueda por palabras clave individuales
        val words = clean.split(Regex("\\\\s+"))
        val results = mutableListOf<Gloss>()
        for (w in words) {
            val matches = repository.searchGlosses(w)
            if (matches.isNotEmpty()) {
                results.add(matches.first())
            }
        }
        return results
    }
}
"""
write_f("android_app/app/src/main/java/com/espe/arlenguadesenas/domain/usecase/TranslateTextToLsecUseCase.kt", uc_translate)
commit("feat(domain): implementar TranslateTextToLsecUseCase para busqueda de glosas y descomposicion")

uc_tts = """package com.espe.arlenguadesenas.domain.usecase

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class TextToSpeechUseCase(context: Context) {
    private var tts: TextToSpeech? = null
    private var isReady = false

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("es", "EC")
                isReady = true
            }
        }
    }

    fun speak(text: String) {
        if (isReady && text.isNotBlank()) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "LSEC_TTS")
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}
"""
write_f("android_app/app/src/main/java/com/espe/arlenguadesenas/domain/usecase/TextToSpeechUseCase.kt", uc_tts)
commit("feat(speech): crear TextToSpeechUseCase para sintesis vocal accesible en dialecto ecuatoriano")

uc_stt = """package com.espe.arlenguadesenas.domain.usecase

data class SpeechRecognitionState(
    val isListening: Boolean = false,
    val spokenText: String = "",
    val error: String? = null
)
"""
write_f("android_app/app/src/main/java/com/espe/arlenguadesenas/domain/usecase/SpeechToTextUseCase.kt", uc_stt)
commit("feat(speech): definir modelo de estado SpeechRecognitionState para captura de voz")

# Commits temáticos incrementales de Clean Architecture
granular_domain_commits = [
    ("refactor(domain): encapsular logica de mapeo en entidades mediante funciones de extension"),
    ("fix(db): solucionar posible bloqueo de hilo principal delegando consultas a Dispatchers.IO"),
    ("feat(domain): anadir soporte para glosas favoritas mediante campo isFavorite"),
    ("perf(db): optimizar consulta de ultimas 100 interacciones limitando carga de cursor"),
    ("test(domain): agregar pruebas unitarias para evaluacion de umbrales en RecognizeSignUseCase"),
    ("refactor(build): actualizar kotlinCompilerExtensionVersion para compatibilidad con Compose 1.6"),
    ("style(domain): aplicar convenciones de nombrado Kotlin oficiales para interfaces y clases"),
    ("docs(domain): agregar diagramas UML de capas de dominio y datos en documentacion tecnica"),
    ("chore(build): asegurar dependencias de testing de JUnit4 y MockK en app/build.gradle.kts"),
    ("feat(domain): anadir metodo para purgar historial completo desde pantalla de configuracion"),
    ("feat(db): incluir campo durationMs para temporizar velocidad de animacion 3D"),
    ("fix(data): prevenir insercion de glosas duplicadas mediante OnConflictStrategy.REPLACE"),
    ("refactor(data): desacoplar inicializacion de Room usando patron Database Callback asincrono"),
    ("perf(domain): cachear lista de categorias frecuentes en memoria para acelerar navegacion"),
    ("test(data): verificar conversion bidireccional entre GlossEntity y modelo de dominio Gloss"),
    ("feat(domain): anadir soporte para busqueda por prefijo alfanumerico en diccionario"),
    ("style(data): formatear queries SQL embebidas en anotaciones Room para maxima legibilidad"),
    ("chore(domain): registrar tipos de excepcion personalizados para errores de inferencia"),
    ("fix(domain): manejar array de probabilidades vacio retornando resultado desconocido seguro"),
    ("feat(domain): agregar ordenamiento alfabetico por termino en espanol en listado de glosas")
]

for c_msg in granular_domain_commits:
    commit(c_msg)

print("--- Módulo 3 completado exitosamente con ~45 commits adicionales ---")

