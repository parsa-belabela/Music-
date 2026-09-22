# Room
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <init>();
}
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.Entity class * { *; }
-dontwarn androidx.room.paging.**

# Moshi / Serialization / Data Models
-keepclassmembers class com.example.data.model.** { *; }
-keep class com.example.data.model.** { *; }
-keepclassmembers class * implements java.io.Serializable { *; }

# Coil Image Loading
-keep class coil.** { *; }
-dontwarn coil.**

# Kotlin Coroutines & Lifecycle
-keepnames class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**
-keep class androidx.lifecycle.** { *; }

# Keep line numbers for stack traces
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*
