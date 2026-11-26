# Keep Kotlinx Serialization generated classes
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.mahavtaar.panchanga.**$$serializer { *; }
-keepclassmembers class com.mahavtaar.panchanga.** {
    *** Companion;
}
-keepclasseswithmembers class com.mahavtaar.panchanga.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep cosinekitty astronomy library
-keep class io.github.cosinekitty.astronomy.** { *; }

# Keep Room entities & DAOs
-keep class com.mahavtaar.panchanga.database.** { *; }

# Keep Jetpack Glance ActionCallback zero-argument constructors
-keep class * implements androidx.glance.appwidget.action.ActionCallback {
    public <init>();
}
