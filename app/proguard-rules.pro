# When release minify is enabled, keep Crashlytics line numbers (Firebase docs).
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception

# Firebase Remote Config + Crashlytics (SDK consumer rules also merge; explicit for Play launch).
-keep class com.google.firebase.remoteconfig.** { *; }
-keep class com.google.firebase.crashlytics.** { *; }
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
