# JNI bridge names and reflected native callbacks must remain stable.
-keep class org.nehuatl.llamacpp.** { *; }
-keepclassmembers class * {
    native <methods>;
}
-keepattributes Signature,InnerClasses,EnclosingMethod
