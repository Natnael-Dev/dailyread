# DailyRead Production ProGuard / R8 Configuration

# ==============================================================================
# Security & Privacy: Strip all Android Logging in Release Builds
# (Enforces AGENTS.md rule: No logging of file paths, titles or personal data)
# ==============================================================================
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
    public static int println(...);
}

# ==============================================================================
# Readium Kotlin Toolkit 3.1.1 (DEC-021)
# Keep Readium streamer, navigator, shared models, and PDFium adapters
# ==============================================================================
-keep class org.readium.r2.** { *; }
-keep interface org.readium.r2.** { *; }
-keep class org.readium.kotlin.** { *; }
-keep interface org.readium.kotlin.** { *; }
-keep class org.readium.adapter.** { *; }
-keep interface org.readium.adapter.** { *; }

# Keep native methods for PDFium C++ / JNI bridge
-keepclasseswithmembernames class * {
    native <methods>;
}

# Kotlinx Serialization & Annotations required by Readium Locator / Manifest models
-keepattributes *Annotation*, InnerClasses, EnclosingMethod, Signature
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
    @kotlinx.serialization.SerialName <fields>;
}
-dontnote kotlinx.serialization.**
-dontwarn org.readium.**

# ==============================================================================
# Room Persistence Library (Version 2.7.2)
# ==============================================================================
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keepclassmembers class * {
    @androidx.room.TypeConverter *;
}
-keep class io.github.natnael_dev.dailyread.data.db.** { *; }

# ==============================================================================
# Glance & AppWidgets (Phase 6 / Widget Architecture)
# ==============================================================================
-keep class androidx.glance.** { *; }
-keep interface androidx.glance.** { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidget { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver { *; }
-dontwarn androidx.glance.**

# ==============================================================================
# Kotlin Coroutines
# ==============================================================================
-dontwarn kotlinx.coroutines.**
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Preserve line numbers for stack trace demangling
-keepattributes SourceFile,LineNumberTable
