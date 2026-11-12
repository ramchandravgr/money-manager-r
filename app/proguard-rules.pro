# When release minify is enabled, keep Crashlytics line numbers (Firebase docs).
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception

# fastexcel-reader (StAX) — not on Android bootclasspath; used at runtime via bundled parsers.
-dontwarn javax.xml.stream.XMLInputFactory
-dontwarn javax.xml.stream.XMLReporter
-dontwarn javax.xml.stream.XMLResolver
-dontwarn javax.xml.stream.XMLStreamException
-dontwarn javax.xml.stream.XMLStreamReader
-dontwarn javax.xml.stream.util.XMLEventAllocator

# Firebase Remote Config + Crashlytics (SDK consumer rules also merge; explicit for Play launch).
-keep class com.google.firebase.remoteconfig.** { *; }
-keep class com.google.firebase.crashlytics.** { *; }
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
