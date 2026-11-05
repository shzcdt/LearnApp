# kotlinx.serialization: keep serializers of DTOs
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class ai.learnharness.mobile.** { kotlinx.serialization.KSerializer serializer(...); }
