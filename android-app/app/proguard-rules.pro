# PhotoCheck Proguard / R8 rules

# Keep Kotlin reflect and coroutines metadata
-keepattributes *Annotation*,InnerClasses,EnclosingMethod,Signature

# Keep data models
-keep class com.fingo.photocheck.model.** { *; }
-keep class com.fingo.photocheck.update.** { *; }

# Compose rules
-keep class androidx.compose.material.icons.** { *; }

# Coil image loader
-keep class coil.** { *; }
-dontwarn coil.**
