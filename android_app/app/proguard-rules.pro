# TensorFlow Lite
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
