########################## Retrofit 2.9.0 ProGuard Rules: ########################
# Retrofit
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions
-keepattributes *Annotation*

########################### OkHttp (used by Retrofit)############################
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**

# Okio (used by OkHttp)
-keep class okio.** { *; }
-dontwarn okio.**

# Gson (if you're using it with Retrofit)
-keep class com.google.gson.** { *; }
-keep class com.google.gson.examples.android.model.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keep class sun.misc.Unsafe { *; }

######################### ExoPlayer 2.19.1 ProGuard Rules: ##############################
# ExoPlayer
-keep class com.google.android.exoplayer2.** { *; }
-keep interface com.google.android.exoplayer2.** { *; }
-dontwarn com.google.android.exoplayer2.**

# For extension libraries you might be using (add as needed)
-keep class com.google.android.exoplayer2.ext.** { *; }
-keep interface com.google.android.exoplayer2.ext.** { *; }
-dontwarn com.google.android.exoplayer2.ext.**

# If using DASH, HLS, or SmoothStreaming
-keep class org.xmlpull.v1.** { *; }
-keep class com.google.android.exoplayer2.source.** { *; }

# If using UI components
-keep class com.google.android.exoplayer2.ui.** { *; }

# If using MediaSession
-keep class com.google.android.exoplayer2.ext.mediasession.** { *; }


-keep class com.cinecraze.free.** { *; }
