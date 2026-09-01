# PhotoCheck Proguard / R8 rules

-keepattributes *Annotation*,InnerClasses,EnclosingMethod,Signature

# Keep data models and update logic
-keep class com.fingo.photocheck.model.** { *; }
-keep class com.fingo.photocheck.update.** { *; }

# Coil image & video loader
-keep class coil.** { *; }
-dontwarn coil.**

# Media3 ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Biometric
-keep class androidx.biometric.** { *; }
-dontwarn androidx.biometric.**

# Coroutines
-keepclassmembers class kotlinx.coroutines.** { *; }
