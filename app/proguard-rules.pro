# Retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions

# OkHttp
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**

# CameraX
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# Gson
-keep class com.google.gson.** { *; }
-keep class com.google.gson.examples.android.model.** { *; }

# Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public enum com.bumptech.glide.load.resource.bitmap.ImageHeaderParser$** {
    **[] $VALUES;
    public *;
}

# Keep model classes
-keep class com.example.aiphoto.model.** { *; }
