# Proguard rules
-keep class com.emergencyroutefinder.model.** { *; }
-keep class com.emergencyroutefinder.network.** { *; }
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions
