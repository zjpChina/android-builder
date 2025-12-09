# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep the MainActivity
-keep class com.example.uriloader.MainActivity { *; }

# Keep JSON parsing classes
-keep class org.json.** { *; }

# WebView
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep line numbers for debugging
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
