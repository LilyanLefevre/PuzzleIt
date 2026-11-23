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

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile# R8 rules for the release build. Keep line numbers for readable stack traces.
-keepattributes SourceFile,LineNumberTable,*Annotation*,Signature,InnerClasses,EnclosingMethod

# Native code calls these classes by name (JNI).
-keep class ai.onnxruntime.** { *; }
-keep class org.opencv.** { *; }

# Typed navigation routes and the models of the server: kotlinx.serialization looks serializers up by name.
-keepclassmembers class **$$serializer { *; }
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    static ** Companion;
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.lilyan_lefevre.puzzleit.**$$serializer { *; }
