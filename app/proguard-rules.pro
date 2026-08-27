# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# --- API models -------------------------------------------------------------
# Moshi (reflective KotlinJsonAdapterFactory) + Retrofit need the DTO classes,
# their fields, and their Kotlin metadata intact.
-keep class com.neerly.mobile.data.dto.** { *; }
-keep class kotlin.Metadata { *; }
-keepclassmembers class kotlin.Metadata { *; }

# --- Retrofit ---------------------------------------------------------------
# Keep generic signatures + annotations so Retrofit can reflect on suspend
# functions and response types.
-keepattributes Signature, InnerClasses, EnclosingMethod, RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations, AnnotationDefault
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-dontwarn kotlinx.coroutines.**

# With R8 full mode, Retrofit's Response/Call generic parameters are kept via:
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# --- Moshi ------------------------------------------------------------------
-dontwarn com.squareup.moshi.**
-keepclassmembers class com.squareup.moshi.internal.Util { *; }

# --- OkHttp -----------------------------------------------------------------
-dontwarn okhttp3.**
-dontwarn okio.**

# --- Razorpay ---------------------------------------------------------------
# Razorpay ships consumer rules in its AAR; these silence residual warnings.
-dontwarn com.razorpay.**
-keep class com.razorpay.** { *; }
-optimizations !method/inlining/*
