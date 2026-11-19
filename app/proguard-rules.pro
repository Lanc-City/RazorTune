# Add project specific ProGuard rules here.
# Preserve attributes needed by kotlinx.serialization, reflection, and coroutines
-keepattributes *Annotation*, InnerClasses, Signature, SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile

# kotlinx.serialization (classes, objects, enums, sealed hierarchies, and companions)
-dontnote kotlinx.serialization.AnnotationsKt
-keep,includedescriptorclasses class com.example.**$$serializer { *; }
-keepclassmembers class com.example.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers class com.example.**$Companion {
    *** serializer(...);
}
-keep @kotlinx.serialization.Serializable class com.example.** { *; }

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class kotlinx.serialization.** { *; }
-dontwarn kotlinx.serialization.**
-dontwarn kotlinx.coroutines.**

# InnerTubeX & Ktor serialization models
-keep class com.metrolist.innertubex.** { *; }
-dontwarn com.metrolist.innertubex.**

-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# Media3 / ExoPlayer, MediaLibraryService & Audio Visualizer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**
-keep class android.media.audiofx.Visualizer** { *; }

# App data models, playback services & navigation routes
-keep class com.example.model.** { *; }
-keep class com.example.data.PlayEvent* { *; }
-keep class com.example.*Route* { *; }
-keep class * extends android.app.Service { *; }
-keep class * extends android.content.BroadcastReceiver { *; }

# Keep Coil & OkHttp optional warnings clean
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn org.slf4j.**
-dontwarn java.lang.invoke.**
