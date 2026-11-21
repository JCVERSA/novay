# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Preserve line numbers for crash diagnostics
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Media3 UI & Player reflection / programmatic access
-keep class androidx.media3.ui.PlayerView { *; }
-keep class androidx.media3.ui.AspectRatioFrameLayout { *; }
-keep class androidx.media3.exoplayer.** { *; }
-dontwarn androidx.media3.**

# OkHttp / Okio internal platform helpers
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# libVLC native JNI callbacks & video layout
-keep class org.videolan.libvlc.** { *; }
-dontwarn org.videolan.libvlc.**

# Lottie JSON animation models
-keep class com.airbnb.lottie.** { *; }
-dontwarn com.airbnb.lottie.**

# Haze blur library
-keep class dev.chrisbanes.haze.** { *; }
-dontwarn dev.chrisbanes.haze.**
